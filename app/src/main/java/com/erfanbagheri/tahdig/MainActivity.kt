package com.erfanbagheri.tahdig

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.ui.theme.TahdigTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = TahdigDatabase.getInstance(this)
        val foodDao = db.foodDao()

        setContent {
            // RTL forced globally for this Farsi-only app.
            // CompositionLocalProvider overrides Activity-level layoutDirection.
            val rtlDirection = LayoutDirection.Rtl

            TahdigTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val foodCount by foodDao.observeCount().collectAsState(initial = -1)
                    Text(
                        text = if (foodCount == -1)
                            "ته‌دیگ"
                        else
                            "ته‌دیگ\n$foodCount غذا آماده است",
                    )
                }
            }
        }

        lifecycleScope.launch {
            // First run — populate from the bundled seed assets
            TahdigDatabase.populateIfEmpty(this@MainActivity)
        }
    }
}
