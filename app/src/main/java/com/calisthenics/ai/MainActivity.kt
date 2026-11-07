package com.calisthenics.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.calisthenics.ai.data.local.SessionManager
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.network.OfflineAccessPolicy
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.ui.navigation.NavGraph
import com.calisthenics.ai.ui.theme.CalisthenicsAITheme
import com.calisthenics.ai.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var connectivityMonitor: ConnectivityMonitor

    @Inject
    lateinit var offlineAccessPolicy: OfflineAccessPolicy

    @Inject
    lateinit var authRepository: AuthRepository

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            CalisthenicsAITheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        sessionManager = sessionManager,
                        connectivityMonitor = connectivityMonitor,
                        offlineAccessPolicy = offlineAccessPolicy,
                        hasValidSession = { authRepository.isLoggedIn() }
                    )
                }
            }
        }
    }
}
