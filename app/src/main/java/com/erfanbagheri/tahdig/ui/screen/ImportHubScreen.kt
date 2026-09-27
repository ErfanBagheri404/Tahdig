package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.RecipeDraft
import com.erfanbagheri.tahdig.util.RecipeTextParser
import kotlinx.coroutines.launch

/**
 * The import hub (#75 foundation for #76–#78).
 *
 * One place to paste text or a link, and one button per capture source. Every
 * route ends at the SAME [RecipeImportScreen] review, because OCR, video and
 * web import are only *sources* — inventing a review screen per source is how
 * a save path starts skipping validation in one of them.
 *
 * `RecipeTextParser.parseAny` already routes a URL to the URL parser and
 * anything else to the text parser, so «تحلیل» needs no branching here.
 */
@Composable
fun ImportHubScreen(
    onDraftReady: (RecipeDraft) -> Unit,
    onBack: () -> Unit,
    /** Provided by #76 (OCR camera), #77 (video) and #78 (web reader). */
    onScanPhoto: (() -> Unit)? = null,
    onPasteVideoUrl: (() -> Unit)? = null,
    onOpenWebReader: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    // #78: survives the reader round-trip — back from the reader restores it.
    var lastUrl by remember { mutableStateOf<String?>(null) }

    fun analyse() {
        if (input.isBlank()) return
        // A bare URL goes to the reader (#78), not the stub draft parser.
        if (RecipeTextParser.looksLikeUrlOnly(input)) {
            lastUrl = input.trim()
            onOpenWebReader?.invoke(lastUrl!!)
            return
        }
        onDraftReady(RecipeTextParser.parseAny(input))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "افزودن دستور",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = YekanBakh,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "متن دستور را اینجا بچسبان یا نشانی ویدیو را بده.",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(text = "متن دستور یا نشانی اینترنتی…", fontFamily = YekanBakh)
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = YekanBakh),
            minLines = 5,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
        )
        Spacer(Modifier.height(8.dp))

        // The button enables on PARSEABILITY, not non-blankness: «،،» is
        // non-blank and parses to nothing, so a non-blank guard would light the
        // button and then hand an empty draft to the review screen.
        val parses = input.isNotBlank() && RecipeTextParser.parseAny(input)
            .let { it.title.isNotBlank() || it.ingredients.isNotEmpty() || it.steps.isNotEmpty() }
        Button(
            onClick = { analyse() },
            enabled = parses,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "تحلیل متن", fontFamily = YekanBakh)
        }

        Spacer(Modifier.height(24.dp))
        Hairline()
        Spacer(Modifier.height(8.dp))
        Text(
            text = "گرفتن از روی کارت یا ویدیو",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Each source button only renders when that source is actually
        // available. A permanently-disabled row («عکس گرفتن از کارت» that does
        // nothing until some later PR lands) is worse than an absent row: it
        // advertises a feature the user cannot use.
        if (onScanPhoto != null || onPasteVideoUrl != null || onOpenWebReader != null) {
            Spacer(Modifier.height(4.dp))
            if (onScanPhoto != null) {
                SourceRow(label = "عکس‌برداری از کارت دستور", onClick = onScanPhoto)
            }
            if (onPasteVideoUrl != null) {
                SourceRow(label = "نشانی ویدیو (یوتیوب، اینستاگرام)", onClick = onPasteVideoUrl)
            }
            if (onOpenWebReader != null) {
                // analyse() routes a bare URL to the reader and guards blanks —
                // one route, no second half-baked path here.
                SourceRow(label = "باز کردن یک صفحهٔ وب", onClick = { analyse() })
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(text = "بازگشت", fontFamily = YekanBakh)
        }
    }
}

@Composable
private fun SourceRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = YekanBakh,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
    }
}
