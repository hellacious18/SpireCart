package com.hellacious.spirecart.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("price")
    val price: Double,
    @SerializedName("discountPercentage")
    val discountPercentage: Double?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("stock")
    val stock: Int?,
    @SerializedName("tags")
    val tags: List<String>?,
    @SerializedName("brand")
    val brand: String?,
    @SerializedName("sku")
    val sku: String?,
    @SerializedName("weight")
    val weight: Double?,
    @SerializedName("dimensions")
    val dimensions: DimensionsDto?,
    @SerializedName("warrantyInformation")
    val warrantyInformation: String?,
    @SerializedName("shippingInformation")
    val shippingInformation: String?,
    @SerializedName("availabilityStatus")
    val availabilityStatus: String?,
    @SerializedName("reviews")
    val reviews: List<ReviewDto>?,
    @SerializedName("returnPolicy")
    val returnPolicy: String?,
    @SerializedName("minimumOrderQuantity")
    val minimumOrderQuantity: Int?,
    @SerializedName("images")
    val images: List<String>?,
    @SerializedName("thumbnail")
    val thumbnail: String?
)

data class DimensionsDto(
    @SerializedName("width")
    val width: Double?,
    @SerializedName("height")
    val height: Double?,
    @SerializedName("depth")
    val depth: Double?
)

data class ReviewDto(
    @SerializedName("rating")
    val rating: Int?,
    @SerializedName("comment")
    val comment: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("reviewerName")
    val reviewerName: String?,
    @SerializedName("reviewerEmail")
    val reviewerEmail: String?
)
