package com.example.drogi

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// tabela
@Entity(tableName = "route_results")
data class RouteResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routeId: String,
    val timeInSeconds: Long,
    val timestamp: Long
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val routeId: String
)

// zapytanie do bazy
@Dao
interface RouteResultDao {

    // ------------ baza danych z czasami na trasach ----------------
    @Insert
    suspend fun insertResult(result: RouteResultEntity)

    @Delete
    suspend fun deleteResult(result: RouteResultEntity)

    // 3 najlepsze czasy
    @Query("SELECT * FROM route_results WHERE routeId = :routeId ORDER BY timeInSeconds ASC LIMIT 3")
    fun getTop3ForRoute(routeId: String): Flow<List<RouteResultEntity>>

    // wszystkie czasy
    @Query("SELECT * FROM route_results WHERE routeId = :routeId ORDER BY timeInSeconds ASC")
    fun getAllForRoute(routeId: String): Flow<List<RouteResultEntity>>


    // ------------ baza danych z ulubionymi ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun removeFavorite(favorite: FavoriteEntity)

    @Query("SELECT routeId FROM favorites")
    fun getAllFavoriteIds(): Flow<List<String>>
}

// konfiguracja bazy
@Database(entities = [RouteResultEntity::class, FavoriteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeResultDao(): RouteResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drogi_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}