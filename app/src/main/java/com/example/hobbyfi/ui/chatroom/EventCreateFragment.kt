package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.hobbyfi.R
import com.example.hobbyfi.ui.base.BaseFragment
import kotlinx.android.synthetic.main.activity_chatroom.*

class EventCreateFragment : ChatroomFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_create, container, false)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }
}