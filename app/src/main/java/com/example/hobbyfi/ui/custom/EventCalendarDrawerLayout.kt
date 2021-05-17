package com.example.hobbyfi.ui.custom

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.hobbyfi.R
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlin.math.roundToInt

class EventCalendarDrawerLayout : DrawerLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    lateinit var calendar: MaterialCalendarView
    lateinit var drawer: View

    override fun onFinishInflate() {
        super.onFinishInflate()
        drawer = getChildAt(1)
        calendar = findViewById(R.id.event_calendar)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val rect = Rect()
        calendar.getGlobalVisibleRect(rect) // get the calendar rect positions

        // respond to proper motions and forward events contained inside the calendar's rect only
        if((event.action == MotionEvent.ACTION_MOVE ||
                    event.action == MotionEvent.ACTION_DOWN) && rect.contains(event.x.roundToInt(), event.y.roundToInt())) {
            return (context as Activity).onTouchEvent(event)
        }

        // otherwise return the default intercept touch event response
        return super.onInterceptTouchEvent(event)
    }
}