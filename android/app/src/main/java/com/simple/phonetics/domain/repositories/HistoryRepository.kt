package com.simple.phonetics.domain.repositories

interface HistoryRepository {

    suspend fun get(limit: Int): List<String>
}