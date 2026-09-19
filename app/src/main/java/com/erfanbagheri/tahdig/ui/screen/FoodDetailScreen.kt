package com.erfanbagheri.tahdig.ui.screen

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    foodId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var food by remember { mutableStateOf<FoodEntity?>(null) }

    LaunchedEffect(foodId) {
        val db = TahdigDatabase.getInstance(context)
        food = db.foodDao().getById(foodId)
    }

    val appContext = context.applicationContext
    var tts: TextToSpeech? = null
    tts = remember(appContext) {
        TextToSpeech(appContext) { status ->
            val engine = tts ?: return@TextToSpeech
            engine.language = if (status == TextToSpeech.SUCCESS) Locale("fa", "IR") else Locale.US
        }
    }
    DisposableEffect(Unit) {
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(food?.name ?: "", fontFamily = YekanBakh) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )

            food?.let { f ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Hero emoji
                    val emoji = com.erfanbagheri.tahdig.util.FoodVisuals.emoji(f.categoryId)
                    val accent = com.erfanbagheri.tahdig.util.FoodVisuals.accent(f.categoryId)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 64.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = f.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = YekanBakh,
                        fontSize = 36.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    if (f.nameEn.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = f.nameEn,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (f.description.isNotBlank()) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = f.description,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = YekanBakh,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (f.prepTimeMin > 0 || f.difficulty.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (f.prepTimeMin > 0) {
                                DetailChip("زمان آماده‌سازی: ${f.prepTimeMin} دقیقه")
                            }
                            if (f.difficulty.isNotBlank()) {
                                DetailChip("سختی: ${f.difficulty}")
                            }
                        }
                    }

                    if (f.ingredients.isNotBlank()) {
                        Spacer(Modifier.height(28.dp))
                        Text(
                            text = "مواد لازم",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = f.ingredients,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (f.tags.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "برچسب‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = f.tags,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { tts?.speak(f.ingredients, TextToSpeech.QUEUE_FLUSH, null, "tahdig") }) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("گوش دادن به مواد", fontFamily = YekanBakh)
                    }

                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = YekanBakh,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
