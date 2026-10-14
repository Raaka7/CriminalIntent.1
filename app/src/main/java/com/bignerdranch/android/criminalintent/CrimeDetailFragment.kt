package com.bignerdranch.android.criminalintent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.util.Date

private const val KEY_PENDING_PHOTO = "pending_photo_name"

class CrimeDetailFragment : Fragment() {

    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) { "Binding is only valid between onCreateView and onDestroyView" }

    private val args: CrimeDetailFragmentArgs by navArgs()

    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeId)
    }

    private val crimeRepository = CrimeRepository.get()

    /** File name handed to the camera app; saved across rotation while the camera is open. */
    private var pendingPhotoName: String? = null

    // Implicit intent #1: pick a suspect from the Contacts app.
    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { parseContactSelection(it) }
    }

    // Implicit intent #2: ask the camera app to write a full-size photo into our file.
    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto: Boolean ->
        val newName = pendingPhotoName ?: return@registerForActivityResult
        pendingPhotoName = null

        if (didTakePhoto) {
            val oldName = crimeDetailViewModel.crime.value?.photoFileName
            crimeDetailViewModel.updateCrime { it.copy(photoFileName = newName) }
            if (oldName != null && oldName != newName) {
                crimeRepository.photoFile(oldName).delete()
            }
        } else {
            // Cancelled: clean up the empty file the camera may have created.
            crimeRepository.photoFile(newName).delete()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingPhotoName = savedInstanceState?.getString(KEY_PENDING_PHOTO)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING_PHOTO, pendingPhotoName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crimeDetailViewModel.updateCrime { it.copy(title = text.toString()) }
            }

            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crimeDetailViewModel.updateCrime { it.copy(isSolved = isChecked) }
            }

            crimeSuspect.setOnClickListener { selectSuspect.launch(null) }
            val pickContactIntent = selectSuspect.contract.createIntent(requireContext(), null)
            crimeSuspect.isEnabled = canResolveIntent(pickContactIntent)

            crimeCamera.setOnClickListener { launchCamera() }
            val captureImageIntent = takePhoto.contract.createIntent(requireContext(), Uri.EMPTY)
            crimeCamera.isEnabled = canResolveIntent(captureImageIntent)
        }

        // Date chosen in DatePickerFragment comes back here.
        setFragmentResultListener(DatePickerFragment.REQUEST_KEY_DATE) { _, bundle ->
            @Suppress("DEPRECATION")
            val newDate = bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_crime_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.delete_crime -> {
                        deleteCrime()
                        true
                    }
                    else -> false
                }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Don't let the user leave with an untitled crime (applies to Back and Up).
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (crimeDetailViewModel.crime.value?.title.isNullOrBlank()) {
                Toast.makeText(requireContext(), R.string.title_required, Toast.LENGTH_SHORT).show()
            } else {
                isEnabled = false
                findNavController().popBackStack()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeDetailViewModel.crime.collect { crime ->
                    crime?.let { updateUi(it) }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(crime: Crime) {
        binding.apply {
            // Only reset the text if it differs, otherwise the cursor jumps while typing.
            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }

            crimeDate.text = crime.date.toDisplayString()
            crimeDate.setOnClickListener {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.crimeDetailFragment) {
                    navController.navigate(CrimeDetailFragmentDirections.selectDate(crime.date))
                }
            }

            crimeSolved.isChecked = crime.isSolved

            crimeSuspect.text = crime.suspect.ifBlank { getString(R.string.crime_suspect_text) }

            // Implicit intent #3: send the report to any app that accepts plain text.
            crimeReport.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
                }
                startActivity(Intent.createChooser(reportIntent, getString(R.string.send_report)))
            }

            updatePhoto(crime.photoFileName)
        }
    }

    private fun getCrimeReport(crime: Crime): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val suspectString = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(
            R.string.crime_report,
            crime.title,
            crime.date.toDisplayString(),
            solvedString,
            suspectString
        )
    }

    private fun parseContactSelection(contactUri: Uri) {
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        requireActivity().contentResolver
            .query(contactUri, projection, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val suspect = cursor.getString(0) ?: return@use
                    crimeDetailViewModel.updateCrime { it.copy(suspect = suspect) }
                }
            }
    }

    private fun launchCamera() {
        val photoName = "IMG_${System.currentTimeMillis()}.JPG"
        val photoFile = crimeRepository.photoFile(photoName)
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        pendingPhotoName = photoName
        takePhoto.launch(photoUri)
    }

    private fun updatePhoto(photoFileName: String?) {
        val photoView = binding.crimePhoto
        if (photoView.tag == photoFileName) return

        val photoFile = photoFileName?.let { crimeRepository.photoFile(it) }
        if (photoFile?.exists() == true) {
            photoView.doOnLayout { measuredView ->
                val bitmap = getScaledBitmap(photoFile.path, measuredView.width, measuredView.height)
                photoView.setImageBitmap(bitmap)
                photoView.tag = photoFileName
                photoView.contentDescription = getString(R.string.crime_photo_image_description)
            }
        } else {
            photoView.setImageBitmap(null)
            photoView.tag = null
            photoView.contentDescription = getString(R.string.crime_photo_no_image_description)
        }
    }

    private fun deleteCrime() {
        crimeDetailViewModel.deleteCrime()
        findNavController().popBackStack()
    }

    private fun canResolveIntent(intent: Intent): Boolean =
        intent.resolveActivity(requireActivity().packageManager) != null
}
