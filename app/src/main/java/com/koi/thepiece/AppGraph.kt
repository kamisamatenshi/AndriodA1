package com.koi.thepiece

import android.content.Context
import androidx.room.Room
import com.koi.thepiece.data.api.NetworkModule
import com.koi.thepiece.data.db.AppDatabase
import com.koi.thepiece.data.repo.AuthRepository
import com.koi.thepiece.data.repo.CatalogRepository

object AppGraph {
    @Volatile private var db: AppDatabase? = null
    @Volatile private var catalogRepo: CatalogRepository? = null

    @Volatile private var authRepo: AuthRepository? = null
    fun provideDb(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "thepiece.db"
            ).build().also { db = it }
        }
    }

    fun provideCatalogRepository(context: Context): CatalogRepository {
        return catalogRepo ?: synchronized(this) {
            catalogRepo ?: CatalogRepository(
                appContext = context.applicationContext,
                api = NetworkModule.catalogApi,
                dao = provideDb(context).cardDao()
            ).also { catalogRepo = it }
        }
    }

    fun provideAuthRepository(): AuthRepository{
        return authRepo?:synchronized(this) {
            authRepo ?: AuthRepository(
                api = NetworkModule.authApi
            ).also { authRepo = it }
        }
    }


    var token = ""
}
