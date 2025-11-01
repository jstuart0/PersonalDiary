package com.jstuart0.personaldiary.data.repository

import com.jstuart0.personaldiary.data.encryption.EncryptionService
import com.jstuart0.personaldiary.data.local.dao.EntryDao
import com.jstuart0.personaldiary.data.local.dao.EntryFtsDao
import com.jstuart0.personaldiary.data.local.entity.EntryEntity
import com.jstuart0.personaldiary.data.remote.api.PersonalDiaryApi
import com.jstuart0.personaldiary.data.remote.model.CreateEntryRequest
import com.jstuart0.personaldiary.data.remote.model.EntryResponse
import com.jstuart0.personaldiary.domain.model.SyncStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class EntryRepositoryTest {

    @MockK
    private lateinit var api: PersonalDiaryApi

    @MockK
    private lateinit var entryDao: EntryDao

    @MockK
    private lateinit var entryFtsDao: EntryFtsDao

    @MockK
    private lateinit var encryptionService: EncryptionService

    private lateinit var repository: EntryRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = EntryRepository(
            api = api,
            entryDao = entryDao,
            entryFtsDao = entryFtsDao,
            encryptionService = encryptionService
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createEntry encrypts content and saves locally`() = runTest {
        // Given
        val userId = "user123"
        val title = "My First Entry"
        val content = "This is my diary entry"
        val tags = listOf("personal", "thoughts")

        val encryptedTitle = "encrypted_title"
        val encryptedContent = "encrypted_content"

        coEvery { encryptionService.encrypt(title) } returns Result.success(encryptedTitle)
        coEvery { encryptionService.encrypt(content) } returns Result.success(encryptedContent)
        coEvery { entryDao.insert(any()) } just Runs
        coEvery { entryFtsDao.insert(any()) } just Runs

        // When
        val result = repository.createEntry(userId, title, content, tags, emptyList())

        // Then
        assertTrue(result.isSuccess)
        val entry = result.getOrNull()
        assertNotNull(entry)
        assertEquals(title, entry?.title)
        assertEquals(content, entry?.content)
        assertEquals(tags, entry?.tags)
        assertEquals(SyncStatus.PENDING, entry?.syncStatus)

        coVerify {
            encryptionService.encrypt(title)
            encryptionService.encrypt(content)
            entryDao.insert(match {
                it.title == encryptedTitle &&
                it.content == encryptedContent &&
                it.syncStatus == SyncStatus.PENDING.name
            })
            entryFtsDao.insert(any())
        }
    }

    @Test
    fun `createEntry without title only encrypts content`() = runTest {
        // Given
        val userId = "user123"
        val content = "Entry without title"
        val encryptedContent = "encrypted_content"

        coEvery { encryptionService.encrypt(content) } returns Result.success(encryptedContent)
        coEvery { entryDao.insert(any()) } just Runs
        coEvery { entryFtsDao.insert(any()) } just Runs

        // When
        val result = repository.createEntry(userId, null, content, emptyList(), emptyList())

        // Then
        assertTrue(result.isSuccess)
        val entry = result.getOrNull()
        assertNull(entry?.title)

        coVerify(exactly = 0) {
            encryptionService.encrypt(match { it.length < 20 }) // Not called for title
        }
        coVerify(exactly = 1) {
            encryptionService.encrypt(content)
        }
    }

    @Test
    fun `updateEntry updates encrypted content and marks as pending`() = runTest {
        // Given
        val entryId = "entry123"
        val newTitle = "Updated Title"
        val newContent = "Updated content"
        val newTags = listOf("updated")

        val existingEntry = EntryEntity(
            entryId = entryId,
            serverEntryId = "server123",
            userId = "user123",
            title = "old_encrypted_title",
            content = "old_encrypted_content",
            tags = listOf("old"),
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED.name,
            lastSyncAt = System.currentTimeMillis()
        )

        val encryptedTitle = "encrypted_new_title"
        val encryptedContent = "encrypted_new_content"

        coEvery { entryDao.getEntry(entryId) } returns existingEntry
        coEvery { encryptionService.encrypt(newTitle) } returns Result.success(encryptedTitle)
        coEvery { encryptionService.encrypt(newContent) } returns Result.success(encryptedContent)
        coEvery { entryDao.update(any()) } just Runs
        coEvery { entryFtsDao.update(any()) } just Runs

        // When
        val result = repository.updateEntry(entryId, newTitle, newContent, newTags, emptyList())

        // Then
        assertTrue(result.isSuccess)
        val entry = result.getOrNull()
        assertEquals(newTitle, entry?.title)
        assertEquals(newContent, entry?.content)
        assertEquals(SyncStatus.PENDING, entry?.syncStatus)

        coVerify {
            entryDao.update(match {
                it.title == encryptedTitle &&
                it.content == encryptedContent &&
                it.syncStatus == SyncStatus.PENDING.name
            })
        }
    }

    @Test
    fun `getEntry decrypts content successfully`() = runTest {
        // Given
        val entryId = "entry123"
        val encryptedTitle = "encrypted_title"
        val encryptedContent = "encrypted_content"
        val decryptedTitle = "My Entry"
        val decryptedContent = "Entry content"

        val entryEntity = EntryEntity(
            entryId = entryId,
            serverEntryId = null,
            userId = "user123",
            title = encryptedTitle,
            content = encryptedContent,
            tags = listOf("tag1"),
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name,
            lastSyncAt = null
        )

        coEvery { entryDao.getEntry(entryId) } returns entryEntity
        coEvery { encryptionService.decrypt(encryptedTitle) } returns Result.success(decryptedTitle)
        coEvery { encryptionService.decrypt(encryptedContent) } returns Result.success(decryptedContent)

        // When
        val entry = repository.getEntry(entryId)

        // Then
        assertNotNull(entry)
        assertEquals(decryptedTitle, entry?.title)
        assertEquals(decryptedContent, entry?.content)
        assertEquals(listOf("tag1"), entry?.tags)

        coVerify {
            encryptionService.decrypt(encryptedTitle)
            encryptionService.decrypt(encryptedContent)
        }
    }

    @Test
    fun `getEntries returns decrypted entries flow`() = runTest {
        // Given
        val encryptedEntries = listOf(
            EntryEntity(
                entryId = "entry1",
                serverEntryId = null,
                userId = "user123",
                title = "encrypted_title_1",
                content = "encrypted_content_1",
                tags = emptyList(),
                timestamp = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name,
                lastSyncAt = null
            ),
            EntryEntity(
                entryId = "entry2",
                serverEntryId = null,
                userId = "user123",
                title = "encrypted_title_2",
                content = "encrypted_content_2",
                tags = emptyList(),
                timestamp = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED.name,
                lastSyncAt = System.currentTimeMillis()
            )
        )

        coEvery { entryDao.getAllEntriesFlow() } returns flowOf(encryptedEntries)
        coEvery { encryptionService.decrypt(any()) } returns Result.success("decrypted")

        // When
        val entries = repository.getEntries().first()

        // Then
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.title == "decrypted" && it.content == "decrypted" })
    }

    @Test
    fun `deleteEntry removes entry locally and marks for server deletion`() = runTest {
        // Given
        val entryId = "entry123"
        val entryEntity = EntryEntity(
            entryId = entryId,
            serverEntryId = "server123",
            userId = "user123",
            title = "encrypted_title",
            content = "encrypted_content",
            tags = emptyList(),
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED.name,
            lastSyncAt = System.currentTimeMillis()
        )

        coEvery { entryDao.getEntry(entryId) } returns entryEntity
        coEvery { api.deleteEntry("server123") } returns Response.success(Unit)
        coEvery { entryDao.delete(any()) } just Runs
        coEvery { entryFtsDao.delete(any()) } just Runs

        // When
        val result = repository.deleteEntry(entryId)

        // Then
        assertTrue(result.isSuccess)

        coVerify {
            api.deleteEntry("server123")
            entryDao.delete(entryEntity)
            entryFtsDao.delete(any())
        }
    }

    @Test
    fun `deleteEntry only deletes locally if not synced to server`() = runTest {
        // Given
        val entryId = "entry123"
        val entryEntity = EntryEntity(
            entryId = entryId,
            serverEntryId = null, // Not synced yet
            userId = "user123",
            title = "encrypted_title",
            content = "encrypted_content",
            tags = emptyList(),
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name,
            lastSyncAt = null
        )

        coEvery { entryDao.getEntry(entryId) } returns entryEntity
        coEvery { entryDao.delete(any()) } just Runs
        coEvery { entryFtsDao.delete(any()) } just Runs

        // When
        val result = repository.deleteEntry(entryId)

        // Then
        assertTrue(result.isSuccess)

        coVerify(exactly = 0) {
            api.deleteEntry(any())
        }
        coVerify {
            entryDao.delete(entryEntity)
        }
    }

    @Test
    fun `getEntriesByDateRange returns entries within range`() = runTest {
        // Given
        val startTime = System.currentTimeMillis() - 86400000 // 1 day ago
        val endTime = System.currentTimeMillis()

        val entries = listOf(
            EntryEntity(
                entryId = "entry1",
                serverEntryId = null,
                userId = "user123",
                title = "encrypted_title",
                content = "encrypted_content",
                tags = emptyList(),
                timestamp = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name,
                lastSyncAt = null
            )
        )

        coEvery { entryDao.getEntriesByDateRange(startTime, endTime) } returns flowOf(entries)
        coEvery { encryptionService.decrypt(any()) } returns Result.success("decrypted")

        // When
        val result = repository.getEntriesByDateRange(startTime, endTime).first()

        // Then
        assertEquals(1, result.size)
        coVerify {
            entryDao.getEntriesByDateRange(startTime, endTime)
        }
    }
}
