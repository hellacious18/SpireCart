package com.hellacious.spirecart.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hellacious.spirecart.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY added_at DESC")
    fun getAllCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE product_id = :productId LIMIT 1")
    suspend fun getCartItemById(productId: Long): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE product_id = :productId LIMIT 1")
    fun observeCartItemById(productId: Long): Flow<CartItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: CartItemEntity)

    @Update
    suspend fun update(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE product_id = :productId")
    suspend fun updateQuantity(productId: Long, quantity: Int)

    @Query("DELETE FROM cart_items WHERE product_id = :productId")
    suspend fun deleteByProductId(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("SELECT COUNT(*) FROM cart_items")
    fun getCartItemTypeCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items")
    fun getTotalQuantity(): Flow<Int>

    @Query("SELECT COALESCE(SUM(price * quantity), 0.0) FROM cart_items")
    fun getTotalPrice(): Flow<Double>
}
