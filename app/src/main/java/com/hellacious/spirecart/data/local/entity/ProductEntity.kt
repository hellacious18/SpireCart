package com.hellacious.spirecart.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_products")
data class ProductEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "price")
    val price: Double,
    @ColumnInfo(name = "discount_percentage")
    val discountPercentage: Double,
    @ColumnInfo(name = "rating")
    val rating: Double,
    @ColumnInfo(name = "stock")
    val stock: Int,
    @ColumnInfo(name = "brand")
    val brand: String,
    @ColumnInfo(name = "thumbnail")
    val thumbnail: String,
    @ColumnInfo(name = "images_csv")
    val imagesCsv: String,
    @ColumnInfo(name = "local_thumbnail_path")
    val localThumbnailPath: String? = null,
    @ColumnInfo(name = "local_images_csv")
    val localImagesCsv: String? = null,
    @ColumnInfo(name = "warranty_information")
    val warrantyInformation: String,
    @ColumnInfo(name = "shipping_information")
    val shippingInformation: String,
    @ColumnInfo(name = "availability_status")
    val availabilityStatus: String,
    @ColumnInfo(name = "return_policy")
    val returnPolicy: String
)
