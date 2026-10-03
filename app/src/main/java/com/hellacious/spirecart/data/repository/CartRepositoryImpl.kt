package com.hellacious.spirecart.data.repository

import com.hellacious.spirecart.data.local.dao.CartDao
import com.hellacious.spirecart.data.mapper.toCartItemEntity
import com.hellacious.spirecart.data.mapper.toDomain
import com.hellacious.spirecart.domain.model.CartItem
import com.hellacious.spirecart.domain.model.CartSummary
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.repository.CartRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CartRepositoryImpl(
    private val cartDao: CartDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CartRepository {

    override fun getCartItems(): Flow<List<CartItem>> {
        return cartDao.getAllCartItems()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getCartSummary(): Flow<CartSummary> {
        return getCartItems().map { items ->
            val totalCount = items.sumOf { it.quantity }
            val totalPrice = items.sumOf { it.totalPrice }
            CartSummary(
                items = items,
                totalItemCount = totalCount,
                totalPrice = totalPrice
            )
        }.flowOn(ioDispatcher)
    }

    override fun getCartItemCount(): Flow<Int> {
        return cartDao.getTotalQuantity()
            .flowOn(ioDispatcher)
    }

    override fun getCartItem(productId: Long): Flow<CartItem?> {
        return cartDao.observeCartItemById(productId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun addToCart(product: Product, quantity: Int): Unit = withContext(ioDispatcher) {
        val existing = cartDao.getCartItemById(product.id)
        if (existing != null) {
            val maxStock = if (product.stock > 0) product.stock else existing.stock
            val newQty = (existing.quantity + quantity).coerceAtMost(maxOf(1, maxStock))
            cartDao.updateQuantity(product.id, newQty)
        } else {
            val initialQty = quantity.coerceAtLeast(1).coerceAtMost(maxOf(1, product.stock))
            cartDao.insertOrUpdate(product.toCartItemEntity(initialQty))
        }
        Unit
    }

    override suspend fun increaseQuantity(productId: Long): Unit = withContext(ioDispatcher) {
        val existing = cartDao.getCartItemById(productId) ?: return@withContext
        if (existing.quantity < existing.stock) {
            cartDao.updateQuantity(productId, existing.quantity + 1)
        }
        Unit
    }

    override suspend fun decreaseQuantity(productId: Long): Unit = withContext(ioDispatcher) {
        val existing = cartDao.getCartItemById(productId) ?: return@withContext
        if (existing.quantity > 1) {
            cartDao.updateQuantity(productId, existing.quantity - 1)
        } else {
            cartDao.deleteByProductId(productId)
        }
        Unit
    }

    override suspend fun updateQuantity(productId: Long, quantity: Int): Unit = withContext(ioDispatcher) {
        if (quantity <= 0) {
            cartDao.deleteByProductId(productId)
        } else {
            val existing = cartDao.getCartItemById(productId) ?: return@withContext
            val cappedQty = quantity.coerceAtMost(maxOf(1, existing.stock))
            cartDao.updateQuantity(productId, cappedQty)
        }
        Unit
    }

    override suspend fun removeFromCart(productId: Long): Unit = withContext(ioDispatcher) {
        cartDao.deleteByProductId(productId)
        Unit
    }

    override suspend fun clearCart(): Unit = withContext(ioDispatcher) {
        cartDao.clearCart()
        Unit
    }
}
