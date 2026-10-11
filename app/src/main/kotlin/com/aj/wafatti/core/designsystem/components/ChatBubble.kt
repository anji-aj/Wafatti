package com.aj.wafatti.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aj.wafatti.core.utils.DateTimeFormatter
import com.aj.wafatti.domain.model.Message
import com.aj.wafatti.domain.model.MessageStatus
import com.aj.wafatti.ui.theme.WafattiGradientBrush

@Composable
fun ChatBubble(
    message: Message,
    isFromMe: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .then(
                    if (isFromMe) {
                        Modifier.background(WafattiGradientBrush)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    }
                )
                .padding(12.dp)
        ) {
            if (!message.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = "Attachment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    color = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateTimeFormatter.formatTime(message.timestamp),
                    color = if (isFromMe) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    fontSize = 10.sp
                )

                if (isFromMe) {
                    Spacer(modifier = Modifier.width(6.dp))

                    val statusDotColor = when (message.status) {
                        MessageStatus.SENT -> Color(0xFFA0A0A0)     // Muted Gray ⚪
                        MessageStatus.RECEIVED -> Color(0xFFEF4444) // Red 🔴
                        MessageStatus.SEEN -> Color(0xFF3B82F6)     // Blue 🔵
                    }

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                }
            }
        }
    }
}
