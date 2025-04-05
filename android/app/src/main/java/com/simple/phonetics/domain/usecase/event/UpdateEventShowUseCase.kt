package com.simple.phonetics.domain.usecase.event

import com.simple.phonetics.domain.repositories.AppRepository

class UpdateEventShowUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param) {

        appRepository.updateEventIdShow(param.eventId)
    }

    data class Param(val eventId: String)
}