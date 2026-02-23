package com.koi.thepiece

import android.content.Context
import androidx.room.Room
import com.koi.thepiece.data.api.NetworkModule
import com.koi.thepiece.data.db.AppDatabase
import com.koi.thepiece.data.local.TokenStore
import com.koi.thepiece.data.repo.AuthRepository
import com.koi.thepiece.data.repo.CatalogRepository
import com.koi.thepiece.data.repo.DeckRepository

/**
 * Simple service locator / dependency graph for the application.
 *
 * Purpose:
 * - Centralizes creation of shared singletons (Room DB, repositories, TokenStore)
 * - Ensures a single instance is reused across the app process
 * - Avoids re-building Room/Retrofit/repositories in multiple places
 *
 * Thread-safety:
 * - Uses double-checked locking with @Volatile and synchronized(this)
 * - Ensures only one instance is created under concurrent access
 *
 * Scope:
 * - All provided instances here are process-singletons (they live as long as the app process).
 *
 * Note:
 * - This is not a full DI framework (e.g., Hilt/Dagger), but is sufficient for a small app.
 */
object AppGraph {
    @Volatile private var db: AppDatabase? = null
    @Volatile private var catalogRepo: CatalogRepository? = null
    @Volatile private var deckRepo: DeckRepository? = null
    @Volatile private var authRepo: AuthRepository? = null
    @Volatile private var tokenStore: TokenStore? = null

    /**
     * Provides a singleton Room database instance.
     *
     * Implementation details:
     * - Uses applicationContext to avoid leaking Activity context.
     * - Database name: "thepiece.db"
     * - No migrations are declared here; schema changes must bump version and supply migrations
     *   (or use fallbackToDestructiveMigration if acceptable for the project).
     */
    fun provideDb(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "thepiece.db"
            ).build().also { db = it }
        }
    }

    /**
     * Provides CatalogRepository singleton.
     *
     * Dependencies:
     * - CatalogApi from NetworkModule (Retrofit)
     * - CardDao from Room database
     *
     * Responsibilities (high level):
     * - Fetch remote card list (server) and upsert into Room
     * - Serve cached cards via Flow for offline-first UI
     * - Update owned quantities locally + sync back to server
     */
    fun provideCatalogRepository(context: Context): CatalogRepository {
        return catalogRepo ?: synchronized(this) {
            catalogRepo ?: CatalogRepository(
                appContext = context.applicationContext,
                api = NetworkModule.catalogApi,
                dao = provideDb(context).cardDao()
            ).also { catalogRepo = it }
        }
    }

    /**
     * Provides DeckRepository singleton.
     *
     * Dependencies:
     * - DeckDao from Room database
     * - DeckApi from NetworkModule (Retrofit)
     *
     * Responsibilities (high level):
     * - Sync owned decks from server and mirror them into Room
     * - Provide local deck persistence for UI screens
     * - Support deck import via share code (server-side import)
     */
    fun provideDeckRepository(context: Context): DeckRepository {
        return deckRepo ?: synchronized(this) {
            deckRepo ?: DeckRepository(
                deckDao = provideDb(context).deckDao(),
                api = NetworkModule.deckApi,
            ).also { deckRepo = it }
        }
    }

    /**
     * Provides AuthRepository singleton.
     *
     * Dependencies:
     * - AuthApi from NetworkModule (Retrofit)
     *
     * Responsibilities (high level):
     * - Login / register calls
     * - Session validation calls
     *
     * Note:
     * - No Context is required because it is purely network-based.
     */
    fun provideAuthRepository(): AuthRepository{
        return authRepo?:synchronized(this) {
            authRepo ?: AuthRepository(
                api = NetworkModule.authApi
            ).also { authRepo = it }
        }
    }

    /**
     * Provides TokenStore singleton.
     *
     * Implementation details:
     * - Uses applicationContext to avoid leaking Activity context.
     * - TokenStore is used by ViewModels/Repositories to attach auth token to API calls.
     */
    fun provideTokenStore(context: Context): TokenStore {
        return tokenStore ?: synchronized(this) {
            tokenStore ?: TokenStore(context.applicationContext).also { tokenStore = it }
        }
    }

}
