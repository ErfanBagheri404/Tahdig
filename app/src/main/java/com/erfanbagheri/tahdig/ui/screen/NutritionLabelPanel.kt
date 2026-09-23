package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.MicroNutrients
import com.erfanbagheri.tahdig.util.NutriLabel
import com.erfanbagheri.tahdig.util.PersianText

/**
 * Full nutrition label (#111): per-serving macro table with %DV bars, the
 * micro rows when the source has them, and the Nutri-Score / NOVA badges.
 *
 * Estimate mode renders the same table dimmed with a «تخمینی» watermark and
 * NO badges — the issue is explicit that a dish without real data must not
 * show fake precision. The badge never claims more than the data supports.
 */
@Composable
fun NutritionLabelPanel(
    label: NutritionLabelData,
    modifier: Modifier = Modifier,
) {
    val dim = if (label.estimated) 0.55f else 1f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(14.dp),
    ) {
        // ── header: confidence badge + score badges ──────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (label.estimated) "برچسب تغذیه‌ای · تخمینی"
                else "برچسب تغذیه‌ای · واقعی",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = YekanBakh,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Grades and NOVA need real data — never shown on an estimate.
                label.score?.let { ScoreBadge(it) }
                label.nova?.let { NovaBadge(it) }
            }
        }

        Spacer(Modifier.height(10.dp))
        LabelHairline()

        // ── macro rows with %DV bars ─────────────────────────────────
        LabelMacroRow("چربی", label.fatG, NutriLabel.percentDv(label.fatG, NutriLabel.DV.FAT_G), dim)
        LabelMacroRow(
            "چربی اشباع", label.satFatG,
            NutriLabel.percentDv(label.satFatG, NutriLabel.DV.SAT_FAT_G), dim,
        )
        LabelMacroRow(
            "کربوهیدرات", label.carbG,
            NutriLabel.percentDv(label.carbG, NutriLabel.DV.CARB_G), dim,
        )
        LabelMacroRow("قند", label.sugarG, null, dim)
        LabelMacroRow(
            "پروتئین", label.proteinG,
            NutriLabel.percentDv(label.proteinG, NutriLabel.DV.PROTEIN_G), dim,
        )
        if (label.fiberG > 0.0) {
            LabelMacroRow(
                "فیبر", label.fiberG,
                NutriLabel.percentDv(label.fiberG, NutriLabel.DV.FIBER_G), dim,
            )
        }
        if (label.saltG > 0.0) {
            LabelMacroRow(
                "نمک", label.saltG,
                NutriLabel.percentDv(label.saltG * 1000, NutriLabel.DV.SODIUM_MG), dim,
            )
        }

        // ── micro table (#117) ───────────────────────────────────────
        // Only on real data. Absent nutrients are rendered «—», never
        // imputed — a zero would claim "none present", which is a different
        // claim from "unknown".
        if (!label.estimated) {
            // Raw nullables, no takeIf: a zero-or-absent value renders «—»
            // through the null contract, and must not be laundered to null
            // selectively only some of the time.
            val micro = MicroNutrients.rows(
                fiberG = label.fiberG,
                sodiumMg = label.saltG.times(1000.0),
                potassiumMg = label.potassiumMg,
                calciumMg = label.calciumMg,
                ironMg = label.ironMg,
                vitDUg = label.vitaminDUg,
                b12Ug = label.b12Ug,
            )
            if (micro.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LabelHairline()
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "ریزمغذی‌ها",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                micro.forEach { row -> LabelMicroRow(row) }
            }
        }

        Spacer(Modifier.height(10.dp))
        LabelHairline()
        Spacer(Modifier.height(8.dp))

        Text(
            text = "${PersianText.toPersianDigits(label.calories.toString())} کیلوکالری در هر پرس",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (label.estimated) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "دادهٔ واقعی برای مواد این غذا نیست — مقادیر از الگوی دسته‌بندی می‌آید.",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One label row: name, grams, and the %DV bar when a reference exists. */
/**
 * A micro row. A null amount renders «—» with no bar: the AC is explicit that
 * missing data is never imputed, so the row stays visible with a dash rather
 * than disappearing or showing a zero.
 */
@Composable
private fun LabelMicroRow(row: MicroNutrients.Row) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.amount?.let {
                    PersianText.toPersianDigits(it.round1()) + " " + row.unit +
                        (row.dvPercent?.let { p -> "  ·  " + PersianText.toPersianDigits("$p") + "٪" } ?: "")
                } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (row.dvPercent != null && row.dvPercent > 0) {
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(2.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (row.dvPercent / 100f).coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun LabelMacroRow(
    name: String,
    grams: Double,
    dvPercent: Int?,
    dim: Float,
    // Micro rows (#117) carry mg/µg, so the unit is a parameter. Macro rows
    // keep the gram default and their call sites stay unchanged.
    unit: String = "گرم",
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim),
            )
            Text(
                text = PersianText.toPersianDigits(grams.round1()) + " " + unit +
                    (dvPercent?.let { "  ·  " + PersianText.toPersianDigits("$it") + "٪" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim),
            )
        }
        if (dvPercent != null && dvPercent > 0) {
            Spacer(Modifier.height(3.dp))
            // Flat bar, hairline track — no gradient candy.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = dim),
                        shape = RoundedCornerShape(2.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (dvPercent / 100f).coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = dim),
                            shape = RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
}

/** Flat coloured letter — A green through E red, matching the published scale. */
@Composable
private fun ScoreBadge(score: NutriLabel.Score) {
    val color = when (score) {
        NutriLabel.Score.A -> Color(0xFF2E7D32)
        NutriLabel.Score.B -> Color(0xFF7CB342)
        NutriLabel.Score.C -> Color(0xFFF9A825)
        NutriLabel.Score.D -> Color(0xFFEF6C00)
        NutriLabel.Score.E -> Color(0xFFC62828)
    }
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(color = color, shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = score.letter,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/** NOVA group with its Farsi subtitle — «کم‌فراوری» … «فراوری‌شدهٔ زیاد». */
@Composable
private fun NovaBadge(group: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = PersianText.toPersianDigits("$group"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = NutriLabel.novaLabel(group),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LabelHairline() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
    )
}

/** One decimal, Persian digits, no trailing «.۰». */
private fun Double.round1(): String {
    val r = Math.round(this * 10) / 10.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}
