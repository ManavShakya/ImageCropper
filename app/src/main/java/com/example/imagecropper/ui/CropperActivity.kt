package com.example.imagecropper.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.imagecropper.R
import com.github.shchurov.horizontalwheelview.HorizontalWheelView
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.IOException
import java.util.*

class CropperActivity : AppCompatActivity() {
    private lateinit var mCropImageView: CropImageView
    private var mCropImageUri: Uri? = null

    private lateinit var rotateView: ImageView

    private lateinit var uploadButton: Button
    private lateinit var cropButton: Button

    private lateinit var horizontalWheelView: HorizontalWheelView

    private lateinit var angleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cropper)

        val decor = window.decorView
        val ui = View.SYSTEM_UI_FLAG_FULLSCREEN
        decor.systemUiVisibility = ui

        mCropImageView = findViewById(R.id.CropImageView)
        rotateView = findViewById(R.id.rotateView)
        uploadButton = findViewById(R.id.button)
        cropButton = findViewById(R.id.button2)
        horizontalWheelView = findViewById(R.id.wheelView)
        angleText = findViewById(R.id.angleText)

        uploadButton.setOnClickListener {
            startActivityForResult(getImageChooserIntent(), 2)
        }

        cropButton.setOnClickListener {
            val cropped = mCropImageView.croppedImage
            mCropImageView.setImageBitmap(cropped)
           // Resetting horizontalWheelView when crop button is clicked
            angleText.text = "0°"
            horizontalWheelView.degreesAngle = 0.0
        }

        rotateView.setOnClickListener {
            mCropImageView.rotateImage(-90)
        }

        setupListeners();
    }

    private fun setupListeners() {
        horizontalWheelView.setListener(object : HorizontalWheelView.Listener() {
            override fun onRotationChanged(radians: Double) {
                updateUi()
            }
        })
    }

    private fun updateUi() {
        val angle1 = horizontalWheelView.degreesAngle.toInt()
        val angle = angle1 - mCropImageView.rotatedDegrees
        if(angle1 in  -45..45) {
            mCropImageView.isAutoZoomEnabled = mCropImageView.rotatedDegrees == 0
            mCropImageView.rotateImage(angle)
            mCropImageView.setMaxCropResultSize(mCropImageView.width*4, mCropImageView.height*4)
            angleText.text = "$angle1°"
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = getPickedImageUri(data)
            try {
                mCropImageUri = imageUri
                mCropImageView.setImageUriAsync(imageUri)
            } catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    private fun getImageChooserIntent(): Intent? {
        val allIntents: MutableList<Intent> = ArrayList()
        val packageManager = packageManager
        val outputFileUri: Uri? = getCapturedImageUri()
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

        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        val listGallery = packageManager.queryIntentActivities(galleryIntent, 0)
        for (res in listGallery) {
            val intent = Intent(galleryIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            allIntents.add(intent)
        }

        val mainIntent: Intent? = allIntents[allIntents.size - 1]
        allIntents.remove(mainIntent)
        val chooserIntent = Intent.createChooser(mainIntent, "Select source")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray<Parcelable>())
        return chooserIntent
    }

    private fun getPickedImageUri(data: Intent?): Uri? {
        var isCamera = true
        if (data?.data != null) {
            val action = data.action
            isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
        }
        return if (isCamera) getCapturedImageUri() else data!!.data
    }

    private fun getCapturedImageUri(): Uri? {
        var capturedImageUri: Uri? = null
        val getImage = externalCacheDir
            capturedImageUri = Uri.fromFile(File(getImage?.path, "pickImageResult.jpeg"))
        return capturedImageUri
    }
}
