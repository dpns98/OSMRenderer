package com.example.osmrenderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.exp

class MapRenderer(val db: DBHelper, val screenWidth: Float, val screenHeight: Float) : GLSurfaceView.Renderer {

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val RADIUS = 6378137.0

    @Volatile
    var polygons = listOf<Polygon>()
    @Volatile
    var positionX: Float = 2279683.5f
    @Volatile
    var positionY: Float = 5587355.5f
    @Volatile
    var scale: Float = 2000f

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        Matrix.setLookAtM(
            viewMatrix, 0,
            positionX, positionY,scale,
            positionX, positionY, 0f,
            0f, 1.0f, 0.0f
        )
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        getGeometriesForExtent()
        polygons.forEach {
            it.draw(vPMatrix)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 10000f)
    }

    fun y2lat(aY: Float): Float {
        return Math.toDegrees(atan(exp(aY / RADIUS)) * 2 - Math.PI / 2).toFloat()
    }

    fun x2lon(aX: Float): Float {
        return Math.toDegrees(aX / RADIUS).toFloat()
    }

    private fun getGeometriesForExtent() {
        val extent = Extent(
            x2lon(positionX - (screenWidth*scale)),
            x2lon(positionX + (screenWidth*scale)),
            y2lat(positionY - (screenHeight*scale)),
            y2lat(positionY + (screenHeight*scale))
        )
        polygons = db.getIdsForExtent(extent)
    }
}