package com.example.osmrenderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MapRenderer(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val initialX: Float,
    private val initialY: Float
) : GLSurfaceView.Renderer {

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
    var positionX: Float = initialX
    @Volatile
    var positionY: Float = initialY
    @Volatile
    var scale: Float = 8000f
    @Volatile
    var arrays = listOf<Triple<FloatArray, String, IntArray?>>()
    private var geometries: List<Geometry> = listOf()

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.87059f, 0.87059f, 0.87059f, 1.0f)
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            GLES20.glUseProgram(it)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
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
            positionX, positionY, scale,
            positionX, positionY, 0f,
            0f, 1.0f, 0.0f
        )
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glLineWidth(100000/scale)

        if (create) {
            geometries.forEach {
                it.release()
            }
            geometries = createGeometries()
        }
        geometries.forEach{
            if (it.isPath()){
                GLES20.glLineWidth(10000/scale)
                it.draw(vPMatrix, mProgram)
                GLES20.glLineWidth(100000/scale)
            }
            else
                it.draw(vPMatrix, mProgram)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        Matrix.frustumM(
            projectionMatrix, 0,
            -screenWidth, screenWidth, -screenHeight, screenHeight,
            1f, 20000f
        )
    }

    private fun createGeometries(): List<Geometry> {
        val newGeometries = mutableListOf<Geometry>()
        arrays.forEach {
            newGeometries.add(
                if (it.second in listOf("highway", "boundary", "path", "way", "motorway", "street")) {
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
            "boundary" -> floatArrayOf(0.7f, 0.4f, 1f, 1.0f)
            in listOf("building", "man_made") -> floatArrayOf(0.6f, 0.6f, 0.6f, 1.0f)
            "motorway" -> floatArrayOf(1f, 0.7f, 0.4f, 1.0f)
            "street" -> floatArrayOf(1.00000f, 0.94902f, 0.40000f, 1.0f)
            "highway" -> floatArrayOf(1f, 1f, 1f, 1.0f)
            "way" -> floatArrayOf(0.96078f, 0.54118f, 0.67843f, 1.0f)
            "path" -> floatArrayOf(0.96078f, 0.54118f, 0.67843f, 1.0f)
            in listOf("beach", "sand") -> floatArrayOf(1.00000f, 1.00000f, 0.70196f, 1.0f)
            "water" -> floatArrayOf(0.30196f, 0.65098f, 1.00000f, 1.0f)
            in listOf("cemetery", "natural", "forest") -> floatArrayOf(0.0f, 0.6f, 0.0f, 1.0f)
            in listOf("farmland", "meadow", "orchard", "pitch", "track") -> floatArrayOf(0.22353f, 0.67451f, 0.45098f, 1.0f)
            in listOf("leisure", "flowerbed", "grass", "recreation_ground") -> floatArrayOf(0.43922f, 0.85882f, 0.43922f, 1.0f)
            in listOf("commercial", "residential", "retail") -> floatArrayOf(0.92549f, 0.92549f, 0.79608f, 1.0f)
            in listOf("industrial", "garages", "construction") -> floatArrayOf(0.80000f, 0.78039f, 0.83922f, 1.0f)
            "landuse" -> floatArrayOf(0.87059f, 0.87059f, 0.87059f, 1.0f)
            else -> floatArrayOf(0.87059f, 0.87059f, 0.87059f, 1.0f)
        }
    }
}