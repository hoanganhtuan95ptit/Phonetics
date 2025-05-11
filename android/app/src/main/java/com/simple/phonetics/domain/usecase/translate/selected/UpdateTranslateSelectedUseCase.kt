package com.simple.phonetics.domain.usecase.translate.selected

import com.simple.phonetics.domain.repositories.AppRepository

class UpdateTranslateSelectedUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param) {

        appRepository.updateTranslateSelected(translateSelected = param.translateSelected)
    }

    data class Param(
        val translateSelected: String
    )
}
