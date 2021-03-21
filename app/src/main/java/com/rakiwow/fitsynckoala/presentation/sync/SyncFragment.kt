package com.rakiwow.fitsynckoala.presentation.sync

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rakiwow.fitsynckoala.R
import com.rakiwow.fitsynckoala.databinding.FragmentSyncBinding
import com.rakiwow.fitsynckoala.util.DataState
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.lang.StringBuilder
import java.util.*

@AndroidEntryPoint
class SyncFragment : Fragment() {

    private val syncViewModel: SyncViewModel by viewModels()

    private val navArgs: SyncFragmentArgs by navArgs()

    private var _binding: FragmentSyncBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun setupView() {

        syncViewModel.setActivityTimeInSeconds(navArgs.elapsedTime)
        syncViewModel.setActivityDistanceInMeters(navArgs.distance)
        syncViewModel.setActivityKcal(navArgs.kcal)

        binding.spinnerType.adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.activity_type,
            R.layout.item_spinner_header
        ).also { arrayAdapter ->
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.btnUploadResult.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Do you want to upload this activity?")
                .setPositiveButton("Upload") { _, _ ->
                    ifLet(
                        syncViewModel.activityTimeInSeconds.value,
                        syncViewModel.activityDistanceInMeters.value,
                        syncViewModel.activityKcal.value
                    ) { (elapsedTime, distance, kcal) ->
                        syncViewModel.createActivity(
                            name = binding.etName.text.toString(),
                            type = binding.spinnerType.selectedItem.toString(),
                            date = Date(),
                            elapsedTime = elapsedTime.toInt(),
                            description = if (binding.cbKcalSwitch.isChecked) {
                                val sb = StringBuilder()
                                if (binding.etDescription.text.toString().isNotEmpty()) {
                                    sb.append("\n\n")
                                }
                                sb.append(getString(R.string.i_burned) + kcal + " kcal!")
                                binding.etDescription.text.toString() + sb.toString()
                            } else {
                                binding.etDescription.text.toString()
                            },
                            distance = distance.toFloat()
                        )
                    }
                }
                .setNegativeButton("Dismiss") { _, _ ->
                }
                .create().show()
        }

        binding.btnRemoveName.setOnClickListener {
            binding.etName.setText("")
        }

