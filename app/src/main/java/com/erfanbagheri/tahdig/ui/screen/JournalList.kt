package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.entity.JournalEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.JalaliDate
import com.erfanbagheri.tahdig.util.PersianText
import java.time.Instant
import java.time.ZoneId

/**
 * Cook journal timeline (#124).
 *
 * Flat rows separated by a hairline — the house look, no rounded cards. A row
 * with no photo and no note still renders (dish + date): the point of the
 * journal is that the cook happened, not that it was documented.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun JournalList(
    entries: List<JournalEntity>,
    foodName: (Long) -> String?,
    onClick: (Long) -> Unit,
    // #125 photo-prompt deep-link: when set, opens the picker for this entry
    // once the first entry loads (today's cook, or nothing when the list is
    // empty — then the row is simply absent and the user attaches by hand).
    attachToId: Long? = null,
    onAttach: (Long, android.net.Uri) -> Unit = { _, _ -> },
    // #127: long-press menu action — the only path to delete, so the row
    // never needs a destructive swipe over a photo.
    onDelete: (Long) -> Unit = {},
    // #126: ghost-row CTA when the journal is empty.
    onGoHome: () -> Unit = {},
) {
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null && attachToId != null) onAttach(attachToId, uri)
    }
    androidx.compose.runtime.LaunchedEffect(attachToId) {
        if (attachToId != null) picker.launch("image/*")
    }
    if (entries.isEmpty()) {
        // #126: preview the real row shape + one CTA. No modal, no wall.
        com.erfanbagheri.tahdig.ui.components.GhostRowsEmptyState(
            title = "هنوز چیزی ثبت نشده",
            subtitle = "بعد از پخت، یه عکس یا یادداشت بذار تا اینجا جمع بشه",
            cta = "برگرد به پیشنهاد امروز",
            onCta = onGoHome,
            modifier = Modifier.padding(top = 32.dp),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(entries, key = { it.id }) { entry ->
            var menuOpen by androidx.compose.runtime.remember(entry.id) {
                androidx.compose.runtime.mutableStateOf(false)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(entry.foodId) }
                    .combinedClickable(
                        onClick = { onClick(entry.foodId) },
                        onLongClick = { menuOpen = true },
                        onLongClickLabel = "گزینه‌های خاطره",
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    JournalThumb(entry.photo)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = foodName(entry.foodId) ?: "غذای حذف‌شده",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = YekanBakh,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = jalaliStamp(entry.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (entry.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                // #127: long-press menu. Anchored to the row, not a floating
                // dialog, so the target stays visually attached to the entry.
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "حذف خاطره",
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete(entry.id)
                        },
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp,
            )
        }
    }
}

/** Photo thumbnail, or nothing at all when the cook has no photo. */
@Composable
private fun JournalThumb(photo: ByteArray?) {
    if (photo == null || photo.isEmpty()) return
    val bitmap = remember(photo) {
        android.graphics.BitmapFactory
            .decodeByteArray(photo, 0, photo.size)
            ?.asImageBitmap()
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

/** «۱۴۰۵/۰۷/۰۱» — Jalali date with Persian digits. */
private fun jalaliStamp(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val j = JalaliDate.toJalali(date)
    return PersianText.toPersianDigits(
        "%04d/%02d/%02d".format(j.year, j.month, j.day),
    )
}
