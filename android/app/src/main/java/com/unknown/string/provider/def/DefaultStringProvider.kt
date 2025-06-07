package com.unknown.string.provider.def

import androidx.fragment.app.FragmentActivity
import com.google.auto.service.AutoService
import com.simple.phonetics.domain.usecase.GetTranslateAsyncUseCase
import com.unknown.string.provider.StringProvider
import kotlinx.coroutines.flow.Flow
import org.koin.core.context.GlobalContext

@AutoService(StringProvider::class)
class DefaultStringProvider : StringProvider {

    override suspend fun provide(activity: FragmentActivity): Flow<Map<String, String>> {

        return GlobalContext.get().get<GetTranslateAsyncUseCase>().execute()
    }
}