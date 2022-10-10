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

class MapRenderer(val db: DBHelper, val screenWidth: Float, val screenHeight: Float) : GLSurfaceView.Renderer {

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val buffer = IntArray(1)
    private var trianglesNum = 0

    private var positionHandle: Int = 0
    private var vPMatrixHandle: Int = 0
    private var mColorHandle: Int = 0

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

    private val RADIUS = 6378137.0

    @Volatile
    var positionX: Float = 2279683.5f
    @Volatile
    var positionY: Float = 5587355.5f
    @Volatile
    var scale: Float = 2000f

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

        GLES20.glDeleteBuffers(1, buffer, 0)

        getGeometriesForExtent()

        GLES20.glUseProgram(mProgram)
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->
            GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f), 0)
        }
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, vPMatrix, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0])
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            0
        )
        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLES,
            0,
            trianglesNum
        )
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
        val triangles = db.getIdsForExtent(extent)
        trianglesNum = triangles.size
        var vertexBuffer: FloatBuffer? =
            ByteBuffer.allocateDirect(triangles.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(triangles)
                    position(0)
                }
            }

        GLES20.glGenBuffers(1, buffer, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer!!.capacity()*4, vertexBuffer, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        vertexBuffer.limit(0)
        vertexBuffer = null
    }
}