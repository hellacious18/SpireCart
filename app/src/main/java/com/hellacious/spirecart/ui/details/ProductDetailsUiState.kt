package com.hellacious.spirecart.ui.details

import com.hellacious.spirecart.domain.model.Product

data class ProductDetailsUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val cartQuantity: Int = 0,
    val selectedQuantityToAdd: Int = 1,
    val errorMessage: String? = null,
    val isAddedToCartSnackbar: Boolean = false,
    val userMessage: String? = null
) {
    val isError: Boolean
        get() = errorMessage != null && product == null
}
