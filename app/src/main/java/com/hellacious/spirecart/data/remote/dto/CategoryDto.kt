package com.hellacious.spirecart.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CategoryDto(
    @SerializedName("slug")
    val slug: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("url")
    val url: String?
)
