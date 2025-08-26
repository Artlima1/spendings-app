package com.arthur.spending

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arthur.spending.ui.dashboard.DashboardScreen
import com.arthur.spending.ui.newtransaction.NewTransactionScreen
import com.arthur.spending.ui.notifications.NotificationsScreen
import com.arthur.spending.ui.theme.SpendingsTheme

// @AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SpendingsTheme {
                val navController = rememberNavController()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination

                            val items = listOf(
                                NavigationItem(
                                    route = "dashboard",
                                    icon = R.drawable.ic_dashboard_black_24dp,
                                    label = "Dashboard"
                                ),
                                NavigationItem(
                                    route = "newtransaction",
                                    icon = android.R.drawable.ic_menu_add,
                                    label = "New Transaction"
                                ),
                                NavigationItem(
                                    route = "notifications",
                                    icon = R.drawable.ic_notifications_black_24dp,
                                    label = "Notifications"
                                )
                            )

                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = item.icon),
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "newtransaction",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen()
                        }
                        composable("newtransaction") {
                            NewTransactionScreen()
                        }
                        composable("notifications") {
                            NotificationsScreen()
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val icon: Int,
    val label: String
)