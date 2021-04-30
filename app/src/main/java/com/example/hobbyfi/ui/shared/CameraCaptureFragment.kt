package com.example.hobbyfi.ui.shared

import android.Manifest
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.CameraCaptureLayoutBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.showSuccessSnackbar
import com.example.hobbyfi.shared.startCamera
import com.example.hobbyfi.shared.takePhoto
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.chatroom.ChatroomModelFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService

class CameraCaptureFragment : ChatroomModelFragment() {
    private var imageCapture: ImageCapture? = null

    private lateinit var binding: CameraCaptureLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CameraCaptureLayoutBinding.inflate(
            inflater, container, false)

        binding.cameraCaptureButton.setOnClickListener {
            imageCapture?.takePhoto(it.context, { output ->
                view?.showSuccessSnackbar(getString(R.string.photo_capture_success))
                navController.previousBackStackEntry?.savedStateHandle?.set(Constants.CAMERA_URI, output.savedUri)
                navController.popBackStack()
            }, {
                view?.showSuccessSnackbar(getString(R.string.photo_capture_failed))
            })
        }

        if(EasyPermissions.hasPermissions(
                requireContext(),
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
            binding.viewFinder.startCamera(this).observe(viewLifecycleOwner, {
                imageCapture = it
            })
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.camera_rationale),
                Constants.cameraPermissionsRequestCode,
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        return binding.root
    }
}