package com.hellacious.spirecart.data

import com.google.gson.Gson
import com.hellacious.spirecart.data.mapper.toDomain
import com.hellacious.spirecart.data.remote.dto.CategoryDto
import com.hellacious.spirecart.data.remote.dto.ProductDto
import com.hellacious.spirecart.data.remote.dto.ProductsResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ApiDtoAndMappingTest {

    private val gson = Gson()

    @Test
    fun `parse products json and map to domain models correctly`() {
        val json = """
            {
              "products": [
                {
                  "id": 1,
                  "title": "Essence Mascara Lash Princess",
                  "description": "Popular mascara for lashes.",
                  "category": "beauty",
                  "price": 9.99,
                  "discountPercentage": 10.48,
                  "rating": 4.94,
                  "stock": 99,
                  "tags": ["beauty", "mascara"],
                  "brand": "Essence",
                  "sku": "BEA-ESS-001",
                  "warrantyInformation": "1 week warranty",
                  "shippingInformation": "Ships in 3-5 business days",
                  "availabilityStatus": "In Stock",
                  "reviews": [
                    {
                      "rating": 5,
                      "comment": "Highly impressed!",
                      "date": "2025-04-30T09:41:02.053Z",
                      "reviewerName": "Eleanor Collins",
                      "reviewerEmail": "eleanor.collins@x.dummyjson.com"
                    }
                  ],
                  "returnPolicy": "No return policy",
                  "images": [
                    "https://cdn.dummyjson.com/product-images/beauty/1.webp"
                  ],
                  "thumbnail": "https://cdn.dummyjson.com/product-images/beauty/thumbnail.webp"
                }
              ],
              "total": 194,
              "skip": 0,
              "limit": 1
            }
        """.trimIndent()

        val response = gson.fromJson(json, ProductsResponseDto::class.java)
        assertEquals(194, response.total)
        assertEquals(1, response.products.size)

        val productDto = response.products.first()
        assertEquals(1L, productDto.id)
        assertEquals("Essence Mascara Lash Princess", productDto.title)
        assertEquals(9.99, productDto.price, 0.001)

        val domainProduct = productDto.toDomain()
        assertEquals(1L, domainProduct.id)
        assertEquals("Essence Mascara Lash Princess", domainProduct.title)
        assertEquals("Essence", domainProduct.brand)
        assertEquals(1, domainProduct.reviews.size)
        assertEquals("Eleanor Collins", domainProduct.reviews.first().reviewerName)
        assertEquals(5, domainProduct.reviews.first().rating)
    }

    @Test
    fun `parse categories and map to domain model correctly`() {
        val json = """
            [
              {"slug": "beauty", "name": "Beauty", "url": "https://dummyjson.com/products/category/beauty"},
              {"slug": "fragrances", "name": "Fragrances", "url": "https://dummyjson.com/products/category/fragrances"}
            ]
        """.trimIndent()

        val categories = gson.fromJson(json, Array<CategoryDto>::class.java).toList()
        assertEquals(2, categories.size)

        val domainCategories = categories.map { it.toDomain() }
        assertEquals("beauty", domainCategories[0].slug)
        assertEquals("Beauty", domainCategories[0].name)
    }

    @Test
    fun `handle null fields gracefully during dto mapping`() {
        val productDto = ProductDto(
            id = 42L,
            title = "Minimal Product",
            description = null,
            category = null,
            price = 29.99,
            discountPercentage = null,
            rating = null,
            stock = null,
            tags = null,
            brand = null,
            sku = null,
            weight = null,
            dimensions = null,
            warrantyInformation = null,
            shippingInformation = null,
            availabilityStatus = null,
            reviews = null,
            returnPolicy = null,
            minimumOrderQuantity = null,
            images = null,
            thumbnail = null
        )

        val domain = productDto.toDomain()
        assertEquals(42L, domain.id)
        assertEquals("Minimal Product", domain.title)
        assertEquals("", domain.description)
        assertEquals("", domain.category)
        assertEquals("Generic", domain.brand)
        assertEquals(0.0, domain.rating, 0.001)
        assertEquals(0, domain.stock)
        assertEquals("Out of Stock", domain.availabilityStatus)
        assertEquals(emptyList<String>(), domain.images)
        assertEquals(emptyList<Any>(), domain.reviews)
    }
}
