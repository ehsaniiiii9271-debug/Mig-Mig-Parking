package com.aistudio.fitmirror.auth2.ui.finder

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.aistudio.fitmirror.auth2.R
import com.aistudio.fitmirror.auth2.databinding.ActivityFinderBinding
import com.aistudio.fitmirror.auth2.databinding.DialogFinderSignupBinding
import com.aistudio.fitmirror.auth2.repository.FirebaseManager
import com.aistudio.fitmirror.auth2.ui.MapBridge
import com.aistudio.fitmirror.auth2.viewmodel.FinderViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.firestore.ListenerRegistration
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FinderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinderBinding
    private val viewModel: FinderViewModel by viewModels()
    private var registration: ListenerRegistration? = null
    private var selectedRequestId: String? = null
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)

        setupWebView()
        setupObservers()
        setupListeners()

        registration = FirebaseManager.observeRequests { requests ->
            val jsonArray = JSONArray()
            requests.forEach { req ->
                val obj = JSONObject()
                obj.put("id", req.id)
                obj.put("lat", req.lat)
                obj.put("lng", req.lng)
                obj.put("type", "driver")
                jsonArray.put(obj)
            }
            binding.webView.post {
                binding.webView.loadUrl("javascript:updateMarkers('${jsonArray}')")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(MapBridge(
                onMapClick = { _, _ -> },
                onMarkerClick = { id ->
                    runOnUiThread {
                        selectedRequestId = id
                        Toast.makeText(this@FinderActivity, "Driver Request Selected", Toast.LENGTH_SHORT).show()
                    }
                }
            ), "AndroidBridge")
            loadUrl("file:///android_asset/map.html")
        }
    }

    private fun setupObservers() {
        viewModel.isSignedUp.observe(this) { isSignedUp ->
            if (isSignedUp) {
                binding.btnSignUp.visibility = View.GONE
                binding.layoutControls.visibility = View.VISIBLE
                binding.btnReportSpot.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSignUp.setOnClickListener { showSignUpDialog() }
        binding.btnReportSpot.setOnClickListener {
            val bitmap = generateQrCode("10.00")
            if (bitmap != null) {
                binding.ivQrCode.setImageBitmap(bitmap)
                binding.qrContainer.visibility = View.VISIBLE
            }
        }
        binding.btnCloseQr.setOnClickListener { binding.qrContainer.visibility = View.GONE }
        binding.btnGps.setOnClickListener {
            binding.webView.loadUrl("javascript:goToLocation(51.505, -0.09)")
        }
    }

    private fun showSignUpDialog() {
        val dialogBinding = DialogFinderSignupBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).setCancelable(true).create()

        dialogBinding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle { email ->
                dialogBinding.etEmail.setText(email)
                Toast.makeText(this, "Google account selected!", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val email = dialogBinding.etEmail.text.toString().trim()
            val password = dialogBinding.etPassword.text.toString()
            val card = dialogBinding.etCardNumber.text.toString().trim()
            
            if (email.isNotEmpty() && password.length >= 6 && card.length >= 10) {
                dialogBinding.btnConfirm.isEnabled = false
                dialogBinding.btnConfirm.text = "Checking..."

                FirebaseManager.signUpOrLogin(email, password,
                    onSuccess = {
                        runOnUiThread {
                            viewModel.signUp(email, card)
                            dialog.dismiss()
                            Toast.makeText(this, "Finder Registered Successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            dialogBinding.btnConfirm.isEnabled = true
                            dialogBinding.btnConfirm.text = getString(R.string.btn_confirm)
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
                Toast.makeText(this, "Invalid fields or password too short (min 6)", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun signInWithGoogle(onSuccess: (String) -> Unit) {
        val webClientId = "334805793168-tq67njkra17icku3lbehfhfs0gglgqc6.apps.googleusercontent.com"
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@FinderActivity, request)
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    onSuccess(credential.id)
                }
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Google Sign-In Error: ${e.message}")
                Toast.makeText(this@FinderActivity, "Google Sign-In Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generateQrCode(text: String): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val bitmap = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        registration?.remove()
    }
}