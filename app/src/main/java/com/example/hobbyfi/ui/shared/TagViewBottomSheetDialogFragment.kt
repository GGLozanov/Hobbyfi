package com.example.hobbyfi.ui.shared

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hobbyfi.R
import com.example.hobbyfi.viewmodels.base.TagBundleHolder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TagViewBottomSheetDialogFragment<T: TagBundleHolder> : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // TODO: Observe currentBackStackEntry for tag list which can be updated from multiple places, thereby updating the tags list?

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tag_view_bottom_sheet_dialog, container, false)
    }
}