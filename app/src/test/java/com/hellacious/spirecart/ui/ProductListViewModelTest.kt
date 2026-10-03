package com.hellacious.spirecart.ui

import androidx.paging.PagingData
import com.hellacious.spirecart.core.network.ConnectivityObserver
import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.domain.model.CartItem
import com.hellacious.spirecart.domain.model.CartSummary
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory
import com.hellacious.spirecart.domain.repository.CartRepository
import com.hellacious.spirecart.domain.repository.ProductRepository
import com.hellacious.spirecart.ui.products.ProductListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class ProductListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleProduct = Product(
        id = 1,
        title = "Gaming Laptop",
        description = "High perf laptop",
        category = "laptops",
        price = 1200.0,
        discountPercentage = 5.0,
        rating = 4.7,
        stock = 10,
        brand = "Dell",
        thumbnail = "https://test.com/thumb.jpg",
        images = emptyList(),
        warrantyInformation = "1 year",
        shippingInformation = "Free shipping",
        availabilityStatus = "In Stock",
        returnPolicy = "30 days",
        reviews = emptyList()
    )

    private class FakeProductRepository(
        var productsResult: NetworkResult<List<Product>>,
        var categoriesResult: NetworkResult<List<ProductCategory>>,
        var searchResult: NetworkResult<List<Product>>
    ) : ProductRepository {
        override suspend fun getProducts(limit: Int, skip: Int): NetworkResult<List<Product>> = productsResult
        override suspend fun searchProducts(query: String, limit: Int, skip: Int): NetworkResult<List<Product>> = searchResult
        override suspend fun getProductDetails(id: Long): NetworkResult<Product> = productsResult.let {
            if (it is NetworkResult.Success) NetworkResult.Success(it.data.first()) else NetworkResult.Error("Not found")
        }
        override suspend fun getCategories(): NetworkResult<List<ProductCategory>> = categoriesResult
        override suspend fun getProductsByCategory(category: String, limit: Int, skip: Int): NetworkResult<List<Product>> = productsResult
        override fun getProductsPaged(category: String?, query: String?): Flow<PagingData<Product>> = flowOf(PagingData.empty())
    }

    private class FakeCartRepository : CartRepository {
        val countFlow = MutableStateFlow(0)
        override fun getCartItems(): Flow<List<CartItem>> = flowOf(emptyList())
        override fun getCartSummary(): Flow<CartSummary> = flowOf(CartSummary.EMPTY)
        override fun getCartItemCount(): Flow<Int> = countFlow
        override fun getCartItem(productId: Long): Flow<CartItem?> = flowOf(null)
        override suspend fun addToCart(product: Product, quantity: Int) {
            countFlow.value += quantity
        }
        override suspend fun increaseQuantity(productId: Long) {}
        override suspend fun decreaseQuantity(productId: Long) {}
        override suspend fun updateQuantity(productId: Long, quantity: Int) {}
        override suspend fun removeFromCart(productId: Long) {}
        override suspend fun clearCart() { countFlow.value = 0 }
    }

    private class FakeConnectivityObserver(initial: Boolean = true) : ConnectivityObserver {
        val connectionFlow = MutableStateFlow(initial)
        override val isConnected: Flow<Boolean> = connectionFlow
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
    fun `initial load fetches products and categories successfully`() = runTest(testDispatcher) {
        val fakeRepo = FakeProductRepository(
            productsResult = NetworkResult.Success(listOf(sampleProduct)),
            categoriesResult = NetworkResult.Success(listOf(ProductCategory("laptops", "Laptops"))),
            searchResult = NetworkResult.Success(emptyList())
        )
        val fakeCartRepo = FakeCartRepository()

        val viewModel = ProductListViewModel(fakeRepo, fakeCartRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.products.size)
        assertEquals("Gaming Laptop", state.products[0].title)
        assertEquals(1, state.categories.size)
        assertEquals("laptops", state.categories[0].slug)
    }

    @Test
    fun `search query triggers debounced search`() = runTest(testDispatcher) {
        val searchedProduct = sampleProduct.copy(id = 2, title = "Wireless Mouse")
        val fakeRepo = FakeProductRepository(
            productsResult = NetworkResult.Success(listOf(sampleProduct)),
            categoriesResult = NetworkResult.Success(emptyList()),
            searchResult = NetworkResult.Success(listOf(searchedProduct))
        )
        val fakeCartRepo = FakeCartRepository()

        val viewModel = ProductListViewModel(fakeRepo, fakeCartRepo)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Mouse")
        advanceTimeBy(400)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.products.size)
        assertEquals("Wireless Mouse", state.products[0].title)
    }

    @Test
    fun `error during fetch updates error message in state`() = runTest(testDispatcher) {
        val fakeRepo = FakeProductRepository(
            productsResult = NetworkResult.Error("No internet connection"),
            categoriesResult = NetworkResult.Success(emptyList()),
            searchResult = NetworkResult.Error("Network error")
        )
        val fakeCartRepo = FakeCartRepository()

        val viewModel = ProductListViewModel(fakeRepo, fakeCartRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isError)
        assertNotNull(state.errorMessage)
        assertEquals("No internet connection", state.errorMessage)
    }

    @Test
    fun `connectivity restored triggers auto retry and state update`() = runTest(testDispatcher) {
        val fakeRepo = FakeProductRepository(
            productsResult = NetworkResult.Error("Offline"),
            categoriesResult = NetworkResult.Success(emptyList()),
            searchResult = NetworkResult.Success(emptyList())
        )
        val fakeCartRepo = FakeCartRepository()
        val fakeConnectivity = FakeConnectivityObserver(initial = false)

        val viewModel = ProductListViewModel(fakeRepo, fakeCartRepo, fakeConnectivity)
        advanceUntilIdle()

        assertEquals("Offline", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isOnline)

        // Internet restored!
        fakeRepo.productsResult = NetworkResult.Success(listOf(sampleProduct))
        fakeConnectivity.connectionFlow.value = true
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOnline)
        assertEquals(1, viewModel.uiState.value.products.size)
        assertEquals("Gaming Laptop", viewModel.uiState.value.products[0].title)
    }
}
