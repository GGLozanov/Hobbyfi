package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hobbyfi.R
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Constants
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChatroomUserBottomSheetDialogFragment : ChatroomBottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Databinding + gridview for tags and other user display
        // TODO: In the FAAAR future (if I'm still slaving away at this project), add ability to add to personal chat and blocking
        return inflater.inflate(
            R.layout.fragment_chatroom_user_bottom_sheet_dialog,
            container,
            false
        )
    }

    companion object {
        @JvmStatic
        fun newInstance(user: User) =
            ChatroomUserBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(Constants.USER, user)
                }
            }
    }
}