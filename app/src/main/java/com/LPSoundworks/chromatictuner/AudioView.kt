package com.LPSoundworks.chromatictuner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import java.lang.System.arraycopy
import kotlin.math.min
import kotlin.math.pow


class AudioView() {
    // VARS FOR AUDIO SAMPLE PROCESSING
    val sec = 10
    val hz = 44100
    val merge = 512
    val history = hz * sec / merge
    val audio: ArrayDeque<Float> = ArrayDeque()
    // VARS FOR AUDIO SAMPLE DRAWING
    val paintAudio: Paint = Paint()
    val path: Path = Path()

    // VARS FOR FFT SAMPLE PROCESSING
    val size = 4096
    val bands = 128
    val bandSize = (size/1.6).toInt() / bands
    val maxConst = 30000000000//1750000000 //reference max value for accum magnitude
    var average = .0f
    val fft: FloatArray = FloatArray(4096)
    val paintBandsFill: Paint = Paint()
    val paintBands: Paint = Paint()
    val paintAvg: Paint = Paint()


    // INIT
    init {
        // VARS FOR AUDIO SAMPLE DRAWING
        paintAudio.color = Color.Green
        paintAudio.strokeWidth = 0f
        paintAudio.style = PaintingStyle.Stroke

        // VARS FOR FFT SAMPLE DRAWING
        paintBandsFill.color = Color(169, 93, 227)
        paintBandsFill.style = PaintingStyle.Fill

        paintBands.color = Color(48, 28, 148)
        paintBands.strokeWidth = 1f
        paintBands.style = PaintingStyle.Stroke

        paintAvg.color = Color(18, 255, 243)
        paintAvg.strokeWidth = 1f
        paintAvg.style = PaintingStyle.Stroke
    }

    fun drawCanvas(canvas: DrawScope, i: Float) {
        val height = canvas.size.height
        val width = canvas.size.width

        // Processing for Audio Samples to create Line Graph
        path.reset()
        synchronized(audio) {
            for ((i, sample) in audio.withIndex()) {
                if (i == 0) path.moveTo(width, sample)
                path.lineTo(
                    width - ((width * i) / history.toFloat()),
                    min(sample * 1.0f + (height / 2f), height)
                )
            }
            if (audio.size in 1..(history - 1)) path.lineTo(0f, height / 2f)
        }


        // ----------------- DRAWING CONTEXT --------------------
        canvas.drawIntoCanvas { drawScope ->

            // Processing for FFT Samlpes to create bar graph
            for (i in 0..bands - 1) {
                var accum = .0f

                synchronized(fft) {
                    for (j in 0..bandSize - 1 step 2) {
                        //convert real and imag part to get energy
                        accum += (Math.pow(fft[j + (i * bandSize)].toDouble(), 2.0) + Math.pow(fft[j + 1 + (i * bandSize)].toDouble(), 2.0)).toFloat()

                    }
                    accum /= bandSize / 2
                }

                average += accum


                accum /=2
                var normalisedAccum:Float =  min(accum / maxConst.toDouble(), 1.0).toFloat()
                normalisedAccum = normalisedAccum.pow(0.5f)


                drawScope.drawRect(
                    width * (i / bands.toFloat()),
                    height - (height * normalisedAccum) - height * .02f,
                    width * (i / bands.toFloat()) + width / bands.toFloat(),
                    height,
                    paintBandsFill
                )
                drawScope.drawRect(
                    width * (i / bands.toFloat()),
                    height - (height * normalisedAccum) - height * .02f,
                    width * (i / bands.toFloat()) + width / bands.toFloat(),
                    height,
                    paintBands
                )
            }

            average /= bands
            average /= 2



            drawScope.drawLine(
                Offset(
                    0f, height - (height * (average / maxConst)) - height * .02f
                ),
                Offset(
                    width, height - (height * (average / maxConst)) - height * .02f
                ),
                paintAvg
            )

            // DRAW AUDIO VOLUME LINE CHART
            drawScope.drawPath(path, paintAudio)

        }

}


    fun onAudio(newValues: FloatArray) {
        // Processing for Audio Samples
        synchronized(audio) {
            var accum = 0f

            for ((i, sample) in newValues.withIndex()) {
                if (i > 0 && i % merge != 0) {
                    accum += sample
                } else {
                    audio.addFirst(accum / merge)
                    accum = 0f
                }
            }

            while (audio.size > history)
                audio.removeLast()
        }

    }




    fun onFFT(fft:FloatArray) {
        // Processing for FFT Samples
        synchronized(this.fft) {
            arraycopy(fft, 2, this.fft, 0, fft.size - 2)
        }

    }

}