package com.simple.phonetics.ui.home.services.detect

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.asFlow
import com.permissionx.guolindev.PermissionX
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.hasPermissions
import com.simple.service.FragmentCreatedService
import com.simple.state.doSuccess
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.flow.filterNotNull
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException

@AutoBind(HomeFragment::class)
class DetectHomeService : FragmentCreatedService {

    override fun setup(fragment: Fragment) {

        val homeFragment = fragment.asObjectOrNull<HomeFragment>() ?: return

        val binding = homeFragment.binding ?: return

        val homeViewModel: HomeViewModel by homeFragment.viewModel()

        val viewModel: DetectHomeViewModel by homeFragment.viewModel()


        var currentPhotoPath: String? = null

        val takeImageFromCameraResult = homeFragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val path = currentPhotoPath ?: return@registerForActivityResult

            if (result.resultCode == Activity.RESULT_OK) {

                homeViewModel.getTextFromImage(path)
            }
        }

        val takeImageFromGalleryResult = homeFragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            uri?.runCatching {

                getFilePath(homeFragment.requireContext(), this)
            }?.getOrNull()?.takeIf {

                it.isNotBlank()
            }?.let {

                homeViewModel.getTextFromImage(it)
            }
        }

        homeFragment.viewLifecycleOwnerLiveData.asFlow().filterNotNull().launchCollect(fragment){ viewLifecycleOwner->

            binding.ivGallery.setDebouncedClickListener {

                takeImageFromGalleryResult.launchTakeImageFromGallery()
            }

            binding.ivCamera.setDebouncedClickListener {

                PermissionX.init(homeFragment.requireActivity())
                    .permissions(REQUIRED_PERMISSIONS_CAMERA.toList())
                    .request { allGranted, _, _ ->
                        if (allGranted && homeFragment.hasPermissions(*REQUIRED_PERMISSIONS_CAMERA)) {
                            currentPhotoPath = takeImageFromCameraResult.launchTakeImageFromCamera(homeFragment.requireContext(), "image")?.absolutePath ?: return@request
                        }
                    }
            }

            homeViewModel.isReverse.observe(viewLifecycleOwner) {

                viewModel.updateReverse(it)
            }

            homeViewModel.detectStateEvent.observe(viewLifecycleOwner) { event ->

                homeFragment.binding ?: return@observe
                val state = event.getContentIfNotHandled() ?: return@observe

                state.doSuccess {

                    homeViewModel.getPhonetics("")
                    binding.etText.setText(it)
                }
            }

            viewModel.detectInfo.collectWithLockTransitionUntilData(fragment = homeFragment, tag = "DETECT") {

                homeFragment.binding ?: return@collectWithLockTransitionUntilData

                binding.ivCamera.setVisible(it.isShow)
                binding.ivGallery.setVisible(it.isShow)
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