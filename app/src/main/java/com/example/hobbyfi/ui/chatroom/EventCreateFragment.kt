package com.example.hobbyfi.ui.chatroom

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventCreateBinding
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.chatroom.EventCreateFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*

@ExperimentalCoroutinesApi
class EventCreateFragment : ChatroomModelFragment(),
        DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private val eventCalendar = Calendar.getInstance()

    private val viewModel: EventCreateFragmentViewModel by viewModels()
    private lateinit var binding: FragmentEventCreateBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_event_create, container, false)

        binding.viewModel = viewModel

        with(binding) {
            eventInfoButtonBar.leftButton.setOnClickListener { // select event date
                val c = Calendar.getInstance()
                val initialYear = c.get(Calendar.YEAR)
                val initialMonth = c.get(Calendar.MONTH)
                val initialDay = c.get(Calendar.DAY_OF_MONTH)
                val dialog = DatePickerDialog(
                    requireContext(),
                    this@EventCreateFragment,
                    initialYear,
                    initialMonth,
                    initialDay
                )
                dialog.show()
            }

            eventInfoButtonBar.rightButton.setOnClickListener { // select location
                navController.navigate(
                    R.id.eventChooseLocationMapsActivity
                )
            }

            eventImage.setOnClickListener {
                Callbacks.requestImage(this@EventCreateFragment)
            }

            return@onCreateView binding.root
        }
    }

    override fun initTextFieldValidators() {
        with(binding) {
            // Code quality TODO:
            //  Extract this method (from fragments and all activities) to a factory which takes a list of views and initialises them
            // with the preset predicate/error pairs
            nameInputField.addTextChangedListener(
                Constants.nameInputError,
                Constants.namePredicate
            )

            descriptionInputField.addTextChangedListener(
                Constants.descriptionInputError,
                Constants.descriptionPredicate
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(nameInputField, Constants.nameInputError) ||
                    FieldUtils.isTextFieldInvalid(descriptionInputField, Constants.descriptionInputError)
        }
    }

    override fun onDateSet(picker: DatePicker?, year: Int, month: Int, day: Int) {
        eventCalendar.set(Calendar.YEAR, year)
        eventCalendar.set(Calendar.MONTH, month)
        eventCalendar.set(Calendar.DAY_OF_MONTH, day)

        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        val dialog = TimePickerDialog(activity, this, hour, minute, DateFormat.is24HourFormat(activity))

        dialog.show()
        dialog.setOnDismissListener {
            viewModel.setEventDate(null)
        }
    }

    override fun onTimeSet(picker: TimePicker?, hours: Int, minutes: Int) {
        eventCalendar.set(Calendar.HOUR_OF_DAY, hours)
        eventCalendar.set(Calendar.MINUTE, minutes)

        viewModel.setEventDate(eventCalendar.time)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // SLIGHT FIXME: slight code dupss
        Callbacks.handleImageRequestWithPermission(
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            binding.eventImage.setImageBitmap(it)
            viewModel.base64Image.setImageBase64(
                ImageUtils.encodeImage(it)
            )
        }
    }
}