package com.simple.phonetics.ui.home.view.detect

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.permissionx.guolindev.PermissionX
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.showAds
import com.simple.state.doSuccess
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException

interface DetectHomeView {

    fun setupDetect(fragment: HomeFragment)
}

class DetectHomeViewImpl : DetectHomeView {

    override fun setupDetect(fragment: HomeFragment) {

        val binding = fragment.binding ?: return

        val viewModel: HomeViewModel by fragment.viewModel()

        val configViewModel: ConfigViewModel by fragment.activityViewModel()

        val detectHomeViewModel: DetectHomeViewModel by fragment.viewModel()


        var currentPhotoPath: String? = null

        val takeImageFromCameraResult = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val path = currentPhotoPath ?: return@registerForActivityResult

            if (result.resultCode == Activity.RESULT_OK) {

                viewModel.getTextFromImage(path)
            }
        }

        val takeImageFromGalleryResult = fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            uri?.runCatching {

                getFilePath(fragment.requireContext(), this)
            }?.getOrNull()?.takeIf {

                it.isNotBlank()
            }?.let {

                viewModel.getTextFromImage(it)
            }
        }


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


        viewModel.isReverse.observe(fragment.viewLifecycleOwner) {

            detectHomeViewModel.updateReverse(it)
        }

        viewModel.detectStateEvent.observe(fragment.viewLifecycleOwner) { event ->

            fragment.binding ?: return@observe
            val state = event.getContentIfNotHandled() ?: return@observe

            state.doSuccess {

                viewModel.getPhonetics("")
                binding.etText.setText(it)
                showAds()
            }
        }

        detectHomeViewModel.detectInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "DETECT") {

            fragment.binding ?: return@collectWithLockTransitionUntilData

            binding.ivCamera.setVisible(it.isShow)
            binding.ivGallery.setVisible(it.isShow)
        }
    }

    companion object {

        private val REQUIRED_PERMISSIONS_CAMERA = arrayOf(Manifest.permission.CAMERA)

        private val REQUIRED_PERMISSIONS_READ_FILE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {

            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        private fun ActivityResultLauncher<String>.launchTakeImageFromGallery() {
            launch("image/*")
        }

        private fun ActivityResultLauncher<Intent>.launchTakeImageFromCamera(context: Context, imageName: String): File? {

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val photoFile: File?

            try {

                val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null

                photoFile = File.createTempFile(imageName, ".jpg", storageDir)
            } catch (_: IOException) {

                return null
            }

            val photoURI = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            launch(takePictureIntent)

            return photoFile
        }

        private fun getFilePath(context: Context, uri: Uri): String? {

            val selection: String?
            val selectionArgs: Array<String>?

            if (DocumentsContract.isDocumentUri(context, uri)) when {

                isExternalStorageDocument(uri) -> {

                    val docId = DocumentsContract.getDocumentId(uri)

                    val split = docId.split(":")

                    return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                }

                isDownloadsDocument(uri) -> {

                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLongOrNull() ?: return null)
                    return getDataColumn(context, contentUri, null, null)
                }

                isMediaDocument(uri) -> {

                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    val contentUri = when (type) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    }
                    selection = "_id=?"
                    selectionArgs = arrayOf(split[1])
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {

                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {

                return uri.path
            }

            return null
        }

        private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {

            val projection = arrayOf(MediaStore.Images.Media.DATA)

            context.contentResolver.query(uri ?: return null, projection, selection, selectionArgs, null)?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    return cursor.getString(columnIndex)
                }
            }
            return null
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        private fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }
    }
}