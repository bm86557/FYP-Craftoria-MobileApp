package com.example.myapplication.model

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CloudiinaryApi {
    @Multipart
    @POST("v1_1/ddgxtgt4q/image/upload")
    fun UploadImage(
        @Part file : MultipartBody.Part,
        @Part("upload_preset") preset: RequestBody
    ): Call<CloudinaryResponse>
}

