package com.example.gqtphotoapp.dropbox

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2

class DropboxManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "dropbox_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        const val APP_KEY = "auldplyanoxkh8e" // Replace with your actual app key from Dropbox

        @Volatile
        private var instance: DropboxManager? = null

        fun getInstance(context: Context): DropboxManager {
            return instance ?: synchronized(this) {
                instance ?: DropboxManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredential(credential: DbxCredential) {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, credential.accessToken)
            putString(KEY_REFRESH_TOKEN, credential.refreshToken)
            putLong(KEY_EXPIRES_AT, credential.expiresAt ?: 0L)
            apply()
        }
    }

    fun getCredential(): DbxCredential? {
        val accessToken = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
        val expiresAt = encryptedPrefs.getLong(KEY_EXPIRES_AT, 0L)

        return DbxCredential(
            accessToken,
            if (expiresAt > 0) expiresAt else null,
            refreshToken,
            APP_KEY
        )
    }

    fun isAuthenticated(): Boolean {
        return getCredential() != null
    }

    fun clearCredential() {
        encryptedPrefs.edit().clear().apply()
    }

    fun getClient(): DbxClientV2? {
        val credential = getCredential() ?: return null
        val config = DbxRequestConfig.newBuilder("GQTPhotoApp").build()
        return DbxClientV2(config, credential)
    }
}