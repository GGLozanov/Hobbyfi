package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventEditDialogBinding
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.ui.shared.CameraCaptureActivity
import com.example.hobbyfi.ui.shared.CameraCaptureFragment
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.chatroom.EventEditDialogFragmentViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*


@ExperimentalCoroutinesApi
class EventEditDialogFragment : ChatroomDialogFragment(), TextFieldInputValidationOnus,
        DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private val eventCalendar = Calendar.getInstance()

    private lateinit var binding: FragmentEventEditDialogBinding
    private val viewModel: EventEditDialogFragmentViewModel by viewModels(factoryProducer = {
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
        binding = DataBindingUtil
            .inflate(layoutInflater, R.layout.fragment_event_edit_dialog, container, false)

        binding.viewModel = viewModel
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@EventEditDialogFragment

            val event: Event = requireArguments().getParcelable(Constants.EVENT)!!
            event.photoUrl?.let {
                it.asFirebaseStorageReference()?.let { ref ->
                    ref.metadata.addOnSuccessListener { metadata ->
                        Glide.with(this@EventEditDialogFragment)
                            .loadReferenceWithMetadataSignature(ref, metadata)
                            .placeholder(eventInfo.eventImage.image.drawable)
                            .into(eventInfo.eventImage.image)
                    }
                }
            }

            eventInfo.eventInfoButtonBar.leftButton.setOnClickListener {
                Callbacks.initDateTimeDatePickerDialog(requireContext(), this@EventEditDialogFragment, viewModel!!)
            }

            eventInfo.eventInfoButtonBar.rightButton.setOnClickListener {
                Callbacks.startChooseEventLocationMapsActivity(
                    this@EventEditDialogFragment,
                    viewModel!!,
                    LatLng(viewModel!!.event.latitude, viewModel!!.event.longitude)
                )
            }

            eventEditDialogButtonBar.leftButton.setOnClickListener {
                dismiss()
            }

            eventEditDialogButtonBar.rightButton.setOnClickListener {
                val fieldMap: MutableMap<String, String?> = mutableMapOf()

                if(viewModel!!.name.value != viewModel!!.event.name) {
                    fieldMap[Constants.NAME] = viewModel!!.name.value
                }

                if(viewModel!!.description.value != viewModel!!.event.description) {
                    fieldMap[Constants.DESCRIPTION] = viewModel!!.description.value
                }

                viewModel!!.base64Image.originalUri?.let {
                    fieldMap[Constants.IMAGE] = it
                }

                viewModel!!.eventLatLng?.let {
                    if(it.latitude != viewModel!!.event.latitude) {
                        fieldMap[Constants.LATITUDE] = it.latitude.toString()
                    }

                    if(it.longitude != viewModel!!.event.longitude) {
                        fieldMap[Constants.LONGITUDE] = it.longitude.toString()
                    }
                }

                viewModel!!.eventDate?.let {
                    val formattedDate = Constants.dateTimeFormatter.format(viewModel!!.eventDate!!)
                    if(viewModel!!.event.date != formattedDate) {
                        fieldMap[Constants.DATE] = formattedDate
                    }
                }

                if(fieldMap.isEmpty()) {
                    context?.showWarningToast(getString(R.string.no_fields))
                    return@setOnClickListener
                } else if(fieldMap.size == 1 && fieldMap.containsKey(Constants.IMAGE)) {
                    WorkerUtils.buildAndEnqueueImageUploadWorker(
                        viewModel!!.event.id,
                        prefConfig.getAuthUserToken()!!,
                        Constants.EDIT_EVENT_TYPE,
                        viewModel!!.base64Image.originalUri!!,
                        requireContext(),
                        R.string.pref_last_events_fetch_time,
                        activityViewModel.authChatroom.value!!.id
                    )
                    return@setOnClickListener
                }

                fieldMap[Constants.ID] = viewModel!!.event.id.toString()
                Log.i("EventEditFragment", "EventFieldMap update: $fieldMap")

                lifecycleScope.launch {
                    viewModel!!.sendIntent(
                        EventIntent.UpdateEvent(
                            fieldMap
                        )
                    )
                }
            }

            eventInfo.eventImage.galleryOption.setOnClickListener {
                Callbacks.requestImage(this@EventEditDialogFragment)
            }

            eventInfo.eventImage.cameraOption.setOnClickListener {
                Intent(requireContext(), CameraCaptureActivity::class.java).run {
                    startActivityForResult(this, Constants.cameraRequestCode)
                }
            }

            observeEventState()

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Uri>(Constants.CAMERA_URI)
            ?.observe(viewLifecycleOwner, Observer {
                binding.eventInfo.eventImage.image.loadUriIntoGlideAndSaveInImageHolder(it, viewModel.base64Image)
        })
    }

    // common ancestor with EventCreateFragment go brrr
    override fun observePredicateValidators() {
        // Code quality TODO:
        // Extract this method (from fragments and all activities) to a factory which takes a list of views and initialises them
        // with the preset predicate/error pairs
        viewModel.name.invalidity.observe(
            viewLifecycleOwner,
            TextInputLayoutFocusValidatorObserver(binding.eventInfo.nameInputField, getString(R.string.name_input_error))
        )

        viewModel.description.invalidity.observe(
            viewLifecycleOwner,
            TextInputLayoutFocusValidatorObserver(binding.eventInfo.descriptionInputField, getString(R.string.description_input_error))
        )
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.eventEditDialogButtonBar.rightButton))
    }

    override fun onStart() {
        super.onStart()
        observePredicateValidators()
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
                        isCancelable = false
                    }
                    is EventState.OnData.EventEditResult -> {
                        isCancelable = true
                        activityViewModel.sendEventsIntent(EventListIntent.UpdateAnEventCache(it.updateFields))
                        if(it.updateFields.containsKey(Constants.IMAGE)) {
                            WorkerUtils.buildAndEnqueueImageUploadWorker(
                                viewModel.event.id,
                                prefConfig.getAuthUserToken()!!,
                                Constants.EDIT_EVENT_TYPE,
                                viewModel.base64Image.originalUri!!,
                                requireContext(),
                                R.string.pref_last_events_fetch_time,
                                activityViewModel.authChatroom.value!!.id
                            )
                        }

                        viewModel.resetState()
                        dismiss()
                    }
                    is EventState.Error -> {
                        isCancelable = true
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

        if(requestCode == Constants.cameraRequestCode) {
            if(resultCode == Activity.RESULT_OK) {
                binding.eventInfo.eventImage.image.loadUriIntoGlideAndSaveInImageHolder(
                    data!!.extras!![Constants.CAMERA_URI] as Uri, viewModel.base64Image)
            }
        }

        Callbacks.handleImageRequestWithPermission(
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            binding.eventInfo.eventImage.image.loadUriIntoGlideAndSaveInImageHolder(data!!.data!!, viewModel.base64Image)
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