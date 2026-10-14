package com.bignerdranch.android.criminalintent

import android.content.Context
import androidx.room.Room
import com.bignerdranch.android.criminalintent.database.CrimeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val DATABASE_NAME = "crime-database"

/**
 * Single source of truth for crime data. Everything else in the app talks to the
 * repository rather than to Room directly.
 *
 * Writes that must finish even after the screen that started them is gone
 * (saving on exit, deleting) run in an app-wide scope instead of viewModelScope.
 */
@OptIn(DelicateCoroutinesApi::class)
class CrimeRepository private constructor(
    context: Context,
    private val coroutineScope: CoroutineScope = GlobalScope
) {
    private val appContext = context.applicationContext

    private val database: CrimeDatabase = Room.databaseBuilder(
        appContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val dao = database.crimeDao()

    fun getCrimes(): Flow<List<Crime>> = dao.getCrimes()

    suspend fun getCrime(id: UUID): Crime? = dao.getCrime(id)

    suspend fun addCrime(crime: Crime) = dao.addCrime(crime)

    fun updateCrime(crime: Crime) {
        coroutineScope.launch { dao.updateCrime(crime) }
    }

    fun deleteCrime(crime: Crime) {
        coroutineScope.launch {
            dao.deleteCrime(crime)
            crime.photoFileName?.let { photoFile(it).delete() }
        }
    }

    fun photoFile(fileName: String): File = File(appContext.filesDir, fileName)

    companion object {
        @Volatile
        private var INSTANCE: CrimeRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository =
            INSTANCE ?: throw IllegalStateException("CrimeRepository must be initialized")
    }
}
