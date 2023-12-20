package com.dicoding.bottomnavigationbar.ui.gizi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dicoding.bottomnavigationbar.R
import com.dicoding.bottomnavigationbar.data.retrofit.FileResponse
import com.dicoding.bottomnavigationbar.data.retrofit.Retro
import com.dicoding.bottomnavigationbar.data.retrofit.Utils.getImageUri
import com.dicoding.bottomnavigationbar.data.retrofit.Utils.reduceFileImage
import com.dicoding.bottomnavigationbar.data.retrofit.Utils.uriToFile
import com.dicoding.bottomnavigationbar.databinding.ActivityGiziBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException

class GiziActivity : AppCompatActivity() {
    private val ApiConfig = Retro()
    private lateinit var binding: ActivityGiziBinding
    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
    private var currentImageUri: Uri? = null
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    private fun ActivityGiziBinding() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    @RequiresApi(Build.VERSION_CODES.Q)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGiziBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }
        binding.galleryButton.setOnClickListener { startGallery() }
        binding.cameraButton.setOnClickListener { startCamera() }
        binding.uploadButton.setOnClickListener { uploadImage() }

    }
    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }
    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
    }
    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    //
    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun uploadImage() {
        currentImageUri?.let { uri ->
            val imageFile = uriToFile(uri, this).reduceFileImage()
            Log.d("Image File", "showImage: ${imageFile.path}")

            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "image",
                imageFile.name,
                requestImageFile
            )
            val intent = Intent(this, GiziActivity::class.java)
            lifecycleScope.launch {
                try {
                    val apiService = ApiConfig.getApiService()
                    val successResponse = apiService.uploadImage(multipartBody)

                    if (successResponse.isSuccessful) {
                        val responseData = successResponse.body()
                        val hasil = responseData?.predictedClass
                        if (responseData != null) {
                            showToast("Upload successful. Server response: ${responseData}")
                        } else {
                            showToast("Upload successful. No additional data from the server.")
                        }
                        showResultDialog(hasil)
                    } else {
                        // Handle unsuccessful response
                        val errorBody = successResponse.errorBody()?.string()
                        val errorResponse = Gson().fromJson(errorBody, FileResponse::class.java)

                        showToast("Upload failed. Server error: ${errorResponse}")
                    }

                    showToast(successResponse.toString())
                    showLoading(false)
                } catch (e: HttpException) {
                    // Handle HTTP exception
                    val errorBody = e.response()?.errorBody()?.string()
                    val errorResponse = Gson().fromJson(errorBody, FileResponse::class.java)

                    showToast("HTTP error: ${errorResponse}")
                    showLoading(false)
                } catch (e: Exception) {
                    // Handle other exceptions
                    showToast("Unexpected error: ${e.message}")
                    showLoading(false)
                }
            }
        } ?: showToast(getString(R.string.empty_image_warning))
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    private fun getGiziValues(foodType: String): GiziValues {
        return when (foodType) {
            "Banana" -> GiziValues("Pisang", "10 gram", "5 gram", "20 IU", "2 mg", "15 mg", "10 gram")
            "Apple" -> GiziValues("Apel", "8 gram", "3 gram", "15 IU", "1 mg", "10 mg", "8 gram")
            "Grape" -> GiziValues("Anggur", "12 gram", "8 gram", "25 IU", "3 mg", "20 mg", "15 gram")
            "Strawberry" -> GiziValues("Strawberry", "5 gram", "2 gram", "10 IU", "1 mg", "8 mg", "5 gram")
            "Mango" -> GiziValues("Mangga", "15 gram", "10 gram", "30 IU", "4 mg", "25 mg", "20 gram")
            else -> GiziValues("Unknown", "0 gram", "0 gram", "0 IU", "0 mg", "0 mg", "0 gram")
        }
    }

    private fun showResultDialog(resultText: String?) {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_gizi, null)
        val giziJudul = dialogView.findViewById<TextView>(R.id.giziJudul)
        val tvLemakValue = dialogView.findViewById<TextView>(R.id.tvLemakValue)
        val tvProteinValue = dialogView.findViewById<TextView>(R.id.tvProteinValue)
        val tvVitaminAValue = dialogView.findViewById<TextView>(R.id.tvVitaminAValue)
        val tvVitaminBValue = dialogView.findViewById<TextView>(R.id.tvVitaminBValue)
        val tvKaliumValue = dialogView.findViewById<TextView>(R.id.tvkaliumValue)
        val tvGulaValue = dialogView.findViewById<TextView>(R.id.tvGulaValue)
        val btnDialogOk = dialogView.findViewById<Button>(R.id.btnDialogOk)

        // Mendapatkan nilai gizi berdasarkan tipe makanan
        val giziValues = getGiziValues(resultText.orEmpty())

        // Mengisi nilai gizi ke dalam TextView
        giziJudul.text = giziValues.foodName
        tvLemakValue.text = giziValues.lemak
        tvProteinValue.text = giziValues.protein
        tvVitaminAValue.text = giziValues.vitaminA
        tvVitaminBValue.text = giziValues.vitaminB
        tvKaliumValue.text = giziValues.kalium
        tvGulaValue.text = giziValues.gula

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val dialog = builder.create()

        btnDialogOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}