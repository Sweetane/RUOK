package com.example.privatecheck

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.privatecheck.data.DataStoreRepository
import com.example.privatecheck.logic.CheckInManager
import com.example.privatecheck.ui.theme.CheckInGreen
import com.example.privatecheck.ui.theme.PrivateCheckTheme
import com.example.privatecheck.ui.theme.WarningRed
import com.example.privatecheck.ui.WidgetPreviewScreen // Add Import
import com.example.privatecheck.ui.ConfettiOverlay // Add Import
import com.example.privatecheck.ui.CalendarScreen
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.collectIsPressedAsState // Fix
import androidx.compose.ui.graphics.graphicsLayer // Fix

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = DataStoreRepository(applicationContext)
        val manager = CheckInManager(repository)

        // Schedule Worker
        // Schedule Background Workers
        com.example.privatecheck.worker.WorkerScheduler.scheduleAllworkers(applicationContext)

        // Trigger Security Migration (Migrate legacy password to EncryptedPrefs)
        lifecycleScope.launch {
            repository.performSecurityMigration()
        }

        val isSystemDark = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && 
                           (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES)
                           
        // Explicitly set transparent system bars for Edge-to-Edge to work effectively
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        // Android 13+ Notification Permission Request
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val launcher = registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                   if (isGranted) {
                       // Permission granted
                   }
                }
                launcher.launch(permission)
            }
        }

        setContent {
            // Fix 1: Theme Flash
            // Use nullable Boolean to detect "Loading" state.
            val isDarkState by repository.isDarkMode.collectAsState(initial = null)
            val appThemeColor by repository.appThemeColor.collectAsState(initial = null)
            
            // Only render content once the preference is actually loaded
            if (isDarkState != null && appThemeColor != null) {
                // Optimistic Override State
                var isDarkOverride by remember { mutableStateOf<Boolean?>(null) }
                val isDark = isDarkOverride ?: isDarkState!!
                
                // Sync override with source of truth when it catches up to avoid desync
                LaunchedEffect(isDarkState) {
                    if (isDarkState == isDarkOverride) {
                        isDarkOverride = null
                    }
                }

                val themeColor = Color(appThemeColor!!)
                
                // Disable dynamicColor to ensure our custom high-contrast colors are used
                PrivateCheckTheme(
                    darkTheme = isDark, 
                    dynamicColor = false,
                    customPrimary = themeColor
                ) {
                    // Now inside Theme, MaterialTheme.colorScheme reflects isDark preference
                    val animatedBgColor by animateColorAsState(
                        targetValue = MaterialTheme.colorScheme.background,
                        animationSpec = tween(durationMillis = 200), // Speed up to 200ms
                        label = "ThemeTransition"
                    )
                
                // Removed Manual SideEffect for Status Bar Color
                // enableEdgeToEdge() makes it transparent, allowing Scaffold background to show through perfectly synced.
                
                // Advanced Status Bar Control
                val view = androidx.compose.ui.platform.LocalView.current
                if (!view.isInEditMode) {
                    // 1. Force Transparent & Sync Icons (Double Safety)
                    // MOVED TO SideEffect: critical for synchronous update with the frame!
                    // LaunchedEffect was causing a 1-frame lag.
                    SideEffect {
                        val window = (view.context as android.app.Activity).window
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        
                        // Immediately update the status bar icon color to match the new theme
                        // isDark = true -> light status bar items = false (White icons)
                        // isDark = false -> light status bar items = true (Black icons)
                        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                    }
                }
    
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = animatedBgColor // Animated transition
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController, 
                        startDestination = "home",
                        enterTransition = { androidx.compose.animation.fadeIn(animationSpec = tween(300)) },
                        exitTransition = { androidx.compose.animation.fadeOut(animationSpec = tween(300)) },
                        popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = tween(300)) },
                        popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = tween(300)) }
                    ) {
                        composable("home") {
                            HomeScreen(
                                manager = manager, 
                                repository = repository,
                                isDark = isDark,
                                backgroundColor = animatedBgColor, // Pass Color!
                                onToggleTheme = { 
                                    // Optimistic Update
                                    isDarkOverride = !isDark
                                    lifecycleScope.launch { repository.toggleDarkMode(!isDark) } 
                                },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToCalendar = { navController.navigate("calendar") }
                            )
                        }
                        composable("settings") {
                            com.example.privatecheck.ui.SettingsScreen(
                                repository = repository,
                                backgroundColor = animatedBgColor,
                                onBack = { navController.popBackStack() },
                                onNavigateToPreview = { uri -> 
                                    // Encode URI to be safe in route
                                    val encoded = java.net.URLEncoder.encode(uri, "UTF-8")
                                    navController.navigate("preview_screen/$encoded")
                                }
                            )
                        }
                        composable("calendar") {
                            CalendarScreen(
                                repository = repository,
                                backgroundColor = animatedBgColor,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "preview_screen/{imageUri}",
                            arguments = listOf(androidx.navigation.navArgument("imageUri") { type = androidx.navigation.NavType.StringType })
                        ) { backStackEntry ->
                            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                            val decodedUri = java.net.URLDecoder.decode(encodedUri, "UTF-8")
                            
                            WidgetPreviewScreen(
                                imageUri = decodedUri,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onSave = { 
                                    // Return to Settings after save
                                    navController.popBackStack() 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    manager: CheckInManager, 
    repository: DataStoreRepository,
    isDark: Boolean,
    backgroundColor: Color, // Accept color
    onToggleTheme: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ... State ... (streak, isCheckedIn, currentTime)
    val streak by repository.streakDays.collectAsState(initial = 0)
    var isCheckedIn by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) } // Confetti Trigger
    var currentTime by remember { mutableStateOf("") }
    
    // Animation State
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "ButtonScale"
    )

    // Initial check
    LaunchedEffect(Unit) {
        isCheckedIn = manager.isCheckedInToday()
    }

    // Time update loop
    LaunchedEffect(Unit) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        while (true) {
            currentTime = LocalDateTime.now().format(formatter)
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = backgroundColor, // Use animated color
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "私了吗", 
                        // Fix 2: Ensure Title Color is visible (not gray) in Dark Mode
                        color = MaterialTheme.colorScheme.onSurface 
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor
                ),
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    // Dark Mode Toggle
                    IconButton(onClick = onToggleTheme) {
                         Text(text = if(isDark) "🌙" else "🌞", fontSize = 20.sp)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Warning
            Text(
                text = "如果连续两天没打卡，就会发送邮件给紧急联系人",
                color = WarningRed,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Check-in Button
            Button(
                onClick = {
                    scope.launch {
                        manager.performCheckIn(context)
                        isCheckedIn = true
                        showConfetti = true // Trigger Celebration
                        Toast.makeText(context, "打卡成功！", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isCheckedIn,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CheckInGreen,
                    contentColor = Color.Black, // Force Black Text in Light/Dark modes
                    disabledContainerColor = Color.Gray
                ),
                interactionSource = interactionSource,
                modifier = Modifier
                    .size(width = 200.dp, height = 100.dp)
                    .padding(top = 32.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Text(
                    text = if (isCheckedIn) "你好就行" else "我还好",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Streak Info
            Text(
                text = "已连续打卡 $streak 天",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface // Fix: Ensure visibility in Dark Mode
            )
            
            // Removed Encouragement Text ("加油！保持连胜") as per request

            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Clock
            Text(
                text = currentTime,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(bottom = 48.dp) // Lift up slightly from edge
            )
        }
    }

    // Confetti Overlay
    if (showConfetti) {
        ConfettiOverlay(
            onFinished = { showConfetti = false }
        )
    }
    }
}
