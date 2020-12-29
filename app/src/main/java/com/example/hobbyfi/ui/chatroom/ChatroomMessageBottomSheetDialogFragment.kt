package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChatroomMessageBottomSheetDialogBinding
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ChatroomMessageBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(message: Message): ChatroomMessageBottomSheetDialogFragment {
            val instance = ChatroomMessageBottomSheetDialogFragment()
            val args = Bundle()
            args.putParcelable(Constants.MESSAGE, message)
            instance.arguments = args

            return instance
        }
    }

    interface OnMessageOptionSelected {
        fun onEditMessageSelect(view: View, message: Message)

        fun onDeleteMessageSelect(view: View, message: Message)
    }
    private lateinit var onMessageOptionSelected: OnMessageOptionSelected

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding = FragmentChatroomMessageBottomSheetDialogBinding.inflate(
            inflater,
            container,
            false
        )

        with(binding) {
            deleteMessageLayout.setOnClickListener {
                Constants.buildDeleteAlertDialog(
                    requireContext(),
                    resources.getString(R.string.delete_message),
                    { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        onMessageOptionSelected.onDeleteMessageSelect(
                            it,
                            requireArguments().getParcelable(Constants.MESSAGE)!!
                        )
                    },
                    {  dialogInterface, _ ->
                        dialogInterface.dismiss()
                        dismiss()
                    }
                )
            }
            editMessageLayout.setOnClickListener {
                onMessageOptionSelected.onEditMessageSelect(
                    it,
                    requireArguments().getParcelable(Constants.MESSAGE)!!
                )
            }
        }

        return binding.root
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is OnMessageOptionSelected) {
            onMessageOptionSelected = context
        } else {
            throw ClassCastException(context.toString()
                    + " must implement OnMessageOptionSelected")
        }
    }
}