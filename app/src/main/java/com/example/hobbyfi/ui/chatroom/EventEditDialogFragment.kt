package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventEditDialogBinding
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.chatroom.EventEditFragmentViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*


@ExperimentalCoroutinesApi
class EventEditDialogFragment : ChatroomDialogFragment(),
        DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private val eventCalendar = Calendar.getInstance()

    // FIXME: Code dup with EventCreateFragment by somehow scoping to common ancestor and perserving dialogfragment functionality
    private lateinit var binding: FragmentEventEditDialogBinding
    private val viewModel: EventEditFragmentViewModel by viewModels(factoryProducer = {
        EventViewModelFactory(
            requireActivity().application,
            requireArguments().getParcelable(Constants.EVENT)!!
        )
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout
        binding = DataBindingUtil
            .inflate(layoutInflater, R.layout.fragment_event_edit_dialog, container, false)

        binding.viewModel = viewModel

        with(binding) {
            val event: Event = requireArguments().getParcelable(Constants.EVENT)!!
            event.photoUrl?.let {
                Glide.with(this@EventEditDialogFragment)
                    .load(it)
                    .placeholder(eventInfo.eventImage.drawable)
                    .into(eventInfo.eventImage)
            }

            eventInfo.eventInfoButtonBar.leftButton.setOnClickListener {
                Callbacks.initDateTimeDatePickerDialog(requireContext(), this@EventEditDialogFragment, viewModel!!)
            }

            eventInfo.eventInfoButtonBar.rightButton.setOnClickListener {
                Callbacks.startChooseEventLocationMapsActivity(this@EventEditDialogFragment, viewModel!!)
            }

            eventEditDialogButtonBar.leftButton.setOnClickListener {
                dismiss()
            }

            eventEditDialogButtonBar.rightButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                val eventUpdateFields: MutableMap<String?, String?> = mutableMapOf()

                if(viewModel!!.name.value != viewModel!!.event.name) {
                    eventUpdateFields[Constants.NAME] = viewModel!!.name.value
                }

                if(viewModel!!.description.value != viewModel!!.event.description) {
                    eventUpdateFields[Constants.DESCRIPTION] = viewModel!!.description.value
                }

                viewModel!!.base64Image.base64?.let {
                    eventUpdateFields[Constants.IMAGE] = it
                }

                viewModel!!.eventLatLng?.let {
                    if(it.latitude != viewModel!!.event.latitude) {
                        eventUpdateFields[Constants.LATITUDE] = it.latitude.toString()
                    }

                    if(it.longitude != viewModel!!.event.longitude) {
                        eventUpdateFields[Constants.LONGITUDE] = it.longitude.toString()
                    }
                }

                viewModel!!.eventDate?.let {
                    val formattedDate = Constants.dateTimeFormatter.format(viewModel!!.eventDate!!)
                    if(viewModel!!.event.date != formattedDate) {
                        eventUpdateFields[Constants.DATE] = formattedDate
                    }
                }

                Log.i("EventEditFragment", "EventFieldMap update: $eventUpdateFields")
                if(eventUpdateFields.isEmpty()) {
                    Toast.makeText(requireContext(), Constants.noUpdateFields, Toast.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel!!.sendIntent(
                        EventIntent.UpdateEvent(
                            eventUpdateFields
                        )
                    )
                }
            }

            eventInfo.eventImage.setOnClickListener {
                Callbacks.requestImage(this@EventEditDialogFragment)
            }

            observeEventState()

            return@onCreateView root
        }
    }

    override fun initTextFieldValidators() {
        with(binding.eventInfo) {
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

    override fun onDateSet(picker: DatePicker?, year: Int, month: Int, day: Int) =
        Callbacks.onEventDateSet(eventCalendar, year, month, day, viewModel, requireContext(), requireActivity(), this)

    override fun onTimeSet(picker: TimePicker?, hours: Int, minutes: Int) =
        Callbacks.onEventTimeSet(eventCalendar, hours, minutes, viewModel)

    private fun observeEventState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when(it) {
                    is EventState.Idle -> {

                    }
                    is EventState.Loading -> {
                    }
                    is EventState.OnData.EventEditResult -> {
                        activityViewModel.sendEventsIntent(EventListIntent.UpdateAnEventCache(it.updateFields))

                        dismiss()
                    }
                    is EventState.Error -> {
                        (requireActivity() as ChatroomActivity)
                            .handleAuthActionableError(it.error, it.shouldReauth)
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Constants.eventLocationRequestCode) {
            if(resultCode == Activity.RESULT_OK) {
                viewModel.eventLatLng = data?.extras?.get(Constants.EVENT_LOCATION) as LatLng
            }
        }
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

    companion object {
        fun newInstance(event: Event): EventEditDialogFragment {
            val instance = EventEditDialogFragment()
            val args = Bundle()
            args.putParcelable(Constants.EVENT, event)
            instance.arguments = args

            return instance
        }
    }
}