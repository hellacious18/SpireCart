package com.hellacious.spirecart.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductsResponseDto(
    @SerializedName("products")
    val products: List<ProductDto>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("skip")
    val skip: Int,
    @SerializedName("limit")
    val limit: Int
)
