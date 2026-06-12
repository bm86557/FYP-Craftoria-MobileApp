package com.example.myapplication

import com.example.myapplication.model.CloudiinaryApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api : CloudiinaryApi by lazy {
        Retrofit.Builder().baseUrl("https://api.cloudinary.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(CloudiinaryApi::class.java)
    }
}