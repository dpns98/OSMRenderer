package com.example.osmrenderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MapRenderer : GLSurfaceView.Renderer {

    private lateinit var mTriangle: Polygon

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        mTriangle.draw()
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        mTriangle = Polygon(
            floatArrayOf(
                -0.2f,  0.5f,
                -0.2f, -0.5f,
                0.5f, -0.5f,
                0.5f, 0.0f,
                0.2f, 0.0f,
                0.2f,  0.5f,
            ),
            floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)
        )
    }
}