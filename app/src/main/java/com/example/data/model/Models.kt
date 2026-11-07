package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val publicKey: String = "",
    val statusMessage: String? = "Tersedia di Halloapp",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantUids: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantEmails: Map<String, String> = emptyMap(),
    val lastMessageCiphertext: String? = null,
    val lastMessageSenderId: String? = null,
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EncryptedMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val participantUids: List<String> = emptyList(),
    val ciphertext: String = "",
    val iv: String = "",
    val encryptedKeySender: String = "",
    val encryptedKeyRecipient: String = "",
    val isEncrypted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Moment(
    @DocumentId val id: String = "",
    val momentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val backgroundStyle: String = "gradient_blue", // gradient_blue, gradient_cyan, gradient_dark, gradient_purple, gradient_sunset
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MomentComment(
    @DocumentId val id: String = "",
    val commentId: String = "",
    val momentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val commentText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
