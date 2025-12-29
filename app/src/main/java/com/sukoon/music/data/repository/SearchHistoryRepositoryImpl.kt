package com.sukoon.music.data.repository

import com.sukoon.music.data.local.dao.SearchHistoryDao
import com.sukoon.music.data.local.entity.SearchHistoryEntity
import com.sukoon.music.domain.model.SearchHistory
import com.sukoon.music.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SearchHistoryRepository.
 * Mediates between SearchHistoryDao (data layer) and domain layer.
 */
@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun getSearchHistory(): Flow<List<SearchHistory>> {
        return searchHistoryDao.getAllHistory().map { entities ->
            entities.map { it.toSearchHistory() }
        }
    }

    override suspend fun saveSearch(query: String) {
        withContext(Dispatchers.IO) {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) return@withContext

            // Insert or update search query
            searchHistoryDao.insertSearch(
                SearchHistoryEntity(
                    query = trimmedQuery,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Clean up old entries to maintain 10-item limit
            searchHistoryDao.deleteOldest()
        }
    }

    override suspend fun deleteSearch(query: String) {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteSearch(query)
        }
    }

    override suspend fun clearAllHistory() {
        withContext(Dispatchers.IO) {
            searchHistoryDao.clearAll()
        }
    }

    /**
     * Convert SearchHistoryEntity (data layer) to SearchHistory (domain layer).
     */
    private fun SearchHistoryEntity.toSearchHistory(): SearchHistory {
        return SearchHistory(
            query = query,
            timestamp = timestamp
        )
    }
}
