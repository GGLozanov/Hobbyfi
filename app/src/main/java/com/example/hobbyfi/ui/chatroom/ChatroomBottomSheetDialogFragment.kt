package com.example.hobbyfi.ui.chatroom

import android.util.DisplayMetrics
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.navArgs
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class ChatroomBottomSheetDialogFragment : BottomSheetDialogFragment() {
    @ExperimentalCoroutinesApi
    protected val activityViewModel: ChatroomActivityViewModel by activityViewModels(factoryProducer = {
        val activityArgs: ChatroomActivityArgs by (requireActivity() as ChatroomActivity).navArgs()
        AuthUserChatroomViewModelFactory(requireActivity().application, activityArgs.user, activityArgs.chatroom)
    })

    protected fun<T: View> scaleViewByScreenSizeAndReLayout(
        view: View,
        behaviour: BottomSheetBehavior<T>,
        bottomSheetView: T,
        coordinatorView: CoordinatorLayout,
        divisor: Int
    ) {
        view.layoutParams.height = DisplayMetrics().heightPixels / divisor
        view.requestLayout()
        behaviour.onLayoutChild(coordinatorView, bottomSheetView, ViewCompat.LAYOUT_DIRECTION_LTR)
    }

    override fun onStart() {
        super.onStart()
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED // reinstating for landscape mode
    }
}