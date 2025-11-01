package com.jstuart0.personaldiary.data.local.dao

import androidx.room.*
import com.jstuart0.personaldiary.data.local.entity.EntryFtsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Full-Text Search operations on entries
 * Used only for E2E tier (local search)
 */
@Dao
interface EntryFtsDao {

    /**
     * Search entries by query
     * FTS4 automatically handles tokenization and ranking
     */
    @Query("SELECT * FROM entries_fts WHERE entries_fts MATCH :query ORDER BY rank")
    fun search(query: String): Flow<List<EntryFtsEntity>>

    /**
     * Search with LIMIT for pagination
     */
    @Query("SELECT * FROM entries_fts WHERE entries_fts MATCH :query ORDER BY rank LIMIT :limit")
    suspend fun searchWithLimit(query: String, limit: Int): List<EntryFtsEntity>

    /**
     * Insert FTS entry (called after inserting encrypted entry)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ftsEntry: EntryFtsEntity)

    /**
     * Update FTS entry
     */
    @Update
    suspend fun update(ftsEntry: EntryFtsEntity)

    /**
     * Delete FTS entry
     */
    @Query("DELETE FROM entries_fts WHERE entryId = :entryId")
    suspend fun delete(entryId: String)

    /**
     * Delete all FTS entries (called on logout for E2E tier)
     */
    @Query("DELETE FROM entries_fts")
    suspend fun deleteAll()

    /**
     * Count search results
     */
    @Query("SELECT COUNT(*) FROM entries_fts WHERE entries_fts MATCH :query")
    suspend fun countResults(query: String): Int
}
