package com.example.hobbyfi.ui.custom

import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan

class EventCalendarDecorator(
    private val color: Int,
    private val calendarDays: Collection<CalendarDay>
) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean = calendarDays.contains(day)

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(5F, color)) // decorate with a simple dot for now
    }
}