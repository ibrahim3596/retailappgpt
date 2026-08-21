package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.retailpos.auth.SupabaseClientProvider
import com.example.retailpos.auth.UserPermissions
import com.example.retailpos.auth.userRole
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.Screen
import com.example.retailpos.ui.screens.*
import com.example.ui.theme.RetailPosTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseClientProvider.client.handleDeeplinks(intent)
        enableEdgeToEdge()
        setContent {
            RetailPosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RetailPosApp(mainViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseClientProvider.client.handleDeeplinks(intent)
    }
}

@Composable
fun RetailPosApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isSetupComplete by viewModel.isSetupComplete.collectAsStateWithLifecycle()
    val loggedInUserId by viewModel.loggedInUserId.collectAsStateWithLifecycle()

    LaunchedEffect(isSetupComplete, loggedInUserId) {
        if (isSetupComplete == true) {
            if (loggedInUserId == null) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == Screen.Login.route || currentRoute == Screen.Setup.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        } else if (isSetupComplete == false) {
            navController.navigate(Screen.Setup.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    if (isSetupComplete == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (isSetupComplete == false) Screen.Setup.route
        else if (loggedInUserId == null) Screen.Login.route
        else Screen.Home.route
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(viewModel = viewModel)
        }

        composable(Screen.Login.route) {
            LoginScreen(viewModel = viewModel)
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPos = { navController.navigate(Screen.POS.route) },
                onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                onNavigateToInventory = { navController.navigate(Screen.Inventory.route) },
                onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                onNavigateToSync = { navController.navigate(Screen.SyncConflicts.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.POS.route) {
            PosScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCameraScanner = {
                    navController.navigate(Screen.CameraScanner.createRoute("BILLING"))
                },
                onNavigateToReceipt = { invoiceId ->
                    navController.navigate(Screen.ReceiptPreview.createRoute(invoiceId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.CameraScanner.route,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "BILLING"
            CameraScannerScreen(
                viewModel = viewModel,
                mode = mode,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPos = {
                    navController.navigate(Screen.POS.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateToProductReview = { barcode ->
                    navController.navigate(Screen.ProductReview.createRoute(barcode))
                }
            )
        }

        composable(Screen.Products.route) {
            ProductListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCameraScanner = {
                    navController.navigate(Screen.CameraScanner.createRoute("PRODUCT_MANAGEMENT"))
                },
                onNavigateToProductDetail = { barcode ->
                    navController.navigate(Screen.ProductReview.createRoute(barcode))
                }
            )
        }

        composable(
            route = Screen.ProductReview.route,
            arguments = listOf(navArgument("barcode") { type = NavType.StringType })
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
            ProductReviewScreen(
                viewModel = viewModel,
                barcode = barcode,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Inventory.route) {
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val role = currentUser.userRole
            if (UserPermissions.canAccessInventory(role)) {
                InventoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Customers.route) {
            CustomerLedgerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SyncConflicts.route) {
            SyncConflictScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ReceiptPreview.route,
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            ReceiptPreviewScreen(
                invoiceId = invoiceId,
                viewModel = viewModel,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Analytics.route) {
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val role = currentUser.userRole
            if (UserPermissions.canAccessAnalytics(role)) {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
