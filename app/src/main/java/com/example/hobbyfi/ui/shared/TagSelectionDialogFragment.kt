package com.example.hobbyfi.ui.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagListAdapter
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.viewmodels.shared.TagSelectionDialogFragmentViewModel
import kotlinx.android.synthetic.main.fragment_tag_selection_dialog.*

class TagSelectionDialogFragment : BaseDialogFragment() {

    private lateinit var adapter: TagListAdapter
    private val viewModel: TagSelectionDialogFragmentViewModel by viewModels()
    private val args: TagSelectionDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tag_selection_dialog, container, false)

        viewModel.setInitialTags(args.tags.toMutableList())

        adapter = TagListAdapter(viewModel.tags.value!!)
        adapter.setOnItemPressed(object : TagListAdapter.OnItemPressed {
            override fun onItemPress(tag: Tag, wasSelected: Boolean) {
                if(!wasSelected) {
                    viewModel.removeTagFromSelected(tag)
                } else {
                    viewModel.addTagToSelected(tag)
                }
            }
        })
        tag_list.adapter = adapter

        viewModel.tags.observe(this, {
            adapter.setItems(it)
        })

        cancel_button.setOnClickListener {
            navController.previousBackStackEntry?.savedStateHandle?.set("selectedTags", viewModel.getInitialTags())

            dismiss()
        }

        confirm_tags_button.setOnClickListener {
            navController.previousBackStackEntry?.savedStateHandle?.set("selectedTags", viewModel.selectedTags.value)

            dismiss()
        }

        custom_tag_create_button.setOnClickListener {
            if(viewModel.customTagCreateCounter >= 3) {
                Toast.makeText(context, "Too many custom tags created!", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            navController.navigate(R.id.action_tagSelectionDialogFragment_to_customTagCreateDialogFragment2)
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Tag>("tag")?.observe(viewLifecycleOwner) {
            viewModel.addTag(it)
            viewModel.incrementCustomTagCounter()
        }

        return view
    }

}