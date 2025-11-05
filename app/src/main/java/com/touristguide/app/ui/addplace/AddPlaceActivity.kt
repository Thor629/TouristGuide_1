package com.touristguide.app.ui.addplace

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dhaval2404.imagepicker.ImagePicker
import com.touristguide.app.R
import com.touristguide.app.TouristGuideApp
import com.touristguide.app.data.api.RetrofitClient
import com.touristguide.app.data.model.Category
import com.touristguide.app.databinding.ActivityAddPlaceBinding
import com.touristguide.app.utils.hide
import com.touristguide.app.utils.show
import com.touristguide.app.utils.showToast
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AddPlaceActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddPlaceBinding
    private val preferenceManager by lazy { (application as TouristGuideApp).preferenceManager }
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imagesAdapter: SelectedImagesAdapter
    private val categories = mutableListOf<Category>()
    private var selectedCategoryId: String? = null
    private var isEditMode = false
    private var placeId: String? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris)
            imagesAdapter.submitList(selectedImages.toList())
            updateImageCounter()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if in edit mode
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        placeId = intent.getStringExtra("PLACE_ID")
        
        setupToolbar()
        setupRecyclerView()
        setupCategorySpinner()
        setupClickListeners()
        loadCategories()
        
        // Load place data if editing
        if (isEditMode && placeId != null) {
            loadPlaceData()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Place" else getString(R.string.add_place)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        imagesAdapter = SelectedImagesAdapter { uri ->
            selectedImages.remove(uri)
            imagesAdapter.submitList(selectedImages.toList())
            updateImageCounter()
        }
        
        binding.rvSelectedImages.apply {
            layoutManager = LinearLayoutManager(
                this@AddPlaceActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = imagesAdapter
        }
    }
    
    private fun setupCategorySpinner() {
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = if (position > 0) categories[position - 1].id else null
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSelectImages.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        
        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
        
        // Update button text for edit mode
        if (isEditMode) {
            binding.btnSubmit.text = "Update Place"
        }
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getCategories()
                if (response.isSuccessful && response.body()?.success == true) {
                    categories.clear()
                    response.body()?.data?.let { categories.addAll(it) }
                    
                    val categoryNames = mutableListOf("Select Category")
                    categoryNames.addAll(categories.map { it.name })
                    
                    val adapter = ArrayAdapter(
                        this@AddPlaceActivity,
                        android.R.layout.simple_spinner_item,
                        categoryNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerCategory.adapter = adapter
                }
            } catch (e: Exception) {
                showToast("Failed to load categories")
            }
        }
    }
    
    private fun loadPlaceData() {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPlace(placeId!!)
                if (response.isSuccessful && response.body()?.success == true) {
                    val place = response.body()?.data
                    place?.let {
                        // Fill in the form
                        binding.etPlaceName.setText(it.name)
                        binding.etLocation.setText(it.location)
                        binding.etCity.setText(it.city)
                        binding.etDescription.setText(it.description)
                        binding.etLink.setText(it.link ?: "")
                        
                        // Select the category
                        val categoryIndex = categories.indexOfFirst { cat -> cat.id == it.category.id }
                        if (categoryIndex >= 0) {
                            binding.spinnerCategory.setSelection(categoryIndex + 1) // +1 for "Select Category"
                            selectedCategoryId = it.category.id
                        }
                    }
                } else {
                    showToast("Failed to load place data")
                    finish()
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                finish()
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun validateAndSubmit() {
        val name = binding.etPlaceName.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val link = binding.etLink.text.toString().trim()
        
        // Validation
        when {
            name.isEmpty() -> {
                binding.tilPlaceName.error = getString(R.string.error_empty_place_name)
                return
            }
            location.isEmpty() -> {
                binding.tilLocation.error = getString(R.string.error_empty_location)
                return
            }
            description.isEmpty() -> {
                binding.tilDescription.error = getString(R.string.error_empty_description)
                return
            }
            selectedCategoryId == null -> {
                showToast(getString(R.string.error_select_category))
                return
            }
            else -> {
                binding.tilPlaceName.error = null
                binding.tilLocation.error = null
                binding.tilDescription.error = null
            }
        }
        
        submitPlace(name, location, city.ifEmpty { "Surat" }, description, link)
    }
    
    private fun submitPlace(
        name: String,
        location: String,
        city: String,
        description: String,
        link: String
    ) {
        showLoading()
        
        lifecycleScope.launch {
            try {
                // Prepare multipart request
                val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val locationPart = location.toRequestBody("text/plain".toMediaTypeOrNull())
                val cityPart = city.toRequestBody("text/plain".toMediaTypeOrNull())
                val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryPart = selectedCategoryId!!.toRequestBody("text/plain".toMediaTypeOrNull())
                val linkPart = if (link.isNotEmpty()) {
                    link.toRequestBody("text/plain".toMediaTypeOrNull())
                } else null
                
                // Prepare image parts (only if new images selected)
                val imageParts = mutableListOf<MultipartBody.Part>()
                selectedImages.forEach { uri ->
                    try {
                        val file = File(getRealPathFromURI(uri))
                        if (file.exists()) {
                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData("images", file.name, requestFile)
                            imageParts.add(part)
                        }
                    } catch (e: Exception) {
                        // Skip problematic images
                    }
                }
                
                val response = if (isEditMode && placeId != null) {
                    // Update existing place
                    RetrofitClient.apiService.updatePlace(
                        placeId!!,
                        namePart,
                        locationPart,
                        cityPart,
                        descriptionPart,
                        categoryPart,
                        linkPart,
                        if (imageParts.isEmpty()) null else imageParts
                    )
                } else {
                    // Create new place
                    RetrofitClient.apiService.createPlace(
                        namePart,
                        locationPart,
                        cityPart,
                        descriptionPart,
                        categoryPart,
                        linkPart,
                        if (imageParts.isEmpty()) null else imageParts
                    )
                }
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val successMsg = if (isEditMode) "Place updated successfully" else response.body()?.message ?: getString(R.string.place_added_success)
                    showToast(successMsg)
                    finish()
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Failed to ${if (isEditMode) "update" else "add"} place"
                    showToast(errorMsg)
                    android.util.Log.e("AddPlace", "Error: $errorMsg, Code: ${response.code()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                android.util.Log.e("AddPlace", "Exception: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun getRealPathFromURI(uri: Uri): String {
        // Simple implementation - creates a temp file
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }
    
    private fun updateImageCounter() {
        binding.tvImageCount.text = "${selectedImages.size} image(s) selected"
        if (selectedImages.isEmpty()) {
            binding.rvSelectedImages.hide()
        } else {
            binding.rvSelectedImages.show()
        }
    }
    
    private fun showLoading() {
        binding.progressBar.show()
        binding.btnSubmit.isEnabled = false
    }
    
    private fun hideLoading() {
        binding.progressBar.hide()
        binding.btnSubmit.isEnabled = true
    }
}
