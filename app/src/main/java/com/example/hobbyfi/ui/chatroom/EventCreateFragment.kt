package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventCreateBinding
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.shared.removeAllEditTextWatchers
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.chatroom.EventCreateFragmentViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalCoroutinesApi
class EventCreateFragment : ChatroomModelFragment(), TextFieldInputValidationOnus,
        DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private val eventCalendar = Calendar.getInstance()

    val viewModel: EventCreateFragmentViewModel by viewModels()
    private lateinit var binding: FragmentEventCreateBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_event_create, container, false)

        binding.viewModel = viewModel

        with(binding) {
            lifecycleOwner = this@EventCreateFragment

            eventInfo.eventInfoButtonBar.leftButton.setOnClickListener { // select event date
                Callbacks.initDateTimeDatePickerDialog(
                    requireContext(),
                    this@EventCreateFragment, viewModel!!
                )
            }

            eventInfo.eventInfoButtonBar.rightButton.setOnClickListener { // select location
                Callbacks.startChooseEventLocationMapsActivity(this@EventCreateFragment, viewModel!!)
            }

            eventInfo.eventImage.setOnClickListener {
                Callbacks.requestImage(this@EventCreateFragment)
            }

            confirmButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel!!.sendIntent(EventIntent.CreateEvent(activityViewModel.authChatroom.value!!.id))
                }
            }

            observeEventState()

            return@onCreateView binding.root
        }
    }

    private fun observeEventState() {
        lifecycleScope.launch {
            viewModel.mainState.collect {
                when(it) {
                    EventState.Idle -> {

                    }
                    EventState.Loading -> {
                        // TODO: Progressbar
                    }
                    is EventState.OnData.EventCreateResult -> {
                        activityViewModel.sendEventsIntent(EventListIntent.AddAnEventCache(it.event))
                        Toast.makeText(requireContext(), "Event successfully created!", Toast.LENGTH_LONG)
                            .show()
                        navController.popBackStack()
                    }
                    is EventState.Error -> {
                        // TODO: Handle shouldReauth
                        Toast.makeText(requireContext(), "Something went wrong! ${it.error}", Toast.LENGTH_LONG)
                            .show()
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    override fun initTextFieldValidators() {
        with(binding.eventInfo) {
            // Code quality TODO:
            // Extract this method (from fragments and all activities) to a factory which takes a list of views and initialises them
            // with the preset predicate/error pairs
            nameInputField.addTextChangedListener(
                Constants.nameInputError,
                Constants.namePredicate
            )

            descriptionInputField.addTextChangedListener(
                Constants.descriptionInputError,
                Constants.descriptionPredicate
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding.eventInfo) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(nameInputField, Constants.nameInputError) ||
                    FieldUtils.isTextFieldInvalid(descriptionInputField, Constants.descriptionInputError)
        }
    }

    override fun onStart() {
        super.onStart()
        initTextFieldValidators()
    }

    override fun onPause() {
        super.onPause()
        with(binding.eventInfo) {
            nameInputField.removeAllEditTextWatchers()
            descriptionInputField.removeAllEditTextWatchers()
        }
    }

    override fun onDateSet(picker: DatePicker?, year: Int, month: Int, day: Int) =
        Callbacks.onEventDateSet(eventCalendar, year, month, day, viewModel, requireContext(), requireActivity(), this)

    override fun onTimeSet(picker: TimePicker?, hours: Int, minutes: Int) =
        Callbacks.onEventTimeSet(eventCalendar, hours, minutes, viewModel)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Constants.eventLocationRequestCode) {
            if(resultCode == Activity.RESULT_OK) {
                viewModel.eventLatLng = data?.extras?.get(Constants.EVENT_LOCATION) as LatLng
            }
        }
        // SLIGHT FIXME: slight code dupss
        Callbacks.handleImageRequestWithPermission(
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            binding.eventInfo.eventImage.setImageBitmap(it)
            lifecycleScope.launch {
                viewModel.base64Image.setImageBase64(
                    ImageUtils.encodeImage(it)
                )
            }
        }
    }
}