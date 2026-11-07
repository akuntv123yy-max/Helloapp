package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.crypto.CryptoManager
import com.example.data.model.Conversation
import com.example.data.model.EncryptedMessage
import com.example.data.model.Moment
import com.example.data.model.MomentComment
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.security.KeyPair
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HalloRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Halloapp.")
    }

    val currentUid: String?
        get() = auth.currentUser?.uid

    // --- User Profile ---

    fun observeCurrentUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { it.toObject(UserProfile::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(
        displayName: String,
        email: String,
        photoUrl: String?,
        publicKey: String
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "displayName" to displayName,
            "email" to email,
            "publicKey" to publicKey,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!photoUrl.isNullOrBlank()) {
            payload["photoUrl"] = photoUrl
        }
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java).filter { it.userId != uid }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- Conversations & E2EE Messages ---

    fun observeConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "conversations"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Conversation::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createOrGetConversation(
        currentUserProfile: UserProfile,
        otherUser: UserProfile
    ): String {
        val uid = requireUserId()
        val sortedUids = listOf(uid, otherUser.userId).sorted()
        val conversationId = "conv_${sortedUids[0]}_${sortedUids[1]}"
        val docRef = db.collection("conversations").document(conversationId)

        val existingDoc = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }

        if (existingDoc == null || !existingDoc.exists()) {
            val payload = mapOf(
                "conversationId" to conversationId,
                "participantUids" to sortedUids,
                "participantNames" to mapOf(
                    uid to currentUserProfile.displayName,
                    otherUser.userId to otherUser.displayName
                ),
                "participantEmails" to mapOf(
                    uid to currentUserProfile.email,
                    otherUser.userId to otherUser.email
                ),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
        return conversationId
    }

    fun observeMessages(conversationId: String): Flow<List<EncryptedMessage>> = flow {
        val uid = requireUserId()
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(EncryptedMessage::class.java).sortedBy {
                        it.createdAt?.toDate()?.time ?: Long.MAX_VALUE
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendEncryptedMessage(
        conversationId: String,
        recipient: UserProfile,
        plaintext: String,
        localKeyPair: KeyPair
    ) {
        val uid = requireUserId()
        val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val msgRef = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(messageId)

        val convRef = db.collection("conversations").document(conversationId)

        // Encrypt with E2EE
        val recipientPublicKey = CryptoManager.parsePublicKey(recipient.publicKey)
        val encryptedPayload = CryptoManager.encryptMessage(
            plaintext = plaintext,
            senderPublicKey = localKeyPair.public,
            recipientPublicKey = recipientPublicKey
        )

        val participantUids = listOf(uid, recipient.userId)

        val msgPayload = mapOf(
            "senderId" to uid,
            "recipientId" to recipient.userId,
            "participantUids" to participantUids,
            "ciphertext" to encryptedPayload.ciphertext,
            "iv" to encryptedPayload.iv,
            "encryptedKeySender" to encryptedPayload.encryptedKeySender,
            "encryptedKeyRecipient" to encryptedPayload.encryptedKeyRecipient,
            "isEncrypted" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            msgRef.set(msgPayload).await()
            convRef.update(
                mapOf(
                    "lastMessageCiphertext" to encryptedPayload.ciphertext,
                    "lastMessageSenderId" to uid,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    // --- Moments (Momen) ---

    fun observeMoments(): Flow<List<Moment>> = flow {
        requireUserId()
        val path = "moments"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Moment::class.java).sortedByDescending {
                        it.createdAt?.toDate()?.time ?: 0L
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createMoment(
        text: String,
        backgroundStyle: String,
        authorProfile: UserProfile
    ) {
        val uid = requireUserId()
        val momentId = "moment_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("moments").document(momentId)

        val payload = mutableMapOf<String, Any>(
            "momentId" to momentId,
            "authorId" to uid,
            "authorName" to authorProfile.displayName,
            "authorEmail" to authorProfile.email,
            "text" to text,
            "backgroundStyle" to backgroundStyle,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!authorProfile.photoUrl.isNullOrBlank()) {
            payload["authorPhotoUrl"] = authorProfile.photoUrl
        }

        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun likeMoment(momentId: String) {
        requireUserId()
        val docRef = db.collection("moments").document(momentId)
        try {
            docRef.update("likesCount", FieldValue.increment(1)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeComments(momentId: String): Flow<List<MomentComment>> = flow {
        requireUserId()
        val path = "moments/$momentId/comments"
        emitAll(
            db.collection("moments")
                .document(momentId)
                .collection("comments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MomentComment::class.java).sortedBy {
                        it.createdAt?.toDate()?.time ?: 0L
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addComment(
        momentId: String,
        commentText: String,
        authorProfile: UserProfile
    ) {
        val uid = requireUserId()
        val commentId = "comment_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("moments").document(momentId).collection("comments").document(commentId)
        val momentRef = db.collection("moments").document(momentId)

        val payload = mapOf(
            "commentId" to commentId,
            "momentId" to momentId,
            "authorId" to uid,
            "authorName" to authorProfile.displayName,
            "authorEmail" to authorProfile.email,
            "commentText" to commentText,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            momentRef.update("commentsCount", FieldValue.increment(1)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
