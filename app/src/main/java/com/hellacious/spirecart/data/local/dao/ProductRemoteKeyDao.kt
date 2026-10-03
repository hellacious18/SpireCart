package com.hellacious.spirecart.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hellacious.spirecart.data.local.entity.ProductRemoteKeyEntity

@Dao
interface ProductRemoteKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKeys: List<ProductRemoteKeyEntity>)

    @Query("SELECT * FROM product_remote_keys WHERE product_id = :productId")
    suspend fun getRemoteKeyForProductId(productId: Long): ProductRemoteKeyEntity?

    @Query("DELETE FROM product_remote_keys")
    suspend fun clearRemoteKeys()
}
