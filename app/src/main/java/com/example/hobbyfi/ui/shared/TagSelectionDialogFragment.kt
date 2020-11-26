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
import com.example.hobbyfi.databinding.FragmentTagSelectionDialogBinding
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.auth.LoginFragment
import com.example.hobbyfi.ui.auth.RegisterFragment
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.ui.create.ChatroomCreateActivity
import com.example.hobbyfi.viewmodels.factories.TagSelectionDialogFragmentViewModelFactory
import com.example.hobbyfi.viewmodels.shared.TagSelectionDialogFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.android.synthetic.main.fragment_tag_selection_dialog.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

class TagSelectionDialogFragment : BaseDialogFragment() {

    private lateinit var adapter: TagListAdapter
    private val args: TagSelectionDialogFragmentArgs by navArgs()

    private val viewModel: TagSelectionDialogFragmentViewModel by viewModels(factoryProducer = {
        TagSelectionDialogFragmentViewModelFactory(requireActivity().application, args.selectedTags.toMutableList())
    })

    private var _binding: FragmentTagSelectionDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding =
            FragmentTagSelectionDialogBinding.inflate(inflater, container, false)

        adapter = TagListAdapter(
            args.tags.toMutableList(),
            viewModel.initialSelectedTags
        )

        binding.tagList.addItemDecoration(VerticalSpaceItemDecoration(5))
        binding.tagList.adapter = adapter

        return binding.root
    }

    @ExperimentalCoroutinesApi
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Tag>(Constants.tagsKey)?.observe(viewLifecycleOwner) {
            adapter.addTag(it)
            viewModel.incrementCustomTagCounter()
        }

        binding.cancelButton.setOnClickListener {
            navController.previousBackStackEntry?.savedStateHandle?.set(Constants.selectedTagsKey, viewModel.initialSelectedTags)

            dismiss()
        }

        binding.confirmTagsButton.setOnClickListener {
            navController.previousBackStackEntry?.savedStateHandle?.set(Constants.selectedTagsKey, adapter.getSelectedTags())

            dismiss()
        }

        if(targetFragment is RegisterFragment || targetFragment is LoginFragment ||
            targetFragment?.activity is ChatroomCreateActivity) { // TODO: Not sure how ChatroomCreateACTIVITY might start a dialog fragment but I'll cross that bridge when I get to it
            binding.customTagCreateButton.setOnClickListener {
                if(viewModel.customTagCreateCounter >= 3) {
                    Toast.makeText(context, "Too many custom tags created!", Toast.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }
                navController.navigate(R.id.action_tagSelectionDialogFragment_to_customTagCreateDialogFragment)
            }
        } else {
            binding.customTagCreateButton.visibility = View.GONE // can't create custom tags in editing/other places
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // nullify the binding (fragment outlives the binding)
    }
}