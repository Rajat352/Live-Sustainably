package com.livesustainably.domain.repository

interface ChatRepository {
    suspend fun reply(message: String): String
}
