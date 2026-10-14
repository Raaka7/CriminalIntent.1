package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Holds the crime being edited. Because the ViewModel survives rotation, in-progress
 * edits aren't lost; they're written to the database once, when the screen is left.
 */
class CrimeDetailViewModel(crimeId: UUID) : ViewModel() {
    private val crimeRepository = CrimeRepository.get()

    private val _crime: MutableStateFlow<Crime?> = MutableStateFlow(null)
    val crime: StateFlow<Crime?> = _crime.asStateFlow()

    private var deleted = false

    init {
        viewModelScope.launch {
            _crime.value = crimeRepository.getCrime(crimeId)
        }
    }

    fun updateCrime(onUpdate: (Crime) -> Crime) {
        _crime.update { oldCrime -> oldCrime?.let { onUpdate(it) } }
    }

    fun deleteCrime() {
        _crime.value?.let { crimeRepository.deleteCrime(it) }
        deleted = true
    }

    override fun onCleared() {
        super.onCleared()
        if (!deleted) {
            _crime.value?.let { crimeRepository.updateCrime(it) }
        }
    }
}

class CrimeDetailViewModelFactory(
    private val crimeId: UUID
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CrimeDetailViewModel(crimeId) as T
}
