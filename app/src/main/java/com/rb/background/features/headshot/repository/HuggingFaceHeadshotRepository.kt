package com.rb.background.features.headshot.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.rb.background.BuildConfig
import com.rb.background.features.headshot.data.HuggingFaceImageRequest
import com.rb.background.features.headshot.data.HuggingFaceParameters
import com.rb.background.features.headshot.data.HuggingFaceRetrofitClient

class HuggingFaceHeadshotRepository(
    private val context: Context
) {

    private val apiService =
        HuggingFaceRetrofitClient.apiService

    suspend fun generateHeadshot(
        imageUri: String,
        profession: String,
        style: String,
        pose: String,
        background: String
    ): Result<ByteArray> {

        return try {

            // -----------------------------
            // Read selected photo
            // -----------------------------

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

            // -----------------------------
            // Convert photo to Base64
            // -----------------------------

            val base64Image =
                Base64.encodeToString(
                    imageBytes,
                    Base64.NO_WRAP
                )

            // -----------------------------
            // Build AI prompt
            // -----------------------------

            val prompt = """
                Create a professional AI headshot using the uploaded
                person's photo as the identity reference.

                Profession: $profession
                Style: $style
                Pose: $pose
                Background: $background

                Requirements:
                - Preserve the person's identity and facial features.
                - Keep the face natural and realistic.
                - Create a photorealistic professional headshot.
                - Apply the selected profession naturally.
                - Apply the selected visual style.
                - Use the selected pose.
                - Use the selected background.
                - Keep realistic skin texture and lighting.
                - Make the result suitable for a professional profile photo.
                - Do not add text.
                - Do not add logos.
                - Do not add watermarks.
                - Do not add extra people.
            """.trimIndent()

            // -----------------------------
            // Create request
            // -----------------------------

            val request =
                HuggingFaceImageRequest(
                    inputs = base64Image,
                    parameters =
                        HuggingFaceParameters(
                            prompt = prompt
                        )
                )

            // -----------------------------
            // Call Hugging Face
            // -----------------------------

            val response =
                apiService.generateHeadshot(
                    authorization =
                        "Bearer ${BuildConfig.HUGGINGFACE_API_KEY}",
                    request = request
                )

            // -----------------------------
            // Check response
            // -----------------------------

            if (!response.isSuccessful) {

                val errorBody =
                    response.errorBody()
                        ?.string()

                return Result.failure(
                    Exception(
                        "Hugging Face API error: " +
                                "${response.code()} " +
                                errorBody.orEmpty()
                    )
                )
            }

            // -----------------------------
            // Get generated image
            // -----------------------------

            val generatedImage =
                response.body()?.bytes()

            if (generatedImage == null ||
                generatedImage.isEmpty()
            ) {

                return Result.failure(
                    Exception(
                        "Hugging Face did not return an image"
                    )
                )
            }

            Result.success(generatedImage)

        } catch (e: Exception) {

            e.printStackTrace()

            Result.failure(e)
        }
    }
}