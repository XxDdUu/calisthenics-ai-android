package com.calisthenics.ai.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.ui.analysis.AnalysisResultScreen
import com.calisthenics.ai.ui.auth.AuthScreen
import com.calisthenics.ai.ui.auth.AuthViewModel
import com.calisthenics.ai.ui.auth.ForgotPasswordScreen
import com.calisthenics.ai.ui.auth.OtpVerificationScreen
import com.calisthenics.ai.ui.auth.PasswordResetViewModel
import com.calisthenics.ai.ui.auth.ResetPasswordScreen
import com.calisthenics.ai.ui.chat.conversation.ChatConversationScreen
import com.calisthenics.ai.ui.chat.list.ChatListScreen
import com.calisthenics.ai.ui.coach.ClientProgressScreen
import com.calisthenics.ai.ui.coach.CoachConnectionsScreen
import com.calisthenics.ai.ui.coach.CoachConnectionsViewModel
import com.calisthenics.ai.ui.coach.CoachProfileScreen
import com.calisthenics.ai.ui.exercise.ExerciseProgressScreen
import com.calisthenics.ai.ui.exercise.ExerciseSelectionScreen
import com.calisthenics.ai.ui.main.HomeDashboard
import com.calisthenics.ai.ui.pr.PersonalRecordsScreen
import com.calisthenics.ai.ui.profile.ClientDetailScreen
import com.calisthenics.ai.ui.profile.ProfileScreen
import com.calisthenics.ai.ui.progression.ProgressionSelectionScreen
import com.calisthenics.ai.ui.training.TrainingAnalyticsScreen
import com.calisthenics.ai.ui.video.VideoSelectionScreen
import com.calisthenics.ai.ui.workoutplan.details.WorkoutPlanDetailsScreen
import com.calisthenics.ai.ui.workoutplan.list.WorkoutPlanListScreen
import com.calisthenics.ai.ui.workoutsession.WorkoutHistoryScreen
import com.calisthenics.ai.ui.workoutsession.WorkoutSessionScreen
import com.calisthenics.ai.ui.workoutsession.WorkoutSummaryScreen
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object PasswordResetGraph : Screen("password_reset")
    data object ForgotPassword : Screen("password_reset/forgot")
    data object VerifyPasswordOtp : Screen("password_reset/verify")
    data object ResetPassword : Screen("password_reset/reset")
    data object Home : Screen("home")
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
    data object ExerciseSelection : Screen("exercise_selection")
    data object WorkoutPlanDetails : Screen("workout_plan_details/{planId}/{dayOfWeek}") {
        fun createRoute(planId: Long, dayOfWeek: WorkoutDayOfWeek = WorkoutDayOfWeek.MONDAY) =
            "workout_plan_details/$planId/${dayOfWeek.name}"
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
    data object WorkoutSummary : Screen("workout_summary/{sessionId}") {
        fun createRoute(sessionId: Long) = "workout_summary/$sessionId"
    }
    data object ProgressionSelection : Screen("progression_selection/{exerciseCode}") {
        fun createRoute(code: String) = "progression_selection/$code"
    }
    data object VideoSelection : Screen("video_selection/{exerciseId}/{progressionName}") {
        fun createRoute(id: Long, name: String) = "video_selection/$id/${java.net.URLEncoder.encode(name, "UTF-8")}"
    }
    data object AnalysisResult : Screen("analysis_result/{analysisId}") {
        fun createRoute(id: Long) = "analysis_result/$id"
    }
    data object ExerciseProgress : Screen("exercise_progress/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "exercise_progress/$exerciseId"
    }
    data object TrainingAnalytics : Screen("training_analytics")
    data object PersonalRecords : Screen("personal_records")
    data object ClientProgress : Screen("coach/client_progress/{clientId}") {
        fun createRoute(clientId: Long) = "coach/client_progress/$clientId"
    }
}

