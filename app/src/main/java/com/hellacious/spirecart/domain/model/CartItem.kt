package com.hellacious.spirecart.domain.model

data class CartItem(
    val productId: Long,
    val title: String,
    val price: Double,
    val thumbnail: String,
    val quantity: Int,
    val stock: Int,
    val category: String,
    val addedAt: Long
) {
    val totalPrice: Double
        get() = price * quantity

    val isMaxStockReached: Boolean
        get() = quantity >= stock
}

data class CartSummary(
    val items: List<CartItem>,
    val totalItemCount: Int,
    val totalPrice: Double
) {
    companion object {
        val EMPTY = CartSummary(
            items = emptyList(),
            totalItemCount = 0,
            totalPrice = 0.0
        )
    }
}
