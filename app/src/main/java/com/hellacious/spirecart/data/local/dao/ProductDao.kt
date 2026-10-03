package com.hellacious.spirecart.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hellacious.spirecart.data.local.entity.ProductEntity

@Dao
interface ProductDao {

    @Query("SELECT * FROM cached_products ORDER BY id ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM cached_products ORDER BY id ASC")
    fun getProductsPagingSource(): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM cached_products WHERE category = :category ORDER BY id ASC")
    fun getProductsByCategoryPagingSource(category: String): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM cached_products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Query("SELECT * FROM cached_products WHERE category = :category ORDER BY id ASC")
    suspend fun getProductsByCategory(category: String): List<ProductEntity>

    @Query("SELECT * FROM cached_products WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY id ASC")
    suspend fun searchProducts(query: String): List<ProductEntity>

    @Query("SELECT * FROM cached_products WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY id ASC")
    fun searchProductsPagingSource(query: String): PagingSource<Int, ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("SELECT DISTINCT category FROM cached_products WHERE category IS NOT NULL AND category != ''")
    suspend fun getDistinctCategories(): List<String>

    @Query("DELETE FROM cached_products")
    suspend fun clearAll()
}
