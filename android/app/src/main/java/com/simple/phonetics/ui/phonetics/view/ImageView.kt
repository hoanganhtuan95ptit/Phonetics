package com.simple.phonetics.ui.phonetics.view

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import com.permissionx.guolindev.PermissionX
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.extentions.launchTakeImageFromCamera
import com.simple.coreapp.utils.extentions.launchTakeImageFromGallery
import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import com.simple.state.doSuccess

interface ImageView {

    fun setupImage(fragment: PhoneticsFragment)
}

class ImageViewImpl() : ImageView {

    override fun setupImage(fragment: PhoneticsFragment) {

        val viewModel = fragment.viewModel

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

        viewModel.detectState.observe(fragment.viewLifecycleOwner) {

            val binding = fragment.binding ?: return@observe

            it.doSuccess {

                binding.etText.setText(it)
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