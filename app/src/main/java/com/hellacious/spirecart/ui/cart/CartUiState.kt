package com.hellacious.spirecart.ui.cart

import com.hellacious.spirecart.domain.model.CartSummary

data class CartUiState(
    val isLoading: Boolean = false,
    val cartSummary: CartSummary = CartSummary.EMPTY,
    val isCheckoutSuccess: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && cartSummary.items.isEmpty()
}
