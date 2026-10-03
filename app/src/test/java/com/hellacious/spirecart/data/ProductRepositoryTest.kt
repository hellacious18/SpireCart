package com.hellacious.spirecart.data

import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.data.remote.api.DummyJsonApiService
import com.hellacious.spirecart.data.remote.dto.CategoryDto
import com.hellacious.spirecart.data.remote.dto.ProductDto
import com.hellacious.spirecart.data.remote.dto.ProductsResponseDto
import com.hellacious.spirecart.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun <T : Any> fakePagingSource(items: List<T>): androidx.paging.PagingSource<Int, T> =
        object : androidx.paging.PagingSource<Int, T>() {
            override fun getRefreshKey(state: androidx.paging.PagingState<Int, T>): Int? = null
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> =
                LoadResult.Page(data = items, prevKey = null, nextKey = null)
        }

    private val fakeProductDto = ProductDto(
        id = 10,
        title = "Test Phone",
        description = "A great test phone",
        category = "smartphones",
        price = 799.0,
        discountPercentage = 5.0,
        rating = 4.5,
        stock = 20,
        tags = listOf("tech"),
        brand = "TestBrand",
        sku = "TEST-01",
        weight = 1.0,
        dimensions = null,
        warrantyInformation = "1 Year",
        shippingInformation = "Free shipping",
        availabilityStatus = "In Stock",
        reviews = emptyList(),
        returnPolicy = "30 days",
        minimumOrderQuantity = 1,
        images = listOf("https://test.com/phone.jpg"),
        thumbnail = "https://test.com/phone_thumb.jpg"
    )

    private class FakeApiService(
        private val shouldThrowError: Boolean = false,
        private val productDto: ProductDto
    ) : DummyJsonApiService {
        override suspend fun getProducts(limit: Int, skip: Int): ProductsResponseDto {
            if (shouldThrowError) throw IOException("Failed to connect")
            return ProductsResponseDto(
                products = listOf(productDto),
                total = 1,
                skip = skip,
                limit = limit
            )
        }

        override suspend fun searchProducts(query: String, limit: Int, skip: Int): ProductsResponseDto {
            if (shouldThrowError) throw IOException("Search timeout")
            return ProductsResponseDto(
                products = if (query == "empty") emptyList() else listOf(productDto),
                total = if (query == "empty") 0 else 1,
                skip = skip,
                limit = limit
            )
        }

        override suspend fun getProductDetails(id: Long): ProductDto {
            if (shouldThrowError) throw IOException("Not found")
            return productDto.copy(id = id)
        }

        override suspend fun getCategories(): List<CategoryDto> {
            if (shouldThrowError) throw IOException("Network error")
            return listOf(CategoryDto("smartphones", "Smartphones", null))
        }

        override suspend fun getProductsByCategory(
            category: String,
            limit: Int,
            skip: Int
        ): ProductsResponseDto {
            if (shouldThrowError) throw IOException("Network error")
            return ProductsResponseDto(
                products = listOf(productDto),
                total = 1,
                skip = skip,
                limit = limit
            )
        }
    }

    @Test
    fun `getProducts returns success when api responds correctly`() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldThrowError = false, productDto = fakeProductDto)
        val repo = ProductRepositoryImpl(apiService = fakeApi, ioDispatcher = testDispatcher)

        val result = repo.getProducts(limit = 10, skip = 0)
        assertTrue(result is NetworkResult.Success)
        val products = (result as NetworkResult.Success).data
        assertEquals(1, products.size)
        assertEquals("Test Phone", products[0].title)
    }

    @Test
    fun `getProducts returns error when network fails`() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldThrowError = true, productDto = fakeProductDto)
        val repo = ProductRepositoryImpl(apiService = fakeApi, ioDispatcher = testDispatcher)

        val result = repo.getProducts(limit = 10, skip = 0)
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("Network error"))
    }

    @Test
    fun `searchProducts returns empty list on empty query results`() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldThrowError = false, productDto = fakeProductDto)
        val repo = ProductRepositoryImpl(apiService = fakeApi, ioDispatcher = testDispatcher)

        val result = repo.searchProducts(query = "empty")
        assertTrue(result is NetworkResult.Success)
        val products = (result as NetworkResult.Success).data
        assertTrue(products.isEmpty())
    }

    @Test
    fun `getProducts returns cached data when api fails and cache is present`() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldThrowError = true, productDto = fakeProductDto)
        val fakeDao = object : com.hellacious.spirecart.data.local.dao.ProductDao {
            private val list = mutableListOf(
                com.hellacious.spirecart.data.local.entity.ProductEntity(
                    id = 10,
                    title = "Cached Phone",
                    description = "A cached test phone",
                    category = "smartphones",
                    price = 799.0,
                    discountPercentage = 5.0,
                    rating = 4.5,
                    stock = 20,
                    brand = "TestBrand",
                    thumbnail = "https://test.com/phone_thumb.jpg",
                    imagesCsv = "https://test.com/phone.jpg",
                    warrantyInformation = "1 Year",
                    shippingInformation = "Free shipping",
                    availabilityStatus = "In Stock",
                    returnPolicy = "30 days"
                )
            )

            override suspend fun getAllProducts(): List<com.hellacious.spirecart.data.local.entity.ProductEntity> = list
            override fun getProductsPagingSource(): androidx.paging.PagingSource<Int, com.hellacious.spirecart.data.local.entity.ProductEntity> = fakePagingSource(list)
            override fun getProductsByCategoryPagingSource(category: String): androidx.paging.PagingSource<Int, com.hellacious.spirecart.data.local.entity.ProductEntity> = fakePagingSource(list.filter { it.category == category })
            override suspend fun getProductById(productId: Long) = list.find { it.id == productId }
            override suspend fun getProductsByCategory(category: String) = list.filter { it.category == category }
            override suspend fun searchProducts(query: String) = list.filter { it.title.contains(query, ignoreCase = true) }
            override fun searchProductsPagingSource(query: String): androidx.paging.PagingSource<Int, com.hellacious.spirecart.data.local.entity.ProductEntity> = fakePagingSource(list.filter { it.title.contains(query, ignoreCase = true) })
            override suspend fun getDistinctCategories() = list.map { it.category }.distinct()
            override suspend fun insertProducts(products: List<com.hellacious.spirecart.data.local.entity.ProductEntity>) { list.addAll(products) }
            override suspend fun clearAll() { list.clear() }
        }

        val repo = ProductRepositoryImpl(apiService = fakeApi, productDao = fakeDao, ioDispatcher = testDispatcher)
        val result = repo.getProducts(limit = 10, skip = 0)
        assertTrue(result is NetworkResult.Success)
        val products = (result as NetworkResult.Success).data
        assertEquals(1, products.size)
        assertEquals("Cached Phone", products[0].title)
    }

    @Test
    fun `getProducts caches images locally and updates entity`() = runTest(testDispatcher) {
        val fakeApi = FakeApiService(shouldThrowError = false, productDto = fakeProductDto)
        val insertedList = mutableListOf<com.hellacious.spirecart.data.local.entity.ProductEntity>()
        val fakeDao = object : com.hellacious.spirecart.data.local.dao.ProductDao {
            override suspend fun getAllProducts() = insertedList
            override fun getProductsPagingSource(): androidx.paging.PagingSource<Int, com.hellacious.spirecart.data.local.entity.ProductEntity> = fakePagingSource(insertedList)
            override fun getProductsByCategoryPagingSource(category: String): androidx.paging.PagingSource<Int, com.hellacious.spirecart.data.local.entity.ProductEntity> = fakePagingSource(insertedList.filter { it.category == category })
            override suspend fun getProductById(productId: Long) = insertedList.find { it.id == productId }
            override suspend fun getProductsByCategory(category: String) = insertedList.filter { it.category == category }
            override suspend fun searchProducts(query: String) = insertedList.filter { it.title.contains(query, ignoreCase = true) }
            override fun searchProductsPagingSource(query: String): androidx.paging.PagingSource<Int, com.hellacious.spirecart.data.local.entity.ProductEntity> = fakePagingSource(insertedList.filter { it.title.contains(query, ignoreCase = true) })
            override suspend fun getDistinctCategories() = insertedList.map { it.category }.distinct()
            override suspend fun insertProducts(products: List<com.hellacious.spirecart.data.local.entity.ProductEntity>) {
                insertedList.clear()
                insertedList.addAll(products)
            }
            override suspend fun clearAll() { insertedList.clear() }
        }

        val fakeStorage = object : com.hellacious.spirecart.data.local.storage.LocalImageStorage {
            override suspend fun saveImageLocally(url: String): String = "/local/path/${url.hashCode()}.jpg"
            override fun getLocalImagePath(url: String): String? = "/local/path/${url.hashCode()}.jpg"
        }

        val repo = ProductRepositoryImpl(
            apiService = fakeApi,
            productDao = fakeDao,
            localImageStorage = fakeStorage,
            ioDispatcher = testDispatcher
        )

        val result = repo.getProducts(limit = 10, skip = 0)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, insertedList.size)
        assertTrue(insertedList[0].localThumbnailPath?.startsWith("/local/path/") == true)
    }
}
