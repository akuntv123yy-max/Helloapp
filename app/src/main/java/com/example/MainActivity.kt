package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.crypto.CryptoManager
import com.example.data.model.UserProfile
import com.example.data.repository.HalloRepository
import com.example.ui.MainScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.theme.HalloBluePrimary
import com.example.ui.theme.HalloappTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HalloappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isCheckingAuth = false
                },
                onUnauthenticated = {
                    isCheckingAuth = false
                },
                scope = scope
            )
        } else {
            isCheckingAuth = false
        }
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = HalloBluePrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    } else if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val user = currentUser!!
        val uid = user.uid

        key(uid) {
            val repository = remember(uid) { HalloRepository(context) }
            val keyPair = remember(uid) { CryptoManager.getOrCreateKeyPair(context) }
            val publicKeyStr = remember(keyPair) { CryptoManager.getPublicKeyString(keyPair) }

            val profileFlow = remember(uid) { repository.observeCurrentUserProfile() }
            val profileState by profileFlow.collectAsState(initial = null)

            // Auto-register profile in Firestore on first login
            LaunchedEffect(uid, profileState) {
                if (profileState == null) {
                    val displayName = user.displayName?.ifBlank { null }
                        ?: user.email?.substringBefore("@")
                        ?: "Pengguna Halloapp"
                    val email = user.email ?: ""
                    val photoUrl = user.photoUrl?.toString()

                    repository.saveUserProfile(
                        displayName = displayName,
                        email = email,
                        photoUrl = photoUrl,
                        publicKey = publicKeyStr
                    )
                }
            }

            val effectiveProfile = profileState ?: UserProfile(
                userId = uid,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Pengguna Halloapp",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString(),
                publicKey = publicKeyStr
            )

            MainScreen(
                currentUserProfile = effectiveProfile,
                localKeyPair = keyPair,
                repository = repository,
                onSignOut = {
                    signOutUser(credentialManager, scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}
