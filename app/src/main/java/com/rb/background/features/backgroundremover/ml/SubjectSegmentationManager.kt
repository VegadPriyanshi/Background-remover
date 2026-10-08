package com.rb.background.features.backgroundremover.ml

import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult

class SubjectSegmentationManager {

    private val options = SubjectSegmenterOptions.Builder()
        .enableForegroundBitmap()
        .build()

    private val subjectSegmenter: SubjectSegmenter =
        SubjectSegmentation.getClient(options)

    fun segmentImage(bitmap: Bitmap): Task<SubjectSegmentationResult> {

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        return subjectSegmenter.process(inputImage)

    }

}