        binding.btnEditTime.setOnClickListener {
            syncViewModel.activityTimeInSeconds.value?.let { seconds ->
                val hourMinSecTriple =
                    syncViewModel.getHourMinSecFromSeconds(seconds)
                showTimeDialog(
                    hourMinSecTriple.first,
                    hourMinSecTriple.second,
                    hourMinSecTriple.third
                )
            }
        }
        binding.btnEditDistance.setOnClickListener {
            syncViewModel.activityDistanceInMeters.value?.let { distance ->
                showDistanceDialog(distance)
            }
        }
        binding.btnEditKcal.setOnClickListener {
            syncViewModel.activityKcal.value?.let { kcal ->
                showKcalDialog(kcal)
            }
        }
    }

    private fun observeViewModel() {
        syncViewModel.createActivityResult.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandled()?.let { dataState ->
                when (dataState) {
                    is DataState.Success<Boolean> -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage("Results were uploaded to Strava!")
                            .setNeutralButton("Great") { _, _ ->
                                findNavController().navigate(SyncFragmentDirections.actionSyncFragmentToPreviewFragment())
                            }
                            .setOnCancelListener {
                                findNavController().navigate(SyncFragmentDirections.actionSyncFragmentToPreviewFragment())
                            }
                            .create().show()
                        binding.pbLogin.visibility = View.GONE
                        syncViewModel.persistLastActivityName(binding.etName.text.toString())
                    }
                    is DataState.Loading -> {
                        binding.pbLogin.visibility = View.VISIBLE
                    }
                    is DataState.Error -> {
                        Timber.d(dataState.exception.code().toString())
                        Toast.makeText(
                            requireActivity(),
                            "401: Please authorize the application again",
                            Toast.LENGTH_LONG
                        ).show()
                        if (dataState.exception.code() == 401) {
                            findNavController().navigate(
                                SyncFragmentDirections.actionSyncFragmentToLoginFragment(
                                    navArgs.distance, navArgs.elapsedTime, navArgs.kcal
                                )
                            )
                        }
                        binding.pbLogin.visibility = View.GONE
                    }
                }
            }
        })

        // Observe datastore data, or else values are null
        syncViewModel.accessTokenFlow.observe(viewLifecycleOwner, {})
        syncViewModel.expiresAtFlow.observe(viewLifecycleOwner, {})
        syncViewModel.expiresInFlow.observe(viewLifecycleOwner, {})
        syncViewModel.refreshTokenFlow.observe(viewLifecycleOwner, {})
        syncViewModel.lastActivityNameFlow.observe(viewLifecycleOwner, {
            binding.etName.setText(it)
        })

        syncViewModel.activityTimeInSeconds.observe(viewLifecycleOwner, { seconds ->
            binding.tvTime.text = syncViewModel.getFormattedTextFromSeconds(seconds)
        })
        syncViewModel.activityDistanceInMeters.observe(viewLifecycleOwner, { distance ->
            val sb = StringBuilder()
            sb.append(distance / 1000)
            sb.append(" km")
            binding.tvDistance.text = sb
        })
        syncViewModel.activityKcal.observe(viewLifecycleOwner, { kcal ->
            binding.tvKcal.text = kcal.toString()
        })
    }

    private fun showTimeDialog(defaultHour: Int = 0, defaultMin: Int = 0, defaultSec: Int = 0) {
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_time)

        val numberPickerHour = dialog.findViewById<NumberPicker>(R.id.numberPickerHour)
        val numberPickerMinute = dialog.findViewById<NumberPicker>(R.id.numberPickerMinute)
        val numberPickerSecond = dialog.findViewById<NumberPicker>(R.id.numberPickerSecond)

        dialog.findViewById<MaterialButton>(R.id.btn_dialog_time_negative).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.btn_dialog_time_positive).setOnClickListener {
            syncViewModel.setActivityTimeInSeconds(
                syncViewModel.getTotalSeconds(
                    numberPickerHour.value,
                    numberPickerMinute.value,
                    numberPickerSecond.value
                )
            )
            dialog.dismiss()
        }

        numberPickerHour.minValue = 0
        numberPickerHour.maxValue = 23
        numberPickerHour.value = defaultHour

        numberPickerMinute.minValue = 0
        numberPickerMinute.maxValue = 59
        numberPickerMinute.value = defaultMin

        numberPickerSecond.minValue = 0
        numberPickerSecond.maxValue = 59
        numberPickerSecond.value = defaultSec

        dialog.show()
    }

    private fun showDistanceDialog(defaultDistance: Float) {
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_distance)

        val etDistance = dialog.findViewById<TextInputEditText>(R.id.et_distance)

        dialog.findViewById<MaterialButton>(R.id.btn_dialog_distance_negative).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.btn_dialog_distance_positive).setOnClickListener {
            syncViewModel.setActivityDistanceInMeters(etDistance.text.toString().toFloat())
            dialog.dismiss()
        }

        etDistance.setText(defaultDistance.toString())

        dialog.show()
    }


    private fun showKcalDialog(defaulKcal: Float) {
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_kcal)

        val etKcal = dialog.findViewById<TextInputEditText>(R.id.et_kcal)

        dialog.findViewById<MaterialButton>(R.id.btn_dialog_kcal_negative).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.btn_dialog_kcal_positive).setOnClickListener {
            syncViewModel.setActivityKcal(etKcal.text.toString().toFloat())
            dialog.dismiss()
        }

        etKcal.setText(defaulKcal.toString())

        dialog.show()
    }

    inline fun <T : Any> ifLet(vararg elements: T?, closure: (List<T>) -> Unit) {
        if (elements.all { it != null }) {
            closure(elements.filterNotNull())
        }
    }
}