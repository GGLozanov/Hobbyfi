package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgs
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.android.x.kodein

abstract class ChatroomBottomSheetDialogFragment : BottomSheetDialogFragment(), KodeinAware {
    override val kodein: Kodein by kodein()

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