package com.hellacious.spirecart.data.repository

import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.core.network.safeApiCall
import com.hellacious.spirecart.data.local.dao.ProductDao
import com.hellacious.spirecart.data.local.storage.LocalImageStorage
import com.hellacious.spirecart.data.mapper.toDomain
import com.hellacious.spirecart.data.mapper.toEntity
import com.hellacious.spirecart.data.remote.api.ApiClient
import com.hellacious.spirecart.data.remote.api.DummyJsonApiService
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory
import com.hellacious.spirecart.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val apiService: DummyJsonApiService = ApiClient.apiService,
    private val productDao: ProductDao? = null,
    private val localImageStorage: LocalImageStorage? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProductRepository {

    private suspend fun cacheProductsWithImages(products: List<Product>) {
        if (productDao == null) return

        // 1. Immediately cache metadata to Room
        val initialEntities = products.map { it.toEntity() }
        productDao.insertProducts(initialEntities)

        // 2. Pre-download images locally and update Room entities with local storage file paths
        if (localImageStorage != null) {
            withContext(ioDispatcher) {
                val updatedEntities = products.map { product ->
                    async {
                        val localThumb = if (product.thumbnail.isNotBlank()) {
                            localImageStorage.saveImageLocally(product.thumbnail)
                        } else null

                        val localImages = product.images.map { imgUrl ->
                            async { localImageStorage.saveImageLocally(imgUrl) }
                        }.awaitAll()

                        product.toEntity(
                            localThumbnailPath = localThumb,
                            localImagesCsv = if (localImages.isNotEmpty()) localImages.joinToString("|||") else null
                        )
                    }
                }.awaitAll()
                productDao.insertProducts(updatedEntities)
            }
        }
    }

    override suspend fun getProducts(limit: Int, skip: Int): NetworkResult<List<Product>> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.getProducts(limit, skip) }) {
                is NetworkResult.Success -> {
                    val domainProducts = result.data.products.map { it.toDomain() }
                    cacheProductsWithImages(domainProducts)
                    NetworkResult.Success(domainProducts)
                }
                is NetworkResult.Error -> {
                    val cached = productDao?.getAllProducts()?.map { it.toDomain() }
                    if (!cached.isNullOrEmpty()) {
                        NetworkResult.Success(cached)
                    } else {
                        NetworkResult.Error(result.message, result.code, result.cause)
                    }
                }
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }

    override suspend fun searchProducts(query: String, limit: Int, skip: Int): NetworkResult<List<Product>> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.searchProducts(query, limit, skip) }) {
                is NetworkResult.Success -> {
                    val domainProducts = result.data.products.map { it.toDomain() }
                    cacheProductsWithImages(domainProducts)
                    NetworkResult.Success(domainProducts)
                }
                is NetworkResult.Error -> {
                    val cachedResults = productDao?.searchProducts(query)?.map { it.toDomain() }
                    if (cachedResults != null) {
                        NetworkResult.Success(cachedResults)
                    } else {
                        NetworkResult.Error(result.message, result.code, result.cause)
                    }
                }
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }

    override suspend fun getProductDetails(id: Long): NetworkResult<Product> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.getProductDetails(id) }) {
                is NetworkResult.Success -> {
                    val domainProduct = result.data.toDomain()
                    cacheProductsWithImages(listOf(domainProduct))
                    NetworkResult.Success(domainProduct)
                }
                is NetworkResult.Error -> {
                    val cached = productDao?.getProductById(id)?.toDomain()
                    if (cached != null) {
                        NetworkResult.Success(cached)
                    } else {
                        NetworkResult.Error(result.message, result.code, result.cause)
                    }
                }
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }

    override suspend fun getCategories(): NetworkResult<List<ProductCategory>> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.getCategories() }) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.data.map { it.toDomain() })
                }
                is NetworkResult.Error -> {
                    val cachedCategories = productDao?.getDistinctCategories()?.map { slug ->
                        ProductCategory(
                            slug = slug,
                            name = slug.replace("-", " ").replaceFirstChar { it.uppercase() }
                        )
                    }
                    if (!cachedCategories.isNullOrEmpty()) {
                        NetworkResult.Success(cachedCategories)
                    } else {
                        NetworkResult.Error(result.message, result.code, result.cause)
                    }
                }
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }

    override suspend fun getProductsByCategory(
        category: String,
        limit: Int,
        skip: Int
    ): NetworkResult<List<Product>> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.getProductsByCategory(category, limit, skip) }) {
                is NetworkResult.Success -> {
                    val domainProducts = result.data.products.map { it.toDomain() }
                    cacheProductsWithImages(domainProducts)
                    NetworkResult.Success(domainProducts)
                }
                is NetworkResult.Error -> {
                    val cached = productDao?.getProductsByCategory(category)?.map { it.toDomain() }
                    if (!cached.isNullOrEmpty()) {
                        NetworkResult.Success(cached)
                    } else {
                        NetworkResult.Error(result.message, result.code, result.cause)
                    }
                }
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }
}
