package com.example.cokeinventory.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cokeinventory.ui.theme.*
import kotlinx.coroutines.launch

data class TutorialPageData(
    val stepNumber: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badgeText: String,
    val accentColor: Color
)

private val TUTORIAL_PAGES = listOf(
    TutorialPageData(
        stepNumber = "STEP 1 OF 4",
        title = "Check Inventory & Restock",
        description = "View softdrinks on the Home tab. Tap '+12 Stock' or 'Add Stock' to add new inventory lines. Enter Admin PIN: admin123 when prompted.",
        icon = Icons.Default.AddShoppingCart,
        badgeText = "STOCK MANAGEMENT",
        accentColor = CokeRed
    ),
    TutorialPageData(
        stepNumber = "STEP 2 OF 4",
        title = "Record Customer Sales",
        description = "Tap 'Record Sale' on any softdrink item card. Choose quantity sold, employee name, and optional customer receipt photo proof.",
        icon = Icons.Default.ShoppingCart,
        badgeText = "EASY POINT OF SALE",
        accentColor = StockGreen
    ),
    TutorialPageData(
        stepNumber = "STEP 3 OF 4",
        title = "View Reports & Earn AdMob Income",
        description = "Go to Sales Report tab. Tap Today, Yesterday, This Week, or This Month and watch a short video ad to unlock full sales breakdowns!",
        icon = Icons.Default.BarChart,
        badgeText = "REPORTS & AD REVENUE",
        accentColor = DrinkStockPink
    ),
    TutorialPageData(
        stepNumber = "STEP 4 OF 4",
        title = "Admin Security & Store Options",
        description = "All restock and delete actions are protected with Admin PIN 'admin123'. Replay this guide anytime from your Profile screen.",
        icon = Icons.Default.Security,
        badgeText = "ADMIN PROTECTION",
        accentColor = CokeBlack
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingTutorialScreen(
    onFinishTutorial: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { TUTORIAL_PAGES.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TUTORIAL_PAGES[pagerState.currentPage].stepNumber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CokeGray,
                    letterSpacing = 1.sp
                )

                if (pagerState.currentPage < TUTORIAL_PAGES.size - 1) {
                    TextButton(onClick = onFinishTutorial) {
                        Text(
                            text = "Skip Tutorial",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CokeGray
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }

            // Middle: Horizontal Pager for Tutorial Slides
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                val page = TUTORIAL_PAGES[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated Icon Box
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = page.accentColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = page.accentColor,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = page.accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = page.badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = page.accentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = page.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    Text(
                        text = page.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        color = CokeGray,
                        lineHeight = 20.sp
                    )
                }
            }

            // Bottom Section: Page Indicator & Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dot Page Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(TUTORIAL_PAGES.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) TUTORIAL_PAGES[pagerState.currentPage].accentColor
                                    else CokeGray.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Action Button (Next / Get Started)
                val isLastPage = pagerState.currentPage == TUTORIAL_PAGES.size - 1

                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinishTutorial()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TUTORIAL_PAGES[pagerState.currentPage].accentColor
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLastPage) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Using DrinkStock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Next Step",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
