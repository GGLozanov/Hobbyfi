package com.example.hobbyfi.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class EventSelectionBottomSheetRecyclerView: RecyclerView {
    private var isBeingDragged = false
    private var lastMotionY = 0f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    // lateinit var coordinatorLayout: CoordinatorLayout

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(canScrollUp())
                // return onTouchEvent(event)
            }
            else -> {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val y = event.rawY
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                run {
                    if (!isBeingDragged) {
                        val deltaY: Float = lastMotionY - y
                        isBeingDragged = (deltaY > 0 && canScrollDown())
                                || (deltaY < 0 && canScrollUp())
                        if (reachedScrollUp()) {
                            parent.requestDisallowInterceptTouchEvent(true)
                        } else {
                            parent.requestDisallowInterceptTouchEvent(false)
                            return false
                        }
                    }
                }
                isBeingDragged = false
            }
            MotionEvent.ACTION_CANCEL -> isBeingDragged = false
        }
        lastMotionY = y
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // coordinatorLayout = parent.parent.parent as CoordinatorLayout
    }

    private fun canScrollUp(): Boolean {
        val childCount = childCount
        if (childCount == 0) {
            return false
        }
        val firstPosition: Int = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val firstTop = getChildAt(0).top
        return firstPosition > 0 || firstTop < (layoutManager as LinearLayoutManager).paddingTop
    }

    private fun reachedScrollUp(): Boolean {
        val childCount = childCount
        if (childCount == 0) {
            return false
        }
        val firstPosition: Int = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val firstTop = getChildAt(0).top
        return firstPosition == 0 && firstTop == 0
    }

    private fun canScrollDown(): Boolean {
        val childCount = childCount
        if (childCount == 0) {
            return false
        }
        val firstPosition: Int = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val lastBottom = getChildAt(childCount - 1).bottom
        val lastPosition = firstPosition + childCount
        return lastPosition < (layoutManager as LinearLayoutManager).itemCount ||
                lastBottom > height - (layoutManager as LinearLayoutManager).paddingBottom
    }
}