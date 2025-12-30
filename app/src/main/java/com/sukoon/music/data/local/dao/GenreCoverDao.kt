package com.sukoon.music.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sukoon.music.data.local.entity.GenreCoverEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for genre cover operations.
 */
@Dao
interface GenreCoverDao {

    /**
     * Get custom cover for a genre.
     */
    @Query("SELECT * FROM genre_covers WHERE genreId = :genreId")
    suspend fun getByGenreId(genreId: Long): GenreCoverEntity?

    /**
     * Get custom cover as Flow for reactive updates.
     */
    @Query("SELECT * FROM genre_covers WHERE genreId = :genreId")
    fun getByGenreIdFlow(genreId: Long): Flow<GenreCoverEntity?>

    /**
     * Insert or update genre cover.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(genreCover: GenreCoverEntity)

    /**
     * Delete custom cover for a genre.
     */
    @Query("DELETE FROM genre_covers WHERE genreId = :genreId")
    suspend fun delete(genreId: Long)

    /**
     * Get all genre covers.
     */
    @Query("SELECT * FROM genre_covers")
    fun getAllCovers(): Flow<List<GenreCoverEntity>>
}
