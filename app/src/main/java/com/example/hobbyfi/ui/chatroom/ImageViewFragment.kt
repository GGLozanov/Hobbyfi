package com.example.hobbyfi.ui.chatroom

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
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
        binding.image.setImageBitmap(args.image)

        return binding.root
    }
}