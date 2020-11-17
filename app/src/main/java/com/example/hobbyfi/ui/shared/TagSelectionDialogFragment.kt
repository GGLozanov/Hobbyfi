package com.example.hobbyfi.ui.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagListAdapter
import com.example.hobbyfi.databinding.FragmentTagSelectionDialogBinding
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.viewmodels.shared.TagSelectionDialogFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.android.synthetic.main.fragment_tag_selection_dialog.*

class TagSelectionDialogFragment : BaseDialogFragment() {

    private lateinit var adapter: TagListAdapter
    private val viewModel: TagSelectionDialogFragmentViewModel by viewModels(factoryProducer = {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    })
    private val args: TagSelectionDialogFragmentArgs by navArgs()

    private var _binding: FragmentTagSelectionDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding =
            FragmentTagSelectionDialogBinding.inflate(inflater, container, false)

        val initialSelectedTags = args.selectedTags.toMutableList()
        viewModel.setInitialSelectedTags(initialSelectedTags)

        adapter = TagListAdapter(
            args.tags.toMutableList(),
            initialSelectedTags
        )

        binding.tagList.addItemDecoration(VerticalSpaceItemDecoration(5))
        binding.tagList.adapter = adapter

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Tag>("tag")?.observe(viewLifecycleOwner) {
            adapter.addTag(it)
            viewModel.incrementCustomTagCounter()
        }


        binding.cancelButton.setOnClickListener {
            navController.previousBackStackEntry?.savedStateHandle?.set("selectedTags", viewModel.getInitialSelectedTags())

            dismiss()
        }

        binding.confirmTagsButton.setOnClickListener {
            navController.previousBackStackEntry?.savedStateHandle?.set("selectedTags", adapter.getSelectedTags())

            dismiss()
        }

        binding.customTagCreateButton.setOnClickListener {
            if(viewModel.customTagCreateCounter >= 3) {
                Toast.makeText(context, "Too many custom tags created!", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            navController.navigate(R.id.action_tagSelectionDialogFragment_to_customTagCreateDialogFragment2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // nullify the binding (fragment outlives the binding)
    }
}