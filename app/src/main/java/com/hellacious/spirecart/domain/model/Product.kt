package com.hellacious.spirecart.domain.model

data class Product(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val brand: String,
    val thumbnail: String,
    val images: List<String>,
    val warrantyInformation: String,
    val shippingInformation: String,
    val availabilityStatus: String,
    val returnPolicy: String,
    val reviews: List<ProductReview>
)

data class ProductReview(
    val rating: Int,
    val comment: String,
    val date: String,
    val reviewerName: String
)

data class ProductCategory(
    val slug: String,
    val name: String
)
