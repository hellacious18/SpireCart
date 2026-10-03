package com.hellacious.spirecart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hellacious.spirecart.core.di.AppContainer
import com.hellacious.spirecart.ui.cart.CartViewModel
import com.hellacious.spirecart.ui.details.ProductDetailsViewModel
import com.hellacious.spirecart.ui.products.ProductListViewModel

class ViewModelFactory(
    private val appContainer: AppContainer,
    private val productId: Long? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ProductListViewModel::class.java) -> {
                ProductListViewModel(
                    productRepository = appContainer.productRepository,
                    cartRepository = appContainer.cartRepository,
                    connectivityObserver = appContainer.connectivityObserver
                ) as T
            }
            modelClass.isAssignableFrom(ProductDetailsViewModel::class.java) -> {
                requireNotNull(productId) { "productId must be provided for ProductDetailsViewModel" }
                ProductDetailsViewModel(
                    productId = productId,
                    productRepository = appContainer.productRepository,
                    cartRepository = appContainer.cartRepository
                ) as T
            }
            modelClass.isAssignableFrom(CartViewModel::class.java) -> {
                CartViewModel(
                    cartRepository = appContainer.cartRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
