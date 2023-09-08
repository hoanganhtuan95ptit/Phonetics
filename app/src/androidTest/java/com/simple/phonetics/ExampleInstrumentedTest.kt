//package com.simple.phonetics
//
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.simple.core.utils.extentions.toJson
//import com.simple.phonetics.data.usecase.GetPhoneticsAsyncUseCase
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.runBlocking
//import org.junit.Assert.*
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.koin.core.component.KoinComponent
//
///**
// * Instrumented test, which will execute on an Android device.
// *
// * See [testing documentation](http://d.android.com/tools/testing).
// */
//@RunWith(AndroidJUnit4::class)
//class ExampleInstrumentedTest : KoinComponent {
//
//    @Test
//    fun useAppContext() = runBlocking {
//
//        getKoin().get<GetPhoneticsAsyncUseCase>().execute(GetPhoneticsAsyncUseCase.Param("Deletes all rows from all the tables that are registered to this database as entities")).first().let {
//
//            println(it.toJson())
//        }
//    }
//}