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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.FirstRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Page(val emoji: String, val title: String, val body: String)

private val pages = listOf(
    Page("🍽", "به طاق دیگ خوش آمدید!", "غذای امروزت رو با یک کلیک پیدا کن"),
    Page("⭐", "امتیاز بده", "غذاها رو امتیازدهی کن تا هر بار بهترین انتخاب رو ببینی"),
    Page("🛒", "لیست خرید", "مواد لازم هر غذا رو مستقیم به لیست خریدت اضافه کن"),
)

/**
 * Page count. Kept separate from [pages] because the final page is not a
 * brochure page any more (#126): it hands off into a real action.
 */
private const val BROCHURE_PAGES = 3

/**
 * Last stop of onboarding (#126) — «اولین غذات رو انتخاب کن».
 *
 * The old last page was a dead end: «شروع!» dismissed into an empty app and
 * the user had to find the first dish themselves. This one loads a seeded
 * sample dish straight from Room and opens it, so onboarding ends in a real
 * dish on screen. [FirstRun.samplePick] is deterministic, so the row does not
 * flicker and «یه نمونه دیگه» is a stable rotation instead of a lottery.
 */
@Composable
private fun FirstDishHandoff(
    onPick: (Long) -> Unit,
    onSkip: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var dish by remember { mutableStateOf<FoodEntity?>(null) }
    var variation by remember { mutableIntStateOf(0) }
    LaunchedEffect(variation) {
        val dao = TahdigDatabase.getInstance(context).foodDao()
        dish = withContext(Dispatchers.IO) { dao.getById(FirstRun.samplePick(variation)) }
            // A sample id that no longer exists must not break onboarding —
            // fall through to the plain pick and let the app pick for itself.
            ?: withContext(Dispatchers.IO) { dao.byIndex(variation) }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🍲", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "اولین غذات رو انتخاب کن",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = YekanBakh,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "یه نمونه — هر وقت خواستی می‌تونی عوضش کنی",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        val current = dish
        if (current != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "نمونه",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = YekanBakh,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onPick(current.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("همین رو باز کن", fontFamily = YekanBakh, fontSize = 16.sp)
            }
            TextButton(onClick = { variation++ }) {
                Text("یه نمونه دیگه", fontFamily = YekanBakh)
            }
        } else {
            // Seed still loading (first launch is exactly when this happens).
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip) {
            Text("خودم انتخاب می‌کنم", fontFamily = YekanBakh)
        }
    }
}

@Composable
fun OnboardingPager(
    onDone: () -> Unit,
    // #126: the last stop opens a real dish instead of dismissing into an empty app.
    onOpenDish: (Long) -> Unit = {},
) {
    // One extra stop after the brochure pages: the seeded first-dish handoff.
    val pagerState = rememberPagerState(pageCount = { BROCHURE_PAGES + 1 })
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
            if (page == BROCHURE_PAGES) {
                FirstDishHandoff(
                    onPick = { id -> onOpenDish(id) },
                    onSkip = onDone,
                )
                return@HorizontalPager
            }
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
            repeat(BROCHURE_PAGES + 1) { i ->
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
                if (pagerState.currentPage < BROCHURE_PAGES)
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                else onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                text = if (pagerState.currentPage < BROCHURE_PAGES) "بعدی" else "شروع!",
                fontFamily = YekanBakh,
                fontSize = 16.sp,
            )
        }
    }
}
