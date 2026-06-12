package com.example.myapplication.model

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.myapplication.RetrofitInstance
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import retrofit2.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response


object CloudinaryRepository {
    fun uploadImageToCloudinary(
        context: Context, 
        uri: Uri,
        preset: String = "profile_upload",
        onSuccess : (String) -> Unit,
        onError: ((String) -> Unit)? = null
    ){
        try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream == null) {
                onError?.invoke("Failed to read image")
                return
            }
            
            val requestBody = stream.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
            stream.close()
            
            val body = MultipartBody.Part.createFormData("file","image.jpg",requestBody)
            val requestBodyPreset = preset.toRequestBody("text/plain".toMediaTypeOrNull())
            
            RetrofitInstance.api.UploadImage(body,requestBodyPreset).enqueue(object : Callback<CloudinaryResponse> {
                override fun onResponse(call: Call<CloudinaryResponse>,response: Response<CloudinaryResponse>) {
                    if (response.isSuccessful){
                       val imageUrl = response.body()?.secure_url
                        if (imageUrl != null) {
                            onSuccess(imageUrl)
                        } else {
                            onError?.invoke("No URL received")
                        }
                    } else {
                        onError?.invoke("Upload failed: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<CloudinaryResponse>,t: Throwable){
                    Log.e("CloudinaryUpload",t.message?:"Error")
                    onError?.invoke(t.message ?: "Network error")
                }
            })
        } catch (e: Exception) {
            Log.e("CloudinaryUpload", "Exception: ${e.message}")
            onError?.invoke(e.message ?: "Unknown error")
        }
    }
}