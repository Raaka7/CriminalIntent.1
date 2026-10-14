package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CrimeListViewModel : ViewModel() {
    private val crimeRepository = CrimeRepository.get()

    /** null until the first load finishes, so the empty-state message doesn't flash. */
    val crimes: StateFlow<List<Crime>?> = crimeRepository.getCrimes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun addCrime(crime: Crime) = crimeRepository.addCrime(crime)
}
