package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.BadgeViewModel
import com.erfanbagheri.tahdig.util.BadgeEngine
import com.erfanbagheri.tahdig.util.PersianText

/**
 * Badge collection (#121) — a Settings overlay, same shape as the diary and
 * the heatmap: unlocked first, then locked with their requirement text.
 *
 * Hairline rows, no progress bars: the issue explicitly rules out anything
 * beyond a simple counter, and a wall of bars reads as a game rather than a
 * record of what the user actually cooked.
 */
@Composable
fun BadgesScreen(vm: BadgeViewModel, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    val colors = MaterialTheme.colorScheme
    val unlockedCount = states.count { it.unlocked }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    text = "نشان‌ها",
                    fontFamily = YekanBakh,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = PersianText.toPersianDigits(unlockedCount.toDouble()) +
                        " از " + PersianText.toPersianDigits(states.size.toDouble()) +
                        " نشان",
                    fontFamily = YekanBakh,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Unlocked block first: the collection should open on what was earned,
        // not on a scroll through thirty locked rows.
        val (won, locked) = states.partition { it.unlocked }

        if (won.isNotEmpty()) {
            SectionLabel("به‌دست‌آمده")
            won.forEach { BadgeRow(it) }
            Spacer(Modifier.height(20.dp))
        }

        if (locked.isNotEmpty()) {
            SectionLabel("قفل")
            locked.forEach { BadgeRow(it) }
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = YekanBakh,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun BadgeRow(state: BadgeEngine.State) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Locked badges stay at reduced opacity rather than greyed to
        // invisible: the emoji is the row's identity, and a hidden row reads
        // as a missing feature.
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (state.unlocked) colors.primaryContainer
                    else colors.surfaceVariant.copy(alpha = 0.35f),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(state.def.emoji, fontFamily = YekanBakh)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = state.def.title,
                fontFamily = YekanBakh,
                fontWeight = if (state.unlocked) FontWeight.Bold else FontWeight.Normal,
                color = if (state.unlocked) colors.onSurface
                else colors.onSurfaceVariant,
            )
            Text(
                text = requirementLine(state),
                fontFamily = YekanBakh,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        if (state.unlocked) {
            Text("✓", fontFamily = YekanBakh, color = colors.primary)
        }
    }
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
}

/**
 * Locked rows show the requirement plus the counter when the badge HAS one.
 * Badges without a counter (یلدا, بی‌فریز) show the rule alone — «۰ از ۰» is
 * noise that reads as a broken counter.
 */
private fun requirementLine(state: BadgeEngine.State): String = when {
    state.unlocked -> "به‌دست آمده"
    state.goal <= 0 -> state.def.requirement
    else -> state.def.requirement + " · " +
        PersianText.toPersianDigits(state.progress.toDouble()) + " از " +
        PersianText.toPersianDigits(state.goal.toDouble())
}
