package com.erfanbagheri.tahdig.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.notify.DailyNotifyScheduler
import com.erfanbagheri.tahdig.ui.theme.YekanBakh

/**
 * Permission priming (#122), shown AFTER the first cook — never on first
 * launch. The value is explained in-app first, the OS dialog comes only when
 * the user presses the button, and a denial is remembered (the row stays
 * re-primable from Settings).
 */
@Composable
fun NotificationPrimingRow(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        SettingsStore.setNotifPermission(primed = true, granted = granted)
        if (granted) {
            SettingsStore.setDailyNotify(context, true)
            DailyNotifyScheduler.schedule(context)
        }
        onDismiss()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "یادآوری غذای روزانه",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "وقتی رکورد پختت در خطره یا چند روز نبختی، یک یادآوری شبانه می‌گیری. " +
                    "ساعت یادآوری و ساعت سکوت را خودت تعیین می‌کنی.",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            SettingsStore.setNotifPermission(primed = true, granted = true)
                            SettingsStore.setDailyNotify(context, true)
                            DailyNotifyScheduler.schedule(context)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("روشن کن", fontFamily = YekanBakh)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("بعداً", fontFamily = YekanBakh, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** True when the OS dialog still needs asking (already granted -> no primer). */
fun needsNotificationPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
