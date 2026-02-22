package com.koi.thepiece

import android.content.Context
import androidx.room.Room
import com.koi.thepiece.data.api.NetworkModule
import com.koi.thepiece.data.db.AppDatabase
import com.koi.thepiece.data.local.TokenStore
import com.koi.thepiece.data.repo.AuthRepository
import com.koi.thepiece.data.repo.CatalogRepository
import com.koi.thepiece.data.repo.DeckRepository

object AppGraph {
    @Volatile private var db: AppDatabase? = null
    @Volatile private var catalogRepo: CatalogRepository? = null
    @Volatile private var deckRepo: DeckRepository? = null
    @Volatile private var authRepo: AuthRepository? = null

    @Volatile private var tokenStore: TokenStore? = null

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


    fun provideDeckRepository(context: Context): DeckRepository {
        return deckRepo ?: synchronized(this) {
            deckRepo ?: DeckRepository(
                deckDao = provideDb(context).deckDao(),
                api = NetworkModule.deckApi,
            ).also { deckRepo = it }
        }
    }

    fun provideAuthRepository(): AuthRepository{
        return authRepo?:synchronized(this) {
            authRepo ?: AuthRepository(
                api = NetworkModule.authApi
            ).also { authRepo = it }
        }
    }

    fun provideTokenStore(context: Context): TokenStore {
        return tokenStore ?: synchronized(this) {
            tokenStore ?: TokenStore(context.applicationContext).also { tokenStore = it }
        }
    }

}
