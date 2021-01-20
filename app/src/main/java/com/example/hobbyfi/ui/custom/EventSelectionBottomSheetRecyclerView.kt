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

    lateinit var coordinatorLayout: CoordinatorLayout

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (!canScrollVertically(this)) {
            coordinatorLayout.requestDisallowInterceptTouchEvent(true)
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        coordinatorLayout = parent.parent.parent as CoordinatorLayout
    }

    private fun canScrollVertically(view: RecyclerView): Boolean {
        var canScroll = false
        if (view.childCount > 0) {
            canScroll = (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0 // canScrollVertically(-1)
        }
        Log.i("EventSelBSRV", "CanScroll: $canScroll")
        return !canScroll
    }
}