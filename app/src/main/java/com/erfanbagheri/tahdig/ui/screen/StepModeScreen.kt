package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh

@Composable
fun StepModeScreen(
    foodId: Long,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var food by remember { androidx.compose.runtime.mutableStateOf<com.erfanbagheri.tahdig.data.local.entity.FoodEntity?>(null) }
    androidx.compose.runtime.LaunchedEffect(foodId) {
        food = com.erfanbagheri.tahdig.data.local.TahdigDatabase.getInstance(context).foodDao().getById(foodId)
    }

    val description = food?.description ?: ""
    val foodName = food?.name ?: ""
    val steps = remember(description) {
        description.split(Regex("[.!؟\n]+")).map { it.trim() }.filter { it.isNotBlank() }
            .ifEmpty { listOf(description) }
    }
    var current by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Text(
                    text = foodName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = YekanBakh,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(32.dp))

            // Step counter
            Text(
                text = "مرحله ${current + 1} از ${steps.size}",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(16.dp))

            // Step text
            Text(
                text = steps[current],
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                fontFamily = YekanBakh,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )

            // Navigation row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = { if (current < steps.lastIndex) current++ },
                    enabled = current < steps.lastIndex,
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "مرحله بعد",
                        modifier = Modifier.padding(24.dp),
                    )
                }
                IconButton(
                    onClick = { if (current > 0) current-- },
                    enabled = current > 0,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "مرحله قبل",
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}
