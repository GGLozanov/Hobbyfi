package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChatroomMessageBottomSheetDialogBinding
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ChatroomMessageBottomSheetDialogFragment : BottomSheetDialogFragment(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: FragmentChatroomMessageBottomSheetDialogBinding
    private lateinit var message: Message

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
        binding = FragmentChatroomMessageBottomSheetDialogBinding.inflate(
            inflater,
            container,
            false
        )

        message = requireArguments().getParcelable(Constants.MESSAGE)!!

        with(binding) {
            bottomSheet.apply {
                BottomSheetBehavior.from(this).apply {
                    peekHeight = 1 / 4 * (DisplayMetrics().heightPixels)
                    state = BottomSheetBehavior.STATE_EXPANDED
                }

                menu.findItem(R.id.action_edit_message).isVisible = !Constants.imageRegex
                    .matches(message.message) && !message.isTimeline // TODO: Change when add text to image messages
                setNavigationItemSelectedListener(this@ChatroomMessageBottomSheetDialogFragment)
            }

            root.requestLayout()
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_delete_message -> {
                Constants.buildDeleteAlertDialog(
                    requireContext(),
                    resources.getString(R.string.delete_message),
                    { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        onMessageOptionSelected.onDeleteMessageSelect(
                            binding.root,
                            message
                        )
                        dismiss()
                    },
                    {  dialogInterface, _ ->
                        dialogInterface.dismiss()
                        dismiss()
                    }
                )
            }
            R.id.action_edit_message -> {
                onMessageOptionSelected.onEditMessageSelect(
                    binding.root,
                    message
                )
                dismiss()
            }
        }
        return true
    }
}