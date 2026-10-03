package com.hellacious.spirecart.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.hellacious.spirecart.data.local.database.SpireCartDatabase
import com.hellacious.spirecart.data.local.entity.ProductEntity
import com.hellacious.spirecart.data.local.entity.ProductRemoteKeyEntity
import com.hellacious.spirecart.data.local.storage.LocalImageStorage
import com.hellacious.spirecart.data.mapper.toDomain
import com.hellacious.spirecart.data.mapper.toEntity
import com.hellacious.spirecart.data.remote.api.DummyJsonApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class ProductRemoteMediator(
    private val apiService: DummyJsonApiService,
    private val database: SpireCartDatabase,
    private val localImageStorage: LocalImageStorage? = null,
    private val category: String? = null,
    private val query: String? = null
) : RemoteMediator<Int, ProductEntity>() {

    private val productDao = database.productDao()
    private val remoteKeyDao = database.productRemoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ProductEntity>
    ): MediatorResult {
        return try {
            val offset = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKey = getRemoteKeyClosestToPosition(state)
                    remoteKey?.nextOffset?.minus(state.config.pageSize) ?: 0
                }
                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    val remoteKey = getRemoteKeyForLastItem(state)
                    val nextOffset = remoteKey?.nextOffset
                        ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
                    nextOffset
                }
            }

            val pageSize = state.config.pageSize
            val response = when {
                !query.isNullOrBlank() -> apiService.searchProducts(query = query, limit = pageSize, skip = offset)
                !category.isNullOrBlank() -> apiService.getProductsByCategory(category = category, limit = pageSize, skip = offset)
                else -> apiService.getProducts(limit = pageSize, skip = offset)
            }

            val products = response.products.map { it.toDomain() }
            val endOfPaginationReached = products.isEmpty() || (offset + products.size >= response.total)

            // Cache images locally if storage is available
            val entities = if (localImageStorage != null) {
                coroutineScope {
                    products.map { product ->
                        async {
                            val localThumb = if (product.thumbnail.isNotBlank()) {
                                localImageStorage.saveImageLocally(product.thumbnail)
                            } else null

                            val localImages = product.images.map { imgUrl ->
                                async { localImageStorage.saveImageLocally(imgUrl) }
                            }.awaitAll()

                            product.toEntity(
                                localThumbnailPath = localThumb,
                                localImagesCsv = if (localImages.isNotEmpty()) localImages.joinToString("|||") else null
                            )
                        }
                    }.awaitAll()
                }
            } else {
                products.map { it.toEntity() }
            }

            val prevOffset = if (offset == 0) null else offset - pageSize
            val nextOffset = if (endOfPaginationReached) null else offset + products.size
            val keys = products.map {
                ProductRemoteKeyEntity(
                    productId = it.id,
                    prevOffset = prevOffset,
                    nextOffset = nextOffset
                )
            }

            if (loadType == LoadType.REFRESH && (category.isNullOrBlank() && query.isNullOrBlank())) {
                remoteKeyDao.clearRemoteKeys()
            }
            remoteKeyDao.insertAll(keys)
            productDao.insertProducts(entities)

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, ProductEntity>): ProductRemoteKeyEntity? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { product ->
            remoteKeyDao.getRemoteKeyForProductId(product.id)
        }
    }

    private suspend fun getRemoteKeyClosestToPosition(state: PagingState<Int, ProductEntity>): ProductRemoteKeyEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { productId ->
                remoteKeyDao.getRemoteKeyForProductId(productId)
            }
        }
    }
}
