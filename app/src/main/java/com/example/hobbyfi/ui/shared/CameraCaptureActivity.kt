package com.example.hobbyfi.ui.shared

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.ImageCapture
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.CameraCaptureLayoutBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.showSuccessToast
import com.example.hobbyfi.shared.startCamera
import com.example.hobbyfi.shared.takePhoto
import pub.devrel.easypermissions.EasyPermissions

class CameraCaptureActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var binding: CameraCaptureLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CameraCaptureLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraCaptureButton.setOnClickListener {
            imageCapture?.takePhoto(it.context, { output ->
                this@CameraCaptureActivity.showSuccessToast(getString(R.string.photo_capture_success))
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(Constants.CAMERA_URI, output.savedUri)
                })
                finish()
            }, {
                this@CameraCaptureActivity.showSuccessToast(getString(R.string.photo_capture_failed))
            })
        }

        if(EasyPermissions.hasPermissions(
                this,
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
            binding.viewFinder.startCamera(this).observe(this, {
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
    }
}