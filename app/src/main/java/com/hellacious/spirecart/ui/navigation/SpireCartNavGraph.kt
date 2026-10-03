package com.hellacious.spirecart.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hellacious.spirecart.core.di.AppContainer
import com.hellacious.spirecart.ui.ViewModelFactory
import com.hellacious.spirecart.ui.cart.CartScreen
import com.hellacious.spirecart.ui.cart.CartViewModel
import com.hellacious.spirecart.ui.details.ProductDetailsScreen
import com.hellacious.spirecart.ui.details.ProductDetailsViewModel
import com.hellacious.spirecart.ui.products.ProductListScreen
import com.hellacious.spirecart.ui.products.ProductListViewModel

@Composable
fun SpireCartNavGraph(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ProductList.route,
        modifier = modifier
    ) {
        composable(route = Screen.ProductList.route) {
            val viewModel: ProductListViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            ProductListScreen(
                viewModel = viewModel,
                onNavigateToDetails = { productId ->
                    navController.navigate(Screen.ProductDetails.createRoute(productId))
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                }
            )
        }

        composable(
            route = Screen.ProductDetails.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
            val viewModel: ProductDetailsViewModel = viewModel(
                key = "details_$productId",
                factory = ViewModelFactory(
                    appContainer = appContainer,
                    productId = productId
                )
            )
            ProductDetailsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                }
            )
        }

        composable(route = Screen.Cart.route) {
            val viewModel: CartViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            CartScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProducts = {
                    navController.popBackStack(Screen.ProductList.route, inclusive = false)
                }
            )
        }
    }
}
