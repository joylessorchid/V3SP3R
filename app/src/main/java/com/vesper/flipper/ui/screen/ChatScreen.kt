package com.vesper.flipper.ui.screen

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vesper.flipper.data.database.ChatSessionSummary
import com.vesper.flipper.domain.model.*
import androidx.compose.foundation.border
import com.vesper.flipper.ui.components.ApprovalDialog
import com.vesper.flipper.ui.components.DiffViewer
import com.vesper.flipper.ui.components.GlassIconButton
import com.vesper.flipper.ui.components.LocalOpenDrawer
import com.vesper.flipper.ui.components.MarkdownText
import com.vesper.flipper.ui.theme.*
import com.vesper.flipper.ui.viewmodel.ChatViewModel
import com.vesper.flipper.voice.SpeechState
import com.vesper.flipper.voice.TtsState
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToAudit: () -> Unit
) {
    val conversationState by viewModel.conversationState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val pendingImages by viewModel.pendingImages.collectAsState()
    val isProcessingImage by viewModel.isProcessingImage.collectAsState()
    val imageError by viewModel.imageError.collectAsState()
    val glassesBridgeState by viewModel.glassesBridgeState.collectAsState()
    val glassesMuted by viewModel.glassesMuted.collectAsState()
    val isGlassesConnected = glassesBridgeState is com.vesper.flipper.glasses.BridgeState.Connected
    val sessionHistory by viewModel.sessionHistory.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }

    // Show snackbar when image processing fails
    LaunchedEffect(imageError) {
        imageError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImageError()
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImage(it) }
    }

    // Multi-photo picker launcher
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris: List<Uri> ->
        uris.forEach { viewModel.addImage(it) }
    }

    // Camera capture: create a temp file URI for the camera to write to.
    // Use rememberSaveable so the URI survives activity recreation (Android
    // may kill the activity while the camera app is in the foreground).
    val context = LocalContext.current
    var cameraImageUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraImageUriString?.let { Uri.parse(it) }
        if (success && uri != null) {
            viewModel.addImage(uri)
        } else if (uri != null) {
            // Some camera apps return false even when the photo was saved.
            // Check if the file actually exists before giving up.
            val exists = try {
                context.contentResolver.openInputStream(uri)?.use { it.available() > 0 } == true
            } catch (_: Exception) { false }
            if (exists) viewModel.addImage(uri)
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val uri = cameraImageUriString?.let { Uri.parse(it) }
        if (success && uri != null) {
            viewModel.addImage(uri)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraCaptureUri(context)
            cameraImageUriString = uri.toString()
            cameraLauncher.launch(uri)
        }
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraCaptureUri(context, video = true)
            cameraImageUriString = uri.toString()
            videoLauncher.launch(uri)
        }
    }

    // TTS state
    val ttsState by viewModel.ttsState.collectAsState()

    // Voice input state
    val voiceState by viewModel.voiceState.collectAsState()
    val voicePartialResult by viewModel.voicePartialResult.collectAsState()
    val voiceError by viewModel.voiceError.collectAsState()
    var hasMicPermission by remember { mutableStateOf(false) }
    // Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startVoiceInput()
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(conversationState.messages.size) {
        if (conversationState.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversationState.messages.size - 1)
        }
    }

    // Show approval dialog if pending
    conversationState.pendingApproval?.let { approval ->
        ApprovalDialog(
            approval = approval,
            onApprove = { viewModel.approveAction() },
            onReject = { viewModel.rejectAction() }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = RiskHigh
                )
            },
            title = { Text("Clear Chat") },
            text = { Text("Delete this conversation? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.clearConversation()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RiskHigh)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Chat history bottom sheet
    if (showHistoryDrawer) {
        ChatHistorySheet(
            sessions = sessionHistory,
            currentSessionId = conversationState.sessionId,
            onSelectSession = { sessionId ->
                showHistoryDrawer = false
                viewModel.loadSession(sessionId)
            },
            onDeleteSession = { sessionId ->
                viewModel.deleteSession(sessionId)
            },
            onNewThread = {
                showHistoryDrawer = false
                viewModel.startNewSession()
            },
            onDismiss = { showHistoryDrawer = false }
        )
    }

    Scaffold(
        // Themed explicitly. The default snackbar is Material's inverseSurface, which
        // in a dark scheme is a near-white slab — it landed on this screen as a bright
        // rectangle over the composer, the one element loud enough to look like a
        // system alert rather than a message from the app.
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = VesperSurface,
                    contentColor = TextPrimary,
                    actionColor = VesperAccent,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        topBar = {
            // No title bar. A chat screen's content IS the identity, and a bar
            // labelled "History" spent a full row of a phone screen saying nothing.
            // Floating controls instead, over the backdrop.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val openDrawer = LocalOpenDrawer.current
                GlassIconButton(
                    icon = Icons.Default.Menu,
                    contentDescription = "Menu",
                    onClick = openDrawer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(
                        icon = Icons.Default.History,
                        contentDescription = "Chat history",
                        onClick = { showHistoryDrawer = true }
                    )
                    // Clearing the thread is destructive and unrecoverable, so it is
                    // dimmed to unavailable when there is nothing to clear rather than
                    // sitting live next to the buttons you actually press.
                    GlassIconButton(
                        icon = Icons.Default.DeleteSweep,
                        contentDescription = "Clear chat",
                        tint = if (conversationState.messages.isNotEmpty()) TextSecondary
                        else TextTertiary.copy(alpha = 0.4f),
                        onClick = {
                            if (conversationState.messages.isNotEmpty()) {
                                showDeleteConfirmation = true
                            }
                        }
                    )
                    GlassIconButton(
                        icon = Icons.Default.Receipt,
                        contentDescription = "Audit log",
                        onClick = onNavigateToAudit
                    )
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "New chat",
                        active = true,
                        onClick = { viewModel.startNewSession() }
                    )
                }
            }
        },
        bottomBar = {
            ChatInputBar(
                value = inputText,
                onValueChange = { viewModel.updateInput(it) },
                onSend = { viewModel.sendMessage() },
                onAttachImage = { showAttachMenu = true },
                showAttachMenu = showAttachMenu,
                onDismissAttachMenu = { showAttachMenu = false },
                onPickFromGallery = {
                    showAttachMenu = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onTakePhoto = {
                    showAttachMenu = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onRecordVideo = {
                    showAttachMenu = false
                    videoPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onVoiceInput = {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onStopVoice = { viewModel.stopVoiceInput() },
                pendingImages = pendingImages,
                onRemoveImage = { viewModel.removeImage(it) },
                isLoading = conversationState.isLoading,
                isProcessingImage = isProcessingImage,
                voiceState = voiceState,
                voicePartialResult = voicePartialResult,
                enabled = !conversationState.isLoading && conversationState.pendingApproval == null,
                isGlassesConnected = isGlassesConnected,
                isGlassesMuted = glassesMuted,
                onToggleGlassesMute = { viewModel.toggleGlassesMute() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (conversationState.messages.isEmpty()) {
                EmptyChat(onSuggestionClick = { suggestion ->
                    viewModel.updateInput(suggestion)
                    viewModel.sendMessage()
                })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val lastIndex = conversationState.messages.lastIndex
                    items(
                        items = conversationState.messages,
                        key = { it.id }
                    ) { message ->
                        val index = conversationState.messages.indexOf(message)
                        val isLastMessage = index == lastIndex
                        ChatMessageItem(
                            message = message,
                            ttsState = ttsState,
                            onSpeak = { viewModel.speakText(it) },
                            onStopSpeaking = { viewModel.stopSpeaking() },
                            showRetry = isLastMessage && !conversationState.isLoading &&
                                    conversationState.error != null,
                            onRetry = { viewModel.retryLastMessage() }
                        )
                    }

                    if (conversationState.isLoading) {
                        item {
                            LoadingIndicator(progress = conversationState.progress)
                        }
                    }
                }
            }

            // Show error via SnackbarHost with retry action
            LaunchedEffect(conversationState.error) {
                val error = conversationState.error ?: return@LaunchedEffect
                val result = snackbarHostState.showSnackbar(
                    message = error,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.retryLastMessage()
                }
            }
        }
    }
}

@Composable
private fun EmptyChat(onSuggestionClick: (String) -> Unit) {
    // Bottom-aligned, not centred. Centring in fillMaxSize put the prompts in the
    // middle of the screen with a third of the display empty between them and the
    // composer — and the composer is where the user is going next. Sitting the
    // block just above it turns that void into ordinary head-room.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            "Flipper AI",
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Ask for a signal, a payload, or what is on the SD card.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Left-aligned and full width. Centred pills of three different lengths
        // read as scattered; a flush left edge gives them a spine.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip("List files on the SD card", onClick = onSuggestionClick)
            SuggestionChip("Forge a Sub-GHz signal", onClick = onSuggestionClick)
            SuggestionChip("Create a universal remote for my TV", onClick = onSuggestionClick)
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: (String) -> Unit) {
    Surface(
        onClick = { onClick(text) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GlassFill1,
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    ttsState: TtsState = TtsState.Idle,
    onSpeak: ((String) -> Unit)? = null,
    onStopSpeaking: (() -> Unit)? = null,
    showRetry: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    val isUser = message.role == MessageRole.USER
    val isAssistant = message.role == MessageRole.ASSISTANT
    val isTool = message.role == MessageRole.TOOL
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // Only tool output keeps an avatar. The assistant's reply does not get one:
        // there are exactly two speakers here, the user's line is already visually
        // distinct, and a 32dp gutter down the left costs width on every line of
        // every answer to disambiguate something that was never ambiguous.
        if (isTool) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GlassFill2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = VesperAqua,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            // The assistant runs the full width; the user's line is capped so the
            // two are told apart by shape rather than by colour. The old code capped
            // BOTH at 300dp, which on a large screen left every answer in a narrow
            // ribbon with the rest of the display empty.
            modifier = if (isUser) Modifier.widthIn(max = 300.dp) else Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 20.dp else 14.dp,
                    topEnd = if (isUser) 6.dp else 14.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                // Transparent for the assistant: a long technical answer reads as a
                // document, not as a speech bubble.
                color = when {
                    isUser -> ChatUser
                    isTool -> ChatTool
                    else -> Color.Transparent
                }
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (isAssistant) 0.dp else 14.dp,
                        vertical = if (isAssistant) 2.dp else 11.dp
                    )
                ) {
                    // Show image attachments if present
                    if (!message.imageAttachments.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            message.imageAttachments.forEach { attachment ->
                                val attachData: Any = attachment.localUri
                                    ?: android.util.Base64.decode(attachment.base64Data, android.util.Base64.NO_WRAP)
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(attachData)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Attached image",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        if (message.content.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (message.content.isNotEmpty()) {
                        if (isAssistant) {
                            // The model answers in markdown. Drawn with a plain Text it
                            // arrived as literal syntax — "**Files:**" with the stars
                            // showing and every path wrapped in grave accents.
                            MarkdownText(text = message.content)
                        } else {
                            Text(
                                text = message.content,
                                color = if (isTool) TextSecondary else TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Show tool calls
                    message.toolCalls?.forEach { toolCall ->
                        Spacer(modifier = Modifier.height(8.dp))
                        ToolCallDisplay(toolCall)
                    }

                    // Show tool results with execution details
                    message.toolResults?.forEach { toolResult ->
                        if (message.content.isNotEmpty() || !message.toolCalls.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        ToolResultDisplay(toolResult)
                    }
                }
            }

            // Status indicator
            if (message.status == MessageStatus.AWAITING_APPROVAL) {
                Text(
                    "Awaiting approval...",
                    style = MaterialTheme.typography.bodySmall,
                    color = RiskMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Action buttons row (copy + TTS) for messages with text content
            if ((isUser || isAssistant) && message.content.isNotBlank()) {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                val showTts = isAssistant && message.toolCalls.isNullOrEmpty() && onSpeak != null
                val isSpeaking = ttsState is TtsState.Speaking
                val isLoading = ttsState is TtsState.Loading
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    // Copy button
                    IconButton(
                        onClick = {
                            clipboard.setPrimaryClip(ClipData.newPlainText("message", message.content))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(14.dp),
                            tint = if (isUser) VesperOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    // TTS button for assistant messages
                    if (showTts) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (isSpeaking) {
                                    onStopSpeaking?.invoke()
                                } else {
                                    onSpeak!!(message.content)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = VesperOrange
                                )
                            } else {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop" else "Speak",
                                    modifier = Modifier.size(16.dp),
                                    tint = VesperOrange
                                )
                            }
                        }
                    }
                }
            }

            // Retry button for failed responses
            if (showRetry && onRetry != null) {
                Surface(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = RiskHigh.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, RiskHigh.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = RiskHigh
                        )
                        Text(
                            "Retry",
                            style = MaterialTheme.typography.labelMedium,
                            color = RiskHigh
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolCallDisplay(toolCall: ToolCall) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = VesperGold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "execute_command",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = VesperGold
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator(progress: AgentProgress?) {
    val label = progress?.detail ?: when (progress?.stage) {
        AgentProgressStage.MODEL_REQUEST -> "Contacting model..."
        AgentProgressStage.TOOL_PLANNED -> "Preparing tool execution..."
        AgentProgressStage.TOOL_EXECUTING -> "Sending command to Flipper..."
        AgentProgressStage.TOOL_COMPLETED -> "Command finished. Summarizing..."
        AgentProgressStage.WAITING_APPROVAL -> "Waiting for your approval..."
        null -> "Thinking..."
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(VesperWine, VesperWineDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "V",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Serif,
                color = VesperGold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = VesperGold,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val toolResultJson = Json { ignoreUnknownKeys = true }

@Composable
private fun ToolResultDisplay(toolResult: ToolResult) {
    val parsed = remember(toolResult.content, toolResult.success) {
        parseToolResult(toolResult)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (parsed.success) {
            VesperGold.copy(alpha = 0.12f)
        } else {
            RiskHigh.copy(alpha = 0.14f)
        }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (parsed.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (parsed.success) VesperGold else RiskHigh
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = parsed.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (parsed.success) VesperGold else RiskHigh
                )
            }

            if (parsed.detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parsed.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}

private data class ParsedToolResult(
    val success: Boolean,
    val title: String,
    val detail: String
)

private fun parseToolResult(toolResult: ToolResult): ParsedToolResult {
    return runCatching {
        val root = toolResultJson.parseToJsonElement(toolResult.content).jsonObject
        val success = root["success"]?.jsonPrimitive?.booleanOrNull ?: toolResult.success
        val action = root["action"]?.jsonPrimitive?.contentOrNull
            ?.replace('_', ' ')
            ?.lowercase()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            ?: if (success) "Command Executed" else "Command Failed"
        val error = root["error"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val data = root["data"]?.jsonObject
        val message = data?.get("message")?.jsonPrimitive?.contentOrNull
        val content = data?.get("content")?.jsonPrimitive?.contentOrNull
        val detail = when {
            error.isNotBlank() -> error
            !message.isNullOrBlank() -> message
            !content.isNullOrBlank() -> content.lineSequence().firstOrNull().orEmpty()
            else -> toolResult.content.take(120)
        }
        ParsedToolResult(
            success = success,
            title = action,
            detail = detail
        )
    }.getOrElse {
        ParsedToolResult(
            success = toolResult.success,
            title = if (toolResult.success) "Command Executed" else "Command Failed",
            detail = toolResult.content.take(120)
        )
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    showAttachMenu: Boolean,
    onDismissAttachMenu: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    onVoiceInput: () -> Unit,
    onStopVoice: () -> Unit,
    pendingImages: List<ImageAttachment>,
    onRemoveImage: (String) -> Unit,
    isLoading: Boolean,
    isProcessingImage: Boolean,
    voiceState: SpeechState,
    voicePartialResult: String,
    enabled: Boolean,
    isGlassesConnected: Boolean = false,
    isGlassesMuted: Boolean = false,
    onToggleGlassesMute: () -> Unit = {}
) {
    val context = LocalContext.current
    val hasContent = value.isNotBlank() || pendingImages.isNotEmpty()
    val isListening = voiceState is SpeechState.Listening
    val isProcessingVoice = voiceState is SpeechState.Processing

    // A floating composer rather than a docked slab. The old Surface spanned the
    // full width with a tonal elevation, which drew a hard horizontal line across
    // the bottom of every screen and cut the conversation off. Letting the backdrop
    // run underneath keeps the thread feeling continuous.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Voice listening indicator
            AnimatedVisibility(
                visible = isListening || isProcessingVoice,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                VoiceInputIndicator(
                    isListening = isListening,
                    partialResult = voicePartialResult,
                    onStop = onStopVoice
                )
            }

            // Image preview row
            AnimatedVisibility(
                visible = pendingImages.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pendingImages.forEach { image ->
                        ImagePreviewChip(
                            image = image,
                            onRemove = { onRemoveImage(image.id) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(VesperSurface.copy(alpha = 0.9f))
                    .border(1.dp, GlassStroke, RoundedCornerShape(28.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment button with menu
                Box {
                    IconButton(
                        onClick = onAttachImage,
                        enabled = enabled && !isProcessingImage && !isListening
                    ) {
                        if (isProcessingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = VesperOrange,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Attach image or take photo",
                                // Neutral. Attaching is a secondary action; colouring it
                                // the same as Send made the bar read as three equal
                                // buttons instead of one primary and two helpers.
                                tint = TextSecondary
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showAttachMenu,
                        onDismissRequest = onDismissAttachMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text("Choose from Gallery") },
                            onClick = onPickFromGallery,
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Take Photo") },
                            onClick = onTakePhoto,
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Record Video") },
                            onClick = onRecordVideo,
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) }
                        )
                    }
                }

                // Mic button: glasses mute toggle when connected, voice input otherwise
                if (isGlassesConnected) {
                    IconButton(
                        onClick = onToggleGlassesMute,
                    ) {
                        Icon(
                            if (isGlassesMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isGlassesMuted) "Unmute glasses mic" else "Mute glasses mic",
                            tint = if (isGlassesMuted) MaterialTheme.colorScheme.error else VesperOrange
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (isListening) {
                                onStopVoice()
                            } else {
                                onVoiceInput()
                            }
                        },
                        enabled = enabled && !isProcessingImage
                    ) {
                        if (isProcessingVoice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = VesperOrange,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Voice input",
                                // Accent only while recording — then it is state, not
                                // decoration, and the one coloured thing on the bar is
                                // the one thing currently happening.
                                tint = if (isListening) VesperAccent else TextSecondary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = if (isListening && voicePartialResult.isNotBlank()) {
                        "$value $voicePartialResult".trim()
                    } else value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when {
                                isListening -> "Listening..."
                                pendingImages.isNotEmpty() -> "Add a message..."
                                else -> "Command your Flipper..."
                            }
                        )
                    },
                    enabled = enabled && !isListening,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (hasContent) onSend() }),
                    shape = RoundedCornerShape(24.dp),
                    // Every border removed. The field sits inside the composer pill,
                    // which already has one — an outlined field there drew a second
                    // rounded rectangle 6dp inside the first, and a box inside a box is
                    // the single thing that made this bar look unfinished.
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = VesperAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(4.dp))

                FilledIconButton(
                    onClick = onSend,
                    enabled = enabled && hasContent && !isListening,
                    // Accent only once there is something to send. A permanently
                    // coloured button next to two coloured attachment icons gave the bar
                    // three competing focal points and none of them meant anything.
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = VesperAccent,
                        contentColor = Color(0xFF04121F),
                        disabledContainerColor = GlassFill2,
                        disabledContentColor = TextTertiary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceInputIndicator(
    isListening: Boolean,
    partialResult: String,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VesperOrange.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated mic icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(VesperOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isListening) "Listening..." else "Processing...",
                    style = MaterialTheme.typography.labelMedium,
                    color = VesperOrange
                )
                if (partialResult.isNotBlank()) {
                    Text(
                        partialResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }

            IconButton(onClick = onStop) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewChip(
    image: ImageAttachment,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.size(64.dp)
    ) {
        // Use localUri if available (current session), otherwise decode base64 (restored session)
        val imageData: Any = image.localUri
            ?: android.util.Base64.decode(image.base64Data, android.util.Base64.NO_WRAP)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageData)
                .crossfade(true)
                .build(),
            contentDescription = "Attached image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // Remove button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(20.dp)
                .clickable { onRemove() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove image",
                modifier = Modifier
                    .padding(2.dp)
                    .size(16.dp),
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHistorySheet(
    sessions: List<ChatSessionSummary>,
    currentSessionId: String,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNewThread: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    // Confirm delete for a history item
    sessionToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Chat") },
            text = { Text("Delete this conversation from history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RiskHigh)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Chat History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onNewThread,
                    colors = ButtonDefaults.buttonColors(containerColor = VesperAccent)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Chat")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Text(
                    "No saved conversations yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.sessionId }) { session ->
                        val isCurrent = session.sessionId == currentSessionId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSession(session.sessionId) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) {
                                VesperAccent.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = if (isCurrent) BorderStroke(1.dp, VesperAccent) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = if (isCurrent) VesperAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateFormat.format(Date(session.lastTimestamp)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${session.messageCount} messages",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = VesperAccent
                                    ) {
                                        Text(
                                            "Active",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                if (!isCurrent) {
                                    IconButton(
                                        onClick = { sessionToDelete = session.sessionId },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Create a temporary file URI via FileProvider for camera capture.
 */
private fun createCameraCaptureUri(context: android.content.Context, video: Boolean = false): Uri {
    val cacheDir = java.io.File(context.cacheDir, "camera").also { it.mkdirs() }
    // Clean up old capture files (> 1 hour) to prevent cache bloat
    val cutoff = System.currentTimeMillis() - 3_600_000L
    cacheDir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }

    val ext = if (video) "mp4" else "jpg"
    val file = java.io.File(cacheDir, "capture_${System.currentTimeMillis()}.$ext")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
