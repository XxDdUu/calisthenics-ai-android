package com.calisthenics.ai.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.calisthenics.ai.R
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.network.OfflineAccessPolicy
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.ui.components.GenderOnboardingDialog
import com.calisthenics.ai.ui.components.OfflineBanner
import com.calisthenics.ai.ui.workoutsession.bubble.UnfinishedWorkoutBubbleHost
import com.calisthenics.ai.ui.analysis.AnalysisResultScreen
import com.calisthenics.ai.ui.admin.AdminGettingStartedProgramScreen
import com.calisthenics.ai.ui.admin.AdminAiAgentScreen
import com.calisthenics.ai.ui.admin.AdminDashboardScreen
import com.calisthenics.ai.ui.admin.AdminExerciseFormScreen
import com.calisthenics.ai.ui.admin.AdminExercisesScreen
import com.calisthenics.ai.ui.admin.AdminNavigationCallbacks
import com.calisthenics.ai.ui.admin.AdminReviewDetailScreen
import com.calisthenics.ai.ui.admin.AdminReviewsScreen
import com.calisthenics.ai.ui.admin.AdminUserDetailScreen
import com.calisthenics.ai.ui.admin.AdminUserReportDetailScreen
import com.calisthenics.ai.ui.admin.AdminUserReportsScreen
import com.calisthenics.ai.ui.admin.AdminUsersScreen
import com.calisthenics.ai.ui.admin.AdminViewModel
import com.calisthenics.ai.ui.admin.AdminWorkoutPlanDetailScreen
import com.calisthenics.ai.ui.admin.AdminWorkoutPlansScreen
import com.calisthenics.ai.ui.admin.AdminWorkoutSessionDetailScreen
import com.calisthenics.ai.ui.admin.AdminWorkoutSessionsScreen
import com.calisthenics.ai.ui.auth.AuthScreen
import com.calisthenics.ai.ui.auth.AuthViewModel
import com.calisthenics.ai.ui.auth.ForgotPasswordScreen
import com.calisthenics.ai.ui.auth.OtpVerificationScreen
import com.calisthenics.ai.ui.auth.PasswordResetViewModel
import com.calisthenics.ai.ui.auth.RegisterOtpVerificationScreen
import com.calisthenics.ai.ui.auth.ResetPasswordScreen
import com.calisthenics.ai.ui.chat.conversation.ChatConversationScreen
import com.calisthenics.ai.ui.chat.list.ChatListScreen
import com.calisthenics.ai.ui.chat.ai.AiChatScreen
import com.calisthenics.ai.ui.coach.ClientProgressScreen
import com.calisthenics.ai.ui.coach.CoachConnectionsScreen
import com.calisthenics.ai.ui.coach.CoachConnectionsViewModel
import com.calisthenics.ai.ui.coach.CoachProfileScreen
import com.calisthenics.ai.ui.exercise.ExerciseProgressScreen
import com.calisthenics.ai.ui.exercise.ExerciseSelectionScreen
import com.calisthenics.ai.ui.liveanalysis.LiveAnalysisHistoryDetailScreen
import com.calisthenics.ai.ui.liveanalysis.LiveAnalysisHistoryScreen
import com.calisthenics.ai.ui.liveanalysis.LiveAnalysisScreen
import com.calisthenics.ai.ui.liveanalysis.WorkoutAnalysisSummaryScreen
import com.calisthenics.ai.ui.main.HomeDashboard
import com.calisthenics.ai.ui.notifications.NotificationNavigationTarget
import com.calisthenics.ai.ui.notifications.NotificationsScreen
import com.calisthenics.ai.ui.bodymetrics.BodyMetricsScreen
import com.calisthenics.ai.ui.pr.PersonalRecordsScreen
import com.calisthenics.ai.ui.profile.ClientDetailScreen
import com.calisthenics.ai.ui.profile.GenderGateViewModel
import com.calisthenics.ai.ui.profile.ProfileScreen
import com.calisthenics.ai.ui.progression.ProgressionSelectionScreen
import com.calisthenics.ai.ui.settings.SettingsScreen
import com.calisthenics.ai.ui.training.TrainingAnalyticsScreen
import com.calisthenics.ai.ui.video.VideoSelectionScreen
import com.calisthenics.ai.ui.workoutplan.ai.AiWorkoutPlannerScreen
import com.calisthenics.ai.ui.workoutplan.details.WorkoutPlanDetailsScreen
import com.calisthenics.ai.ui.workoutplan.list.WorkoutPlanListScreen
import com.calisthenics.ai.ui.workoutsession.WorkoutHistoryScreen
import com.calisthenics.ai.ui.workoutsession.WorkoutSessionScreen
import com.calisthenics.ai.ui.workoutsession.WorkoutSummaryScreen
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object VerifyRegisterOtp : Screen("auth/verify-register-otp")
    data object PasswordResetGraph : Screen("password_reset")
    data object ForgotPassword : Screen("password_reset/forgot")
    data object VerifyPasswordOtp : Screen("password_reset/verify")
    data object ResetPassword : Screen("password_reset/reset")
    data object Home : Screen("home")
    data object AuthenticatedEntry : Screen("authenticated_entry")
    data object AdminDashboard : Screen("admin/dashboard")
    data object AdminAiAgent : Screen("admin/ai-agent")
    data object AdminGettingStartedProgram : Screen("admin/getting-started-program")
    data object AdminUsers : Screen("admin/users")
    data object AdminUserDetails : Screen("admin/users/{userId}") {
        fun createRoute(userId: Long) = "admin/users/$userId"
    }
    data object AdminExercises : Screen("admin/exercises")
    data object AdminCreateExercise : Screen("admin/exercises/create")
    data object AdminEditExercise : Screen("admin/exercises/{exerciseId}/edit") {
        fun createRoute(exerciseId: Long) = "admin/exercises/$exerciseId/edit"
    }
    data object AdminWorkoutPlans : Screen("admin/workout-plans")
    data object AdminWorkoutPlanDetails : Screen("admin/workout-plans/{planId}") {
        fun createRoute(planId: Long) = "admin/workout-plans/$planId"
    }
    data object AdminWorkoutSessions : Screen("admin/workout-sessions")
    data object AdminWorkoutSessionDetails : Screen("admin/workout-sessions/{sessionId}") {
        fun createRoute(sessionId: Long) = "admin/workout-sessions/$sessionId"
    }
    data object AdminReviews : Screen("admin/reviews")
    data object AdminReviewDetails : Screen("admin/reviews/{reviewId}") {
        fun createRoute(reviewId: Long) = "admin/reviews/$reviewId"
    }
    data object AdminUserReports : Screen("admin/user-reports")
    data object AdminUserReportDetails : Screen("admin/user-reports/{reportId}") {
        fun createRoute(reportId: Long) = "admin/user-reports/$reportId"
    }
    data object Training : Screen("training")
    data object Analysis : Screen("analysis")
    data object Coach : Screen("coach")
    data object CoachProfile : Screen("coach/{coachId}") {
        fun createRoute(coachId: Long) = "coach/$coachId"
    }
    data object ClientProfile : Screen("client/{clientId}") {
        fun createRoute(clientId: Long) = "client/$clientId"
    }
    data object Chat : Screen("chat")
    data object AiChat : Screen("ai_chat?conversationId={conversationId}") {
        fun createRoute(conversationId: Long? = null): String {
            return if (conversationId != null && conversationId > 0L) {
                "ai_chat?conversationId=$conversationId"
            } else {
                "ai_chat"
            }
        }
    }
    data object ChatConversation : Screen("chat/{conversationId}?otherUserName={otherUserName}&otherUserAvatarUrl={otherUserAvatarUrl}&otherUserId={otherUserId}") {
        fun createRoute(
            conversationId: Long,
            otherUserName: String = "Chat",
            otherUserAvatarUrl: String? = null,
            otherUserId: Long = 0L
        ): String {
            val encodedName = java.net.URLEncoder.encode(otherUserName, "UTF-8")
            val encodedAvatar = if (!otherUserAvatarUrl.isNullOrBlank()) {
                java.net.URLEncoder.encode(otherUserAvatarUrl, "UTF-8")
            } else {
                ""
            }
            return "chat/$conversationId?otherUserName=$encodedName&otherUserAvatarUrl=$encodedAvatar&otherUserId=$otherUserId"
        }
    }
    data object Profile : Screen("profile")
    data object SwitchAccount : Screen("switch_account")
    data object ExerciseSelection : Screen("exercise_selection")
    data object LiveAnalysis : Screen("live_analysis/{exerciseCode}") {
        fun createRoute(code: String) = "live_analysis/$code"
    }
    data object WorkoutPlanDetails : Screen("workout_plan_details/{planId}/{dayOfWeek}?startDate={startDate}") {
        fun createRoute(
            planId: Long,
            dayOfWeek: WorkoutDayOfWeek = WorkoutDayOfWeek.MONDAY,
            startDate: String? = null
        ) = if (startDate.isNullOrBlank()) {
            "workout_plan_details/$planId/${dayOfWeek.name}"
        } else {
            "workout_plan_details/$planId/${dayOfWeek.name}?startDate=${java.net.URLEncoder.encode(startDate, "UTF-8")}"
        }
    }
    data object WorkoutSession : Screen(
        "workout_session/{workoutId}/{date}/{planId}/{dayId}?sessionId={sessionId}&assignmentId={assignmentId}"
    ) {
        fun createRoute(
            workoutId: String,
            date: String,
            planId: Long = 0L,
            dayId: Long = 0L,
            sessionId: Long? = null,
            assignmentId: Long? = null
        ) = "workout_session/$workoutId/$date/$planId/$dayId" +
            listOfNotNull(
                sessionId?.let { "sessionId=$it" },
                assignmentId?.let { "assignmentId=$it" }
            ).takeIf { it.isNotEmpty() }?.joinToString("&", prefix = "?").orEmpty()
    }
    data object WorkoutHistory : Screen("workout_history")
    data object LiveAnalysisHistory : Screen("live_analysis_history")
    data object LiveAnalysisHistoryDetail : Screen("live_analysis_history/{sessionId}") {
        fun createRoute(sessionId: String) = "live_analysis_history/$sessionId"
    }
    data object WorkoutAnalysisSummary : Screen("workout_analysis_summary/{sessionId}") {
        fun createRoute(sessionId: String) = "workout_analysis_summary/$sessionId"
    }
    data object WorkoutSummary : Screen("workout_summary/{sessionId}") {
        fun createRoute(sessionId: Long) = "workout_summary/$sessionId"
    }
    data object ProgressionSelection : Screen("progression_selection/{exerciseCode}") {
        fun createRoute(code: String) = "progression_selection/$code"
    }
    data object VideoSelection : Screen("video_selection/{exerciseId}/{progressionName}?exerciseCode={exerciseCode}") {
        fun createRoute(id: Long, name: String, exerciseCode: String = ""): String {
            val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
            return if (exerciseCode.isNotBlank()) {
                val encodedCode = java.net.URLEncoder.encode(exerciseCode, "UTF-8")
                "video_selection/$id/$encodedName?exerciseCode=$encodedCode"
            } else {
                "video_selection/$id/$encodedName"
            }
        }
    }
    data object AnalysisResult : Screen("analysis_result/{analysisId}") {
        fun createRoute(id: Long) = "analysis_result/$id"
    }
    data object ExerciseProgress : Screen("exercise_progress/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "exercise_progress/$exerciseId"
    }
    data object TrainingAnalytics : Screen("training_analytics")
    data object PersonalRecords : Screen("personal_records")
    data object ClientProgress : Screen("coach/client_progress/{clientId}?initialTab={initialTab}") {
        fun createRoute(clientId: Long, initialTab: Int = 0) =
            "coach/client_progress/$clientId?initialTab=$initialTab"
    }
    data object BodyMetrics : Screen("body_metrics")
    data object Settings : Screen("settings")
    data object Notifications : Screen("notifications")
    data object AiWorkoutPlanner : Screen("ai_workout_planner?clientId={clientId}&clientName={clientName}") {
        fun createRoute(clientId: Long? = null, clientName: String? = null): String {
            val params = listOfNotNull(
                clientId?.let { "clientId=$it" },
                clientName?.let { "clientName=${java.net.URLEncoder.encode(it, "UTF-8")}" }
            )
            return if (params.isNotEmpty()) "ai_workout_planner?${params.joinToString("&")}" else "ai_workout_planner"
        }
    }
}

