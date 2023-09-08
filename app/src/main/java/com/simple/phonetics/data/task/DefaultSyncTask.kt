package com.simple.phonetics.data.task

import android.content.Context
import com.simple.core.utils.extentions.toListOrEmpty
import com.simple.phonetics.R
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.dao.PhoneticsDao
import com.simple.phonetics.data.dao.RoomPhonetics
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream


class DefaultSyncTask(
    private val context: Context,
    private val appCache: AppCache,
    private val phoneticsDao: PhoneticsDao
) : SyncTask {

    private val version = 1L

    override fun priority(): Int = Int.MAX_VALUE

    override suspend fun executeTask(param: Unit) {

        val versionCachePhonetics = appCache.getVersionCachePhonetics()

        if (version <= versionCachePhonetics) return

        readTextFile(context.resources.openRawResource(R.raw.en)).toListOrEmpty<RoomPhonetics>().let {

            appCache.saveVersionCachePhonetics(version)

            phoneticsDao.insertOrUpdate(it)
        }
    }

    private fun readTextFile(inputStream: InputStream): String {

        val outputStream = ByteArrayOutputStream()

        val buf = ByteArray(1024)

        var len: Int

        try {
            while (inputStream.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len)
            }
            outputStream.close()
            inputStream.close()
        } catch (_: IOException) {
        }

        return outputStream.toString()
    }
}