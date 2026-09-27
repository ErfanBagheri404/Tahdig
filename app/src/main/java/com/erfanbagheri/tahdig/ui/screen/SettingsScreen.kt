package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import com.erfanbagheri.tahdig.util.NutrientCaps
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.ui.viewmodel.SettingsViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onOpenHeatmap: () -> Unit = {},
    onOpenDiary: () -> Unit = {},
) {
    val themeMode by viewModel.themeMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "تنظیمات",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        // Theme section
        Text(
            text = "پوسته",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        ThemeOption("سیستم", selected = themeMode == 0) { viewModel.setThemeMode(0) }
        ThemeOption("روشن", selected = themeMode == 1) { viewModel.setThemeMode(1) }
        ThemeOption("تاریک", selected = themeMode == 2) { viewModel.setThemeMode(2) }

        Spacer(Modifier.height(32.dp))

        // ── Nutrition profile (#110) ───────────────────────────────────
        // Skippable by design: leaving «بدون هدف» on shows totals only, so the
        // section never blocks the app behind a form (AC).
        val profile by viewModel.profile.collectAsState()
        Text(
            text = "هدف تغذیه",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        if (profile.hasGoal) {
            Text(
                text = "روزانه ${PersianText.toPersianDigits(
                    com.erfanbagheri.tahdig.util.DailyBudget.budget(profile).toString(),
                )} کیلوکالری",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = "بدون هدف — فقط مجموع نشون داده می‌شه",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Switch(
                checked = profile.hasGoal,
                onCheckedChange = { viewModel.setProfile(profile.copy(hasGoal = it)) },
            )
            Spacer(Modifier.width(12.dp))
            Text("محاسبه هدف روزانه", fontFamily = YekanBakh)
        }
        if (profile.hasGoal) {
            StepperRow("سن", profile.age, 10..100) { viewModel.setProfile(profile.copy(age = it)) }
            StepperRow(
                "وزن (کیلوگرم)", profile.weightKg.toInt(), 30..200,
            ) { viewModel.setProfile(profile.copy(weightKg = it.toDouble())) }
            StepperRow(
                "قد (سانتی‌متر)", profile.heightCm.toInt(), 120..220,
            ) { viewModel.setProfile(profile.copy(heightCm = it.toDouble())) }
            StepperRow(
                label = "فعالیت",
                value = profile.activity,
                range = 0..com.erfanbagheri.tahdig.util.DailyBudget.ACTIVITY.lastIndex,
                labelFor = { ACTIVITY_LABELS.getOrElse(it) { "" } },
                onChange = { viewModel.setProfile(profile.copy(activity = it)) },
            )
            StepperRow(
                label = "هدف",
                value = profile.goal,
                range = 0..com.erfanbagheri.tahdig.util.DailyBudget.GOALS.lastIndex,
                labelFor = { GOAL_LABELS.getOrElse(it) { "" } },
                onChange = { viewModel.setProfile(profile.copy(goal = it)) },
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── Allergy profile (#112) ───────────────────────────────────
        // Options come from the seed's own allergen taxonomy, so every chip
        // can actually match a dish. Detection is conservative: unknown
        // ingredients never warn.
        val allergens by viewModel.allergens.collectAsState()
        val allergenHide by viewModel.allergenHide.collectAsState()
        Text(
            text = "آلرژی‌ها",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        if (allergens.isEmpty()) {
            Text(
                text = "چیزی انتخاب نشده — تشخیصی انجام نمی‌شه",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            com.erfanbagheri.tahdig.util.AllergenDetector.PROFILE_OPTIONS.forEach { option ->
                val on = option in allergens
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = if (on) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .background(
                            color = if (on) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable {
                            viewModel.setAllergens(
                                allergens.toMutableSet().apply {
                                    if (!remove(option)) add(option)
                                },
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Switch(
                checked = allergenHide,
                onCheckedChange = { viewModel.setAllergenHide(it) },
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "پنهان‌سازی غذاهای آلرژن‌دار",
                fontFamily = YekanBakh,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Halal-style flags (#118) ────────────────────────────────
        // A check, not a certification: the toggle only hides dishes whose
        // ingredient text POSITIVELY names pork, alcohol or animal gelatin.
        val halalStrict by viewModel.halalStrict.collectAsState()
        Text(
            "پرچم مواد غیرحلال",
            fontFamily = YekanBakh,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "اگر دستور غذایی گوشت خوک، الکل یا ژلاتین حیوانی داشته باشد، «بررسی کن» نشان می‌دهد. " +
                "این گواهی حلال نیست.",
            fontFamily = YekanBakh,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Switch(
                checked = halalStrict,
                onCheckedChange = { viewModel.setHalalStrict(it) },
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "پنهان‌سازی غذاهای پرچم‌خورده",
                fontFamily = YekanBakh,
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── Nutrient caps (#113) ────────────────────────────────────
        // «محدودیت‌های من». Preset first, then overrides: a personal cap wins
        // over the preset for the same nutrient, and caps for other nutrients
        // stack on top. These are personal limits, not medical advice.
        val capPreset by viewModel.capPreset.collectAsState()
        val capCustom by viewModel.capCustom.collectAsState()
        Text(
            text = "محدودیت‌های من",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "سقف هر ماده روی هر وعده حساب می‌شه، نه کل روز",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NutrientCaps.Preset.entries.forEach { p ->
                val on = capPreset == p.name
                Text(
                    text = p.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = if (on) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .background(
                            color = if (on) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable {
                            viewModel.setCapPreset(if (on) null else p.name)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        // Every active cap, preset or personal, in one flat list.
        val activeCaps = remember(capPreset, capCustom) {
            NutrientCaps.merge(
                NutrientCaps.Preset.entries.firstOrNull { it.name == capPreset },
                capCustom.mapNotNull { (k, v) ->
                    NutrientCaps.Nutrient.entries.firstOrNull { it.name == k }?.let { it to v }
                }.toMap(),
            )
        }
        if (activeCaps.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            activeCaps.forEach { (n, cap) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Text(
                        text = n.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = YekanBakh,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "سقف ${PersianText.toPersianDigits(
                            if (cap == cap.toLong().toDouble()) cap.toLong().toString()
                            else String.format("%.1f", cap),
                        )} ${n.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // Per-nutrient override steppers. Off by default (0) = follow the preset.
        Spacer(Modifier.height(12.dp))
        NutrientCaps.Nutrient.entries.forEach { n ->
            val current = capCustom[n.name]?.toInt() ?: 0
            StepperRow(
                label = n.label,
                value = current,
                range = 0..5000,
                onChange = { viewModel.setCapCustom(n.name, it.toDouble()) },
            )
        }

        Spacer(Modifier.height(32.dp))

        // Notifications section
        Text(
            text = "اعلان‌ها",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        val context = androidx.compose.ui.platform.LocalContext.current
        val dailyNotify by viewModel.dailyNotify.collectAsState()

        // API 33+: notifications need runtime permission, so ask when the user enables the toggle.
        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        ) { granted ->
            viewModel.setDailyNotify(context, granted)
        }
        val needsPermission = android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "پیشنهاد روزانه غذا",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = dailyNotify,
                onCheckedChange = { enabled ->
                    if (enabled && needsPermission) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setDailyNotify(context, enabled)
                    }
                },
            )
        }

        Spacer(Modifier.height(32.dp))

        // Barcode scanner (#116) — off removes the camera entirely from the
        // search screen, so no user is ever asked for the camera again.
        val scannerEnabled by viewModel.scannerEnabled.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "اسکنر بارکد",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = scannerEnabled,
                onCheckedChange = { viewModel.setScannerEnabled(it) },
            )
        }

        Spacer(Modifier.height(32.dp))

        // Voice control in cook mode (#94)
        Text(
            text = "کنترل صوتی",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        val voiceControl by viewModel.voiceControl.collectAsState()
        val voiceReadAloud by viewModel.voiceReadAloud.collectAsState()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "کنترل صوتی هنگام پخت",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = voiceControl,
                onCheckedChange = { viewModel.setVoiceControl(it) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "خواندن مرحله با صدا",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = voiceReadAloud,
                onCheckedChange = { viewModel.setVoiceReadAloud(it) },
            )
        }

        Spacer(Modifier.height(32.dp))

        // Shake to advance (#96) — off by default; additive to the buttons.
        Text(
            text = "تکان دادن گوشی",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        val shakeAdvance by viewModel.shakeAdvance.collectAsState()
        val shakeSensitivity by viewModel.shakeSensitivity.collectAsState()
        val shakeSpin by viewModel.shakeSpin.collectAsState()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "مرحله بعد با تکان دادن",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = shakeAdvance,
                onCheckedChange = { viewModel.setShakeAdvance(it) },
            )
        }

        // Shake-to-spin the dinner roulette (#123) — same gesture, same
        // sensitivity slider, disabled by nature in cook mode (the home
        // screen — and its watcher — isn't composed there).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "چرخوندن با تکان دادن",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = shakeSpin,
                onCheckedChange = { viewModel.setShakeSpin(it) },
            )
        }

        // Sensitivity only matters when the gesture is on — a slider over a
        // disabled feature is noise.
        if (shakeAdvance || shakeSpin) {
            Text(
                text = "حساسیت",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.material3.Slider(
                value = shakeSensitivity,
                onValueChange = { viewModel.setShakeSensitivity(it) },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "حساس‌تر = با تکان ملایم‌تر مرحله عوض می‌شود",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))

        // About section
        Text(
            text = "درباره",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "ته‌دیگ v0.1.0",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "۵۴۳ غذای ایرانی و بین‌المللی",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Backup / Restore section
        Spacer(Modifier.height(32.dp))

        Text(
            text = "پشتیبان‌گیری",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        BackupRestoreRow("ذخیره پشتیبان", onBackup)
        BackupRestoreRow("بازیابی پشتیبان", onRestore)

        // Cooking history heatmap
        Spacer(Modifier.height(32.dp))

        Text(
            text = "سابقه پخت",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        BackupRestoreRow("تقویم پخت", onOpenHeatmap)

        // Meal diary + weekly report (#114)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "دفترچه وعده‌ها",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        BackupRestoreRow("گزارش هفتگی", onOpenDiary)

        // Carry-over is off by default; the diary reads the same flow
        // through SettingsViewModel, exactly like every other toggle here.
        val carryOver by viewModel.carryOver.collectAsState()
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Switch(
                checked = carryOver,
                onCheckedChange = { viewModel.setCarryOver(it) },
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("انتقال کالری باقی‌مانده", fontFamily = YekanBakh)
                Text(
                    "کالری خرج‌نشدهٔ امروز به فردا اضافه شود",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

/** Human labels for the activity / goal ordinals (#110). */
private val ACTIVITY_LABELS = listOf("کم", "سبک", "متوسط", "زیاد", "ورزشی")
private val GOAL_LABELS = listOf("کاهش", "ثبات", "افزایش")

/**
 * One ± stepper row (#110): label, Persian-digit value, 48dp targets. Keeps the
 * flat settings look — no sliders, no dialogs for one integer.
 */
@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    labelFor: (Int) -> String = { it.toString() },
    onChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = YekanBakh,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = PersianText.toPersianDigits(labelFor(value)),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(56.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TextButton(enabled = value > range.first, onClick = { onChange(value - 1) }) {
            Text("\u2212", fontFamily = YekanBakh)
        }
        TextButton(enabled = value < range.last, onClick = { onChange(value + 1) }) {
            Text("+", fontFamily = YekanBakh)
        }
    }
}

@Composable
private fun BackupRestoreRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = YekanBakh,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = "انتخاب تم $label",
            )
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "انتخاب‌شده",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
