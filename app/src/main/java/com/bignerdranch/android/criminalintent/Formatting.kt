package com.bignerdranch.android.criminalintent

import android.text.format.DateFormat
import java.util.Date

private const val DATE_FORMAT = "EEE, MMM dd, yyyy"

/** One date format used everywhere (list rows, detail button, report text). */
fun Date.toDisplayString(): String = DateFormat.format(DATE_FORMAT, this).toString()
