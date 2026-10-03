package com.hellacious.spirecart.domain.repository

import com.hellacious.spirecart.domain.model.CartItem
import com.hellacious.spirecart.domain.model.CartSummary
import com.hellacious.spirecart.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartItems(): Flow<List<CartItem>>
    fun getCartSummary(): Flow<CartSummary>
    fun getCartItemCount(): Flow<Int>
    fun getCartItem(productId: Long): Flow<CartItem?>
    suspend fun addToCart(product: Product, quantity: Int = 1)
    suspend fun increaseQuantity(productId: Long)
    suspend fun decreaseQuantity(productId: Long)
    suspend fun updateQuantity(productId: Long, quantity: Int)
    suspend fun removeFromCart(productId: Long)
    suspend fun clearCart()
}
