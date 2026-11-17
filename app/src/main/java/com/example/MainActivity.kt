package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.repository.ResumeRepository
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.ResumeEditorScreen
import com.example.ui.screens.ResumePreviewScreen
import com.example.ui.theme.ResumeCraftTheme
import com.example.ui.viewmodel.PortfolioViewModel
import com.example.ui.viewmodel.ResumeEditorViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val ROUTE_PORTFOLIO = "portfolio"
private const val ROUTE_EDITOR = "editor/{resumeId}"
private const val ROUTE_PREVIEW = "preview/{resumeId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResumeCraftTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedNavHost(user = user)
        }
    }
}

@Composable
private fun AuthenticatedNavHost(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val uid = user.uid

    NavHost(
        navController = navController,
        startDestination = ROUTE_PORTFOLIO
    ) {
        composable(ROUTE_PORTFOLIO) {
            val portfolioViewModel: PortfolioViewModel = viewModel(
                key = "portfolio_$uid",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        PortfolioViewModel(ResumeRepository(db), uid)
                    }
                }
            )
            PortfolioScreen(
                viewModel = portfolioViewModel,
                userDisplayName = user.displayName ?: "",
                userEmail = user.email ?: "",
                userPhotoUrl = user.photoUrl?.toString(),
                onOpenEditor = { resumeId ->
                    navController.navigate("editor/$resumeId")
                },
                onOpenPreview = { resumeId ->
                    navController.navigate("preview/$resumeId")
                },
                onSignOutClick = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }

        composable(
            route = ROUTE_EDITOR,
            arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resumeId = backStackEntry.arguments?.getString("resumeId").orEmpty()
            val editorViewModel: ResumeEditorViewModel = viewModel(
                key = "editor_${uid}_$resumeId",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        ResumeEditorViewModel(ResumeRepository(db), uid, resumeId)
                    }
                }
            )
            ResumeEditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenPreview = { navController.navigate("preview/$resumeId") }
            )
        }

        composable(
            route = ROUTE_PREVIEW,
            arguments = listOf(navArgument("resumeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resumeId = backStackEntry.arguments?.getString("resumeId").orEmpty()
            val editorViewModel: ResumeEditorViewModel = viewModel(
                key = "preview_${uid}_$resumeId",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        ResumeEditorViewModel(ResumeRepository(db), uid, resumeId)
                    }
                }
            )
            ResumePreviewScreen(
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = {
                    val poppedToEditor = navController.popBackStack("editor/$resumeId", inclusive = false)
                    if (!poppedToEditor) {
                        navController.navigate("editor/$resumeId")
                    }
                }
            )
        }
    }
}
