package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.HalloRepository
import com.example.ui.chats.ChatDetailScreen
import com.example.ui.chats.ChatListScreen
import com.example.ui.moments.MomentsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.HalloBluePrimary
import com.example.ui.theme.HalloCyanAccent
import java.security.KeyPair

data class ActiveChat(
    val conversationId: String,
    val recipient: UserProfile
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentUserProfile: UserProfile,
    localKeyPair: KeyPair,
    repository: HalloRepository,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeChat by remember { mutableStateOf<ActiveChat?>(null) }

    if (activeChat != null) {
        ChatDetailScreen(
            conversationId = activeChat!!.conversationId,
            currentUserProfile = currentUserProfile,
            recipientProfile = activeChat!!.recipient,
            localKeyPair = localKeyPair,
            repository = repository,
            onBack = { activeChat = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.halloapp_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Halloapp",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = HalloBluePrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    actions = {
                        Surface(
                            shape = CircleShape,
                            color = HalloCyanAccent.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "E2EE",
                                    tint = HalloBluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "E2EE Aktif",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HalloBluePrimary
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Chat else Icons.Outlined.Chat,
                                contentDescription = "Pesan"
                            )
                        },
                        label = { Text("Pesan", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HalloBluePrimary,
                            selectedTextColor = HalloBluePrimary,
                            indicatorColor = HalloCyanAccent.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_tab_chats")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "Momen"
                            )
                        },
                        label = { Text("Momen", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HalloBluePrimary,
                            selectedTextColor = HalloBluePrimary,
                            indicatorColor = HalloCyanAccent.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_tab_moments")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Profil"
                            )
                        },
                        label = { Text("Profil", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HalloBluePrimary,
                            selectedTextColor = HalloBluePrimary,
                            indicatorColor = HalloCyanAccent.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_animation"
                ) { tab ->
                    when (tab) {
                        0 -> ChatListScreen(
                            currentUserProfile = currentUserProfile,
                            localKeyPair = localKeyPair,
                            repository = repository,
                            onOpenChat = { convId, recipient ->
                                activeChat = ActiveChat(convId, recipient)
                            }
                        )
                        1 -> MomentsScreen(
                            currentUserProfile = currentUserProfile,
                            repository = repository
                        )
                        2 -> ProfileScreen(
                            currentUserProfile = currentUserProfile,
                            localKeyPair = localKeyPair,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}
