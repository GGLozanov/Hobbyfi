package com.example.hobbyfi.ui.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagSelectionListAdapter
import com.example.hobbyfi.databinding.FragmentTagSelectionBinding
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.safeNavigate
import com.example.hobbyfi.shared.showWarningToast
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.factories.TagListViewModelFactory
import com.example.hobbyfi.viewmodels.shared.TagSelectionFragmentViewModel
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import kotlinx.coroutines.ExperimentalCoroutinesApi

class TagSelectionFragment : BaseFragment() {

    private lateinit var adapter: TagSelectionListAdapter
    private val args: TagSelectionFragmentArgs by navArgs()

    private val viewModel: TagSelectionFragmentViewModel by viewModels(factoryProducer = {
        TagListViewModelFactory(requireActivity().application, args.selectedTags.toList())
    })

    private var _binding: FragmentTagSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding =
            FragmentTagSelectionBinding.inflate(inflater, container, false)

        adapter = TagSelectionListAdapter(
            args.tags.toMutableList(),
            savedInstanceState?.getParcelableArrayList<Tag>(Constants.TAGS)?.toMutableList() ?:
                viewModel.initialSelectedTags.toMutableList() // new list to modify tags
        )

        with(binding) {
            tagList.addItemDecoration(VerticalSpaceItemDecoration(15))
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
            buttonBar.leftButton.setOnClickListener {
                navController.previousBackStackEntry?.savedStateHandle?.set(Constants.selectedTagsKey,
                    viewModel.initialSelectedTags)
                navController.popBackStack() } // dismiss button
            buttonBar.rightButton.setOnClickListener {
                navController.previousBackStackEntry?.savedStateHandle?.set(Constants.selectedTagsKey,
                    if(adapter.getSelectedTags() == viewModel.initialSelectedTags)
                        viewModel.initialSelectedTags else adapter.getSelectedTags())
                navController.popBackStack() } // confirm button
        }

        with(navController.previousBackStackEntry?.destination) {
            val targetFragmentId = this?.id
            if(targetFragmentId == R.id.registerFragment || targetFragmentId == R.id.chatroomCreateFragment) {
                binding.customTagCreateButton.setOnClickListener {
                    if(viewModel.customTagCreateCounter >= 3) {
                        context?.showWarningToast(getString(R.string.too_many_custom_tags))
                        return@setOnClickListener
                    }
                    navController.safeNavigate(R.id.action_tagSelectionDialogFragment_to_customTagCreateDialogFragment)
                }
            } else {
                binding.customTagCreateButton.visibility = View.GONE // can't create custom tags in editing/other places
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
}