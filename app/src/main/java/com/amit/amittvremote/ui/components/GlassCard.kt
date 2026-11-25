package com.amit.amittvremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.amit.amittvremote.ui.theme.GlassBorder
import com.amit.amittvremote.ui.theme.GlassFill

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 22,
    contentPadding: Int = 16,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(GlassFill)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(GlassBorder, Color(0x0AFFFFFF))),
                shape = shape
            )
            .padding(contentPadding.dp)
    ) { content() }
}
