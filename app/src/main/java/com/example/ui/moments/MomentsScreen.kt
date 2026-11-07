package com.example.ui.moments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Moment
import com.example.data.model.MomentComment
import com.example.data.model.UserProfile
import com.example.data.repository.HalloRepository
import com.example.ui.theme.HalloBluePrimary
import com.example.ui.theme.HalloCyanAccent
import com.example.ui.theme.HalloDarkBlue
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

data class GradientPreset(
    val id: String,
    val name: String,
    val colors: List<Color>
)

val gradientPresets = listOf(
    GradientPreset("gradient_blue", "Samudra", listOf(HalloDarkBlue, HalloBluePrimary, HalloCyanAccent)),
    GradientPreset("gradient_sunset", "Senja", listOf(Color(0xFFEA580C), Color(0xFFDB2777))),
    GradientPreset("gradient_cyan", "Zamrud", listOf(Color(0xFF0F766E), Color(0xFF06B6D4))),
    GradientPreset("gradient_purple", "Modern", listOf(Color(0xFF6D28D9), Color(0xFFA855F7))),
    GradientPreset("gradient_dark", "Malam", listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
)

fun getBrushForStyle(styleId: String): Brush {
    val preset = gradientPresets.firstOrNull { it.id == styleId } ?: gradientPresets[0]
    return Brush.linearGradient(preset.colors)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    currentUserProfile: UserProfile,
    repository: HalloRepository
) {
    val scope = rememberCoroutineScope()
    val moments by repository.observeMoments().collectAsState(initial = emptyList())

    var isCreateMomentOpen by remember { mutableStateOf(false) }
    var selectedMomentForComments by remember { mutableStateOf<Moment?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Info Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = HalloBluePrimary.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = HalloBluePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Momen Halloapp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Berbagi cerita pendek setiap hari & beri komentar pada teman",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (moments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = HalloCyanAccent.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = HalloBluePrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum Ada Momen Hari Ini",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Jadilah yang pertama membagikan cerita harian Anda di Halloapp!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { isCreateMomentOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HalloBluePrimary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan Momen")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(moments, key = { it.momentId }) { moment ->
                        MomentCard(
                            moment = moment,
                            onLike = {
                                scope.launch {
                                    repository.likeMoment(moment.momentId)
                                }
                            },
                            onOpenComments = {
                                selectedMomentForComments = moment
                            }
                        )
                    }
                }
            }
        }

        // FAB to share new moment
        FloatingActionButton(
            onClick = { isCreateMomentOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_moment_fab"),
            containerColor = HalloBluePrimary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Buat Momen Baru"
            )
        }
    }

    // Dialog: Create Moment
    if (isCreateMomentOpen) {
        CreateMomentDialog(
            currentUserProfile = currentUserProfile,
            onDismiss = { isCreateMomentOpen = false },
            onPostMoment = { text, style ->
                scope.launch {
                    repository.createMoment(
                        text = text,
                        backgroundStyle = style,
                        authorProfile = currentUserProfile
                    )
                    isCreateMomentOpen = false
                }
            }
        )
    }

    // Modal Bottom Sheet: Comments on Moment
    if (selectedMomentForComments != null) {
        val currentMoment = selectedMomentForComments!!
        CommentsBottomSheet(
            moment = currentMoment,
            currentUserProfile = currentUserProfile,
            repository = repository,
            onDismiss = { selectedMomentForComments = null }
        )
    }
}

@Composable
fun MomentCard(
    moment: Moment,
    onLike: () -> Unit,
    onOpenComments: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val formattedTime = remember(moment.createdAt) {
        val date = moment.createdAt?.toDate()
        if (date != null) timeFormat.format(date) else "Baru saja"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_card_${moment.momentId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = HalloBluePrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = moment.authorName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = moment.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$formattedTime • ${moment.authorEmail}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Styled Story Card Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush = getBrushForStyle(moment.backgroundStyle))
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = moment.text,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }

            // Actions Row (Likes & Comments)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike, modifier = Modifier.testTag("like_moment_button_${moment.momentId}")) {
                        Icon(
                            imageVector = if (moment.likesCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Suka",
                            tint = if (moment.likesCount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${moment.likesCount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(onClick = onOpenComments, modifier = Modifier.testTag("comment_moment_button_${moment.momentId}")) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Komentar",
                            tint = HalloBluePrimary
                        )
                    }
                    Text(
                        text = "${moment.commentsCount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onOpenComments) {
                    Text(
                        text = "Lihat Komentar",
                        color = HalloBluePrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CreateMomentDialog(
    currentUserProfile: UserProfile,
    onDismiss: () -> Unit,
    onPostMoment: (text: String, styleId: String) -> Unit
) {
    var storyText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("gradient_blue") }
    var isPosting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Bagikan Momen",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tulis cerita pendek Anda hari ini:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = storyText,
                    onValueChange = { if (it.length <= 280) storyText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("moment_text_input"),
                    placeholder = { Text("Apa kabar hari ini? Berbagi cerita...", fontSize = 14.sp) },
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${storyText.length}/280",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Pilih Tema Tampilan:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Color Presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(gradientPresets) { preset ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(preset.colors))
                                .clickable { selectedStyle = preset.id }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedStyle == preset.id) {
                                Surface(
                                    modifier = Modifier.size(14.dp),
                                    shape = CircleShape,
                                    color = Color.White
                                ) {}
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview
                Text(
                    text = "Pratinjau Momen:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush = getBrushForStyle(selectedStyle))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (storyText.isBlank()) "Cerita Anda akan tampil seperti ini..." else storyText,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (storyText.isNotBlank() && !isPosting) {
                        isPosting = true
                        onPostMoment(storyText.trim(), selectedStyle)
                    }
                },
                enabled = storyText.isNotBlank() && !isPosting,
                colors = ButtonDefaults.buttonColors(containerColor = HalloBluePrimary),
                modifier = Modifier.testTag("post_moment_submit_button")
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Bagikan")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    moment: Moment,
    currentUserProfile: UserProfile,
    repository: HalloRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val comments by repository.observeComments(moment.momentId).collectAsState(initial = emptyList())
    var commentInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = "Komentar Momen",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Momen oleh ${moment.authorName}: \"${moment.text.take(45)}...\"",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Comments List
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada komentar. Jadilah yang pertama berkomentar!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.commentId }) { comment ->
                        CommentItem(comment = comment)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add Comment Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    placeholder = { Text("Tulis komentar...", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = HalloBluePrimary,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = commentInput.isNotBlank() && !isSubmitting) {
                            val text = commentInput.trim()
                            commentInput = ""
                            isSubmitting = true
                            scope.launch {
                                try {
                                    repository.addComment(
                                        momentId = moment.momentId,
                                        commentText = text,
                                        authorProfile = currentUserProfile
                                    )
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                        .testTag("send_comment_button"),
                    shape = CircleShape,
                    color = if (commentInput.isNotBlank() && !isSubmitting) HalloBluePrimary else Color.LightGray
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim Komentar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: MomentComment) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(comment.createdAt) {
        val d = comment.createdAt?.toDate()
        if (d != null) timeFormat.format(d) else ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = HalloCyanAccent.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = comment.authorName.take(1).uppercase(),
                    color = HalloBluePrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = comment.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.commentText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
