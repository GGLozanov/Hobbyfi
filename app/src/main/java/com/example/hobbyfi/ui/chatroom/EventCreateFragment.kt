package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventCreateBinding
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.chatroom.EventCreateFragmentViewModel

class EventCreateFragment : ChatroomModelFragment() {
    private val viewModel: EventCreateFragmentViewModel by viewModels()
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


            return@onCreateView binding.root
        }
    }

    override fun initTextFieldValidators() {
        TODO("Not yet implemented")
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        TODO("Not yet implemented")
    }
}