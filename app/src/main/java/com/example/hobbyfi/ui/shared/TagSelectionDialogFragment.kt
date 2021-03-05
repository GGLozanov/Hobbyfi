package com.example.hobbyfi.ui.shared

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagSelectionListAdapter
import com.example.hobbyfi.databinding.FragmentTagSelectionDialogBinding
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.viewmodels.factories.TagListViewModelFactory
import com.example.hobbyfi.viewmodels.shared.TagSelectionDialogFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.coroutines.ExperimentalCoroutinesApi

class TagSelectionDialogFragment : BaseDialogFragment() {

    private lateinit var adapter: TagSelectionListAdapter
    private val args: TagSelectionDialogFragmentArgs by navArgs()

    private val viewModel: TagSelectionDialogFragmentViewModel by viewModels(factoryProducer = {
        TagListViewModelFactory(requireActivity().application, args.selectedTags.toList())
    })

    private var _binding: FragmentTagSelectionDialogBinding? = null
    private val binding get() = _binding!!

    private var dismissedFromConfirm = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding =
            FragmentTagSelectionDialogBinding.inflate(inflater, container, false)

        adapter = TagSelectionListAdapter(
            args.tags.toMutableList(),
            savedInstanceState?.getParcelableArrayList<Tag>(Constants.TAGS)?.toMutableList() ?:
                viewModel.initialSelectedTags.toMutableList() // new list to modify tags
        )

        with(binding) {
            tagList.addItemDecoration(VerticalSpaceItemDecoration(5))
            tagList.adapter = adapter
            return@onCreateView root
        }
    }

    @ExperimentalCoroutinesApi
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Tag>(Constants.tagsKey)?.observe(viewLifecycleOwner) {
            adapter.addTag(it)
            viewModel.incrementCustomTagCounter()
        }

        with(binding) {
            buttonBar.leftButton.setOnClickListener { dismiss() } // dismiss button
            buttonBar.rightButton.setOnClickListener { dismissedFromConfirm = true
                dismiss() } // confirm button
        }

        with(navController.previousBackStackEntry?.destination) {
            val targetFragmentId = this?.id
            if(targetFragmentId == R.id.registerFragment || targetFragmentId == R.id.chatroomCreateFragment) {
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            putParcelableArrayList(Constants.TAGS, ArrayList(adapter.getSelectedTags()))
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // nullify the binding (fragment outlives the binding)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        navController.previousBackStackEntry?.savedStateHandle?.set(Constants.selectedTagsKey,
            if(adapter.getSelectedTags() == viewModel.initialSelectedTags || !dismissedFromConfirm)
                viewModel.initialSelectedTags else adapter.getSelectedTags()
        )
    }
}