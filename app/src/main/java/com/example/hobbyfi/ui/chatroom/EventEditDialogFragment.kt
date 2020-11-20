package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.hobbyfi.R
import com.example.hobbyfi.ui.base.BaseDialogFragment


class EventEditDialogFragment : BaseDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout

        return inflater.inflate(R.layout.fragment_event_edit_dialog, container, false)
    }

}