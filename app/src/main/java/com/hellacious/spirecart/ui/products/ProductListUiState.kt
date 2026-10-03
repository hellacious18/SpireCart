package com.hellacious.spirecart.ui.products

import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory

data class ProductListUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val isSearching: Boolean = false,
    val cartItemCount: Int = 0,
    val userMessageResId: Int? = null,
    val isOnline: Boolean = true
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && products.isEmpty()

    val isError: Boolean
        get() = errorMessage != null && products.isEmpty()
}
