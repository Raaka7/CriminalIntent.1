package com.bignerdranch.android.criminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import java.util.Calendar

/**
 * A DialogFragment wrapping the platform DatePickerDialog. Being a fragment means
 * the dialog survives rotation. The chosen date goes back to CrimeDetailFragment
 * through the Fragment Result API.
 */
class DatePickerFragment : DialogFragment() {

    private val args: DatePickerFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance().apply { time = args.crimeDate }

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            // Keep the original time of day; only the calendar date changes.
            val resultDate = Calendar.getInstance().apply {
                time = args.crimeDate
                set(year, month, day)
            }.time
            setFragmentResult(REQUEST_KEY_DATE, bundleOf(BUNDLE_KEY_DATE to resultDate))
        }

        return DatePickerDialog(
            requireContext(),
            dateListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        const val REQUEST_KEY_DATE = "REQUEST_KEY_DATE"
        const val BUNDLE_KEY_DATE = "BUNDLE_KEY_DATE"
    }
}
