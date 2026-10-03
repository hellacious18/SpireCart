package com.hellacious.spirecart.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.map
import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.core.network.safeApiCall
import com.hellacious.spirecart.data.local.dao.ProductDao
import com.hellacious.spirecart.data.local.database.SpireCartDatabase
import com.hellacious.spirecart.data.local.entity.ProductEntity
import com.hellacious.spirecart.data.local.storage.LocalImageStorage
import com.hellacious.spirecart.data.mapper.toDomain
import com.hellacious.spirecart.data.mapper.toEntity
import com.hellacious.spirecart.data.paging.ProductRemoteMediator
import com.hellacious.spirecart.data.remote.api.ApiClient
import com.hellacious.spirecart.data.remote.api.DummyJsonApiService
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory
import com.hellacious.spirecart.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val apiService: DummyJsonApiService = ApiClient.apiService,
    private val productDao: ProductDao? = null,
    private val database: SpireCartDatabase? = null,
    private val localImageStorage: LocalImageStorage? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProductRepository {

    private val activeProductDao: ProductDao? = productDao ?: database?.productDao()

    private suspend fun cacheProductsWithImages(products: List<Product>) {
        if (activeProductDao == null) return

        // 1. Immediately cache metadata to Room
        val initialEntities = products.map { it.toEntity() }
        activeProductDao.insertProducts(initialEntities)

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
                activeProductDao.insertProducts(updatedEntities)
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getProductsPaged(
        category: String?,
        query: String?
    ): Flow<PagingData<Product>> {
        val pagingSourceFactory = {
            when {
                !query.isNullOrBlank() -> activeProductDao?.searchProductsPagingSource(query)
                    ?: EmptyProductPagingSource()
                !category.isNullOrBlank() -> activeProductDao?.getProductsByCategoryPagingSource(category)
                    ?: EmptyProductPagingSource()
                else -> activeProductDao?.getProductsPagingSource()
                    ?: EmptyProductPagingSource()
            }
        }

        val mediator = database?.let {
            ProductRemoteMediator(
                apiService = apiService,
                database = it,
                localImageStorage = localImageStorage,
                category = category,
                query = query
            )
        }

        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            remoteMediator = mediator,
            pagingSourceFactory = pagingSourceFactory
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
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
                    val cached = activeProductDao?.getAllProducts()?.map { it.toDomain() }
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
                    val cachedResults = activeProductDao?.searchProducts(query)?.map { it.toDomain() }
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
                    val cached = activeProductDao?.getProductById(id)?.toDomain()
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
                    val cachedCategories = activeProductDao?.getDistinctCategories()?.map { slug ->
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
                    val cached = activeProductDao?.getProductsByCategory(category)?.map { it.toDomain() }
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

private class EmptyProductPagingSource : PagingSource<Int, ProductEntity>() {
    override fun getRefreshKey(state: PagingState<Int, ProductEntity>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ProductEntity> {
        return LoadResult.Page(
            data = emptyList(),
            prevKey = null,
            nextKey = null
        )
    }
}
