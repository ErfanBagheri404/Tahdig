package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import kotlinx.coroutines.launch

private data class Page(val emoji: String, val title: String, val body: String)

private val pages = listOf(
    Page("🍽", "به طاق دیگ خوش آمدید!", "غذای امروزت رو با یک کلیک پیدا کن"),
    Page("⭐", "امتیاز بده", "غذاها رو امتیازدهی کن تا هر بار بهترین انتخاب رو ببینی"),
    Page("🛒", "لیست خرید", "مواد لازم هر غذا رو مستقیم به لیست خریدت اضافه کن"),
)

@Composable
fun OnboardingPager(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = p.emoji, fontSize = 80.sp)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = YekanBakh,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = p.body,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Dot indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            repeat(pages.size) { i ->
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (i == pagerState.currentPage)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .height(8.dp)
                        .let { m ->
                            if (i == pagerState.currentPage) m.weight(2f) else m.weight(1f)
                        },
                ) {}
            }
        }

        // Next / Done button
        Button(
            onClick = {
                if (pagerState.currentPage < pages.lastIndex)
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                else onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                text = if (pagerState.currentPage < pages.lastIndex) "بعدی" else "شروع!",
                fontFamily = YekanBakh,
                fontSize = 16.sp,
            )
        }
    }
}
