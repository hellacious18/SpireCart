package com.hellacious.spirecart.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hellacious.spirecart.data.local.dao.CartDao
import com.hellacious.spirecart.data.local.dao.ProductDao
import com.hellacious.spirecart.data.local.dao.ProductRemoteKeyDao
import com.hellacious.spirecart.data.local.entity.CartItemEntity
import com.hellacious.spirecart.data.local.entity.ProductEntity
import com.hellacious.spirecart.data.local.entity.ProductRemoteKeyEntity

@Database(
    entities = [CartItemEntity::class, ProductEntity::class, ProductRemoteKeyEntity::class],
    version = 4,
    exportSchema = false
)
abstract class SpireCartDatabase : RoomDatabase() {

    abstract fun cartDao(): CartDao
    abstract fun productDao(): ProductDao
    abstract fun productRemoteKeyDao(): ProductRemoteKeyDao

    companion object {
        @Volatile
        private var INSTANCE: SpireCartDatabase? = null

        fun getInstance(context: Context): SpireCartDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpireCartDatabase::class.java,
                    "spire_cart_db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