private const val AuthenticatedGraph = "authenticated"

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    sessionManager: com.calisthenics.ai.data.local.SessionManager? = null
) {
    sessionManager?.let { sm ->
        LaunchedEffect(sm) {
            sm.sessionExpiredEvent.collect {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
    val showBottomBar = currentRoute in topLevelRoutes

    val appGraphEntry = remember(navController, currentRoute) {
        if (showBottomBar) {
            runCatching { navController.getBackStackEntry(AuthenticatedGraph) }.getOrNull()
        } else {
            null
        }
    }
    val connectionsViewModel: CoachConnectionsViewModel? = appGraphEntry?.let { hiltViewModel(it) }
    val connectionsState by (connectionsViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) })

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, connectionsViewModel) {
        if (connectionsViewModel != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(30_000L)
                    connectionsViewModel.refresh()
                }
            }
        }
    }

    val destinations = remember {
        listOf(
            Screen.Home to (Icons.Default.Home to "Home"),
            Screen.Training to (Icons.Default.FitnessCenter to "Training"),
            Screen.Analysis to (Icons.Default.AutoGraph to "AI Analysis"),
            Screen.Coach to (Icons.Default.Groups to "Coaching"),
            Screen.Chat to (Icons.Default.ChatBubble to "Chat"),
            Screen.Profile to (Icons.Default.Person to "Profile")
        )
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
                badgeCount = badge
            )
        }
    }

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
                    navController.navigate(if (authState.authenticated) AuthenticatedGraph else Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                SplashScreen()
            }
            composable(
                route = "auth?resetSuccess={resetSuccess}",
                arguments = listOf(navArgument("resetSuccess") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { backStackEntry ->
                AuthScreen(onAuthenticated = {
                    navController.navigate(AuthenticatedGraph) { popUpTo(Screen.Auth.route) { inclusive = true } }
                }, onForgotPassword = {
                    navController.navigate(Screen.PasswordResetGraph.route) { launchSingleTop = true }
                }, resetSuccessMessage = if (backStackEntry.arguments?.getBoolean("resetSuccess") == true) {
                    "Mật khẩu đã được cập nhật. Vui lòng đăng nhập bằng mật khẩu mới."
                } else {
                    null
                })
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
                        onOtpSent = { navController.navigate(Screen.VerifyPasswordOtp.route) },
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
                            }
                        },
                        onBack = { navController.popBackStack() }
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
            navigation(route = AuthenticatedGraph, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    HomeDashboard(
                        onAnalysis = { navController.navigate(Screen.Analysis.route) },
                        onTraining = { navController.navigate(Screen.Training.route) },
                        onHistoryItem = { navController.navigate(Screen.AnalysisResult.createRoute(it)) },
                        onWorkoutDetails = { planId, day ->
                            navController.navigate(Screen.WorkoutPlanDetails.createRoute(planId, day))
                        },
                        onStartWorkout = { planId, dayId, date, assignmentId ->
                            navController.navigate(
                                Screen.WorkoutSession.createRoute(
                                    workoutId = "server-plan-$planId-day-$dayId",
                                    date = date,
                                    planId = planId,
                                    dayId = dayId,
                                    assignmentId = assignmentId
                                )
                            )
                        },
                        onWorkoutHistory = { navController.navigate(Screen.WorkoutHistory.route) },
                        onResumeWorkout = { session ->
                            navController.navigate(
                                Screen.WorkoutSession.createRoute(
                                    workoutId = "resume-${session.id}",
                                    date = session.workoutDate ?: java.time.LocalDate.now().toString(),
                                    sessionId = session.id
                                )
                            )
                        },
                        onAnalytics = { navController.navigate(Screen.TrainingAnalytics.route) }
                    )
                }
                composable(Screen.Training.route) {
                    WorkoutPlanListScreen(
                        onPlanClick = { planId ->
                            navController.navigate(Screen.WorkoutPlanDetails.createRoute(planId))
                        }
                    )
                }
                composable(Screen.WorkoutPlanDetails.route) {
                    WorkoutPlanDetailsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Analysis.route) {
                    ExerciseSelectionScreen(
                        onSelectExercise = { navController.navigate(Screen.ProgressionSelection.createRoute(it)) },
                        onBackClick = { navController.popBackStack() }
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
                        }
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
                ) {
                    ChatConversationScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onLogout = {
                            navController.navigate(Screen.Auth.route) { popUpTo(0) }
                        },
                        onSwitchAccount = {
                            navController.navigate(Screen.Auth.route) { popUpTo(0) }
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
                        onSessionSelected = { id -> navController.navigate(Screen.WorkoutSummary.createRoute(id)) }
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
                        }
                    )
                }
                composable(Screen.ExerciseSelection.route) {
                    ExerciseSelectionScreen(
                        onSelectExercise = { navController.navigate(Screen.ProgressionSelection.createRoute(it)) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.ProgressionSelection.route) {
                    ProgressionSelectionScreen(
                        onProgressionConfirmed = { id, name -> navController.navigate(Screen.VideoSelection.createRoute(id, name)) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.VideoSelection.route) {
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
                    arguments = listOf(navArgument("clientId") { type = NavType.LongType })
                ) {
                    ClientProgressScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (showBottomBar) {
            FloatingBottomBar(
                items = navItems,
                currentRoute = currentRoute,
                onItemClick = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
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
        Text("Calisthenics AI")
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
    }
}