private const val AuthenticatedGraph = "authenticated"

internal fun authenticatedStartRoute(role: String?): String =
    if (role == "ROLE_ADMIN") Screen.AdminDashboard.route else Screen.Home.route

internal fun visibleBottomNavigationScreens(role: String?): List<Screen> {
    val existingScreens = listOf(
        Screen.Home,
        Screen.Training,
        Screen.Analysis,
        Screen.Coach,
        Screen.Chat,
        Screen.Profile
    )
    return if (role == "ROLE_ADMIN") existingScreens + Screen.AdminDashboard else existingScreens
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    sessionManager: com.calisthenics.ai.data.local.SessionManager? = null,
    connectivityMonitor: ConnectivityMonitor? = null,
    offlineAccessPolicy: OfflineAccessPolicy? = null,
    hasValidSession: () -> Boolean = { false }
) {
    val isOnline by (connectivityMonitor?.isOnline?.collectAsState(initial = connectivityMonitor.isConnected())
        ?: remember { mutableStateOf(true) })

    val topLevelRoutes = remember {
        setOf(
            Screen.Home.route,
            Screen.Training.route,
            Screen.Analysis.route,
            Screen.Coach.route,
            Screen.Chat.route,
            Screen.Profile.route
        )
    }

    val clearUserSessionNavigationState = remember(navController, topLevelRoutes) {
        {
            topLevelRoutes.forEach { route -> runCatching { navController.clearBackStack(route) } }
            runCatching { navController.clearBackStack(Screen.AdminDashboard.route) }
            runCatching { navController.clearBackStack(AuthenticatedGraph) }
        }
    }

    val performAdminLogout: () -> Unit = remember(navController, clearUserSessionNavigationState) {
        {
            clearUserSessionNavigationState()
            navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
        }
    }

    val adminNavCallbacks = remember(navController, performAdminLogout) {
        val navigateSection: (String) -> Unit = { route ->
            if (route == Screen.AdminDashboard.route) {
                navController.navigate(Screen.AdminDashboard.route) {
                    popUpTo(Screen.AdminDashboard.route) { inclusive = false }
                    launchSingleTop = true
                }
            } else {
                navController.navigate(route) {
                    popUpTo(Screen.AdminDashboard.route) { saveState = false }
                    launchSingleTop = true
                }
            }
        }
        AdminNavigationCallbacks(
            onOverview = { navigateSection(Screen.AdminDashboard.route) },
            onAiAgent = { navigateSection(Screen.AdminAiAgent.route) },
            onUsers = { navigateSection(Screen.AdminUsers.route) },
            onExercises = { navigateSection(Screen.AdminExercises.route) },
            onGettingStarted = { navigateSection(Screen.AdminGettingStartedProgram.route) },
            onWorkoutPlans = { navigateSection(Screen.AdminWorkoutPlans.route) },
            onWorkoutSessions = { navigateSection(Screen.AdminWorkoutSessions.route) },
            onReviews = { navigateSection(Screen.AdminReviews.route) },
            onUserReports = { navigateSection(Screen.AdminUserReports.route) },
            onLogout = null
        )
    }

    sessionManager?.let { sm ->
        LaunchedEffect(sm) {
            sm.sessionExpiredEvent.collect {
                if (hasValidSession()) return@collect
                clearUserSessionNavigationState()
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isAuthenticatedRootRoute = currentRoute in topLevelRoutes ||
        currentRoute == Screen.AdminDashboard.route

    val isInsideAuthenticatedGraph = currentRoute != null &&
        currentRoute != Screen.Splash.route &&
        currentRoute != Screen.Auth.route &&
        !currentRoute.startsWith("auth?") &&
        currentRoute != Screen.VerifyRegisterOtp.route &&
        !currentRoute.startsWith(Screen.PasswordResetGraph.route)

    val authenticatedGraphEntry = remember(navController, currentRoute) {
        if (isInsideAuthenticatedGraph) {
            runCatching { navController.getBackStackEntry(AuthenticatedGraph) }.getOrNull()
        } else {
            null
        }
    }
    val appGraphEntry = remember(navController, currentRoute, authenticatedGraphEntry) {
        if (isAuthenticatedRootRoute) authenticatedGraphEntry else null
    }
    val authViewModel: AuthViewModel? = authenticatedGraphEntry?.let { hiltViewModel(it) }
    val authState = authViewModel?.state?.collectAsState()?.value
    val isAdmin = authState?.authenticated == true && authState.role == "ROLE_ADMIN"
    val showBottomBar = currentRoute in topLevelRoutes ||
        (currentRoute == Screen.AdminDashboard.route && isAdmin)

    val genderGateViewModel: GenderGateViewModel? = authenticatedGraphEntry?.let { hiltViewModel(it) }
    val genderGateState = genderGateViewModel?.uiState?.collectAsState()?.value

    LaunchedEffect(genderGateViewModel, authState?.authenticated, isOnline) {
        genderGateViewModel?.syncSession(
            isAuthenticated = authState?.authenticated == true,
            isOnline = isOnline
        )
    }

    val connectionsViewModel: CoachConnectionsViewModel? = appGraphEntry?.let { hiltViewModel(it) }
    val connectionsState by (connectionsViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) })

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, genderGateViewModel, authState?.authenticated, isOnline) {
        if (genderGateViewModel != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                genderGateViewModel.retryVerificationIfNeeded(
                    isAuthenticated = authState?.authenticated == true,
                    isOnline = isOnline
                )
            }
        }
    }
    LaunchedEffect(lifecycleOwner, connectionsViewModel, authState?.authenticated) {
        if (connectionsViewModel != null && authState?.authenticated == true) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(30_000L)
                    connectionsViewModel.refresh()
                }
            }
        }
    }

    val destinations = visibleBottomNavigationScreens(if (isAdmin) "ROLE_ADMIN" else null).map { screen ->
        screen to when (screen) {
            Screen.Home -> Icons.Default.Home to stringResource(R.string.nav_home)
            Screen.Training -> Icons.Default.FitnessCenter to stringResource(R.string.nav_training)
            Screen.Analysis -> Icons.Default.AutoGraph to stringResource(R.string.nav_analysis)
            Screen.Coach -> Icons.Default.Groups to stringResource(R.string.nav_coaching)
            Screen.Chat -> Icons.Default.ChatBubble to stringResource(R.string.nav_chat)
            Screen.Profile -> Icons.Default.Person to stringResource(R.string.nav_profile)
            Screen.AdminDashboard -> Icons.Default.AdminPanelSettings to stringResource(R.string.nav_admin)
            else -> error("Unexpected bottom navigation destination: ${screen.route}")
        }
    }

    val navItems = remember(destinations, connectionsState?.incomingRequestCount, connectionsState?.hasLoaded) {
        destinations.map { (screen, item) ->
            val (icon, label) = item
            val badge = if (screen == Screen.Coach && connectionsState?.hasLoaded == true) {
                connectionsState?.incomingRequestCount ?: 0
            } else {
                0
            }
            FloatingBottomBarItem(
                route = screen.route,
                icon = icon,
                label = label,
                badgeCount = badge,
                animatedRainbow = screen == Screen.AdminDashboard
            )
        }
    }

    val offlineFeatureMessage = stringResource(R.string.feature_requires_internet)
    val signInRequiredMessage = stringResource(R.string.sign_in_required)

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Splash.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.state.collectAsState()
                LaunchedEffect(Unit) {
                    delay(450)
                    val destination = if (authState.authenticated || !isOnline) {
                        AuthenticatedGraph
                    } else {
                        Screen.Auth.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                SplashScreen()
            }
            composable(
                route = "auth?resetSuccess={resetSuccess}&isAddingAccount={isAddingAccount}",
                arguments = listOf(
                    navArgument("resetSuccess") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("isAddingAccount") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val isAddingAccount = backStackEntry.arguments?.getBoolean("isAddingAccount") == true
                AuthScreen(
                    onAuthenticated = {
                        clearUserSessionNavigationState()
                        navController.navigate(AuthenticatedGraph) { popUpTo(0) { inclusive = true } }
                    },
                    onForgotPassword = {
                        navController.navigate(Screen.PasswordResetGraph.route) { launchSingleTop = true }
                    },
                    onNavigateToVerifyRegisterOtp = {
                        navController.navigate(Screen.VerifyRegisterOtp.route) { launchSingleTop = true }
                    },
                    resetSuccessMessage = if (backStackEntry.arguments?.getBoolean("resetSuccess") == true) {
                        stringResource(R.string.password_updated)
                    } else {
                        null
                    },
                    isAddingAccount = isAddingAccount,
                    onBackClick = if (isAddingAccount) {
                        { navController.popBackStack() }
                    } else null
                )
            }
            composable(Screen.VerifyRegisterOtp.route) { backStackEntry ->
                val authBackStackEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Auth.route)
                }
                val authViewModel: AuthViewModel = hiltViewModel(authBackStackEntry)
                RegisterOtpVerificationScreen(
                    viewModel = authViewModel,
                    onVerified = {
                        clearUserSessionNavigationState()
                        navController.navigate(AuthenticatedGraph) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = {
                        authViewModel.clearRegistrationOtpState()
                        navController.popBackStack()
                    }
                )
            }
            navigation(
                route = Screen.PasswordResetGraph.route,
                startDestination = Screen.ForgotPassword.route
            ) {
                composable(Screen.ForgotPassword.route) { backStackEntry ->
                    val resetGraphEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Screen.PasswordResetGraph.route)
                    }
                    val viewModel: PasswordResetViewModel = hiltViewModel(resetGraphEntry)
                    ForgotPasswordScreen(
                        viewModel = viewModel,
                        onOtpSent = {
                            navController.navigate(Screen.VerifyPasswordOtp.route) {
                                launchSingleTop = true
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.VerifyPasswordOtp.route) { backStackEntry ->
                    val resetGraphEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Screen.PasswordResetGraph.route)
                    }
                    val viewModel: PasswordResetViewModel = hiltViewModel(resetGraphEntry)
                    OtpVerificationScreen(
                        viewModel = viewModel,
                        onVerified = {
                            navController.navigate(Screen.ResetPassword.route) {
                                popUpTo(Screen.VerifyPasswordOtp.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onChangeEmail = { navController.popBackStack() }
                    )
                }
                composable(Screen.ResetPassword.route) { backStackEntry ->
                    val resetGraphEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Screen.PasswordResetGraph.route)
                    }
                    val viewModel: PasswordResetViewModel = hiltViewModel(resetGraphEntry)
                    ResetPasswordScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onResetComplete = {
                            navController.navigate("${Screen.Auth.route}?resetSuccess=true") {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            navigation(route = AuthenticatedGraph, startDestination = Screen.AuthenticatedEntry.route) {
                composable(Screen.AuthenticatedEntry.route) {
                    val authenticatedGraphEntry = remember(navController) {
                        navController.getBackStackEntry(AuthenticatedGraph)
                    }
                    val authViewModel: AuthViewModel = hiltViewModel(authenticatedGraphEntry)
                    val authState by authViewModel.state.collectAsState()
                    LaunchedEffect(authState.authenticated, authState.role, isOnline) {
                        val destination = if (authState.authenticated) {
                            authenticatedStartRoute(authState.role)
                        } else if (!isOnline) {
                            Screen.Home.route
                        } else {
                            Screen.Auth.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.AuthenticatedEntry.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                composable(Screen.AdminDashboard.route) {
                    AdminOnlyRoute(navController) {
                        AdminDashboardScreen(
                            onUsers = adminNavCallbacks.onUsers,
                            onUserClick = { navController.navigate(Screen.AdminUserDetails.createRoute(it)) },
                            onExercises = adminNavCallbacks.onExercises,
                            onWorkoutPlans = adminNavCallbacks.onWorkoutPlans,
                            onWorkoutSessions = adminNavCallbacks.onWorkoutSessions,
                            onReviews = adminNavCallbacks.onReviews,
                            onUserReports = adminNavCallbacks.onUserReports,
                            onAdminAiAgent = adminNavCallbacks.onAiAgent,
                            onGettingStartedProgram = adminNavCallbacks.onGettingStarted,
                            onLogout = performAdminLogout,
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(Screen.AdminAiAgent.route) {
                    AdminOnlyRoute(navController) {
                        AdminAiAgentScreen(
                            onBack = { navController.popBackStack() },
                            onOpenExercise = { exerciseId ->
                                navController.navigate(Screen.AdminEditExercise.createRoute(exerciseId))
                            },
                            onOpenWorkoutPlan = { planId ->
                                navController.navigate(Screen.AdminWorkoutPlanDetails.createRoute(planId))
                            },
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(Screen.AdminGettingStartedProgram.route) {
                    AdminOnlyRoute(navController) {
                        AdminGettingStartedProgramScreen(
                            onBack = { navController.popBackStack() },
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(Screen.AdminExercises.route) {
                    AdminOnlyRoute(navController) {
                        AdminExercisesScreen(
                            onBack = { navController.popBackStack() },
                            onCreateExercise = { navController.navigate(Screen.AdminCreateExercise.route) },
                            onEditExercise = { navController.navigate(Screen.AdminEditExercise.createRoute(it)) },
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(Screen.AdminCreateExercise.route) {
                    AdminOnlyRoute(navController) {
                        AdminExerciseFormScreen(
                            exerciseId = null,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(
                    route = Screen.AdminEditExercise.route,
                    arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val exerciseId = requireNotNull(backStackEntry.arguments?.getLong("exerciseId"))
                    AdminOnlyRoute(navController) {
                        AdminExerciseFormScreen(
                            exerciseId = exerciseId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.AdminUsers.route) {
                    AdminOnlyRoute(navController) {
                        AdminUsersScreen(
                            onBack = { navController.popBackStack() },
                            onUserClick = { navController.navigate(Screen.AdminUserDetails.createRoute(it)) },
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(
                    route = Screen.AdminUserDetails.route,
                    arguments = listOf(navArgument("userId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val userId = requireNotNull(backStackEntry.arguments?.getLong("userId"))
                    AdminOnlyRoute(navController) {
                        AdminUserDetailScreen(userId, onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.AdminWorkoutPlans.route) {
                    AdminOnlyRoute(navController) {
                        AdminWorkoutPlansScreen(
                            onBack = { navController.popBackStack() },
                            onPlanClick = { navController.navigate(Screen.AdminWorkoutPlanDetails.createRoute(it)) },
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(
                    route = Screen.AdminWorkoutPlanDetails.route,
                    arguments = listOf(navArgument("planId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val planId = requireNotNull(backStackEntry.arguments?.getLong("planId"))
                    AdminOnlyRoute(navController) {
                        AdminWorkoutPlanDetailScreen(planId, onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.AdminWorkoutSessions.route) {
                    AdminOnlyRoute(navController) {
                        AdminWorkoutSessionsScreen(
                            onBack = { navController.popBackStack() },
                            onSessionClick = { navController.navigate(Screen.AdminWorkoutSessionDetails.createRoute(it)) },
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(
                    route = Screen.AdminWorkoutSessionDetails.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val sessionId = requireNotNull(backStackEntry.arguments?.getLong("sessionId"))
                    AdminOnlyRoute(navController) {
                        AdminWorkoutSessionDetailScreen(sessionId, onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.AdminReviews.route) {
                    AdminOnlyRoute(navController) {
                        AdminReviewsScreen(
                            onBack = { navController.popBackStack() },
                            onReviewClick = { navController.navigate(Screen.AdminReviewDetails.createRoute(it)) },
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(
                    route = Screen.AdminReviewDetails.route,
                    arguments = listOf(navArgument("reviewId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val reviewId = requireNotNull(backStackEntry.arguments?.getLong("reviewId"))
                    AdminOnlyRoute(navController) {
                        AdminReviewDetailScreen(reviewId, onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.AdminUserReports.route) {
                    AdminOnlyRoute(navController) {
                        AdminUserReportsScreen(
                            onBack = { navController.popBackStack() },
                            onReportClick = { navController.navigate(Screen.AdminUserReportDetails.createRoute(it)) },
                            adminNavigation = withAdminLogout(adminNavCallbacks, performAdminLogout)
                        )
                    }
                }
                composable(
                    route = Screen.AdminUserReportDetails.route,
                    arguments = listOf(navArgument("reportId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val reportId = requireNotNull(backStackEntry.arguments?.getLong("reportId"))
                    AdminOnlyRoute(navController) {
                        AdminUserReportDetailScreen(
                            reportId = reportId,
                            onBack = { navController.popBackStack() },
                            onReportClick = { navController.navigate(Screen.AdminUserReportDetails.createRoute(it)) }
                        )
                    }
                }
                composable(Screen.Home.route) { backStackEntry ->
                    val context = LocalContext.current
                    val openProgramFlag by backStackEntry.savedStateHandle
                        .getStateFlow("open_program_details", false)
                        .collectAsState()
                    HomeDashboard(
                        onAnalysis = {
                            if (!isOnline) {
                                Toast.makeText(context, offlineFeatureMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(Screen.Analysis.route)
                            }
                        },
                        onTraining = { navController.navigate(Screen.Training.route) },
                        onHistoryItem = { navController.navigate(Screen.AnalysisResult.createRoute(it)) },
                        onWorkoutDetails = { planId, day ->
                            navController.navigate(Screen.WorkoutPlanDetails.createRoute(planId, day))
                        },
                        onStartWorkout = { planId, dayId, date, assignmentId ->
                            if (!isOnline) {
                                Toast.makeText(context, offlineFeatureMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(
                                    Screen.WorkoutSession.createRoute(
                                        workoutId = "server-plan-$planId-day-$dayId",
                                        date = date,
                                        planId = planId,
                                        dayId = dayId,
                                        assignmentId = assignmentId
                                    )
                                )
                            }
                        },
                        onWorkoutHistory = {
                            if (!isOnline) {
                                Toast.makeText(context, offlineFeatureMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(Screen.WorkoutHistory.route)
                            }
                        },
                        onResumeWorkout = { session ->
                            if (!isOnline) {
                                Toast.makeText(context, offlineFeatureMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(
                                    Screen.WorkoutSession.createRoute(
                                        workoutId = "resume-${session.id}",
                                        date = session.workoutDate ?: java.time.LocalDate.now().toString(),
                                        sessionId = session.id
                                    )
                                )
                            }
                        },
                        onAnalytics = {
                            if (!isOnline) {
                                Toast.makeText(context, offlineFeatureMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(Screen.TrainingAnalytics.route)
                            }
                        },
                        onOpenAiPlanner = {
                            if (!isOnline) {
                                Toast.makeText(context, offlineFeatureMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(Screen.AiWorkoutPlanner.createRoute())
                            }
                        },
                        onOpenWorkoutPlans = {
                            navController.navigate(Screen.Training.route)
                        },
                        onNotificationsClick = {
                            navController.navigate(Screen.Notifications.route)
                        },
                        onProfileClick = {
                            navController.navigate(Screen.Profile.route)
                        },
                        openProgramDetailsSignal = openProgramFlag,
                        onConsumeOpenProgramDetailsSignal = {
                            backStackEntry.savedStateHandle["open_program_details"] = false
                        }
                    )
                }
                composable(Screen.Notifications.route) {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateTarget = { target ->
                            when (target) {
                                is NotificationNavigationTarget.WorkoutDetails -> {
                                    navController.navigate(Screen.WorkoutPlanDetails.createRoute(target.planId))
                                }
                                is NotificationNavigationTarget.ProgramAvailable -> {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    runCatching {
                                        navController.getBackStackEntry(Screen.Home.route)
                                            .savedStateHandle["open_program_details"] = true
                                    }
                                }
                                is NotificationNavigationTarget.CoachRelationships -> {
                                    navController.navigate(Screen.Coach.route) {
                                        launchSingleTop = true
                                    }
                                }
                                is NotificationNavigationTarget.PersonalRecords -> {
                                    navController.navigate(Screen.PersonalRecords.route) {
                                        launchSingleTop = true
                                    }
                                }
                                is NotificationNavigationTarget.AnnouncementDetail,
                                is NotificationNavigationTarget.None -> Unit
                            }
                        }
                    )
                }
                composable(Screen.Training.route) {
                    WorkoutPlanListScreen(
                        onPlanClick = { planId ->
                            navController.navigate(Screen.WorkoutPlanDetails.createRoute(planId))
                        },
                        onPlanClickWithDate = { planId, startDate ->
                            navController.navigate(Screen.WorkoutPlanDetails.createRoute(planId = planId, startDate = startDate))
                        },
                        onOpenAiPlanner = { clientId, clientName ->
                            navController.navigate(Screen.AiWorkoutPlanner.createRoute(clientId, clientName))
                        },
                        onStartGettingStartedSession = { session ->
                            navController.navigate(
                                Screen.WorkoutSession.createRoute(
                                    workoutId = session.source,
                                    date = session.workoutDate ?: java.time.LocalDate.now().toString(),
                                    planId = session.workoutPlanId ?: 0L,
                                    dayId = session.workoutPlanDayId ?: 0L,
                                    sessionId = session.id,
                                    assignmentId = session.assignmentId
                                )
                            )
                        }
                    )
                }
                composable(
                    route = Screen.AiWorkoutPlanner.route,
                    arguments = listOf(
                        navArgument("clientId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("clientName") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val clientIdStr = backStackEntry.arguments?.getString("clientId")
                    val clientId = clientIdStr?.toLongOrNull()
                    val clientName = backStackEntry.arguments?.getString("clientName")?.let {
                        java.net.URLDecoder.decode(it, "UTF-8")
                    }
                    AiWorkoutPlannerScreen(
                        clientId = clientId,
                        clientName = clientName,
                        onBackClick = { navController.popBackStack() },
                        onPlanCommitted = { committedPlan ->
                            navController.popBackStack()
                            navController.navigate(
                                Screen.WorkoutPlanDetails.createRoute(
                                    planId = committedPlan.id,
                                    startDate = committedPlan.startDate
                                )
                            )
                        }
                    )
                }
                composable(
                    route = Screen.WorkoutPlanDetails.route,
                    arguments = listOf(
                        navArgument("planId") { type = NavType.StringType },
                        navArgument("dayOfWeek") { type = NavType.StringType },
                        navArgument("startDate") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    WorkoutPlanDetailsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Analysis.route) {
                    ExerciseSelectionScreen(
                        onSelectExercise = { navController.navigate(Screen.ProgressionSelection.createRoute(it)) },
                        onSelectVideoExercise = { id, name, code ->
                            navController.navigate(Screen.VideoSelection.createRoute(id, name, code))
                        },
                        onOpenLiveAnalysis = { exerciseCode -> navController.navigate(Screen.LiveAnalysis.createRoute(exerciseCode)) },
                        onOpenHistory = { navController.navigate(Screen.LiveAnalysisHistory.route) },
                        onOpenAnalysisResult = { analysisId ->
                            navController.navigate(Screen.AnalysisResult.createRoute(analysisId))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.LiveAnalysis.route,
                    arguments = listOf(navArgument("exerciseCode") { type = NavType.StringType })
                ) { backStackEntry ->
                    val exerciseCode = requireNotNull(backStackEntry.arguments?.getString("exerciseCode"))
                    LiveAnalysisScreen(
                        exerciseCode = exerciseCode,
                        onBackClick = { navController.popBackStack() },
                        onViewDetails = { sessionId ->
                            navController.navigate(Screen.WorkoutAnalysisSummary.createRoute(sessionId))
                        }
                    )
                }
                composable(Screen.Coach.route) {
                    val appEntry = remember(navController) {
                        navController.getBackStackEntry(AuthenticatedGraph)
                    }
                    val viewModel: CoachConnectionsViewModel = hiltViewModel(appEntry)
                    CoachConnectionsScreen(
                        onCoachClick = { coachId ->
                            navController.navigate(Screen.CoachProfile.createRoute(coachId))
                        },
                        onClientClick = { clientId ->
                            navController.navigate(Screen.ClientProfile.createRoute(clientId))
                        },
                        onChatClick = { conversationId, otherUserId, otherUserName, otherUserAvatarUrl ->
                            navController.navigate(
                                Screen.ChatConversation.createRoute(
                                    conversationId = conversationId,
                                    otherUserName = otherUserName,
                                    otherUserAvatarUrl = otherUserAvatarUrl,
                                    otherUserId = otherUserId
                                )
                            )
                        },
                        onSharedPrClick = { sharedPr ->
                            val currentUserId = viewModel.uiState.value.currentUserId ?: connectionsState?.currentUserId
                            if (currentUserId != null && sharedPr.clientUserId > 0L && sharedPr.clientUserId != currentUserId) {
                                navController.navigate(Screen.ClientProgress.createRoute(sharedPr.clientUserId, initialTab = 1))
                            } else {
                                navController.navigate(Screen.PersonalRecords.route)
                            }
                        },
                        viewModel = viewModel
                    )
                }
                composable(
                    route = Screen.CoachProfile.route,
                    arguments = listOf(navArgument("coachId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val coachId = requireNotNull(backStackEntry.arguments?.getLong("coachId"))
                    val appEntry = remember(navController) {
                        navController.getBackStackEntry(AuthenticatedGraph)
                    }
                    val viewModel: CoachConnectionsViewModel = hiltViewModel(appEntry)
                    CoachProfileScreen(
                        coachId = coachId,
                        onBack = { navController.popBackStack() },
                        onChatClick = { conversationId, otherUserId, otherUserName, otherUserAvatarUrl ->
                            navController.navigate(
                                Screen.ChatConversation.createRoute(
                                    conversationId = conversationId,
                                    otherUserName = otherUserName,
                                    otherUserAvatarUrl = otherUserAvatarUrl,
                                    otherUserId = otherUserId
                                )
                            )
                        },
                        viewModel = viewModel
                    )
                }
                composable(
                    route = Screen.ClientProfile.route,
                    arguments = listOf(navArgument("clientId") { type = NavType.LongType })
                ) {
                    ClientDetailScreen(
                        onBack = { navController.popBackStack() },
                        onChatClick = { conversationId, otherUserId, otherUserName, otherUserAvatarUrl ->
                            navController.navigate(
                                Screen.ChatConversation.createRoute(
                                    conversationId = conversationId,
                                    otherUserName = otherUserName,
                                    otherUserAvatarUrl = otherUserAvatarUrl,
                                    otherUserId = otherUserId
                                )
                            )
                        },
                        onViewPlan = { planId ->
                            navController.navigate(Screen.WorkoutPlanDetails.createRoute(planId))
                        },
                        onViewProgress = { clientId ->
                            navController.navigate(Screen.ClientProgress.createRoute(clientId))
                        }
                    )
                }
                composable(Screen.Chat.route) {
                    ChatListScreen(
                        onConversationClick = { conversationId, otherUserId, otherUserName, otherUserAvatarUrl ->
                            navController.navigate(
                                Screen.ChatConversation.createRoute(
                                    conversationId = conversationId,
                                    otherUserName = otherUserName,
                                    otherUserAvatarUrl = otherUserAvatarUrl,
                                    otherUserId = otherUserId
                                )
                            )
                        },
                        onAiAssistantClick = {
                            navController.navigate(Screen.AiChat.createRoute())
                        }
                    )
                }
                composable(
                    route = Screen.AiChat.route,
                    arguments = listOf(
                        navArgument("conversationId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) {
                    AiChatScreen(
                        onBackClick = { navController.popBackStack() },
                        onViewPrsClick = { navController.navigate(Screen.PersonalRecords.route) },
                        onViewPlansClick = { navController.navigate(Screen.Training.route) },
                        onViewProfileClick = { navController.navigate(Screen.Profile.route) }
                    )
                }
                composable(
                    route = Screen.ChatConversation.route,
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.LongType },
                        navArgument("otherUserName") {
                            type = NavType.StringType
                            defaultValue = "Chat"
                        },
                        navArgument("otherUserAvatarUrl") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("otherUserId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) { backStackEntry ->
                    val otherUserId = backStackEntry.arguments?.getLong("otherUserId") ?: 0L
                    ChatConversationScreen(
                        onBackClick = { navController.popBackStack() },
                        onSharedPrClick = { sharedPr ->
                            val currentUserId = connectionsState?.currentUserId
                            val isClientPrFromOtherUser = sharedPr.clientUserId > 0L &&
                                (sharedPr.clientUserId == otherUserId || (currentUserId != null && sharedPr.clientUserId != currentUserId))
                            if (isClientPrFromOtherUser) {
                                navController.navigate(Screen.ClientProgress.createRoute(sharedPr.clientUserId, initialTab = 1))
                            } else {
                                navController.navigate(Screen.PersonalRecords.route)
                            }
                        }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onLogout = {
                            clearUserSessionNavigationState()
                            navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
                        },
                        onSwitchAccount = {
                            clearUserSessionNavigationState()
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onViewCoach = { coachId ->
                            navController.navigate(Screen.CoachProfile.createRoute(coachId))
                        },
                        onDiscoverCoaches = {
                            navController.navigate(Screen.Coach.route)
                        },
                        onOpenChat = { conversationId, otherUserId, otherUserName, otherUserAvatarUrl ->
                            navController.navigate(
                                Screen.ChatConversation.createRoute(
                                    conversationId = conversationId,
                                    otherUserName = otherUserName,
                                    otherUserAvatarUrl = otherUserAvatarUrl,
                                    otherUserId = otherUserId
                                )
                            )
                        },
                        onViewPlan = { planId ->
                            navController.navigate(Screen.WorkoutPlanDetails.createRoute(planId))
                        },
                        onBrowsePlans = {
                            navController.navigate(Screen.Training.route)
                        },
                        onViewClient = { clientId ->
                            navController.navigate(Screen.ClientProfile.createRoute(clientId))
                        },
                        onNavigateToBodyMetrics = {
                            navController.navigate(Screen.BodyMetrics.route)
                        }
                    )
                }
                composable(Screen.SwitchAccount.route) {
                    com.calisthenics.ai.ui.profile.SwitchAccountScreen(
                        onAccountSwitched = {
                            clearUserSessionNavigationState()
                            navController.navigate(AuthenticatedGraph) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onAddAccount = {
                            clearUserSessionNavigationState()
                            navController.navigate("${Screen.Auth.route}?isAddingAccount=true") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onAllAccountsRemoved = {
                            clearUserSessionNavigationState()
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
                composable(
                    route = Screen.WorkoutSession.route,
                    arguments = listOf(
                        navArgument("workoutId") { type = NavType.StringType },
                        navArgument("date") { type = NavType.StringType },
                        navArgument("planId") { type = NavType.LongType },
                        navArgument("dayId") { type = NavType.LongType },
                        navArgument("sessionId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                        navArgument("assignmentId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) {
                    WorkoutSessionScreen(
                        onCancel = { navController.popBackStack() },
                        onFinished = { id ->
                            navController.navigate(Screen.WorkoutSummary.createRoute(id)) {
                                popUpTo(Screen.WorkoutSession.route) { inclusive = true }
                            }
                        },
                        onViewHistory = { navController.navigate(Screen.WorkoutHistory.route) }
                    )
                }
                composable(Screen.WorkoutHistory.route) {
                    WorkoutHistoryScreen(
                        onBack = { navController.popBackStack() },
                        onSessionSelected = { id -> navController.navigate(Screen.WorkoutSummary.createRoute(id)) },
                        onResumeSession = { session ->
                            navController.navigate(
                                Screen.WorkoutSession.createRoute(
                                    workoutId = session.source,
                                    date = session.workoutDate ?: java.time.LocalDate.now().toString(),
                                    planId = session.workoutPlanId ?: 0L,
                                    dayId = session.workoutPlanDayId ?: 0L,
                                    sessionId = session.id,
                                    assignmentId = session.assignmentId
                                )
                            )
                        },
                        onAnalysisSessionSelected = { sessionId ->
                            navController.navigate(Screen.WorkoutAnalysisSummary.createRoute(sessionId))
                        },
                        onExploreAiPlanner = {
                            navController.navigate(Screen.AiWorkoutPlanner.createRoute())
                        },
                        onCreateWorkoutPlan = {
                            navController.navigate(Screen.Training.route)
                        }
                    )
                }
                composable(Screen.LiveAnalysisHistory.route) {
                    LiveAnalysisHistoryScreen(
                        onBackClick = { navController.popBackStack() },
                        onSessionClick = { sessionId ->
                            navController.navigate(Screen.WorkoutAnalysisSummary.createRoute(sessionId))
                        },
                        onStartWorkout = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.LiveAnalysisHistoryDetail.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                ) {
                    WorkoutAnalysisSummaryScreen(
                        onBackClick = { navController.popBackStack() },
                        onViewHistoryClick = {
                            navController.navigate(Screen.LiveAnalysisHistory.route) {
                                launchSingleTop = true
                            }
                        },
                        onDoneClick = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.WorkoutAnalysisSummary.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                ) {
                    WorkoutAnalysisSummaryScreen(
                        onBackClick = { navController.popBackStack() },
                        onViewHistoryClick = {
                            navController.navigate(Screen.LiveAnalysisHistory.route) {
                                launchSingleTop = true
                            }
                        },
                        onDoneClick = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.WorkoutSummary.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                ) {
                    WorkoutSummaryScreen(
                        onReturnToToday = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onViewHistory = { navController.navigate(Screen.WorkoutHistory.route) },
                        onResume = { session ->
                            navController.navigate(
                                Screen.WorkoutSession.createRoute(
                                    workoutId = "resume-${session.id}",
                                    date = session.workoutDate ?: java.time.LocalDate.now().toString(),
                                    sessionId = session.id
                                )
                            )
                        },
                        onExploreAiPlanner = {
                            navController.navigate(Screen.AiWorkoutPlanner.createRoute())
                        },
                        onCreateWorkoutPlan = {
                            navController.navigate(Screen.Training.route)
                        }
                    )
                }
                composable(Screen.ExerciseSelection.route) {
                    ExerciseSelectionScreen(
                        onSelectExercise = { navController.navigate(Screen.ProgressionSelection.createRoute(it)) },
                        onSelectVideoExercise = { id, name, code ->
                            navController.navigate(Screen.VideoSelection.createRoute(id, name, code))
                        },
                        onOpenLiveAnalysis = { exerciseCode -> navController.navigate(Screen.LiveAnalysis.createRoute(exerciseCode)) },
                        onOpenHistory = { navController.navigate(Screen.LiveAnalysisHistory.route) },
                        onOpenAnalysisResult = { analysisId ->
                            navController.navigate(Screen.AnalysisResult.createRoute(analysisId))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.ProgressionSelection.route) {
                    ProgressionSelectionScreen(
                        onProgressionConfirmed = { id, name -> navController.navigate(Screen.VideoSelection.createRoute(id, name)) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.VideoSelection.route,
                    arguments = listOf(
                        navArgument("exerciseId") { type = NavType.StringType },
                        navArgument("progressionName") { type = NavType.StringType },
                        navArgument("exerciseCode") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) {
                    VideoSelectionScreen(
                        onAnalysisSuccess = { id -> navController.navigate(Screen.AnalysisResult.createRoute(id)) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.AnalysisResult.route) {
                    AnalysisResultScreen(
                        onAnalyzeAnother = { navController.navigate(Screen.Analysis.route) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.TrainingAnalytics.route) {
                    TrainingAnalyticsScreen(
                        onBack = { navController.popBackStack() },
                        onExerciseClick = { exerciseId ->
                            navController.navigate(Screen.ExerciseProgress.createRoute(exerciseId))
                        },
                        onManagePrs = {
                            navController.navigate(Screen.PersonalRecords.route)
                        }
                    )
                }
                composable(Screen.PersonalRecords.route) {
                    PersonalRecordsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.ExerciseProgress.route,
                    arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
                ) {
                    ExerciseProgressScreen(
                        onBack = { navController.popBackStack() },
                        onSessionClick = { sessionId ->
                            navController.navigate(Screen.WorkoutSummary.createRoute(sessionId))
                        }
                    )
                }
                composable(
                    route = Screen.ClientProgress.route,
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.LongType },
                        navArgument("initialTab") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { backStackEntry ->
                    val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
                    ClientProgressScreen(
                        onBack = { navController.popBackStack() },
                        initialTab = initialTab
                    )
                }
                composable(Screen.BodyMetrics.route) {
                    BodyMetricsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        OfflineBanner(
            isOffline = !isOnline,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        UnfinishedWorkoutBubbleHost(
            onResumeSession = { session ->
                navController.navigate(
                    Screen.WorkoutSession.createRoute(
                        workoutId = session.source,
                        date = session.workoutDate ?: java.time.LocalDate.now().toString(),
                        planId = session.workoutPlanId ?: 0L,
                        dayId = session.workoutPlanDayId ?: 0L,
                        sessionId = session.id,
                        assignmentId = session.assignmentId
                    )
                )
            },
            currentRoute = currentRoute,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showBottomBar) 88.dp else 16.dp)
        )

        if (showBottomBar) {
            val context = LocalContext.current
            FloatingBottomBar(
                items = navItems,
                currentRoute = currentRoute,
                onItemClick = { route ->
                    if (route != currentRoute) {
                        if (!isOnline && offlineAccessPolicy?.isFeatureRestrictedOffline(route) == true) {
                            Toast.makeText(context, offlineFeatureMessage, Toast.LENGTH_SHORT).show()
                        } else if (isOnline && authState?.authenticated != true && route != Screen.Home.route && route != Screen.Training.route) {
                            Toast.makeText(context, signInRequiredMessage, Toast.LENGTH_SHORT).show()
                        } else {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        val activeGenderGateVm = genderGateViewModel
        val activeGenderGateState = genderGateState
        if (authState?.authenticated == true &&
            currentRoute != null &&
            currentRoute != Screen.AuthenticatedEntry.route &&
            activeGenderGateVm != null &&
            activeGenderGateState?.shouldShowModal == true
        ) {
            GenderOnboardingDialog(
                selectedGender = activeGenderGateState.selectedGender,
                isSaving = activeGenderGateState.isSaving,
                errorMessage = activeGenderGateState.errorMessage,
                onSelect = activeGenderGateVm::selectGender,
                onConfirm = activeGenderGateVm::confirmGender
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.padding(16.dp))
        Text(stringResource(R.string.app_name))
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AdminOnlyRoute(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val authenticatedGraphEntry = remember(navController) {
        navController.getBackStackEntry(AuthenticatedGraph)
    }
    val authViewModel: AuthViewModel = hiltViewModel(authenticatedGraphEntry)
    val authState by authViewModel.state.collectAsState()
    val adminViewModel: AdminViewModel = hiltViewModel()
    val adminState by adminViewModel.uiState.collectAsState()
    val isAdmin = authState.authenticated && authState.role == "ROLE_ADMIN"

    LaunchedEffect(isAdmin, authState.authenticated) {
        if (!isAdmin) {
            if (authState.authenticated) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(AuthenticatedGraph) { inclusive = false }
                    launchSingleTop = true
                }
            } else {
                navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    LaunchedEffect(adminState.authorizationErrorCode) {
        when (adminState.authorizationErrorCode) {
            401 -> authViewModel.logout()
            403 -> authViewModel.refreshRole()
        }
    }

    if (isAdmin) {
        content()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun withAdminLogout(
    base: AdminNavigationCallbacks,
    performAdminLogout: () -> Unit
): AdminNavigationCallbacks {
    val adminViewModel: AdminViewModel = hiltViewModel()
    return remember(base, performAdminLogout, adminViewModel) {
        base.copy(
            onLogout = {
                adminViewModel.logout()
                performAdminLogout()
            }
        )
    }
}
