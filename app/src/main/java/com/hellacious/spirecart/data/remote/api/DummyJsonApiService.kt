package com.hellacious.spirecart.data.remote.api

import com.hellacious.spirecart.data.remote.dto.CategoryDto
import com.hellacious.spirecart.data.remote.dto.ProductDto
import com.hellacious.spirecart.data.remote.dto.ProductsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DummyJsonApiService {

    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int = 30,
        @Query("skip") skip: Int = 0
    ): ProductsResponseDto

    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("limit") limit: Int = 30,
        @Query("skip") skip: Int = 0
    ): ProductsResponseDto

    @GET("products/{id}")
    suspend fun getProductDetails(
        @Path("id") id: Long
    ): ProductDto

    @GET("products/categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("products/category/{category}")
    suspend fun getProductsByCategory(
        @Path("category") category: String,
        @Query("limit") limit: Int = 30,
        @Query("skip") skip: Int = 0
    ): ProductsResponseDto
}
