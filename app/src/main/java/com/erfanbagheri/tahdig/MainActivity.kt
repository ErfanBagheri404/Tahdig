package com.erfanbagheri.tahdig

import android.content.Intent
import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.ui.screen.FavoritesScreen
import com.erfanbagheri.tahdig.ui.screen.FoodDetailScreen
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.screen.HomeScreen
import com.erfanbagheri.tahdig.ui.screen.OnboardingPager
import com.erfanbagheri.tahdig.ui.screen.SearchScreen
import com.erfanbagheri.tahdig.ui.screen.SettingsScreen
import com.erfanbagheri.tahdig.ui.screen.StepModeScreen
import com.erfanbagheri.tahdig.ui.screen.ShoppingListScreen
import com.erfanbagheri.tahdig.ui.theme.TahdigTheme
import com.erfanbagheri.tahdig.ui.screen.MealPlanScreen
import com.erfanbagheri.tahdig.ui.viewmodel.MealPlanViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.FavoritesViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.HistoryViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.HomeViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.RatingViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.SearchViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.SettingsViewModel
import com.erfanbagheri.tahdig.util.BackupRestore
import com.erfanbagheri.tahdig.ui.viewmodel.ShoppingViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            TahdigDatabase.populateIfEmpty(this@MainActivity)
        }

        setContent {
            TahdigTheme {
                TahdigApp()
            }
        }
    }
}

@Composable
private fun TahdigApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var detailFoodId by rememberSaveable { mutableLongStateOf(-1L) }
    val context = LocalContext.current

    // Backup/restore SAF launchers
    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let { BackupRestore.backup(context, it) } }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let {
        // Only restart when the restore actually replaced the DB; a bad pick leaves it intact.
        if (BackupRestore.restore(context, it)) {
            context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName))
            (context as? Activity)?.finish()
        } else {
            android.widget.Toast.makeText(
                context, "فایل پشتیبان معتبر نیست", android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    } }

    // Onboarding gate — first launch only
    val onboarded by com.erfanbagheri.tahdig.data.prefs.SettingsStore.onboarded.collectAsState()
    if (!onboarded) {
        OnboardingPager(onDone = { SettingsStore.setOnboarded() })
        return
    }
    var stepModeFoodId by rememberSaveable { mutableLongStateOf(-1L) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (detailFoodId < 0) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("خانه") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("جستجو") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("علاقه‌مندی‌ها") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                        label = { Text("لیست خرید") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("تنظیمات") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        label = { Text("برنامه") },
                    )
                }
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                stepModeFoodId >= 0 -> {
                    StepModeScreen(
                        foodId = stepModeFoodId,
                        onBack = { stepModeFoodId = -1L },
                    )
                }
                detailFoodId >= 0 -> {
                    val rvm: RatingViewModel = viewModel()
                    val svm: ShoppingViewModel = viewModel()
                    FoodDetailScreen(
                        foodId = detailFoodId,
                        onBack = { detailFoodId = -1L },
                        onStartStepMode = { id -> detailFoodId = -1L; stepModeFoodId = id },
                        ratingViewModel = rvm,
                        onAddToShoppingList = { id, ingredients ->
                            svm.addIngredients(id, ingredients)
                            detailFoodId = -1L
                        },
                        onShare = { food ->
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, food.name)
                                putExtra(Intent.EXTRA_TEXT, shareText(food))
                            }
                            context.startActivity(
                                Intent.createChooser(send, "اشتراک‌گذاری غذا"),
                            )
                        },
                    )
                }
                selectedTab == 0 -> {
                    val vm: HomeViewModel = viewModel()
                    HomeScreen(viewModel = vm)
                }
                selectedTab == 1 -> {
                    val vm: SearchViewModel = viewModel()
                    SearchScreen(
                        viewModel = vm,
                        onFoodClick = { detailFoodId = it },
                    )
                }
                selectedTab == 2 -> {
                    val fvm: FavoritesViewModel = viewModel()
                    val hvm: HistoryViewModel = viewModel()
                    FavoritesScreen(
                        favoritesViewModel = fvm,
                        historyViewModel = hvm,
                        onFoodClick = { detailFoodId = it },
                    )
                }
                selectedTab == 3 -> {
                    val vm: ShoppingViewModel = viewModel()
                    ShoppingListScreen(viewModel = vm)
                }
                selectedTab == 4 -> {
                    val vm: SettingsViewModel = viewModel()
                    SettingsScreen(
                        viewModel = vm,
                        onBackup = { backupLauncher.launch("tahdig-backup.db") },
                        onRestore = { restoreLauncher.launch(arrayOf("*/*")) },
                    )
                }
                selectedTab == 4 -> {
                    val vm: MealPlanViewModel = viewModel()
                    MealPlanScreen(
                        viewModel = vm,
                        onFoodClick = { detailFoodId = it },
                    )
                }
            }
        }
    }
}

private fun shareText(food: FoodEntity): String = buildString {
    appendLine("🍽 ${food.name}")
    if (food.nameEn.isNotBlank()) appendLine("(${food.nameEn})")
    appendLine()
    if (food.ingredients.isNotBlank()) {
        appendLine("مواد لازم:")
        appendLine(food.ingredients)
        appendLine()
    }
    if (food.description.isNotBlank()) {
        appendLine(food.description)
        appendLine()
    }
    append("پیشنهاد از اپلیکیشن ته‌دیگ 🍚")
}
