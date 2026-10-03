package com.hellacious.spirecart.data

import com.hellacious.spirecart.data.local.dao.CartDao
import com.hellacious.spirecart.data.local.entity.CartItemEntity
import com.hellacious.spirecart.data.repository.CartRepositoryImpl
import com.hellacious.spirecart.domain.model.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCartDao : CartDao {
        private val itemsMap = mutableMapOf<Long, CartItemEntity>()
        private val itemsFlow = MutableStateFlow<List<CartItemEntity>>(emptyList())

        private fun emit() {
            itemsFlow.value = itemsMap.values.toList()
        }

        override fun getAllCartItems(): Flow<List<CartItemEntity>> = itemsFlow

        override suspend fun getCartItemById(productId: Long): CartItemEntity? = itemsMap[productId]

        override fun observeCartItemById(productId: Long): Flow<CartItemEntity?> =
            itemsFlow.map { list -> list.find { it.productId == productId } }

        override suspend fun insertOrUpdate(item: CartItemEntity) {
            itemsMap[item.productId] = item
            emit()
        }

        override suspend fun update(item: CartItemEntity) {
            itemsMap[item.productId] = item
            emit()
        }

        override suspend fun updateQuantity(productId: Long, quantity: Int) {
            val existing = itemsMap[productId]
            if (existing != null) {
                itemsMap[productId] = existing.copy(quantity = quantity)
                emit()
            }
        }

        override suspend fun deleteByProductId(productId: Long) {
            itemsMap.remove(productId)
            emit()
        }

        override suspend fun clearCart() {
            itemsMap.clear()
            emit()
        }

        override fun getCartItemTypeCount(): Flow<Int> = itemsFlow.map { it.size }

        override fun getTotalQuantity(): Flow<Int> = itemsFlow.map { list -> list.sumOf { it.quantity } }

        override fun getTotalPrice(): Flow<Double> = itemsFlow.map { list -> list.sumOf { it.price * it.quantity } }
    }

    private val sampleProduct = Product(
        id = 1,
        title = "Gaming Laptop",
        description = "Powerful laptop",
        category = "laptops",
        price = 1000.0,
        discountPercentage = 10.0,
        rating = 4.8,
        stock = 5,
        brand = "Asus",
        thumbnail = "https://test.com/laptop.png",
        images = emptyList(),
        warrantyInformation = "2 years",
        shippingInformation = "Free",
        availabilityStatus = "In Stock",
        returnPolicy = "30 days",
        reviews = emptyList()
    )

    @Test
    fun `adding product to cart stores item and calculates summary`() = runTest(testDispatcher) {
        val fakeDao = FakeCartDao()
        val repository = CartRepositoryImpl(cartDao = fakeDao, ioDispatcher = testDispatcher)

        repository.addToCart(sampleProduct, quantity = 2)

        val summary = repository.getCartSummary().first()
        assertEquals(1, summary.items.size)
        assertEquals(2, summary.totalItemCount)
        assertEquals(2000.0, summary.totalPrice, 0.001)
    }

    @Test
    fun `adding existing product increments quantity up to stock limit`() = runTest(testDispatcher) {
        val fakeDao = FakeCartDao()
        val repository = CartRepositoryImpl(cartDao = fakeDao, ioDispatcher = testDispatcher)

        repository.addToCart(sampleProduct, quantity = 3)
        repository.addToCart(sampleProduct, quantity = 4) // Total would be 7, but stock is 5

        val summary = repository.getCartSummary().first()
        assertEquals(1, summary.items.size)
        assertEquals(5, summary.items[0].quantity)
        assertEquals(5000.0, summary.totalPrice, 0.001)
        assertTrue(summary.items[0].isMaxStockReached)
    }

    @Test
    fun `decreasing quantity below 1 removes item from cart`() = runTest(testDispatcher) {
        val fakeDao = FakeCartDao()
        val repository = CartRepositoryImpl(cartDao = fakeDao, ioDispatcher = testDispatcher)

        repository.addToCart(sampleProduct, quantity = 1)
        assertEquals(1, repository.getCartSummary().first().items.size)

        repository.decreaseQuantity(sampleProduct.id)
        val summaryAfter = repository.getCartSummary().first()
        assertEquals(0, summaryAfter.items.size)
        assertEquals(0, summaryAfter.totalItemCount)
        assertEquals(0.0, summaryAfter.totalPrice, 0.001)
    }

    @Test
    fun `clearing cart removes all items`() = runTest(testDispatcher) {
        val fakeDao = FakeCartDao()
        val repository = CartRepositoryImpl(cartDao = fakeDao, ioDispatcher = testDispatcher)

        repository.addToCart(sampleProduct, quantity = 2)
        val product2 = sampleProduct.copy(id = 2, title = "Mouse", price = 50.0)
        repository.addToCart(product2, quantity = 1)

        assertEquals(2, repository.getCartSummary().first().items.size)

        repository.clearCart()
        val summary = repository.getCartSummary().first()
        assertTrue(summary.items.isEmpty())
        assertEquals(0, summary.totalItemCount)
        assertEquals(0.0, summary.totalPrice, 0.001)
    }
}
