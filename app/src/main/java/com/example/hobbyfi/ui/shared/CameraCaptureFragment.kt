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
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentCameraCaptureBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.showSuccessSnackbar
import com.example.hobbyfi.ui.base.BaseFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService

class CameraCaptureFragment : BaseFragment() {
    private var imageCapture: ImageCapture? = null

    private lateinit var binding: FragmentCameraCaptureBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraCaptureBinding.inflate(
            inflater, container, false)

        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }

        if(EasyPermissions.hasPermissions(
                requireContext(),
                Manifest.permission.CAMERA
            )) {
            startCamera()
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.camera_rationale),
                Constants.cameraPermissionsRequestCode,
                Manifest.permission.CAMERA
            )
        }

        return binding.root
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(binding.viewFinder.display.rotation)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e("CameraCaptureFragment", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraCaptureFragment", "Photo capture failed! ${exc.message}", exc)
                    view?.showSuccessSnackbar(getString(R.string.photo_capture_failed))
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    view?.showSuccessSnackbar(getString(R.string.photo_capture_success))
                    navController.previousBackStackEntry?.savedStateHandle?.set(Constants.CAMERA_URI, output.savedUri)
                    navController.popBackStack()
                }
            })
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}