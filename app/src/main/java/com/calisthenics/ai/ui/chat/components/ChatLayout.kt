package com.calisthenics.ai.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.R
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

/**
 * Standard reusable Chat layout wrapper across all messaging interfaces in the application.
 *
 * Ensures consistent edge-to-edge window inset handling:
 * - Scaffold uses zero contentWindowInsets so the screen extends behind system bars.
 * - TopAppBar consumes status bar insets naturally.
 * - Message list occupies remaining vertical weight (1f) and resizes naturally with adjustResize.
 * - ChatInputBar stays flush against the top edge of the Android keyboard (via WindowInsets.ime.union(WindowInsets.navigationBars)),
 *   with no duplicate imePadding or unexpected blank gaps.
 */
@Composable
fun ChatLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    containerColor: Color = Ink,
    messageContent: @Composable (PaddingValues) -> Unit,
    inputBar: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = topBar,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
        containerColor = containerColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                messageContent(PaddingValues(0.dp))
            }

            inputBar()
        }
    }
}

/**
 * Standardized chat message input bar.
 *
 * Anchors flush above the software keyboard using [WindowInsets.ime.union(WindowInsets.navigationBars)],
 * ensuring that when the keyboard is open the input bar is attached to the keyboard's top edge, and when
 * closed it sits neatly above the system navigation bar with no gap or overlapping content.
 */
@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.type_message),
    isSending: Boolean = false,
    sendIcon: ImageVector = Icons.AutoMirrored.Filled.Send,
    sendContentDescription: String = stringResource(R.string.loc_send_message),
    sendButtonBackground: Color = Accent,
    sendButtonContentColor: Color = Ink,
    containerColor: Color = com.calisthenics.ai.ui.theme.Surface,
    maxLines: Int = 4,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    bottomInsetEnabled: Boolean = true,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp
) {
    val insetsModifier = if (bottomInsetEnabled) {
        Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    } else {
        Modifier
    }

    Surface(
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .then(insetsModifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(8.dp))
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 100.dp),
                enabled = enabled && !isSending,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Elevated,
                    unfocusedContainerColor = Elevated,
                    disabledContainerColor = Elevated,
                    focusedBorderColor = Border,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText
                ),
                maxLines = maxLines,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isSending && enabled) {
                            onSend()
                        }
                    }
                )
            )

            Spacer(Modifier.width(8.dp))

            val canSend = inputText.isNotBlank() && !isSending && enabled

            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) sendButtonBackground else Elevated)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = sendButtonBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = sendIcon,
                        contentDescription = sendContentDescription,
                        tint = if (canSend) sendButtonContentColor else SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
