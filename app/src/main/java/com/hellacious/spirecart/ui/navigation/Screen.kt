package com.hellacious.spirecart.ui.navigation

sealed class Screen(val route: String) {
    object ProductList : Screen("product_list")
    object ProductDetails : Screen("product_details/{productId}") {
        fun createRoute(productId: Long): String = "product_details/$productId"
    }
    object Cart : Screen("cart")
}
