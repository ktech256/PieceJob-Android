package com.piecejob.core.utils

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.InputStream

object FileUtils {
    fun uriToBase64(uri: Uri, context: Context): String {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        return if (bytes != null) {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else {
            ""
        }
    }
}
