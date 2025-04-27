package com.cloudsheeptech.shoppinglist.fragments.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentCameraxBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class CameraFragment : Fragment() {
    private val viewModel: CameraViewModel by viewModels()
    private lateinit var binding: FragmentCameraxBinding

    private var imageCapture: ImageCapture? = null
    private var activityResultLauncher: ActivityResultLauncher<Array<String>>? = null

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ).toTypedArray()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camerax, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        viewModel.takePhoto.observe(
            viewLifecycleOwner,
            Observer {
                if (it) {
                    takePhoto()
                    viewModel.onPhotoTaken()
                }
            },
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupPermissionActivityLauncher()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraPreview =
                Preview
                    .Builder()
                    .build()
                    .also {
                        it.surfaceProvider = binding.viewFinder.surfaceProvider
                    }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, cameraPreview, imageCapture)
            } catch (ex: Exception) {
                Log.e("CameraFragment", "Unbinding or binding the camera failed: $ex")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = this.imageCapture
        if (imageCapture == null) {
            Log.e("CameraFragment", "Cannot start camera since image capture is null")
            return
        }
        val filename = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/recipes")
            }

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(
                    requireContext().contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues,
                ).build()

        val imagePaths = mutableListOf<String>()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraFragment", "Failed to save image: $exception")
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val message = "Image ${outputFileResults.savedUri} successfully captured"
                    imagePaths.add(outputFileResults.savedUri.toString())
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    Log.d("CameraFragment", message)
                }
            },
        )
        Log.d("CameraFragment", "All images taken: $imagePaths")
    }

    private fun requestPermissions() {
        activityResultLauncher?.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it,
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun setupPermissionActivityLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                var allPermissionsGranted = true
                for (permission in permissions.entries) {
                    if (permission.key in REQUIRED_PERMISSIONS && permission.value == false) {
                        allPermissionsGranted = false
                    }
                }
                if (!allPermissionsGranted) {
                    Toast.makeText(requireContext(), "Not all permissions granted", Toast.LENGTH_LONG).show()
                } else {
                    startCamera()
                }
            }
    }
}
