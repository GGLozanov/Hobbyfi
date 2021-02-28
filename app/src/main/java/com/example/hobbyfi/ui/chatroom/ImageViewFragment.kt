package com.example.hobbyfi.ui.chatroom

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentImageViewBinding
import com.example.hobbyfi.ui.base.BaseFragment

class ImageViewFragment : ChatroomModelFragment() {

    private val args: ImageViewFragmentArgs by navArgs()
    private lateinit var binding: FragmentImageViewBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentImageViewBinding.inflate(layoutInflater, container, false)
        Glide.with(this)
            .load(args.imageUrl)
            .placeholder(R.drawable.ic_baseline_image_42)
            .into(binding.image)

        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.chatroom_image_view_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_download -> {
                // TODO: Download file in SAF-compliant way
            }
        }

        return super.onOptionsItemSelected(item)
    }
}