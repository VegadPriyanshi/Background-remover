package com.rb.background.features.headshot.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.rb.background.BuildConfig
import com.rb.background.features.headshot.api.GeminiHeadshotRequest
import com.rb.background.features.headshot.api.GeminiContent
import com.rb.background.features.headshot.api.GeminiPart
import com.rb.background.features.headshot.api.GeminiInlineData
import com.rb.background.features.headshot.api.GeminiGenerationConfig
import com.rb.background.features.headshot.api.GeminiRetrofitClient

class HeadshotRepository(
    private val context: Context
) {

    private val apiService =
        GeminiRetrofitClient.apiService

    suspend fun generateHeadshot(
        imageUri: String,
        profession: String,
        style: String,
        pose: String,
        background: String
    ): Result<String> {

        return try {

            // --------------------------------
            // Read image from URI
            // --------------------------------

            val uri = Uri.parse(imageUri)

            val imageBytes =
                context.contentResolver
                    .openInputStream(uri)
                    ?.use { inputStream ->
                        inputStream.readBytes()
                    }
                    ?: return Result.failure(
                        Exception("Unable to read selected photo")
                    )

            // --------------------------------
            // Convert image to Base64
            // --------------------------------

            val base64Image =
                Base64.encodeToString(
                    imageBytes,
                    Base64.NO_WRAP
                )

            // --------------------------------
            // Get MIME type
            // --------------------------------

            val mimeType =
                context.contentResolver
                    .getType(uri)
                    ?: "image/jpeg"

            // --------------------------------
            // Build AI prompt
            // --------------------------------

            val prompt = """
                Create a professional AI headshot using the uploaded person's
                photo as the identity reference.

                Profession: $profession
                Style: $style
                Pose: $pose
                Background: $background

                Requirements:
                - Preserve the person's identity and facial features.
                - Keep the face natural and realistic.
                - Create a photorealistic professional portrait.
                - Apply the requested profession, style and pose naturally.
                - Use the requested background.
                - Keep realistic skin texture and lighting.
                - Make the result suitable for a professional profile photo.
                - Do not add text, logos, watermarks or extra people.
            """.trimIndent()

            // --------------------------------
            // Build Gemini request
            // --------------------------------

            val request =
                GeminiHeadshotRequest(

                    contents = listOf(

                        GeminiContent(

                            parts = listOf(

                                GeminiPart(
                                    text = prompt
                                ),

                                GeminiPart(
                                    inline_data =
                                        GeminiInlineData(
                                            mime_type = mimeType,
                                            data = base64Image
                                        )
                                )
                            )
                        )
                    ),

                    generationConfig =
                        GeminiGenerationConfig(
                            responseModalities =
                                listOf("IMAGE")
                        )
                )

            // --------------------------------
            // Call Gemini
            // --------------------------------

            val response =
                apiService.generateHeadshot(
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = request
                )

            // --------------------------------
            // Check HTTP response
            // --------------------------------

            if (!response.isSuccessful) {

                return Result.failure(
                    Exception(
                        "Gemini API error: ${response.code()}"
                    )
                )
            }

            // --------------------------------
            // Find generated image
            // --------------------------------

            val generatedImage =
                response.body()
                    ?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull { part ->
                        part.inlineData?.data != null
                    }
                    ?.inlineData
                    ?.data

            if (generatedImage.isNullOrEmpty()) {

                return Result.failure(
                    Exception(
                        "Gemini did not return an image"
                    )
                )
            }

            // --------------------------------
            // Return Base64 generated image
            // --------------------------------

            Result.success(generatedImage)

        } catch (e: Exception) {

            e.printStackTrace()

            Result.failure(e)
        }
    }
}