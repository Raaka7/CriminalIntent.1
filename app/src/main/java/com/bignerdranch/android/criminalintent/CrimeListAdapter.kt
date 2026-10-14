package com.bignerdranch.android.criminalintent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimeBinding
import java.util.UUID

class CrimeHolder(
    private val binding: ListItemCrimeBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(crime: Crime, onCrimeClicked: (crimeId: UUID) -> Unit) {
        val context = binding.root.context
        binding.crimeTitle.text = crime.title.ifBlank { context.getString(R.string.untitled_crime) }
        binding.crimeDate.text = crime.date.toDisplayString()

        // Solved crimes get a visible badge in the list.
        binding.crimeSolved.visibility = if (crime.isSolved) View.VISIBLE else View.GONE

        binding.root.setOnClickListener { onCrimeClicked(crime.id) }
    }
}

/** ListAdapter diffs each new list against the old one and animates only what changed. */
class CrimeListAdapter(
    private val onCrimeClicked: (crimeId: UUID) -> Unit
) : ListAdapter<Crime, CrimeHolder>(CrimeDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
        return CrimeHolder(binding)
    }

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        holder.bind(getItem(position), onCrimeClicked)
    }

    private object CrimeDiff : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Crime, newItem: Crime) = oldItem == newItem
    }
}
