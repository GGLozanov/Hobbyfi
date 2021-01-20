package com.example.hobbyfi.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AbsListView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EventSelectionBottomSheetRecyclerView: RecyclerView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (canScrollVertically(this)) {
            parent.parent.parent.parent
                .requestDisallowInterceptTouchEvent(true)
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun canScrollVertically(view: RecyclerView): Boolean {
        var canScroll = false
        if (view.childCount > 0) {
            canScroll = (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0 // canScrollVertically(-1)
        }
        Log.i("EventSelBSRV", "CanScroll: $canScroll")
        return canScroll
    }
}