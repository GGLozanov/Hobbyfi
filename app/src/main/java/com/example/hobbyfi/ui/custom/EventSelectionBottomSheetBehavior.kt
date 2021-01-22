package com.example.hobbyfi.ui.custom

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.customview.widget.ViewDragHelper.STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.roundToInt

class EventSelectionBottomSheetBehavior<T : View>(context: Context, attrs: AttributeSet?) :
    BottomSheetBehavior<T>(context, attrs) {

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: T,
        event: MotionEvent
    ): Boolean {
        if(state == STATE_DRAGGING && event.action == MotionEvent.ACTION_UP) {
            val recyclerView: RecyclerView = parent.findViewById(R.id.event_list)
            val rect = Rect()
            recyclerView.getGlobalVisibleRect(rect)

            if(rect.contains(event.x.roundToInt(), event.y.roundToInt())) {
                return recyclerView.onTouchEvent(event)
            }
        }
        return super.onInterceptTouchEvent(parent, child, event)
    }
}