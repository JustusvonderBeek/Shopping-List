package com.cloudsheeptech.shoppinglist.ui.recipe.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentCameraxBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class CameraFragment : Fragment() {
    private val viewModel: CameraViewModel by viewModels()
    private lateinit var binding: FragmentCameraxBinding

    private var imageCapture: ImageCapture? = null
    private var activityResultLauncher: ActivityResultLauncher<Array<String>>? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            if (uris.isEmpty()) {
                Log.d("ReceiptEditFragment", "No images selected")
                return@registerForActivityResult
            }
            viewModel.setSelectedImages(uris)
            viewModel.onPhotosSelected()
        }

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

        viewModel.selectPhotos.observe(
            viewLifecycleOwner,
            Observer {
                if (it) {
                    Log.d("CameraFragment", "Selecting photos")
                    selectPhotos()
                    viewModel.onPhotosSelected()
                }
            },
        )

        viewModel.navigateUp.observe(
            viewLifecycleOwner,
            Observer { finish ->
                if (finish) {
                    viewModel.onFinished()
                    makePhotoPathsAvailable()
                    findNavController().navigateUp()
                }
            },
        )

        viewModel.imagePaths.observe(
            viewLifecycleOwner,
            Observer {
                if (it.isNotEmpty()) {
                    Glide
                        .with(requireContext())
                        .load(it.first().toUri())
                        .centerCrop()
                        .into(binding.filterPhotoButton)
                }
            },
        )

        // Make sure captured images which are not to be used get delete
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    lifecycleScope.launch {
                        viewModel.clearTemporaryImages()
                        findNavController().navigateUp()
                    }
                }
            },
        )

        // This is necessary to force calling the onOptionsItemSelected method
        setHasOptionsMenu(true)

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

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                lifecycleScope.launch {
                    viewModel.clearTemporaryImages()
                    findNavController().navigateUp()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
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

                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    cameraPreview,
                    imageCapture,
                )
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
        val filename =
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
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

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraFragment", "Failed to save image: $exception")
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val message = "Image ${outputFileResults.savedUri} successfully captured"
                    viewModel.addPhotoPath(outputFileResults.savedUri.toString())
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    Log.d("CameraFragment", message)
                }
            },
        )
    }

    private fun makePhotoPathsAvailable() {
        val imagePaths = viewModel.getImagePaths()
        Log.d("CameraFragment", "Took ${imagePaths.size} images: $imagePaths")
        val bundle =
            Bundle().apply {
                putStringArrayList("uris", ArrayList(imagePaths))
            }
        parentFragmentManager.setFragmentResult("captured_image_uris", bundle)
    }

    private fun selectPhotos() {
        Log.d("ReceiptEditFragment", "Select photo button clicked")
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                    Toast
                        .makeText(
                            requireContext(),
                            "Not all permissions granted",
                            Toast.LENGTH_LONG,
                        ).show()
                } else {
                    startCamera()
                }
            }
    }
}
