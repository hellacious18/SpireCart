package com.hellacious.spirecart.ui.cart

import com.hellacious.spirecart.domain.model.CartSummary

data class CartUiState(
    val isLoading: Boolean = false,
    val cartSummary: CartSummary = CartSummary.EMPTY,
    val checkoutSuccessMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && cartSummary.items.isEmpty()
}
