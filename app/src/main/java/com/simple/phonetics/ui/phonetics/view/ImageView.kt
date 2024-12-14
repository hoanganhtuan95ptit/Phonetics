package com.simple.phonetics.ui.phonetics.view

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.permissionx.guolindev.PermissionX
import com.simple.coreapp.utils.FileUtils
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.extentions.launchTakeImageFromCamera
import com.simple.coreapp.utils.extentions.launchTakeImageFromGallery
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import com.simple.phonetics.ui.phonetics.PhoneticsViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

interface ImageView {

    fun setupImage(fragment: PhoneticsFragment)
}

class ImageViewImpl() : ImageView {

    override fun setupImage(fragment: PhoneticsFragment) {

        val viewModel by fragment.viewModels<PhoneticsViewModel>()

        var currentPhotoPath: String? = null

        val takeImageFromCameraResult = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val path = currentPhotoPath ?: return@registerForActivityResult

            if (result.resultCode == Activity.RESULT_OK) {

                viewModel.getTextFromImage(path)
            }
        }

        val takeImageFromGalleryResult = fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            uri?.let {

                FileUtils.uriToImageFile(fragment.requireContext(), it)
            }?.let {

                viewModel.getTextFromImage(it.absolutePath)
            }
        }


        val binding = fragment.binding ?: return

        binding.ivGallery.setDebouncedClickListener {

            PermissionX.init(fragment.requireActivity())
                .permissions(REQUIRED_PERMISSIONS_READ_FILE.toList())
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        takeImageFromGalleryResult.launchTakeImageFromGallery()
                    }
                }
        }

        binding.ivCamera.setDebouncedClickListener {

            PermissionX.init(fragment.requireActivity())
                .permissions(REQUIRED_PERMISSIONS_CAMERA.toList())
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        currentPhotoPath = takeImageFromCameraResult.launchTakeImageFromCamera(fragment.requireContext(), "image")?.absolutePath ?: return@request
                    }
                }
        }
    }

    companion object {

        private val REQUIRED_PERMISSIONS_CAMERA = arrayOf(Manifest.permission.CAMERA)

        private val REQUIRED_PERMISSIONS_READ_FILE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {

            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}