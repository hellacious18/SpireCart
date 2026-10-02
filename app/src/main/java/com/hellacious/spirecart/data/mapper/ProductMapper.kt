package com.hellacious.spirecart.data.mapper

import com.hellacious.spirecart.data.remote.dto.CategoryDto
import com.hellacious.spirecart.data.remote.dto.ProductDto
import com.hellacious.spirecart.data.remote.dto.ReviewDto
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory
import com.hellacious.spirecart.domain.model.ProductReview

fun ProductDto.toDomain(): Product {
    return Product(
        id = id,
        title = title,
        description = description.orEmpty(),
        category = category.orEmpty(),
        price = price,
        discountPercentage = discountPercentage ?: 0.0,
        rating = rating ?: 0.0,
        stock = stock ?: 0,
        brand = brand ?: "Generic",
        thumbnail = thumbnail.orEmpty(),
        images = images ?: emptyList(),
        warrantyInformation = warrantyInformation.orEmpty(),
        shippingInformation = shippingInformation.orEmpty(),
        availabilityStatus = availabilityStatus ?: if ((stock ?: 0) > 0) "In Stock" else "Out of Stock",
        returnPolicy = returnPolicy.orEmpty(),
        reviews = reviews?.map { it.toDomain() } ?: emptyList()
    )
}

fun ReviewDto.toDomain(): ProductReview {
    return ProductReview(
        rating = rating ?: 0,
        comment = comment.orEmpty(),
        date = date.orEmpty(),
        reviewerName = reviewerName ?: "Anonymous"
    )
}

fun CategoryDto.toDomain(): ProductCategory {
    return ProductCategory(
        slug = slug,
        name = name
    )
}
