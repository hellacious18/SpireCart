package com.hellacious.spirecart.core.di

import android.content.Context
import com.hellacious.spirecart.data.local.database.SpireCartDatabase
import com.hellacious.spirecart.data.local.storage.LocalImageStorage
import com.hellacious.spirecart.data.local.storage.LocalImageStorageImpl
import com.hellacious.spirecart.data.remote.api.ApiClient
import com.hellacious.spirecart.data.remote.api.DummyJsonApiService
import com.hellacious.spirecart.data.repository.CartRepositoryImpl
import com.hellacious.spirecart.data.repository.ProductRepositoryImpl
import com.hellacious.spirecart.domain.repository.CartRepository
import com.hellacious.spirecart.domain.repository.ProductRepository

interface AppContainer {
    val apiService: DummyJsonApiService
    val productRepository: ProductRepository
    val cartRepository: CartRepository
    val localImageStorage: LocalImageStorage
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val apiService: DummyJsonApiService by lazy {
        ApiClient.apiService
    }

    private val database: SpireCartDatabase by lazy {
        SpireCartDatabase.getInstance(context)
    }

    override val localImageStorage: LocalImageStorage by lazy {
        LocalImageStorageImpl(context)
    }

    override val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            apiService = apiService,
            productDao = database.productDao(),
            localImageStorage = localImageStorage
        )
    }

    override val cartRepository: CartRepository by lazy {
        CartRepositoryImpl(cartDao = database.cartDao())
    }
}
