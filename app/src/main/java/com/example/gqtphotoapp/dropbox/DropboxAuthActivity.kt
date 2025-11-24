package com.example.gqtphotoapp.dropbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dropbox.core.android.Auth
import com.example.gqtphotoapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DropboxAuthActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DropboxAuthActivity"
    }

    private lateinit var tvAuthStatus: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var btnTestConnection: Button
    private lateinit var btnBack: Button

    private lateinit var dropboxManager: DropboxManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dropbox_auth)

        dropboxManager = DropboxManager.getInstance(this)

        tvAuthStatus = findViewById(R.id.tv_auth_status)
        btnConnect = findViewById(R.id.btn_connect)
        btnDisconnect = findViewById(R.id.btn_disconnect)
        btnTestConnection = findViewById(R.id.btn_test_connection)
        btnBack = findViewById(R.id.btn_back)

        setupUI()
        updateAuthStatus()
    }

    override fun onResume() {
        super.onResume()

        // Check if we returned from Dropbox auth
        val credential = Auth.getDbxCredential()
        if (credential != null) {
            dropboxManager.saveCredential(credential)
            updateAuthStatus()
            Toast.makeText(this, "Connected to Dropbox!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        btnConnect.setOnClickListener {
            try {
                Log.d(TAG, "Starting OAuth with APP_KEY: ${DropboxManager.APP_KEY}")

                val requestConfig = com.dropbox.core.DbxRequestConfig.newBuilder("GQTPhotoApp").build()

                Auth.startOAuth2PKCE(
                    this,
                    DropboxManager.APP_KEY,
                    requestConfig,
                    null as Collection<String>?
                )

                Log.d(TAG, "OAuth call completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting OAuth", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnDisconnect.setOnClickListener {
            dropboxManager.clearCredential()
            updateAuthStatus()
            Toast.makeText(this, "Disconnected from Dropbox", Toast.LENGTH_SHORT).show()
        }

        btnTestConnection.setOnClickListener {
            testConnection()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateAuthStatus() {
        val isAuthenticated = dropboxManager.isAuthenticated()

        tvAuthStatus.text = if (isAuthenticated) {
            "✓ Connected to Dropbox"
        } else {
            "Not connected"
        }

        btnConnect.isEnabled = !isAuthenticated
        btnDisconnect.isEnabled = isAuthenticated
        btnTestConnection.isEnabled = isAuthenticated
    }

    private fun testConnection() {
        lifecycleScope.launch {
            try {
                tvAuthStatus.text = "Testing connection..."

                val accountInfo = withContext(Dispatchers.IO) {
                    val client = dropboxManager.getClient()
                    client?.users()?.currentAccount
                }

                if (accountInfo != null) {
                    tvAuthStatus.text =
                        "✓ Connected as: ${accountInfo.name.displayName}\nEmail: ${accountInfo.email}"
                } else {
                    tvAuthStatus.text = "Connection test failed"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed", e)
                tvAuthStatus.text = "Error: ${e.message}"
            }
        }
    }
}