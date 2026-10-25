package com.example.cokeinventory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cokeinventory.R
import com.example.cokeinventory.ui.theme.*

@Composable
fun DrinkStockIcon(
    size: Dp = 60.dp,
    backgroundColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "DrinkStock Logo",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun DrinkStockHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp,
    titleColor: Color = Color.White,
    isCentered: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isCentered) Arrangement.Center else Arrangement.Start
    ) {
        DrinkStockIcon(size = iconSize)

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            horizontalAlignment = if (isCentered) Alignment.Start else Alignment.Start
        ) {
            Text(
                text = "DrinkStock",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = DrinkStockPink, fontWeight = FontWeight.Bold)) {
                        append("Inventory ")
                    }
                    withStyle(SpanStyle(color = DrinkStockOrange, fontWeight = FontWeight.Bold)) {
                        append("Manager ")
                    }
                    withStyle(SpanStyle(color = DrinkStockCyan, fontWeight = FontWeight.Bold)) {
                        append("Pro")
                    }
                },
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12131C)
@Composable
fun DrinkStockHeaderPreview() {
    Box(
        modifier = Modifier
            .background(Color(0xFF12131C))
            .padding(24.dp)
    ) {
        DrinkStockHeader(
            iconSize = 80.dp,
            titleColor = Color.White,
            isCentered = true
        )
    }
}
