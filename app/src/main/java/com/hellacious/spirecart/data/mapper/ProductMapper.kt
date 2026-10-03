package com.hellacious.spirecart.data.mapper

import com.hellacious.spirecart.data.local.entity.ProductEntity
import com.hellacious.spirecart.data.remote.dto.CategoryDto
import com.hellacious.spirecart.data.remote.dto.ProductDto
import com.hellacious.spirecart.data.remote.dto.ReviewDto
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.model.ProductCategory
import com.hellacious.spirecart.domain.model.ProductReview
import java.io.File

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

fun ProductEntity.toDomain(): Product {
    val effectiveThumbnail = if (!localThumbnailPath.isNullOrBlank() && File(localThumbnailPath).exists()) {
        localThumbnailPath
    } else {
        thumbnail
    }

    val effectiveImages = if (!localImagesCsv.isNullOrBlank()) {
        val validLocalImages = localImagesCsv.split("|||").filter { it.isNotBlank() && File(it).exists() }
        if (validLocalImages.isNotEmpty()) {
            validLocalImages
        } else if (imagesCsv.isNotBlank()) {
            imagesCsv.split("|||")
        } else {
            listOf(effectiveThumbnail)
        }
    } else if (imagesCsv.isNotBlank()) {
        imagesCsv.split("|||")
    } else {
        listOf(effectiveThumbnail)
    }

    return Product(
        id = id,
        title = title,
        description = description,
        category = category,
        price = price,
        discountPercentage = discountPercentage,
        rating = rating,
        stock = stock,
        brand = brand,
        thumbnail = effectiveThumbnail,
        images = effectiveImages,
        warrantyInformation = warrantyInformation,
        shippingInformation = shippingInformation,
        availabilityStatus = availabilityStatus,
        returnPolicy = returnPolicy,
        reviews = emptyList()
    )
}

fun Product.toEntity(
    localThumbnailPath: String? = null,
    localImagesCsv: String? = null
): ProductEntity {
    return ProductEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        price = price,
        discountPercentage = discountPercentage,
        rating = rating,
        stock = stock,
        brand = brand,
        thumbnail = thumbnail,
        imagesCsv = images.joinToString("|||"),
        localThumbnailPath = localThumbnailPath,
        localImagesCsv = localImagesCsv,
        warrantyInformation = warrantyInformation,
        shippingInformation = shippingInformation,
        availabilityStatus = availabilityStatus,
        returnPolicy = returnPolicy
    )
}
