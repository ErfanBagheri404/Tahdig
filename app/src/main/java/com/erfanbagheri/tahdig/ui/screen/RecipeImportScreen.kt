package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.RecipeDraft

/**
 * Import review (#75) — the one screen every import path lands on.
 *
 * Share sheet, pasted text, and #76–#78 (OCR / video / web reader) all produce
 * a [RecipeDraft] and show it here before anything touches the DB: a parser is
 * best-effort, and saving a wrong split silently would be worse than asking.
 *
 * Flat by design: hairline separators, no cards. Each section is one editable
 * field holding one item per line, which is also the format the parser emits —
 * so edit and re-parse cannot disagree about structure.
 *
 * [onSave] receives the *edited* draft. Persisting is the caller's job.
 */
@Composable
fun RecipeImportScreen(
    draft: RecipeDraft,
    onSave: (RecipeDraft) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    // #76: lines the OCR capture flagged as low-confidence. The review shows
    // them as a warning row so the user can fix shaky lines; raw text stays
    // editable regardless.
    lowConfidenceLines: List<String> = emptyList(),
    // #76: the original card photo, shown collapsed under the steps.
    provenancePhotoUri: String? = null,
) {
    var title by remember(draft) { mutableStateOf(draft.title) }
    var ingredients by remember(draft) { mutableStateOf(draft.ingredients.joinToString("\n")) }
    var steps by remember(draft) { mutableStateOf(draft.steps.joinToString("\n")) }
    var notes by remember(draft) { mutableStateOf(draft.notes) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            TextButton(onClick = onBack) { Text("بازگشت", fontFamily = YekanBakh, fontSize = 14.sp) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                onSave(
                    draft.copy(
                        title = title.trim(),
                        ingredients = lines(ingredients),
                        steps = lines(steps),
                        notes = notes.trim(),
                    ),
                )
            }) { Text("ذخیره", fontFamily = YekanBakh, fontSize = 14.sp) }
        }
        Hairline()

        Text(
            "بازبینی دستور",
            fontFamily = YekanBakh,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        draft.sourceUrl?.let {
            Text(
                "منبع: $it",
                fontFamily = YekanBakh,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        Hairline()

        // #77: a video/web import carries its own thumbnail. Shown read-only —
        // an editable text field for a URL the user cannot see would be worse
        // than no preview at all. Replacement is the detail screen's photo flow.
        draft.photoUrl?.takeIf { it.isNotBlank() }?.let { url ->
            Spacer(Modifier.height(8.dp))
            Text(
                "تصویر دستور",
                fontFamily = YekanBakh,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(4.dp))
            coil3.compose.AsyncImage(
                model = url,
                contentDescription = "تصویر ویدیو",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 16.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Hairline()

        ImportField("نام دستور", title, singleLine = true) { title = it }
        Hairline()
        ImportField(
            "مواد لازم — هر خط یک ماده",
            ingredients,
            supporting = "${PersianText.toPersianDigits(lines(ingredients).size)} ماده",
        ) { ingredients = it }
        Hairline()
        ImportField(
            "طرز تهیه — هر خط یک مرحله",
            steps,
            supporting = "${PersianText.toPersianDigits(lines(steps).size)} مرحله",
        ) { steps = it }
        Hairline()
        ImportField("یادداشت", notes) { notes = it }
        Hairline()

        // #76: shaky OCR lines. The text above stays editable regardless —
        // this row says which lines to look at, it does not gate saving.
        if (lowConfidenceLines.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "خطوط کم‌اعتماد — بازبینی کنید:",
                fontFamily = YekanBakh,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            lowConfidenceLines.forEach { line ->
                Text(
                    "• $line",
                    fontFamily = YekanBakh,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 1.dp),
                )
            }
        }

        // #76 «کارت اصلی»: the photo is the provenance of a heirloom card —
        // collapsed under the steps so it never outranks the recipe itself.
        provenancePhotoUri?.let { uri ->
            Spacer(Modifier.height(12.dp))
            val expanded = remember { mutableStateOf(false) }
            TextButton(onClick = { expanded.value = !expanded.value }) {
                Text(
                    if (expanded.value) "پنهان کردن کارت اصلی" else "نمایش کارت اصلی",
                    fontFamily = YekanBakh,
                    fontSize = 13.sp,
                )
            }
            if (expanded.value) {
                coil3.compose.AsyncImage(
                    model = uri,
                    contentDescription = "کارت اصلی",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

/** One editable section. Blank lines are dropped on save, not on every keystroke. */
@Composable
private fun ImportField(
    label: String,
    value: String,
    supporting: String? = null,
    singleLine: Boolean = false,
    onChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, fontFamily = YekanBakh, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = YekanBakh),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            modifier = Modifier.fillMaxWidth(),
        )
        supporting?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, fontFamily = YekanBakh, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Text field -> list. Trims each line, drops blanks, keeps order. */
internal fun lines(text: String): List<String> =
    text.lines().map { it.trim() }.filter { it.isNotEmpty() }
