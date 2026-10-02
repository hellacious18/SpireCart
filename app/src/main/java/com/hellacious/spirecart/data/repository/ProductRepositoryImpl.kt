package com.hellacious.spirecart.data.repository

import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.core.network.safeApiCall
import com.hellacious.spirecart.data.mapper.toDomain
import com.hellacious.spirecart.data.remote.api.ApiClient
import com.hellacious.spirecart.data.remote.api.DummyJsonApiService
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory
import com.hellacious.spirecart.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val apiService: DummyJsonApiService = ApiClient.apiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProductRepository {

    override suspend fun getProducts(limit: Int, skip: Int): NetworkResult<List<Product>> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.getProducts(limit, skip) }) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.data.products.map { it.toDomain() })
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message, result.code, result.cause)
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }

    override suspend fun searchProducts(query: String, limit: Int, skip: Int): NetworkResult<List<Product>> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.searchProducts(query, limit, skip) }) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.data.products.map { it.toDomain() })
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message, result.code, result.cause)
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }

    override suspend fun getProductDetails(id: Long): NetworkResult<Product> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.getProductDetails(id) }) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.data.toDomain())
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message, result.code, result.cause)
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }

    override suspend fun getCategories(): NetworkResult<List<ProductCategory>> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { apiService.getCategories() }) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.data.map { it.toDomain() })
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message, result.code, result.cause)
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
                    NetworkResult.Success(result.data.products.map { it.toDomain() })
                }
                is NetworkResult.Error -> NetworkResult.Error(result.message, result.code, result.cause)
                is NetworkResult.Loading -> NetworkResult.Loading
            }
        }
}
