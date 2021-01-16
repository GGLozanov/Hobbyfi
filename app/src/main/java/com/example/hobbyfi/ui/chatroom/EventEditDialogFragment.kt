package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventEditDialogBinding
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.chatroom.EventEditFragmentViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi


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
            return@onCreateView root
        }
    }

    override fun initTextFieldValidators() {
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        // return FieldUtils.isTextFieldInvalid()
        return true
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