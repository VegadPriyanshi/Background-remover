package com.rb.background.features.headshot.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.rb.background.BuildConfig
import com.rb.background.features.headshot.data.InstantIdRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class InstantIdHeadshotRepository(
    private val context: Context
) {

    private val apiService =
        InstantIdRetrofitClient.apiService

    private  val baseUrl =
        "https://instantx-instantid.hf.space"

    private val httpClient =
        OkHttpClient()

    // InstantID's own face detector rejects faces that are
    // too small relative to the frame (e.g. a full-body shot),
    // even though our on-device gate in HeadshotUploadFragment
    // is lenient enough to pass those. We crop tight around the
    // detected face before uploading so the ratio matches what
    // it expects.
    private val faceDetector by lazy {

        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
                )
                .setLandmarkMode(
                    FaceDetectorOptions.LANDMARK_MODE_NONE
                )
                .setClassificationMode(
                    FaceDetectorOptions.CLASSIFICATION_MODE_NONE
                )
                .build()
        )
    }

    suspend fun generateHeadshot(
        imageUri: String,
        profession: String,
        templateUrl: String,
        onStatusUpdate: (String) -> Unit = {}
    ): Result<ByteArray> {

        return try {

            // ---------------------------------------------
            // 1. Copy user's selected image to cache
            // ---------------------------------------------

            onStatusUpdate("Preparing your photo...")

            val uri = Uri.parse(imageUri)

            val inputStream =
                context.contentResolver.openInputStream(uri)
                    ?: return Result.failure(
                        Exception("Unable to read selected photo")
                    )

            val inputFile =
                File(
                    context.cacheDir,
                    "instantid_input_${System.currentTimeMillis()}.jpg"
                )

            inputStream.use { input ->
                inputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            cropToFace(inputFile)

            // ---------------------------------------------
            // 2. Upload user's image
            // ---------------------------------------------

            onStatusUpdate("Uploading your photo...")

            val facePath =
                uploadImage(inputFile)

                    ?: return Result.failure(
                        Exception("Unable to upload user's photo")
                    )

            // ---------------------------------------------
            // 3. Download Pexels template
            // ---------------------------------------------

            onStatusUpdate("Preparing the selected template...")

            val templateFile =
                downloadTemplateImage(templateUrl)

                    ?: return Result.failure(
                        Exception("Unable to download selected template")
                    )

            // ---------------------------------------------
            // 4. Upload template
            // ---------------------------------------------

            val posePath =
                uploadImage(templateFile)

                    ?: return Result.failure(
                        Exception("Unable to upload selected template")
                    )

            // ---------------------------------------------
            // 5. Prompt
            // ---------------------------------------------
            // InstantID does not copy pixels from the pose
            // reference image — it only derives a depth/canny
            // structure map from it, which carries edges/pose
            // but not color. Clothing color has to be spelled
            // out in the prompt itself (sampled from the actual
            // template photo), or the model invents its own.

            val dominantClothingColor =
                detectDominantClothingColor(templateFile)

            val attireDescription =
                attireDescriptionFor(
                    profession = profession,
                    dominantColor = dominantClothingColor
                )

            val prompt = """
                A professional photorealistic headshot of the person.

                Profession: $profession.

                The person is wearing $attireDescription.

                The clothing color, clothing style and pose must
                exactly match the reference/pose image. Do not
                invent, change or recolor the clothing.

                Create a realistic professional portrait suitable
                for a professional profile photo.

                Preserve the identity, facial structure and facial
                features of the person in the uploaded face image
                exactly as they are.

                Keep the exact same facial expression, eye
                direction and mouth position as the uploaded face
                image. Do not smile more, smile less, or otherwise
                change the expression.

                Match the pose, composition and lighting of the
                reference image.

                Professional head and shoulders composition.
                Realistic skin texture.
                High quality photography.
            """.trimIndent()

            // ---------------------------------------------
            // 6. Negative prompt
            // ---------------------------------------------

            val negativePrompt = """
                low quality,
                blurry,
                distorted face,
                deformed face,
                different person,
                altered facial features,
                changed facial expression,
                different expression,
                altered identity,
                bad anatomy,
                extra people,
                extra face,
                cartoon,
                anime,
                illustration,
                painting,
                unrealistic skin,
                excessive makeup,
                beauty filter,
                different clothing color,
                changed outfit,
                altered clothing style,
                mismatched attire,
                text,
                logo,
                watermark
            """.trimIndent()

            // ---------------------------------------------
            // 7. Face FileData
            // ---------------------------------------------

            val faceFileData =
                createGradioFileData(
                    path = facePath,
                    originalName = inputFile.name
                )

            // ---------------------------------------------
            // 8. Pose FileData
            // ---------------------------------------------

            val poseFileData =
                createGradioFileData(
                    path = posePath,
                    originalName = templateFile.name
                )

            // ---------------------------------------------
            // 9. Current InstantID parameters
            // ---------------------------------------------
            // Must match the live Space's generate_image
            // signature exactly (16 positional params, in
            // order) — see /info on the Space for the schema.

            val data =
                JsonArray().apply {

                    // 1. face_image_path
                    add(faceFileData)

                    // 2. pose_image_path
                    add(poseFileData)

                    // 3. prompt
                    add(prompt)

                    // 4. negative_prompt
                    add(negativePrompt)

                    // 5. style_name
                    add("(No style)")

                    // 6. num_steps
                    add(30)

                    // 7. identitynet_strength_ratio
                    add(0.9)

                    // 8. adapter_strength_ratio
                    add(0.9)

                    // 9. canny_strength
                    add(0.6)

                    // 10. depth_strength
                    add(0.6)

                    // 11. controlnet_selection
                    // "canny" keeps the garment/collar edges
                    // from the pose reference, "depth" keeps
                    // body position/composition.
                    add(
                        JsonArray().apply {
                            add("canny")
                            add("depth")
                        }
                    )

                    // 12. guidance_scale
                    add(5.0)

                    // 13. seed
                    add(42)

                    // 14. scheduler
                    add("EulerDiscreteScheduler")

                    // 15. enable_LCM
                    add(false)

                    // 16. enhance_face_region
                    add(true)
                }

            // ---------------------------------------------
            // 10. Build request
            // ---------------------------------------------

            val requestJson =
                JsonObject().apply {
                    add("data", data)
                }

            val requestBody =
                requestJson
                    .toString()
                    .toRequestBody(
                        "application/json".toMediaType()
                    )

            // ---------------------------------------------
            // 11-13. Start generation, then wait for result.
            // Retried: this public demo Space intermittently
            // errors on valid input under GPU/queue pressure,
            // and a fresh attempt often succeeds where the
            // previous one didn't.
            // ---------------------------------------------

            val generatedImageUrl =
                generateAndWaitWithRetry(
                    requestBody,
                    onStatusUpdate
                )

            // ---------------------------------------------
            // 14. Download result
            // ---------------------------------------------

            onStatusUpdate("Finalizing your headshot...")

            val imageBytes =
                downloadGeneratedImage(
                    generatedImageUrl
                )

                    ?: return Result.failure(
                        Exception(
                            "Unable to download generated image"
                        )
                    )

            // ---------------------------------------------
            // 15. Cleanup
            // ---------------------------------------------

            inputFile.delete()
            templateFile.delete()

            Result.success(imageBytes)

        } catch (e: Exception) {

            e.printStackTrace()

            Result.failure(e)
        }
    }

    private suspend fun cropToFace(
        file: File
    ) {

        withContext(Dispatchers.IO) {

            try {

                val bitmap =
                    BitmapFactory.decodeFile(file.absolutePath)
                        ?: return@withContext

                val faces =
                    Tasks.await(
                        faceDetector.process(
                            InputImage.fromBitmap(bitmap, 0)
                        )
                    )

                val face =
                    faces.maxByOrNull {
                        it.boundingBox.width() *
                                it.boundingBox.height()
                    } ?: return@withContext

                val box = face.boundingBox

                // Pad generously around the face so InstantID
                // gets a proper head-and-shoulders framing
                // instead of a tiny face lost in a full-body
                // photo.
                val paddingX = box.width()
                val paddingY = (box.height() * 1.2f).toInt()

                val left =
                    (box.left - paddingX).coerceAtLeast(0)

                val top =
                    (box.top - paddingY).coerceAtLeast(0)

                val right =
                    (box.right + paddingX).coerceAtMost(bitmap.width)

                val bottom =
                    (box.bottom + paddingY).coerceAtMost(bitmap.height)

                if (right <= left || bottom <= top) {
                    return@withContext
                }

                val cropped =
                    Bitmap.createBitmap(
                        bitmap,
                        left,
                        top,
                        right - left,
                        bottom - top
                    )

                FileOutputStream(file).use { output ->
                    cropped.compress(
                        Bitmap.CompressFormat.JPEG,
                        90,
                        output
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                // Fall back silently to the original,
                // uncropped photo if detection fails for any
                // reason — better to try the full image than
                // to block generation entirely.
            }
        }
    }

    private suspend fun uploadImage(
        file: File
    ): String? {

        return withContext(Dispatchers.IO) {

            val requestBody =
                file.asRequestBody(
                    "image/jpeg".toMediaType()
                )

            val multipart =
                MultipartBody.Part.createFormData(
                    "files",
                    file.name,
                    requestBody
                )

            val response =
                apiService.uploadFile(
                    authorization =
                        "Bearer ${BuildConfig.HUGGINGFACE_API_KEY}",
                    file = multipart
                )

            if (!response.isSuccessful) {

                throw Exception(
                    "InstantID upload error: " +
                            "${response.code()} " +
                            response.errorBody()
                                ?.string()
                                .orEmpty()
                )
            }

            response.body()?.firstOrNull()
        }
    }

    private suspend fun downloadTemplateImage(
        templateUrl: String
    ): File? {

        return withContext(Dispatchers.IO) {

            try {

                val request =
                    Request.Builder()
                        .url(templateUrl)
                        .get()
                        .build()

                httpClient
                    .newCall(request)
                    .execute()
                    .use { response ->

                        if (!response.isSuccessful) {
                            return@withContext null
                        }

                        val body =
                            response.body
                                ?: return@withContext null

                        val file =
                            File(
                                context.cacheDir,
                                "headshot_template_${System.currentTimeMillis()}.jpg"
                            )

                        file.outputStream().use { output ->
                            body.byteStream().use { input ->
                                input.copyTo(output)
                            }
                        }

                        file
                    }

            } catch (e: Exception) {

                e.printStackTrace()

                null
            }
        }
    }

    private fun attireDescriptionFor(
        profession: String,
        dominantColor: String?
    ): String {

        val garment =
            when (profession.trim().lowercase()) {

                "doctor" ->
                    "medical lab coat over a collared shirt, with a stethoscope around the neck"

                "lawyer" ->
                    "tailored business suit with a dress shirt and a conservative tie"

                "engineer" ->
                    "smart-casual button-up shirt, optionally with a blazer"

                "business" ->
                    "formal business suit with a dress shirt and tie"

                "teacher" ->
                    "smart-casual blazer or cardigan over a collared shirt"

                "designer" ->
                    "stylish smart-casual outfit, such as a fitted shirt or turtleneck"

                "developer" ->
                    "smart-casual t-shirt or button-up shirt, optionally with a hoodie or jacket"

                "photographer" ->
                    "smart-casual attire such as a jacket over a plain shirt"

                else ->
                    "professional business attire appropriate for a $profession"
            }

        val colorPrefix =
            dominantColor?.let { "$it " }.orEmpty()

        return "a $colorPrefix$garment"
    }

    // Named colors to map a sampled RGB average onto a word the
    // prompt can actually use. Kept deliberately small/generic —
    // this only needs to be close enough to steer the model away
    // from inventing an unrelated color, not colorimetrically exact.
    private data class NamedColor(
        val name: String,
        val r: Int,
        val g: Int,
        val b: Int
    )

    private val namedColors = listOf(
        NamedColor("white", 255, 255, 255),
        NamedColor("black", 20, 20, 20),
        NamedColor("charcoal gray", 60, 60, 62),
        NamedColor("gray", 128, 128, 128),
        NamedColor("light gray", 200, 200, 200),
        NamedColor("navy blue", 25, 35, 90),
        NamedColor("blue", 40, 80, 190),
        NamedColor("light blue", 130, 175, 220),
        NamedColor("red", 175, 35, 35),
        NamedColor("maroon", 100, 25, 35),
        NamedColor("green", 45, 110, 65),
        NamedColor("olive green", 95, 100, 45),
        NamedColor("brown", 105, 70, 45),
        NamedColor("beige", 220, 200, 170),
        NamedColor("tan", 200, 170, 130),
        NamedColor("purple", 100, 45, 130),
        NamedColor("pink", 225, 155, 180),
        NamedColor("yellow", 220, 200, 60),
        NamedColor("orange", 220, 120, 45),
        NamedColor("cream", 240, 232, 212)
    )

    private fun nearestColorName(
        r: Int,
        g: Int,
        b: Int
    ): String {

        return namedColors.minBy {

            val dr = it.r - r
            val dg = it.g - g
            val db = it.b - b

            dr * dr + dg * dg + db * db

        }.name
    }

    // Samples the shoulders/chest area just below the template's
    // detected face to estimate the garment's dominant color, so
    // the prompt can ask for that exact color instead of guessing.
    private suspend fun detectDominantClothingColor(
        file: File
    ): String? {

        return withContext(Dispatchers.IO) {

            try {

                val bitmap =
                    BitmapFactory.decodeFile(file.absolutePath)
                        ?: return@withContext null

                val faces =
                    Tasks.await(
                        faceDetector.process(
                            InputImage.fromBitmap(bitmap, 0)
                        )
                    )

                val face =
                    faces.maxByOrNull {
                        it.boundingBox.width() *
                                it.boundingBox.height()
                    } ?: return@withContext null

                val box = face.boundingBox

                val left =
                    (box.left - box.width() / 2)
                        .coerceAtLeast(0)

                val right =
                    (box.right + box.width() / 2)
                        .coerceAtMost(bitmap.width)

                val top =
                    (box.bottom + box.height() / 4)
                        .coerceAtMost(bitmap.height - 1)

                val bottom =
                    (box.bottom + box.height() * 2)
                        .coerceAtMost(bitmap.height)

                if (right <= left || bottom <= top) {
                    return@withContext null
                }

                var totalR = 0L
                var totalG = 0L
                var totalB = 0L
                var sampleCount = 0L

                val stride = 6

                var y = top
                while (y < bottom) {

                    var x = left
                    while (x < right) {

                        val pixel = bitmap.getPixel(x, y)

                        totalR += android.graphics.Color.red(pixel)
                        totalG += android.graphics.Color.green(pixel)
                        totalB += android.graphics.Color.blue(pixel)
                        sampleCount++

                        x += stride
                    }

                    y += stride
                }

                if (sampleCount == 0L) {
                    return@withContext null
                }

                nearestColorName(
                    (totalR / sampleCount).toInt(),
                    (totalG / sampleCount).toInt(),
                    (totalB / sampleCount).toInt()
                )

            } catch (e: Exception) {

                e.printStackTrace()

                null
            }
        }
    }

    private fun createGradioFileData(
        path: String,
        originalName: String
    ): JsonObject {

        return JsonObject().apply {

            addProperty(
                "path",
                path
            )

            addProperty(
                "orig_name",
                originalName
            )

            add(
                "meta",
                JsonObject().apply {
                    addProperty(
                        "_type",
                        "gradio.FileData"
                    )
                }
            )
        }
    }

    private fun extractEventId(
        response: String
    ): String? {

        val regex =
            Regex(
                "\"event_id\"\\s*:\\s*\"([^\"]+)\""
            )

        return regex
            .find(response)
            ?.groupValues
            ?.getOrNull(1)
    }

    private suspend fun generateAndWaitWithRetry(
        requestBody: RequestBody,
        onStatusUpdate: (String) -> Unit
    ): String {

        val maxAttempts = 3

        var lastError: Exception =
            Exception("InstantID did not return generated image")

        repeat(maxAttempts) { attempt ->

            onStatusUpdate(
                if (attempt == 0)
                    "Generating your headshot — this can take a minute..."
                else
                    "The AI service is busy. Retrying " +
                            "(attempt ${attempt + 1} of $maxAttempts)..."
            )

            try {

                val generationResponse =
                    apiService.generateImage(
                        authorization =
                            "Bearer ${BuildConfig.HUGGINGFACE_API_KEY}",
                        request = requestBody
                    )

                if (!generationResponse.isSuccessful) {

                    throw Exception(
                        "InstantID generation error: " +
                                "${generationResponse.code()} " +
                                generationResponse
                                    .errorBody()
                                    ?.string()
                                    .orEmpty()
                    )
                }

                val eventResponse =
                    generationResponse.body()
                        ?.string()
                        .orEmpty()

                val eventId =
                    extractEventId(eventResponse)
                        ?: throw Exception(
                            "InstantID did not return event ID: " +
                                    eventResponse
                        )

                val url =
                    waitForGeneratedImage(
                        eventId,
                        onStatusUpdate
                    )

                if (url != null) {
                    return url
                }

            } catch (e: Exception) {

                lastError = e
            }

            // Skip the wait after the final attempt.
            if (attempt < maxAttempts - 1) {
                delay(8_000L * (attempt + 1))
            }
        }

        throw lastError
    }

    private suspend fun waitForGeneratedImage(
        eventId: String,
        onStatusUpdate: (String) -> Unit
    ): String? {

        repeat(60) { pollIndex ->

            // The status text would otherwise sit frozen for the
            // entire poll — this Space can genuinely take a
            // couple of minutes, so keep refreshing it with the
            // elapsed time instead of looking stuck.
            if (pollIndex > 0 && pollIndex % 5 == 0) {

                onStatusUpdate(
                    "Still working (${pollIndex * 3}s)... " +
                            "the shared AI service can be slow."
                )
            }

            val response =
                apiService.getGenerationResult(
                    authorization =
                        "Bearer ${BuildConfig.HUGGINGFACE_API_KEY}",
                    eventId = eventId
                )

            if (response.isSuccessful) {

                val sse =
                    response.body()
                        ?.string()
                        .orEmpty()

                if (
                    sse.contains(
                        "event: complete",
                        ignoreCase = true
                    )
                ) {

                    val url =
                        extractImageUrl(sse)

                    if (url != null) {
                        return url
                    }
                }

                if (
                    sse.contains(
                        "event: error",
                        ignoreCase = true
                    )
                ) {

                    // The Space returns this generic event
                    // (with no detail) both when it can't
                    // detect a usable face and when the shared
                    // demo Space is overloaded. The photo is
                    // already auto-cropped to the face and this
                    // call gets retried by the caller, so a
                    // failure surfaced after retries is more
                    // likely the latter.
                    throw Exception(
                        "The AI service is temporarily busy. " +
                                "Please try again in a few minutes."
                    )
                }
            }

            delay(3000)
        }

        return null
    }

    private fun extractImageUrl(
        response: String
    ): String? {

        // The SSE "complete" event's "url" field is malformed
        // on this Space (points at a non-existent
        // call/gen/file= route and 404s), so the download URL
        // must always be built from "path" via the plain
        // /file= route instead of trusting "url" directly.

        val pathRegex =
            Regex(
                "\"path\"\\s*:\\s*\"([^\"]+)\""
            )

        val path =
            pathRegex
                .find(response)
                ?.groupValues
                ?.getOrNull(1)

        if (path != null) {

            return "$baseUrl/file=${
                path.replace("\\/", "/")
            }"
        }

        return null
    }

    private suspend fun downloadGeneratedImage(
        url: String
    ): ByteArray? {

        return withContext(Dispatchers.IO) {

            try {

                val request =
                    Request.Builder()
                        .url(url)
                        .get()
                        .build()

                httpClient
                    .newCall(request)
                    .execute()
                    .use { response ->

                        if (!response.isSuccessful) {
                            return@withContext null
                        }

                        response.body?.bytes()
                    }

            } catch (e: Exception) {

                e.printStackTrace()

                null
            }
        }
    }
}