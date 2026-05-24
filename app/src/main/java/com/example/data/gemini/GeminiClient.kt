package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "JarvisGeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeTranscript(transcript: String, actionType: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            Log.e(TAG, "BuildConfig.GEMINI_API_KEY is not defined or unreadable. Make sure to define it in user secrets.", e)
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "ERROR: [SECURE KEY MISSING]\n\nJarvis requires a valid GEMINI_API_KEY initialized in the Secrets panel inside the AI Studio UI to activate this holographic diagnostic."
        }

        val prompt = when (actionType) {
            "summary" -> "You are J.A.R.V.I.S., the ultimate futuristic cybernetic artificial intelligence system. Formulate a brief, highly-polished, tech-centric visual summary of the following transcript. Break it down under cool futuristic headings like [VOCAL PROTOCOL SUMMARY], [KEY METRICS], and [ACTION SCHEMES]:\n\n\"$transcript\""
            "sci_fi" -> "You are J.A.R.V.I.S., Tony Stark's advanced AI companion. Rewrite this audio transcript in a dramatic, high-tech sci-fi format, styled with technical telemetry, system diagnostics, and elite scientific commentary, as if Tony himself just voiced this in his workshop:\n\n\"$transcript\""
            "tone" -> "You are J.A.R.V.I.S., evaluating audio telemetry. Perform a vocal pattern analysis of this transcript text. Rate factors like Emotional Frequency, Speech Rhythm, and Cognitive Focus out of 100% inside cool telemetry bars, and provide brief diagnostic advice:\n\n\"$transcript\""
            else -> "You are J.A.R.V.I.S., an elite futuristic assistant. Answer this query about this transcript: \"$actionType\" \nTranscript Content:\n\"$transcript\""
        }

        try {
            // Build body using Android JSON APIs
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    }
                    put(JSONObject().apply {
                        put("parts", partsArray)
                    })
                }
                put("contents", contentsArray)

                // Optional system instruction
                val systemInstructionJson = JSONObject().apply {
                    val systemPartArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are J.A.R.V.I.S., a sophisticated holographic AI. Your output formatting should be visually clean, scientific, structured, and speak with Tony Stark's companion voice. Avoid markdown blocks that look too generic; use clean dividers and sleek technical headers.")
                        })
                    }
                    put("parts", systemPartArray)
                }
                put("systemInstruction", systemInstructionJson)

                // Add temperature config
                val configJson = JSONObject().apply {
                    put("temperature", 0.7f)
                }
                put("generationConfig", configJson)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API failed: Code ${response.code}, Detail: $errorMsg")
                    return@withContext "ERROR: JARVIS systems offline. Code: ${response.code}. Remote diagnostic details:\n$errorMsg"
                }

                val responseBody = response.body?.string() ?: return@withContext "ERROR: Telemetry feed returned void output."
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
                "ERROR: Jarvis parsing failure (Malformed response structure)."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Jarvis analysis", e)
            "ERROR: Critical system exception. JARVIS is unable to reach the neural gateway. Check your workspace network settings or key status. \n\nDiagnostic message:\n${e.message}"
        }
    }
}
