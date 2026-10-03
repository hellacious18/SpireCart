package com.hellacious.spirecart.ui

import com.hellacious.spirecart.domain.model.CartItem
import com.hellacious.spirecart.domain.model.CartSummary
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.repository.CartRepository
import com.hellacious.spirecart.ui.cart.CartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleCartItem = CartItem(
        productId = 1,
        title = "Headphones",
        price = 150.0,
        thumbnail = "https://test.com/hp.jpg",
        quantity = 2,
        stock = 10,
        category = "electronics",
        addedAt = System.currentTimeMillis()
    )

    private class FakeCartRepository : CartRepository {
        val summaryFlow = MutableStateFlow(
            CartSummary(
                items = emptyList(),
                totalItemCount = 0,
                totalPrice = 0.0
            )
        )

        override fun getCartItems(): Flow<List<CartItem>> = flowOf(emptyList())
        override fun getCartSummary(): Flow<CartSummary> = summaryFlow
        override fun getCartItemCount(): Flow<Int> = flowOf(0)
        override fun getCartItem(productId: Long): Flow<CartItem?> = flowOf(null)
        override suspend fun addToCart(product: Product, quantity: Int) {}
        override suspend fun increaseQuantity(productId: Long) {
            val current = summaryFlow.value.items.firstOrNull { it.productId == productId }
            if (current != null) {
                val updated = current.copy(quantity = current.quantity + 1)
                val newList = listOf(updated)
                summaryFlow.value = CartSummary(newList, updated.quantity, updated.totalPrice)
            }
        }
        override suspend fun decreaseQuantity(productId: Long) {
            val current = summaryFlow.value.items.firstOrNull { it.productId == productId }
            if (current != null) {
                if (current.quantity > 1) {
                    val updated = current.copy(quantity = current.quantity - 1)
                    val newList = listOf(updated)
                    summaryFlow.value = CartSummary(newList, updated.quantity, updated.totalPrice)
                } else {
                    summaryFlow.value = CartSummary.EMPTY
                }
            }
        }
        override suspend fun updateQuantity(productId: Long, quantity: Int) {}
        override suspend fun removeFromCart(productId: Long) {
            summaryFlow.value = CartSummary.EMPTY
        }
        override suspend fun clearCart() {
            summaryFlow.value = CartSummary.EMPTY
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `observing cart reflects items and summary totals`() = runTest(testDispatcher) {
        val fakeCartRepo = FakeCartRepository()
        val viewModel = CartViewModel(fakeCartRepo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)

        fakeCartRepo.summaryFlow.value = CartSummary(
            items = listOf(sampleCartItem),
            totalItemCount = 2,
            totalPrice = 300.0
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(1, state.cartSummary.items.size)
        assertEquals(2, state.cartSummary.totalItemCount)
        assertEquals(300.0, state.cartSummary.totalPrice, 0.001)
    }

    @Test
    fun `checkout clears cart and presents success confirmation message`() = runTest(testDispatcher) {
        val fakeCartRepo = FakeCartRepository()
        fakeCartRepo.summaryFlow.value = CartSummary(
            items = listOf(sampleCartItem),
            totalItemCount = 2,
            totalPrice = 300.0
        )
        val viewModel = CartViewModel(fakeCartRepo)
        advanceUntilIdle()

        viewModel.checkout()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertTrue(state.isCheckoutSuccess)
    }
}
