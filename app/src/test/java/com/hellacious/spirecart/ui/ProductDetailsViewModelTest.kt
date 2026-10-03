package com.hellacious.spirecart.ui

import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.domain.model.CartItem
import com.hellacious.spirecart.domain.model.CartSummary
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory
import com.hellacious.spirecart.domain.repository.CartRepository
import com.hellacious.spirecart.domain.repository.ProductRepository
import com.hellacious.spirecart.ui.details.ProductDetailsViewModel
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
class ProductDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleProduct = Product(
        id = 99,
        title = "Smartphone Ultra",
        description = "Flagship smartphone",
        category = "smartphones",
        price = 999.0,
        discountPercentage = 10.0,
        rating = 4.9,
        stock = 5,
        brand = "FlagshipTech",
        thumbnail = "https://test.com/phone.jpg",
        images = listOf("https://test.com/phone1.jpg"),
        warrantyInformation = "2 years",
        shippingInformation = "Express",
        availabilityStatus = "In Stock",
        returnPolicy = "14 days",
        reviews = emptyList()
    )

    private class FakeProductRepository(
        var productResult: NetworkResult<Product>
    ) : ProductRepository {
        override suspend fun getProducts(limit: Int, skip: Int): NetworkResult<List<Product>> = NetworkResult.Success(emptyList())
        override suspend fun searchProducts(query: String, limit: Int, skip: Int): NetworkResult<List<Product>> = NetworkResult.Success(emptyList())
        override suspend fun getProductDetails(id: Long): NetworkResult<Product> = productResult
        override suspend fun getCategories(): NetworkResult<List<ProductCategory>> = NetworkResult.Success(emptyList())
        override suspend fun getProductsByCategory(category: String, limit: Int, skip: Int): NetworkResult<List<Product>> = NetworkResult.Success(emptyList())
        override fun getProductsPaged(category: String?, query: String?): Flow<androidx.paging.PagingData<Product>> = kotlinx.coroutines.flow.flowOf(androidx.paging.PagingData.empty())
    }

    private class FakeCartRepository : CartRepository {
        val cartItemFlow = MutableStateFlow<CartItem?>(null)
        var addedQty = 0

        override fun getCartItems(): Flow<List<CartItem>> = flowOf(emptyList())
        override fun getCartSummary(): Flow<CartSummary> = flowOf(CartSummary.EMPTY)
        override fun getCartItemCount(): Flow<Int> = flowOf(0)
        override fun getCartItem(productId: Long): Flow<CartItem?> = cartItemFlow
        override suspend fun addToCart(product: Product, quantity: Int) {
            addedQty += quantity
            cartItemFlow.value = CartItem(
                productId = product.id,
                title = product.title,
                price = product.price,
                thumbnail = product.thumbnail,
                quantity = addedQty,
                stock = product.stock,
                category = product.category,
                addedAt = System.currentTimeMillis()
            )
        }
        override suspend fun increaseQuantity(productId: Long) {}
        override suspend fun decreaseQuantity(productId: Long) {}
        override suspend fun updateQuantity(productId: Long, quantity: Int) {}
        override suspend fun removeFromCart(productId: Long) {}
        override suspend fun clearCart() {}
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
    fun `loadProductDetails populates state with product information`() = runTest(testDispatcher) {
        val fakeRepo = FakeProductRepository(NetworkResult.Success(sampleProduct))
        val fakeCartRepo = FakeCartRepository()

        val viewModel = ProductDetailsViewModel(
            productId = 99L,
            productRepository = fakeRepo,
            cartRepository = fakeCartRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.product)
        assertEquals("Smartphone Ultra", state.product?.title)
        assertEquals(999.0, state.product?.price ?: 0.0, 0.001)
    }

    @Test
    fun `addToCart increments quantity and triggers snackbar state`() = runTest(testDispatcher) {
        val fakeRepo = FakeProductRepository(NetworkResult.Success(sampleProduct))
        val fakeCartRepo = FakeCartRepository()

        val viewModel = ProductDetailsViewModel(
            productId = 99L,
            productRepository = fakeRepo,
            cartRepository = fakeCartRepo
        )
        advanceUntilIdle()

        viewModel.incrementQuantityToAdd()
        assertEquals(2, viewModel.uiState.value.selectedQuantityToAdd)

        viewModel.addToCart()
        advanceUntilIdle()

        assertEquals(2, fakeCartRepo.addedQty)
        assertEquals(2, viewModel.uiState.value.cartQuantity)
        assertTrue(viewModel.uiState.value.isAddedToCartSnackbar)
    }
}
