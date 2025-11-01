package com.touristguide.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.touristguide.app.R
import com.touristguide.app.TouristGuideApp
import com.touristguide.app.data.api.RetrofitClient
import com.touristguide.app.data.model.LoginRequest
import com.touristguide.app.data.model.RegisterRequest
import com.touristguide.app.databinding.ActivityAuthBinding
import com.touristguide.app.ui.main.MainActivity
import com.touristguide.app.utils.hide
import com.touristguide.app.utils.isValidEmail
import com.touristguide.app.utils.isValidPassword
import com.touristguide.app.utils.show
import com.touristguide.app.utils.showToast
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    private var isLoginMode = true
    private val preferenceManager by lazy { (application as TouristGuideApp).preferenceManager }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        updateUIForMode()
    }
    
    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            if (isLoginMode) {
                performLogin()
            } else {
                performRegister()
            }
        }
        
        binding.tvToggleMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUIForMode()
        }
        
        binding.tvToggleLink.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUIForMode()
        }
    }
    
    private fun updateUIForMode() {
        if (isLoginMode) {
            binding.tilName.hide()
            binding.tilConfirmPassword.hide()
            binding.btnSubmit.text = getString(R.string.login)
            binding.tvToggleMode.text = getString(R.string.dont_have_account)
            binding.tvToggleLink.text = getString(R.string.sign_up)
        } else {
            binding.tilName.show()
            binding.tilConfirmPassword.show()
            binding.btnSubmit.text = getString(R.string.register)
            binding.tvToggleMode.text = getString(R.string.already_have_account)
            binding.tvToggleLink.text = getString(R.string.sign_in)
        }
    }
    
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        // Validation
        if (!validateLoginInputs(email, password)) {
            return
        }
        
        showLoading()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(email, password))
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()?.data
                    authData?.let {
                        preferenceManager.saveAuthData(
                            it.token,
                            it.id,
                            it.name,
                            it.email,
                            it.role
                        )
                        
                        showToast("Welcome back, ${it.name}!")
                        navigateToMain()
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Login failed"
                    showToast(errorMsg)
                    hideLoading()
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message ?: "Network error"}")
                hideLoading()
            }
        }
    }
    
    private fun performRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        // Validation
        if (!validateRegisterInputs(name, email, password, confirmPassword)) {
            return
        }
        
        showLoading()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(name, email, password)
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()?.data
                    authData?.let {
                        preferenceManager.saveAuthData(
                            it.token,
                            it.id,
                            it.name,
                            it.email,
                            it.role
                        )
                        
                        showToast("Registration successful!")
                        navigateToMain()
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Registration failed"
                    showToast(errorMsg)
                    hideLoading()
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message ?: "Network error"}")
                hideLoading()
            }
        }
    }
    
    private fun validateLoginInputs(email: String, password: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.tilEmail.error = getString(R.string.error_empty_email)
                return false
            }
            !email.isValidEmail() -> {
                binding.tilEmail.error = getString(R.string.error_invalid_email)
                return false
            }
            password.isEmpty() -> {
                binding.tilPassword.error = getString(R.string.error_empty_password)
                return false
            }
            else -> {
                binding.tilEmail.error = null
                binding.tilPassword.error = null
                return true
            }
        }
    }
    
    private fun validateRegisterInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        when {
            name.isEmpty() -> {
                binding.tilName.error = getString(R.string.error_empty_name)
                return false
            }
            email.isEmpty() -> {
                binding.tilEmail.error = getString(R.string.error_empty_email)
                return false
            }
            !email.isValidEmail() -> {
                binding.tilEmail.error = getString(R.string.error_invalid_email)
                return false
            }
            password.isEmpty() -> {
                binding.tilPassword.error = getString(R.string.error_empty_password)
                return false
            }
            !password.isValidPassword() -> {
                binding.tilPassword.error = getString(R.string.error_short_password)
                return false
            }
            password != confirmPassword -> {
                binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
                return false
            }
            else -> {
                binding.tilName.error = null
                binding.tilEmail.error = null
                binding.tilPassword.error = null
                binding.tilConfirmPassword.error = null
                return true
            }
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
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
