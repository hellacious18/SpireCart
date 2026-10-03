package com.hellacious.spirecart.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_remote_keys")
data class ProductRemoteKeyEntity(
    @PrimaryKey
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "prev_offset")
    val prevOffset: Int?,
    @ColumnInfo(name = "next_offset")
    val nextOffset: Int?
)
