package com.example.hobbyfi.ui.custom

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import com.example.hobbyfi.ui.chatroom.EventSelectionBottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.math.roundToInt

class EventSelectionBottomSheetBehavior<T : View>(private val context: Context, attrs: AttributeSet?) :
    BottomSheetBehavior<T>(context, attrs) {

    @ExperimentalCoroutinesApi
    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: T, event: MotionEvent): Boolean {
        if(state == STATE_DRAGGING &&
            (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_MOVE)) {
                val recyclerView: RecyclerView = child.findViewById(R.id.event_list)
                val rect = Rect()
                recyclerView.getGlobalVisibleRect(rect)


            parent.requestDisallowInterceptTouchEvent(true)
            return !rect.contains(event.x.roundToInt(), event.y.roundToInt())
        }
        return super.onInterceptTouchEvent(parent, child, event)
    }
}