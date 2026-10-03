package com.hellacious.spirecart.domain.repository

import androidx.paging.PagingData
import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun getProducts(limit: Int = 30, skip: Int = 0): NetworkResult<List<Product>>
    suspend fun searchProducts(query: String, limit: Int = 30, skip: Int = 0): NetworkResult<List<Product>>
    suspend fun getProductDetails(id: Long): NetworkResult<Product>
    suspend fun getCategories(): NetworkResult<List<ProductCategory>>
    suspend fun getProductsByCategory(category: String, limit: Int = 30, skip: Int = 0): NetworkResult<List<Product>>
    fun getProductsPaged(category: String? = null, query: String? = null): Flow<PagingData<Product>>
}
