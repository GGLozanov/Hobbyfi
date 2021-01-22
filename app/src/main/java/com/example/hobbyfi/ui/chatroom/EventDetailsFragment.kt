package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.EventDetailsFragmentBinding
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.chatroom.EventDetailsViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi

class EventDetailsFragment : ChatroomModelFragment() {
    // TODO: Pass Event here
    // TODO: Add users subscribed users recyclerview (geoPoint fetch from viewModel for given event)
    // TODO: Add join/leave buttons depending on usergeopoint contains event id


    private val viewModel: EventDetailsViewModel by viewModels(factoryProducer = {
        EventViewModelFactory(requireActivity().application, requireArguments()[Constants.EVENT] as Event)
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: EventDetailsFragmentBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.event_details_fragment,
            container,
            false
        )
        binding.viewModel = viewModel

        // TODO: startActivityForResult + intent extra parcelable event

        return binding.root
    }

    companion object {
        fun newInstance(event: Event): EventDetailsFragment {
            val instance = EventDetailsFragment()
            val args = Bundle()
            args.putParcelable(Constants.EVENT, event)
            instance.arguments = args

            return instance
        }
    }

    @ExperimentalCoroutinesApi
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = requireActivity() as ChatroomActivity
        activity.title = viewModel.eventName
        // TODO: Handle navdrawer
        // activity.enableNavDrawer(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO: If result == cancelled && activity request code => pop self from backstack
    }
}