package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventCreateBinding
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.chatroom.EventCreateFragmentViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@EventCreateFragment

            with(eventInfo) {
                eventInfoButtonBar.leftButton.setOnClickListener { // select event date
                    Callbacks.initDateTimeDatePickerDialog(
                        requireContext(),
                        this@EventCreateFragment, this@EventCreateFragment.viewModel
                    )
                }

                eventInfoButtonBar.rightButton.setOnClickListener { // select location
                    Callbacks.startChooseEventLocationMapsActivity(this@EventCreateFragment,
                        this@EventCreateFragment.viewModel)
                }

                eventInfo.eventImage.galleryOption.setOnClickListener {
                    Callbacks.requestImage(this@EventCreateFragment)
                }

                eventInfo.eventImage.cameraOption.setOnClickListener {
                    navController.navigate(R.id.action_global_camera_capture_nav_graph)
                }
            }

            confirmButton.setOnClickListener {
                lifecycleScope.launch {
                    viewModel!!.sendIntent(EventIntent.CreateEvent(activityViewModel.authChatroom.value!!.id))
                }
            }

            return@onCreateView binding.root
        }
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEventState()

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Uri>(Constants.CAMERA_URI)
            ?.observe(viewLifecycleOwner, Observer {
                binding.eventInfo.eventImage.image.loadUriIntoGlideAndSaveInImageHolder(it, viewModel.base64Image)
            })
    }

    private fun observeEventState() {
        lifecycleScope.launch {
            viewModel.mainState.collectLatestWithLoading(viewLifecycleOwner, navController,
                    EventCreateFragmentDirections.actionGlobalLoadingNavGraph(R.id.eventCreateFragment),
                    EventState.Loading::class, viewModel::resetState) {
                when(it) {
                    EventState.Idle -> {

                    }
                    is EventState.OnData.EventCreateResult -> {
                        viewModel.base64Image.originalUri?.let { image ->
                            WorkerUtils.buildAndEnqueueImageUploadWorker(
                                it.event.id,
                                prefConfig.getAuthUserToken()!!,
                                Constants.EVENTS,
                                image,
                                requireContext(),
                                R.string.pref_last_events_fetch_time
                            )
                        }

                        activityViewModel.sendEventsIntent(EventListIntent.AddAnEventCache(it.event))
                        context?.showSuccessToast(getString(R.string.event_create_success))

                        if(navController.currentDestination?.id == R.id.eventCreateFragment) {
                            navController.popBackStack(R.id.chatroomMessageListFragment, false)
                        }
                    }
                    is EventState.Error -> {
                        // TODO: Handle shouldReauth
                        context?.showFailureToast(getString(R.string.something_wrong) + " ${it.error}")
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

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
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.confirmButton))
    }

    override fun onStart() {
        super.onStart()
        observePredicateValidators()
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
            binding.eventInfo.eventImage.image
                .loadUriIntoGlideAndSaveInImageHolder(data!!.data!!, viewModel.base64Image)
        }
    }
}