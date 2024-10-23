package com.example.kashdaddy

import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SupabaseService {

    private val baseUrl = "https://zvsttixrednvsvguesfl.supabase.co"
    private val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp2c3R0aXhyZWRudnN2Z3Vlc2ZsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MjcyOTI4NTQsImV4cCI6MjA0Mjg2ODg1NH0.He5aDBr4AGa2SG7lqSt8ej_nyyXWO50U_3sAVyNiW30"
    private val client = OkHttpClient()

    // Helper function to create headers
    private fun getHeaders(): Map<String, String> {
        return mapOf(
            "apikey" to apiKey,
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json",
            "Accept" to "application/json" // To ensure the server returns JSON
        )
    }

    // Function for GET requests (e.g., get all users, transactions, goals, reminders)
    fun getData(endpoint: String): String? {
        val url = "$baseUrl$endpoint"
        val request = Request.Builder()
            .url(url)
            .headers(getHeaders().toHeaders())
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body?.string()
        }
    }

    // Function for POST requests (e.g., add new user, transaction, goal, or reminder)
    fun postData(endpoint: String, jsonBody: JSONObject): String? {
        val url = "$baseUrl$endpoint"
        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .headers(getHeaders().toHeaders())
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body?.string()
        }
    }

    // Function for PUT requests (e.g., update an existing user, transaction, goal, or reminder)
    fun putData(endpoint: String, jsonBody: JSONObject): String? {
        val url = "$baseUrl$endpoint"
        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .put(body)
            .headers(getHeaders().toHeaders())
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body?.string()
        }
    }

    // Function for DELETE requests (e.g., delete an existing user, transaction, goal, or reminder)
    fun deleteData(endpoint: String): String? {
        val url = "$baseUrl$endpoint"
        val request = Request.Builder()
            .url(url)
            .delete()
            .headers(getHeaders().toHeaders())
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body?.string()
        }
    }
}
