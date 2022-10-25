package com.example.osmrenderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.exp

class MapRenderer: GLSurfaceView.Renderer {

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private var mProgram: Int = 0
    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    @Volatile
    var create = true
    @Volatile
    var positionX: Float = 2272360f
    @Volatile
    var positionY: Float = 5590777f
    @Volatile
    var scale: Float = 2000f
    @Volatile
    var arrays = listOf<Triple<FloatArray, String, IntArray?>>()
    private var geometries: List<Geometry> = listOf()

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        Matrix.setLookAtM(
            viewMatrix, 0,
            positionX, positionY,scale,
            positionX, positionY, 0f,
            0f, 1.0f, 0.0f
        )
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        if (create) {
            geometries.forEach {
                it.release()
            }
            geometries = createGeometries()
        }
        geometries.forEach{
            it.draw(vPMatrix, mProgram)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 10000f)
    }

    private fun createGeometries(): List<Geometry> {
        val newGeometries = mutableListOf<Geometry>()
        arrays.forEach {
            newGeometries.add(
                if (it.second in listOf("highway", "boundary")) {
                    Line(
                        it.first,
                        getTagColor(it.second)
                    )
                } else {
                    Polygon(
                        it.first,
                        getTagColor(it.second),
                        it.third
                    )
                }
            )
        }
        return newGeometries
    }

    private fun getTagColor(tag: String): FloatArray {
        return when(tag) {
            "building" -> floatArrayOf(0.6f, 0.6f, 0.6f, 1.0f)
            "highway" -> floatArrayOf(1f, 0.7f, 0.4f, 1.0f)
            "natural" -> floatArrayOf(0.4f, 1f, 0.4f, 1.0f)
            "landuse" -> floatArrayOf(0f, 0.5f, 1f, 1.0f)
            "boundary" -> floatArrayOf(0.7f, 0.4f, 1f, 1.0f)
            else -> floatArrayOf(0.6f, 0.7f, 0.2f, 1.0f)
        }
    }
}