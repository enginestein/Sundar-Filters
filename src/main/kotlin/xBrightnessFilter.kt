package com.aryavart.sundar

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.renderscript.*
import androidx.renderscript.ScriptGroup


class xBrightnessFilter(private val context: Context)   {
    private val TAG = xBrightnessFilter::class.java.simpleName
    private var rs: RenderScript = RenderScript.create(context)
    private var brightnessScript: com.android.demoeditor.ScriptC_brightness =
        com.android.demoeditor.ScriptC_brightness(rs)

    private var contrastScript: com.android.demoeditor.ScriptC_contrast =
        com.android.demoeditor.ScriptC_contrast(rs)

    private var saturationFilter: com.android.demoeditor.ScriptC_saturation =
        com.android.demoeditor.ScriptC_saturation(rs)

    private var convolution: ScriptIntrinsicConvolve3x3 =
        ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))

    private var vignetteFilter: VignetteFilterxxxxx = VignetteFilterxxxxx(rs)
    private var vignetteFilterOriginal: com.android.demoeditor.ScriptC_vignette =
        com.android.demoeditor.ScriptC_vignette(rs)
    private var builder: ScriptGroup.Builder2 = ScriptGroup.Builder2(rs)

    private lateinit var scriptGroup: ScriptGroup

    private lateinit var brightClosure: ScriptGroup.Closure
    private lateinit var contrastClosure: ScriptGroup.Closure
    private lateinit var saturationClosure: ScriptGroup.Closure
    private lateinit var sharpenClosure: ScriptGroup.Closure
    private lateinit var vignetteClosure: ScriptGroup.Closure
    private lateinit var inAllocation: Allocation
    private lateinit var mOutputImages: Array<Bitmap>
    private lateinit var mOutAllocations: Array<Allocation>
    private lateinit var xxOutAllocations: Array<Allocation>

    private lateinit var newBitmap: Bitmap


    private lateinit var xxxOutAllocation: Allocation


      fun configureInputAndOutput(inputImage: Bitmap, numberOfOutputImages: Int) {
        if (numberOfOutputImages <= 0) {
            throw RuntimeException("Invalid number of output images: $numberOfOutputImages")
        }

        inAllocation = Allocation.createFromBitmap(
            rs,
            inputImage,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT or Allocation.USAGE_SHARED
        )

        mOutputImages = Array(numberOfOutputImages) {
            Bitmap.createBitmap(inputImage.width, inputImage.height, inputImage.config)
        }
        mOutAllocations =
            Array(numberOfOutputImages) { i ->
                Allocation.createFromBitmap(
                    rs,
                    mOutputImages[i],
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT or Allocation.USAGE_SHARED
                )
            }

        xxOutAllocations =
            Array(numberOfOutputImages) { Allocation.createTyped(rs, inAllocation.type) }
        newBitmap = inputImage.copy(inputImage.config, true)


        initRenderScriptFilterGroups(numberOfOutputImages)
//


        Log.i(TAG, " configureInputAndOutput() is execution finished")
    }


    private fun initRenderScriptFilterGroups(numberOfOutputImages: Int) {
        val unbound: ScriptGroup.Input? = builder.addInput()

        val type = inAllocation.type

        brightClosure =
            builder.addKernel(brightnessScript.kernelID_brightness, type, unbound)

        contrastClosure =
            builder.addKernel(
                contrastScript.kernelID_contrastness,
                type,
                brightClosure.`return`
            )
        saturationClosure =
            builder.addKernel(
                saturationFilter.kernelID_saturation,
                type,
                contrastClosure.`return`
            )

        vignetteClosure =
            builder.addKernel(
                vignetteFilterOriginal.kernelID_root,
                type,
                saturationClosure.`return`
            )
        // builder.addKernel(vignetteFilterOriginal.filter.kernelID_root, inAllocation.type)


        scriptGroup = builder.create(
            "xyz",
            vignetteClosure.`return`
        )


    }


      fun setBrightness(bright: Float, outputIndex: Int): Bitmap {

        Log.i(TAG, "setBrightness: bright-$bright , outputIndex-$outputIndex")

        brightnessScript.invoke_setBright(bright)
        brightnessScript.forEach_brightness(inAllocation, mOutAllocations[outputIndex])
        mOutAllocations[outputIndex].copyTo(mOutputImages[outputIndex])
        return mOutputImages[outputIndex]
    }


      fun setContrast(contrast: Float, outputIndex: Int): Bitmap {
        Log.i(TAG, "setContrast: contrast:$contrast-outputIndex:$outputIndex")
        contrastScript._contrast = contrast
        contrastScript.forEach_contrastness(inAllocation, mOutAllocations[outputIndex])
        mOutAllocations[outputIndex].copyTo(mOutputImages[outputIndex])
        return mOutputImages[outputIndex]
    }

    fun setSaturation(saturation: Float, outputIndex: Int): Bitmap {
        Log.i(TAG, "setContrast: saturation:$saturation-outputIndex:$outputIndex")

        saturationFilter._saturationValue = saturation

        saturationFilter.forEach_saturation(inAllocation, mOutAllocations[outputIndex])
        mOutAllocations[outputIndex].copyTo(mOutputImages[outputIndex])
        return mOutputImages[outputIndex]
    }


    fun setVignette(vignette: Float, outputIndex: Int): Bitmap {
        val center_x = 0.5f
        val center_y = 0.5f
        val scale = vignette
        val shade = 0.5f
        val slope = 7.0f

        vignetteFilterOriginal.invoke_init_vignette(
            inAllocation.type.x.toLong(),
            inAllocation.type.y.toLong(),
            center_x,
            center_y,
            scale,
            shade,
            slope
        )

        vignetteFilterOriginal.forEach_root(inAllocation, mOutAllocations[outputIndex])
        mOutAllocations[outputIndex].copyTo(mOutputImages[outputIndex])
        return mOutputImages[outputIndex]


    }


    fun setSharpen(sharpen: Float, outputIndex: Int): Bitmap {
        val matrix = floatArrayOf(
            0f, -sharpen, 0f,
            -sharpen, 1 + 4 * sharpen, -sharpen,
            0f, -sharpen, 0f
        )

        return applySharpenConvolve(matrix, outputIndex)
    }


    private fun applySharpenConvolve(coefficients: FloatArray, outputIndex: Int): Bitmap {
        convolution.setInput(inAllocation)
        convolution.setCoefficients(coefficients)
        convolution.forEach(mOutAllocations[outputIndex])

        mOutAllocations[outputIndex].copyTo(mOutputImages[outputIndex])
        return mOutputImages[outputIndex]

    }


    fun setBrightnessX(values: Float, inputImage: Bitmap): Bitmap {
        val outBitmap = Bitmap.createBitmap(inputImage)
        val tempIn = Allocation.createFromBitmap(rs, inputImage)
        val tempOut = Allocation.createFromBitmap(rs, outBitmap)

        brightnessScript.invoke_setBright(values)
        brightnessScript.forEach_brightness(tempIn, tempOut)

        tempOut.copyTo(outBitmap)
        return outBitmap

    }

    fun setContrastX(contrast: Float, inputImage: Bitmap): Bitmap {
        val outBitmap = Bitmap.createBitmap(inputImage)
        val tempIn = Allocation.createFromBitmap(rs, inputImage)
        val tempOut = Allocation.createFromBitmap(rs, outBitmap)

        contrastScript._contrast = contrast
        contrastScript.forEach_contrastness(tempIn, tempOut)

        tempOut.copyTo(outBitmap)
        tempIn.destroy()
        tempOut.destroy()
        return outBitmap

    }

    fun setSaturationX(saturation: Float, inputImage: Bitmap): Bitmap {
        val outBitmap = Bitmap.createBitmap(inputImage)
        val tempIn = Allocation.createFromBitmap(rs, inputImage)
        val tempOut = Allocation.createFromBitmap(rs, outBitmap)

        saturationFilter._saturationValue = saturation
        saturationFilter.forEach_saturation(tempIn, tempOut)

        tempOut.copyTo(outBitmap)
        tempIn.destroy()
        tempOut.destroy()
        return outBitmap

    }


    fun sharpenX(sharpen: Float, bitmap: Bitmap): Bitmap {

        val matrix = floatArrayOf(
            0f, -sharpen, 0f,
            -sharpen, 1 + 4 * sharpen, -sharpen,
            0f, -sharpen, 0f
        )

        return applySharpenConvolveX(bitmap, matrix)
    }


    private fun applySharpenConvolveX(inputImage: Bitmap, coefficients: FloatArray): Bitmap {
        val outBitmap = Bitmap.createBitmap(inputImage)
        val tempIn = Allocation.createFromBitmap(rs, inputImage)
        val tempOut = Allocation.createFromBitmap(rs, outBitmap)

        convolution.setInput(tempIn)
        convolution.setCoefficients(coefficients)
        convolution.forEach(tempOut)

        tempOut.copyTo(outBitmap)
        return outBitmap

    }


    fun vignetteX(values: Float, inputImage: Bitmap): Bitmap {

        vignetteFilter.setScale(values)
        return vignetteFilter.x(inputImage)

    }


    fun destroyAllocation() {
//        inAllocation.destroy()
//        mOutAllocations.forEach {
//            it.destroy()
//            Log.i("$TAG=========>", "mOutAllocations is Destroyed: $it")
//        }
    }


      fun destroy() {
//        rs.destroy()
//        brightnessScript.destroy()
//        contrastScript.destroy()
//        Log.i(TAG, "destroy: ${saturationFilter==null}")
//       // saturationFilter.destroy()
//        destroyAllocation()

    }

    fun brightnessWithGroup(bright: Float, outputIndex: Int): Bitmap {
        brightnessScript.invoke_setBright(bright)
        val result = scriptGroup.execute(inAllocation)

        return mOutputImages[outputIndex]
    }

    fun contrastWithGroup(contrast: Float, outputIndex: Int): Bitmap {
        contrastScript._contrast = contrast
        val output = scriptGroup.execute(inAllocation)[0]
        val outAlloc = Allocation.createTyped(rs, inAllocation.type)
        outAlloc.copyTo(output)

        return mOutputImages[outputIndex]

    }


    fun executeAll(
        bright: Float,
        contrast: Float,
        saturation: Float,
        sharpen: Float,
        vignette: Float,
        outputIndex: Int
    ): Bitmap {

        Log.w(TAG + "--->>>", "Called execute")


        val matrix = floatArrayOf(
            0f, -sharpen, 0f,
            -sharpen, 1 + 4 * sharpen, -sharpen,
            0f, -sharpen, 0f
        )

        brightnessScript.invoke_setBright(bright)


        contrastScript._contrast = contrast

        saturationFilter._saturationValue = saturation


        convolution.setCoefficients(matrix)
        val center_x = 0.5f
        val center_y = 0.5f
        val scale = vignette
        val shade = 0.5f

        val slope = 7.0f

        vignetteFilterOriginal.invoke_init_vignette(
            inAllocation.type.x.toLong(),
            inAllocation.type.y.toLong(),
            center_x,
            center_y,
            scale,
            shade,
            slope
        )


        val outAllocation = scriptGroup.execute(inAllocation)[0] as Allocation
        outAllocation.copyTo(newBitmap)
        Log.e("$TAG - OutPutIndex <----->", "$outputIndex")
        return newBitmap
    }
}