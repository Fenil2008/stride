package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.ProfileSetupScreen
import com.example.ui.explore.ExploreScreen
import com.example.ui.feed.ActivityDetailScreen
import com.example.ui.feed.FeedScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.navigation.LiquidGlassBottomBar
import com.example.ui.navigation.Screen
import com.example.ui.profile.ProfileScreen
import com.example.ui.record.PostActivityScreen
import com.example.ui.record.RecordScreen
import com.example.ui.theme.StrideTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val isLoggedIn = intent.getBooleanExtra(EXTRA_IS_LOGGED_IN, false)
        val openRecordTab = intent.getBooleanExtra("open_record_tab", false)

        val app = application as StrideApp
        val strideRepository = app.strideRepository
        val authRepository = app.authRepository

        setContent {
            StrideTheme {
                StrideMainApp(
                    isLoggedIn = isLoggedIn,
                    openRecordTab = openRecordTab,
                    strideRepository = strideRepository,
                    authRepository = authRepository
                )
            }
        }
    }

    companion object {
        const val EXTRA_IS_LOGGED_IN = "extra_is_logged_in"
    }
}

@Composable
fun StrideMainApp(
    isLoggedIn: Boolean,
    openRecordTab: Boolean,
    strideRepository: com.example.data.repository.StrideRepository,
    authRepository: com.example.data.repository.AuthRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Temporary post-workout passing variables
    var pendingActivityType by remember { mutableStateOf("RUN") }
    var pendingDistanceMeters by remember { mutableDoubleStateOf(0.0) }
    var pendingDurationSeconds by remember { mutableLongStateOf(0L) }
    var pendingAvgPace by remember { mutableDoubleStateOf(0.0) }
    var pendingCalories by remember { mutableIntStateOf(0) }
    var pendingPolylineJson by remember { mutableStateOf("[]") }

    val startDestination = remember {
        when {
            openRecordTab -> Screen.Record.route
            isLoggedIn -> Screen.Feed.route
            else -> Screen.Auth.route
        }
    }

    val isMainTab = currentRoute in listOf(
        Screen.Feed.route,
        Screen.Explore.route,
        Screen.Record.route,
        Screen.History.route,
        Screen.Profile.route
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Liquid glass bottom bar is floating and overlaid
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(350)) + slideInHorizontally(animationSpec = tween(350), initialOffsetX = { 80 }) },
                exitTransition = { fadeOut(animationSpec = tween(250)) },
                popEnterTransition = { fadeIn(animationSpec = tween(350)) },
                popExitTransition = { fadeOut(animationSpec = tween(250)) + slideOutHorizontally(animationSpec = tween(250), targetOffsetX = { 80 }) }
            ) {
                // Auth Screen
                composable(Screen.Auth.route) {
                    AuthScreen(
                        authRepository = authRepository,
                        onAuthSuccess = { isNewUser ->
                            if (isNewUser) {
                                navController.navigate(Screen.ProfileSetup.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Feed.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // Profile Setup Screen
                composable(Screen.ProfileSetup.route) {
                    ProfileSetupScreen(
                        authRepository = authRepository,
                        onSetupComplete = {
                            navController.navigate(Screen.Feed.route) {
                                popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Home Feed Screen
                composable(Screen.Feed.route) {
                    FeedScreen(
                        strideRepository = strideRepository,
                        authRepository = authRepository,
                        onActivityClick = { activityId ->
                            navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                        }
                    )
                }

                // Explore Screen
                composable(Screen.Explore.route) {
                    ExploreScreen(
                        strideRepository = strideRepository,
                        authRepository = authRepository,
                        onActivityClick = { activityId ->
                            navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                        },
                        onNavigateToRecord = {
                            navController.navigate(Screen.Record.route)
                        }
                    )
                }

                // Record Screen
                composable(Screen.Record.route) {
                    RecordScreen(
                        onFinishWorkout = { type, dist, duration, pace, cals, polyline ->
                            pendingActivityType = type
                            pendingDistanceMeters = dist
                            pendingDurationSeconds = duration
                            pendingAvgPace = pace
                            pendingCalories = cals
                            pendingPolylineJson = polyline
                            navController.navigate(Screen.PostActivity.route)
                        }
                    )
                }

                // History Screen
                composable(Screen.History.route) {
                    HistoryScreen(
                        strideRepository = strideRepository,
                        onActivityClick = { activityId ->
                            navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                        }
                    )
                }

                // Profile Screen
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        authRepository = authRepository,
                        strideRepository = strideRepository,
                        onActivityClick = { activityId ->
                            navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                        },
                        onLogout = {
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // Post-Activity Summary Screen
                composable(Screen.PostActivity.route) {
                    PostActivityScreen(
                        activityType = pendingActivityType,
                        distanceMeters = pendingDistanceMeters,
                        durationSeconds = pendingDurationSeconds,
                        avgPaceSecPerKm = pendingAvgPace,
                        calories = pendingCalories,
                        polylineJson = pendingPolylineJson,
                        strideRepository = strideRepository,
                        authRepository = authRepository,
                        onSavedOrDiscarded = {
                            navController.navigate(Screen.Feed.route) {
                                popUpTo(Screen.Feed.route) { inclusive = false }
                            }
                        }
                    )
                }

                // Activity Detail Screen
                composable(
                    route = Screen.ActivityDetail.route,
                    arguments = listOf(navArgument("activityId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
                    ActivityDetailScreen(
                        activityId = activityId,
                        strideRepository = strideRepository,
                        authRepository = authRepository,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // Overlay Liquid Glass Floating Bottom Navigation Bar
        AnimatedVisibility(
            visible = isMainTab,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LiquidGlassBottomBar(
                currentRoute = currentRoute,
                onNavigate = { targetScreen ->
                    if (currentRoute != targetScreen.route) {
                        navController.navigate(targetScreen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
