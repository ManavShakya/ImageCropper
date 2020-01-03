package com.example.imagecropper.ui

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.system.ErrnoException
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imagecropper.R
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class CropActivity : AppCompatActivity() {
    private lateinit var mCropImageView: CropImageView
    private var mCropImageUri: Uri? = null
    private lateinit var rotateView: ImageView
    private lateinit var uploadButton: Button
    private lateinit var cropButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val decor = window.decorView
        val ui = View.SYSTEM_UI_FLAG_FULLSCREEN
        decor.systemUiVisibility = ui

        setContentView(R.layout.activity_crop)

        mCropImageView = findViewById(R.id.CropImageView)
        rotateView = findViewById(R.id.imageView)
        uploadButton = findViewById(R.id.button)
        cropButton = findViewById(R.id.button2)

        uploadButton.setOnClickListener {
            startActivityForResult(getImageChooserIntent(), 200)
        }

        cropButton.setOnClickListener {
            val cropped: Bitmap?  = mCropImageView.getCroppedImage(500, 500)
            mCropImageView.setImageBitmap(cropped)
        }

        rotateView.setOnClickListener {
            mCropImageView.rotateImage(-90)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = getPickImageResultUri(data)
            var requirePermissions = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                isUriRequiresPermissions(imageUri)) {
                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true
                mCropImageUri = imageUri
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    0
                )
            }
            if (!requirePermissions) {
                mCropImageView.setImageUriAsync(imageUri)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        if (mCropImageUri != null && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mCropImageView.setImageUriAsync(mCropImageUri)
        } else {
            Toast.makeText(this, "Required permissions are not granted", Toast.LENGTH_LONG).show()
        }

    }


    private fun getImageChooserIntent(): Intent? {
        val allIntents: MutableList<Intent> = ArrayList()
        val packageManager = packageManager
        // collect all camera intents
        val outputFileUri: Uri? = getCapturedImageUri() // Determine Uri of camera image to  save.
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val listCam = packageManager.queryIntentActivities(captureIntent, 0)
        for (res in listCam) {
            val intent = Intent(captureIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            }
            allIntents.add(intent)
        }
        // collect all gallery intents
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        val listGallery = packageManager.queryIntentActivities(galleryIntent, 0)
        for (res in listGallery) {
            val intent = Intent(galleryIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            allIntents.add(intent)
        }
        // the main intent is the last in the  list so pickup the useless one
        var mainIntent: Intent? = allIntents[allIntents.size - 1]
        for (intent in allIntents) {
            if (intent.component!!.className == "com.android.documentsui.DocumentsActivity") {
                mainIntent = intent
                break
            }
        }
        allIntents.remove(mainIntent)
        val chooserIntent = Intent.createChooser(mainIntent, "Select source")  // Create a chooser from the main  intent
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray<Parcelable>())  // Add all other intents
        return chooserIntent
    }

    private fun getCapturedImageUri(): Uri? {
        var outputFileUri: Uri? = null
        val getImage = externalCacheDir
        if (getImage != null) {
            outputFileUri =
                Uri.fromFile(File(getImage.path, "pickImageResult.jpeg"))
        }
        return outputFileUri
    }

    private fun getPickImageResultUri(data: Intent?): Uri? {
        var isCamera = true
        if (data != null && data.data != null) {
            val action = data.action
            isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
        }
        return if (isCamera) getCapturedImageUri() else data!!.data
    }

    private fun isUriRequiresPermissions(uri: Uri?): Boolean {
        try {
            val resolver = contentResolver
            val stream = resolver.openInputStream(uri!!)!!
            stream.close()
            return false
        } catch (e: FileNotFoundException) {
            if (e.cause is ErrnoException) {
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

}
