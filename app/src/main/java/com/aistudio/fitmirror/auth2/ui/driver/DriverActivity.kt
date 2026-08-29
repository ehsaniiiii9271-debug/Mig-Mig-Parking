package com.aistudio.fitmirror.auth2.ui.driver

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.aistudio.fitmirror.auth2.R
import com.aistudio.fitmirror.auth2.databinding.ActivityDriverBinding
import com.aistudio.fitmirror.auth2.databinding.DialogDriverSignupBinding
import com.aistudio.fitmirror.auth2.repository.FirebaseManager
import com.aistudio.fitmirror.auth2.ui.MapBridge
import com.aistudio.fitmirror.auth2.viewmodel.DriverViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class DriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverBinding
    private val viewModel: DriverViewModel by viewModels()
    private var lastClickedLat: Double = 0.0
    private var lastClickedLng: Double = 0.0
    private var registration: ListenerRegistration? = null
    private lateinit var credentialManager: CredentialManager

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrData = result.data?.getStringExtra("qr_data")
            qrData?.let { processPayment(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverBinding.inflate(layoutInflater)
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
                onMapClick = { lat, lng ->
                    lastClickedLat = lat
                    lastClickedLng = lng
                }
            ), "AndroidBridge")
            loadUrl("file:///android_asset/map.html")
        }
    }

    private fun setupObservers() {
        viewModel.isSignedUp.observe(this) { isSignedUp ->
            if (isSignedUp) {
                binding.btnSignUp.visibility = View.GONE
                binding.btnStart.visibility = View.VISIBLE
            }
        }

        viewModel.isStarted.observe(this) { isStarted ->
            if (isStarted) {
                binding.btnStart.visibility = View.GONE
                binding.layoutControls.visibility = View.VISIBLE
                binding.btnRequestSpot.visibility = View.VISIBLE
                binding.btnScan.visibility = View.VISIBLE
            }
        }

        viewModel.walletBalance.observe(this) { balance ->
            binding.tvWallet.text = String.format(Locale.US, "$%.2f", balance)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSignUp.setOnClickListener { showSignUpDialog() }
        binding.btnStart.setOnClickListener { viewModel.start() }
        binding.btnRequestSpot.setOnClickListener {
            if (lastClickedLat != 0.0) {
                FirebaseManager.sendDriverRequest(lastClickedLat, lastClickedLng, "driver@example.com") {
                    Toast.makeText(this, "Parking requested!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Tap map to set location", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnScan.setOnClickListener {
            scanLauncher.launch(Intent(this, ScannerActivity::class.java))
        }
        binding.btnGps.setOnClickListener {
            binding.webView.loadUrl("javascript:goToLocation(51.505, -0.09)")
        }
    }

    private fun showSignUpDialog() {
        val dialogBinding = DialogDriverSignupBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).setCancelable(true).create()

        dialogBinding.btnNext.setOnClickListener {
            val email = dialogBinding.etEmail.text.toString().trim()
            val password = dialogBinding.etPassword.text.toString()
            val carModel = dialogBinding.etCarModel.text.toString().trim()

            if (email.isNotEmpty() && password.length >= 6 && carModel.isNotEmpty()) {
                dialogBinding.btnNext.isEnabled = false
                dialogBinding.btnNext.text = "Checking..."
                
                FirebaseManager.signUpOrLogin(email, password, 
                    onSuccess = {
                        runOnUiThread {
                            dialogBinding.step1.visibility = View.GONE
                            dialogBinding.step2.visibility = View.VISIBLE
                            dialogBinding.tvTitle.text = getString(R.string.title_wallet_recharge)
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            dialogBinding.btnNext.isEnabled = true
                            dialogBinding.btnNext.text = getString(R.string.btn_next)
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
                Toast.makeText(this, "Invalid Email or Password (min 6)", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle { email ->
                dialogBinding.etEmail.setText(email)
                Toast.makeText(this, "Google account selected!", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val amountStr = dialogBinding.etAmount.text.toString()
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount >= 5.0) {
                viewModel.signUp(dialogBinding.etEmail.text.toString(), dialogBinding.etCarModel.text.toString(), amount)
                dialog.dismiss()
                Toast.makeText(this, "Success! Press START to begin.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Minimum recharge is $5.00", Toast.LENGTH_SHORT).show()
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
                val result = credentialManager.getCredential(this@DriverActivity, request)
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    onSuccess(credential.id)
                } else {
                    Log.e("Auth", "Unknown credential type")
                }
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Google Sign-In Error: ${e.message}")
                Toast.makeText(this@DriverActivity, "Google Sign-In Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun processPayment(qrData: String) {
        val amount = qrData.toDoubleOrNull() ?: 10.0
        if (viewModel.pay(amount)) {
            Toast.makeText(this, "Payment Successful: $${amount}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Insufficient Funds", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        registration?.remove()
    }
}