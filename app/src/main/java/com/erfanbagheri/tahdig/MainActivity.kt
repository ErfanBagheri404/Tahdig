package com.erfanbagheri.tahdig

import android.content.Intent
import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.ui.screen.BarcodeScreen
import com.erfanbagheri.tahdig.ui.screen.CategoryBrowseScreen
import com.erfanbagheri.tahdig.ui.screen.CategoryDishesScreen
import com.erfanbagheri.tahdig.ui.screen.FavoritesScreen
import com.erfanbagheri.tahdig.ui.screen.FoodDetailScreen
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.screen.HomeScreen
import com.erfanbagheri.tahdig.ui.screen.LeftoverScreen
import androidx.compose.ui.Alignment
import com.erfanbagheri.tahdig.ui.components.UndoSnackbarHost
import com.erfanbagheri.tahdig.ui.screen.OnboardingPager
import com.erfanbagheri.tahdig.ui.screen.PantryScreen
import com.erfanbagheri.tahdig.ui.screen.SearchScreen
import com.erfanbagheri.tahdig.ui.screen.CookHeatmapScreen
import com.erfanbagheri.tahdig.ui.screen.DiaryScreen
import com.erfanbagheri.tahdig.ui.screen.BadgesScreen
import com.erfanbagheri.tahdig.ui.screen.SettingsScreen
import com.erfanbagheri.tahdig.ui.screen.StepModeScreen
import com.erfanbagheri.tahdig.ui.screen.ShoppingListScreen
import com.erfanbagheri.tahdig.ui.screen.OccasionsScreen
import com.erfanbagheri.tahdig.ui.screen.CuisineMapScreen
import com.erfanbagheri.tahdig.ui.screen.RegionDishesScreen
import com.erfanbagheri.tahdig.ui.screen.TechniqueDetailScreen
import com.erfanbagheri.tahdig.ui.screen.TechniquesScreen
import com.erfanbagheri.tahdig.ui.theme.TahdigTheme
import com.erfanbagheri.tahdig.ui.screen.MealPlanScreen
import com.erfanbagheri.tahdig.ui.viewmodel.MealPlanViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.FavoritesViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.CategoryViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.HistoryViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.HomeViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.PantryViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.RatingViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.SearchViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.SettingsViewModel
import com.erfanbagheri.tahdig.util.BackupRestore
import com.erfanbagheri.tahdig.util.ShareCard
import com.erfanbagheri.tahdig.ui.viewmodel.ShoppingViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.LeftoverViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.CookHeatmapViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        /** Intent extra consumed by [onCreate]/[onNewIntent] to route into a screen. */
        const val EXTRA_DESTINATION = "tahdig.destination"
        const val DEST_JOURNAL_PHOTO = "journal_photo"

        /** Widget tap (#131): open one dish's detail straight away. */
        const val DEST_DETAIL = "detail"
        const val EXTRA_FOOD_ID = "tahdig.food_id"
    }

    /** Set when a photo-prompt notification fires while the app is already alive. */
    private var pendingJournalAttach = false

    /** Set when the widget asks for one dish's detail (#131). */
    private var pendingDetailId by mutableStateOf<Long?>(null)

    private fun consumeDestination(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_DESTINATION)) {
            DEST_JOURNAL_PHOTO -> pendingJournalAttach = true
            DEST_DETAIL -> {
                val id = intent.getLongExtra(EXTRA_FOOD_ID, -1L)
                if (id > 0) pendingDetailId = id
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeDestination(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeDestination(intent)

        lifecycleScope.launch {
            TahdigDatabase.populateIfEmpty(this@MainActivity)
        }

        setContent {
            // Farsi-only app: force RTL regardless of device locale.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TahdigTheme {
                    TahdigApp(
                        startAttachPhoto = pendingJournalAttach,
                        onAttachHandled = { pendingJournalAttach = false },
                        initialDetailId = pendingDetailId,
                        onDetailHandled = { pendingDetailId = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun TahdigApp(
    startAttachPhoto: Boolean = false,
    onAttachHandled: () -> Unit = {},
    initialDetailId: Long? = null,
    onDetailHandled: () -> Unit = {},
) {
    // Tab 2 is Favorites/History where the journal tab lives.
    var selectedTab by rememberSaveable { mutableIntStateOf(if (startAttachPhoto) 2 else 0) }
    androidx.compose.runtime.LaunchedEffect(startAttachPhoto) {
        if (startAttachPhoto) {
            selectedTab = 2
            onAttachHandled()
        }
    }
    var detailFoodId by rememberSaveable { mutableLongStateOf(-1L) }
    // Widget tap (#131) opens a dish straight from the home screen.
    androidx.compose.runtime.LaunchedEffect(initialDetailId) {
        if (initialDetailId != null && initialDetailId > 0) {
            detailFoodId = initialDetailId
            onDetailHandled()
        }
    }
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
        // #126: onboarding's last stop opens a real seeded dish, so the first
        // run ends with a dish on screen instead of an empty home.
        OnboardingPager(
            onDone = { SettingsStore.setOnboarded() },
            onOpenDish = { id ->
                SettingsStore.setOnboarded()
                SettingsStore.setSamplePick(id)
                detailFoodId = id
            },
        )
        return
    }
    var stepModeFoodId by rememberSaveable { mutableLongStateOf(-1L) }
    // Resume-vs-fresh for the mode above (#93); lives across config changes.
    var stepModeResume by rememberSaveable { mutableStateOf(false) }
    var categoryRoute by rememberSaveable { mutableLongStateOf(-1L) }
    var browseCategories by rememberSaveable { mutableStateOf(false) }
    var showPantry by rememberSaveable { mutableStateOf(false) }
    var showLeftover by rememberSaveable { mutableStateOf(false) }
    var showScanner by rememberSaveable { mutableStateOf(false) }
    var showHeatmap by rememberSaveable { mutableStateOf(false) }
    var showDiary by rememberSaveable { mutableStateOf(false) }
    var showBadges by rememberSaveable { mutableStateOf(false) }
    var browseTechniques by rememberSaveable { mutableStateOf(false) }
    var browseOccasions by rememberSaveable { mutableStateOf(false) }
    var browseCuisineMap by rememberSaveable { mutableStateOf(false) }
    /** Region opened from the map (#90) — "" = map list, else the region key. */
    var regionRoute by rememberSaveable { mutableStateOf("") }
    // Technique opened from a step: back must restore the exact step, so the step
    // screen stays on the back stack and this only overlays the technique page.
    var techniqueRoute by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Fullscreen cook-along (#93): no bottom nav while the mode owns the screen.
            if (detailFoodId < 0 && stepModeFoodId < 0) {
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
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        label = { Text("برنامه") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("تنظیمات") },
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
        Box(Modifier.fillMaxSize()) {
        // #127: one undo host for the whole app. Any ViewModel arms UndoHub
        // and the snackbar appears here, above every screen.
        UndoSnackbarHost(modifier = Modifier.align(Alignment.BottomCenter))
        when {
                stepModeFoodId >= 0 -> {
                    // Captured at composition: onCooked runs outside composable context (#98).
                    val homeVm: HomeViewModel = viewModel()
                    StepModeScreen(
                        foodId = stepModeFoodId,
                        onBack = { stepModeFoodId = -1L; stepModeResume = false },
                        resume = stepModeResume,
                        // A technique overlay owns back while open (#101).
                        backEnabled = techniqueRoute.isEmpty(),
                        onTechnique = { techniqueRoute = it },
                        // Done state (#98): rating lands on detail; «پختم» → leftovers + Home.
                        onRate = {
                            detailFoodId = stepModeFoodId
                            stepModeFoodId = -1L
                            stepModeResume = false
                        },
                        onCooked = { f ->
                            homeVm.showLeftoversFor(f)
                            // Journal stamp (#124) — same event as the history
                            // and nutrition rows, and the prompt rides Home.
                            homeVm.stampCook(f.id)
                            // Streak line on the widget moved (#131).
                            com.erfanbagheri.tahdig.widget.DishOfDayWidget.refreshAll(context)
                            detailFoodId = -1L
                            stepModeFoodId = -1L
                            stepModeResume = false
                            selectedTab = 0
                        },
                    )
                }
                detailFoodId >= 0 -> {
                    val rvm: RatingViewModel = viewModel()
                    val svm: ShoppingViewModel = viewModel()
                    val mvm: com.erfanbagheri.tahdig.ui.viewmodel.MilestoneViewModel = viewModel()
                    FoodDetailScreen(
                        foodId = detailFoodId,
                        onBack = { detailFoodId = -1L },
                        onStartStepMode = { id -> detailFoodId = -1L; stepModeResume = false; stepModeFoodId = id },
                        onResumeStepMode = { id -> detailFoodId = -1L; stepModeResume = true; stepModeFoodId = id },
                        ratingViewModel = rvm,
                        milestoneViewModel = mvm,
                        onAddToShoppingList = { id, ingredients ->
                            svm.addIngredients(id, ingredients)
                            detailFoodId = -1L
                        },
                        onAddMissing = { missing ->
                            // Same dish context: only the gap goes on the list.
                            svm.addIngredients(detailFoodId, missing)
                            detailFoodId = -1L
                        },
                        onShare = { food ->
                            ShareCard.share(context, food)
                        },
                    )
                }
                browseCategories -> {
                    val cm = viewModel<CategoryViewModel>()
                    CategoryBrowseScreen(
                        viewModel = cm,
                        onCategoryClick = { categoryRoute = it },
                        onBack = { browseCategories = false },
                        onTechniques = { browseTechniques = true },
                        onOccasions = { browseOccasions = true },
                        onCuisineMap = { browseCuisineMap = true },
                    )
                }
                showPantry -> {
                    val vm: PantryViewModel = viewModel()
                    val svm: ShoppingViewModel = viewModel()
                    PantryScreen(
                        viewModel = vm,
                        onFoodClick = { detailFoodId = it },
                        onBack = { showPantry = false },
                        onOpenLeftover = { showPantry = false; showLeftover = true },
                        // Swipe on an expiring row (#106): the replacement goes on
                        // the shopping list, the stale row leaves the pantry.
                        onAddToShopping = { item -> svm.addItems(item.item) },
                    )
                }
                showLeftover -> {
                    // #107: single-shot session — closing forgets it (VM has no persistence).
                    val lvm: LeftoverViewModel = viewModel()
                    LeftoverScreen(
                        viewModel = lvm,
                        onFoodClick = { detailFoodId = it },
                        onBack = { showLeftover = false },
                    )
                }
                showScanner -> {
                    // #116: a scanned product is added by NAME — it is not one of
                    // the seed dishes, so it has no foodId to add by.
                    val bvm: com.erfanbagheri.tahdig.ui.viewmodel.BarcodeViewModel = viewModel()
                    val svm: ShoppingViewModel = viewModel()
                    val pvm: PantryViewModel = viewModel()
                    BarcodeScreen(
                        viewModel = bvm,
                        onAddToShopping = { name -> svm.addItems(name) },
                        onAddToPantry = { name -> pvm.addItem(name) },
                        onBack = { showScanner = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                showHeatmap -> {
                    val vm: CookHeatmapViewModel = viewModel()
                    CookHeatmapScreen(
                        viewModel = vm,
                        onBack = { showHeatmap = false },
                    )
                }
                showDiary -> {
                    val vm: com.erfanbagheri.tahdig.ui.viewmodel.DiaryViewModel = viewModel()
                    DiaryScreen(vm = vm, onBack = { showDiary = false })
                }
                showBadges -> {
                    val bvm: com.erfanbagheri.tahdig.ui.viewmodel.BadgeViewModel = viewModel()
                    BadgesScreen(vm = bvm, onBack = { showBadges = false })
                }
                categoryRoute >= 0 -> {
                    val cm = viewModel<CategoryViewModel>()
                    CategoryDishesScreen(
                        viewModel = cm,
                        categoryId = categoryRoute,
                        categoryName = "",
                        onFoodClick = { detailFoodId = it },
                        onBack = { categoryRoute = -1L },
                    )
                }
                selectedTab == 0 -> {
                    val vm: HomeViewModel = viewModel()
                    val wvm: com.erfanbagheri.tahdig.ui.viewmodel.WellnessViewModel = viewModel()
                    val cvm: com.erfanbagheri.tahdig.ui.viewmodel.CaffeineViewModel = viewModel()
                    val bvm: com.erfanbagheri.tahdig.ui.viewmodel.BadgeViewModel = viewModel()
                    HomeScreen(
                        viewModel = vm,
                        wellness = wvm,
                        caffeine = cvm,
                        badges = bvm,
                        onBrowseCategories = { browseCategories = true },
                        onFoodClick = { detailFoodId = it },
                        onOpenLeftover = { showLeftover = true },
                        onOpenPantry = { showPantry = true },
                    )
                }
                selectedTab == 1 -> {
                    val vm: SearchViewModel = viewModel()
                    SearchScreen(
                        viewModel = vm,
                        onFoodClick = { detailFoodId = it },
                        onOpenPantry = { showPantry = true },
                        onOpenScanner = { showScanner = true },
                    )
                }
                selectedTab == 2 -> {
                    val fvm: FavoritesViewModel = viewModel()
                    val hvm: HistoryViewModel = viewModel()
                    val jvm: com.erfanbagheri.tahdig.ui.viewmodel.JournalViewModel = viewModel()
                    FavoritesScreen(
                        favoritesViewModel = fvm,
                        historyViewModel = hvm,
                        journalViewModel = jvm,
                        onFoodClick = { detailFoodId = it },
                        // #126: every ghost-row CTA needs a way out of the
                        // empty list it was shown in.
                        onGoHome = { selectedTab = 0 },
                        startInJournal = startAttachPhoto,
                        onAttachHandled = onAttachHandled,
                    )
                }
                selectedTab == 3 -> {
                    val vm: ShoppingViewModel = viewModel()
                    ShoppingListScreen(viewModel = vm, onGoHome = { selectedTab = 0 })
                }
                selectedTab == 4 -> {
                    val vm: MealPlanViewModel = viewModel()
                    val svm: ShoppingViewModel = viewModel()
                    MealPlanScreen(
                        viewModel = vm,
                        onFoodClick = { detailFoodId = it },
                        onAddPlanToShopping = { svm.addPlanIngredients() },
                    )
                }
                selectedTab == 5 -> {
                    val vm: SettingsViewModel = viewModel()
                    SettingsScreen(
                        viewModel = vm,
                        onBackup = { backupLauncher.launch("tahdig-backup.db") },
                        onRestore = { restoreLauncher.launch(arrayOf("*/*")) },
                        onOpenHeatmap = { showHeatmap = true },
                        onOpenDiary = { showDiary = true },
                        onOpenBadges = { showBadges = true },
                    )
                }
            }
            // Technique library overlays (#101) — stacked ABOVE the current screen:
            // a technique opened from a step never disposes the step screen, so
            // back lands on the exact step again (acceptance criterion).
            // #129: the hand-rolled `when` nav ignores the system back key —
            // without this, pressing back on a detail screen EXITS the app.
            // This pops in reverse overlay order (mirrors the close buttons),
            // and no-ops on tabs so the system still exits from Home.
            val backOpen = detailFoodId >= 0 ||
                (stepModeFoodId < 0 && (
                techniqueRoute.isNotEmpty() || regionRoute.isNotEmpty()
                    || browseOccasions || browseCuisineMap || browseTechniques
                    || categoryRoute >= 0 || browseCategories || showPantry
                    || showLeftover || showScanner || showHeatmap || showDiary
                    || showBadges
                    )
                )
            androidx.activity.compose.BackHandler(enabled = backOpen) {
                when {
                    techniqueRoute.isNotEmpty() -> techniqueRoute = ""
                    regionRoute.isNotEmpty() -> regionRoute = ""
                    browseOccasions -> browseOccasions = false
                    browseCuisineMap -> browseCuisineMap = false
                    browseTechniques -> browseTechniques = false
                    categoryRoute >= 0 -> categoryRoute = -1L
                    detailFoodId >= 0 -> detailFoodId = -1L
                    browseCategories -> browseCategories = false
                    showPantry -> showPantry = false
                    showLeftover -> showLeftover = false
                    showScanner -> showScanner = false
                    showHeatmap -> showHeatmap = false
                    showDiary -> showDiary = false
                    showBadges -> showBadges = false
                }
            }
            if (techniqueRoute.isNotEmpty()) {
                TechniqueDetailScreen(
                    techniqueId = techniqueRoute,
                    onBack = { techniqueRoute = "" },
                    onDishClick = { id ->
                        techniqueRoute = ""
                        browseTechniques = false
                        detailFoodId = id
                    },
                )
            }
            if (browseOccasions) {
                OccasionsScreen(
                    onBack = { browseOccasions = false },
                    onFoodClick = { id -> browseOccasions = false; detailFoodId = id },
                )
            }
            // Cuisine map (#90): region detail overlays the map so back returns to it.
            if (browseCuisineMap && regionRoute.isEmpty()) {
                CuisineMapScreen(
                    onBack = { browseCuisineMap = false },
                    onRegionClick = { regionRoute = it },
                )
            }
            if (browseCuisineMap && regionRoute.isNotEmpty()) {
                RegionDishesScreen(
                    cuisine = regionRoute,
                    onFoodClick = { id -> regionRoute = ""; browseCuisineMap = false; detailFoodId = id },
                    onBack = { regionRoute = "" },
                )
            }
            if (browseTechniques && techniqueRoute.isEmpty()) {
                TechniquesScreen(
                    onBack = { browseTechniques = false },
                    onOpen = { techniqueRoute = it },
                )
            }
            } // Box (base screen + technique overlays)
        }
    }
}
