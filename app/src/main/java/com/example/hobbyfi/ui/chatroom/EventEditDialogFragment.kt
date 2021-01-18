package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventEditDialogBinding
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.EventState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.viewmodels.chatroom.EventEditFragmentViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect


@ExperimentalCoroutinesApi
class EventEditDialogFragment : ChatroomDialogFragment() {
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


            observeEventState()

            return@onCreateView root
        }
    }

    override fun initTextFieldValidators() {
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        // return FieldUtils.isTextFieldInvalid()
        return true
    }

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
                        // TODO: Handle error
                    }
                    else -> throw State.InvalidStateException()
                }
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