package com.hellacious.spirecart.data.mapper

import com.hellacious.spirecart.data.local.entity.CartItemEntity
import com.hellacious.spirecart.domain.model.CartItem
import com.hellacious.spirecart.domain.model.Product

fun CartItemEntity.toDomain(): CartItem {
    return CartItem(
        productId = productId,
        title = title,
        price = price,
        thumbnail = thumbnail,
        quantity = quantity,
        stock = stock,
        category = category,
        addedAt = addedAt
    )
}

fun Product.toCartItemEntity(quantity: Int = 1): CartItemEntity {
    return CartItemEntity(
        productId = id,
        title = title,
        price = price,
        thumbnail = thumbnail,
        quantity = quantity,
        stock = stock,
        category = category,
        addedAt = System.currentTimeMillis()
    )
}
