package com.jstuart0.personaldiary.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Full-Text Search entity for E2E tier
 * Stores decrypted content for local search only
 *
 * Note: This data is stored unencrypted locally for search purposes
 * It is only accessible on the device and cleared when user logs out
 *
 * This is a standalone FTS4 table, not linked to a content entity
 * to avoid field mapping issues between encrypted and decrypted content.
 */
@Fts4
@Entity(tableName = "entries_fts")
data class EntryFtsEntity(
    @PrimaryKey
    val rowid: Long,
    val entryId: String,
    val title: String?,
    val content: String, // Decrypted content for search
    val tags: String // Comma-separated tags
)
