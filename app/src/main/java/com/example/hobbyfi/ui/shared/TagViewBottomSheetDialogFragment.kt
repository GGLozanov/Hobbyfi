package com.example.hobbyfi.ui.shared

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentTagViewBottomSheetDialogBinding
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.reinitChipsByTags
import com.example.hobbyfi.utils.ColourUtils
import com.example.hobbyfi.viewmodels.base.TagBundleHolder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

// Used to only display static tags (like Chatrooms)
open class TagViewBottomSheetDialogFragment : BottomSheetDialogFragment() {
    protected lateinit var binding: FragmentTagViewBottomSheetDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_tag_view_bottom_sheet_dialog, container, false)
        with(binding) {
            modelTitle = "What ${requireArguments().getString(Constants.NAME)} engages with:"
            tagGroup.setChipSpacing(10)

            return@onCreateView root
        }
    }

    override fun onStart() {
        super.onStart()
        binding.noTagsText.isVisible = !binding.tagGroup.reinitChipsByTags(requireArguments()
            .getParcelableArrayList(Constants.TAGS))
    }


    companion object {
        fun newInstance(tags: List<Tag>?, name: String): TagViewBottomSheetDialogFragment {
            val bundle = Bundle().apply {
                putParcelableArrayList(Constants.TAGS, if(tags != null) ArrayList(tags.toMutableList()) else null)
                putString(Constants.NAME, name)
            }

            return TagViewBottomSheetDialogFragment().apply {
                arguments = bundle
            }
        }
    }
}