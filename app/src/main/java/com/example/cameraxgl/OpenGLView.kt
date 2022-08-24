package com.example.cameraxgl

import android.graphics.SurfaceTexture
import android.content.Context

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

import com.example.cameraxgl.gpuinterface.GPUInterface


class OpenGLView(ctx: Context, val onTextureAvailableCallback: (SurfaceTexture) -> Unit) :
    GLSurfaceView(ctx), GLSurfaceView.Renderer {

    private var textureInterface: GPUInterface? = null
    private var vbuf: FloatBuffer? = null
    private var cameraFeedSurfaceTexture: SurfaceTexture? = null
    private var ibuf: ShortBuffer? = null

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
    }


    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.3f, 0.0f)
        GLES20.glClearDepthf(1.0f)

        createShapes()

        // http://stackoverflow.com/questions/6414003/using-surfacetexture-in-android
        val GL_TEXTURE_EXTERNAL_OES = 0x8d65
        val textureId = IntArray(1)
        GLES20.glGenTextures(1, textureId, 0)
        if (textureId[0] != 0) {
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId[0])
            GLES20.glTexParameteri(
                GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST
            )

            cameraFeedSurfaceTexture = SurfaceTexture(textureId[0])

            // Must negate y when calculating texcoords from vertex coords as bitmap image data assumes
            // y increases downwards
            val texVertexShader =
                "attribute vec4 aVertex;\n" +
                        "varying vec2 vTextureValue;\n" +
                        "void main (void)\n" +
                        "{\n" +
                        "gl_Position = aVertex;\n" +
                        "vTextureValue = vec2(0.5*(1.0 + aVertex.x), 0.5*(1.0-aVertex.y));\n" +
                        "}\n"
            val texFragmentShader =
                "#extension GL_OES_EGL_image_external: require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureValue;\n" +
                        "uniform samplerExternalOES uTexture;\n" +
                        "void main(void)\n" +
                        "{\n" +
                        "gl_FragColor = texture2D(uTexture,vTextureValue);\n" +
                        "}\n"
            textureInterface = GPUInterface(texVertexShader, texFragmentShader)
            com.example.cameraxgl.gpuinterface.setupTexture(textureId[0])
            textureInterface?.setUniform1i(
                "uTexture",
                0
            ) // this is the on-gpu texture register not the texture id


            Log.d("CAMERAXGL", "setting up texture available callback")
            onTextureAvailableCallback(cameraFeedSurfaceTexture!!)

        }
    }


    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        cameraFeedSurfaceTexture?.updateTexImage()
        textureInterface?.select()
        if (vbuf != null && ibuf != null) {
            textureInterface?.drawIndexedBufferedData(vbuf!!, ibuf!!, 0, "aVertex")
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    override fun onSurfaceChanged(unused: GL10, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
    }

    private fun createShapes() {

        val cameraRect = floatArrayOf(-1f, 1f, 0f, -1f, -1f, 0f, 1f, -1f, 0f, 1f, 1f, 0f)
        val indices = shortArrayOf(0, 1, 2, 2, 3, 0)

        val vbuf0 = ByteBuffer.allocateDirect(cameraRect.size * Float.SIZE_BYTES)
        vbuf0.order(ByteOrder.nativeOrder())
        vbuf = vbuf0.asFloatBuffer()
        vbuf?.apply {
            put(cameraRect)
            position(0)
        }

        val ibuf0 = ByteBuffer.allocateDirect(indices.size * Short.SIZE_BYTES)
        ibuf0.order(ByteOrder.nativeOrder())
        ibuf = ibuf0.asShortBuffer()
        ibuf?.apply {
            put(indices)
            position(0)
        }

    }

}

