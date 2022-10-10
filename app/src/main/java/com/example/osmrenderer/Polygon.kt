package com.example.osmrenderer

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Polygon(
    val coords: FloatArray,
    private val color: FloatArray
) {
    private var positionHandle: Int = 0
    private var vPMatrixHandle: Int = 0
    private var mColorHandle: Int = 0
    private var drawOrder = shortArrayOf()
    var triangulationVertices = listOf<Float>()

    init {
        drawOrder = Triangulation.earcut(coords)
        triangulationVertices = drawOrder
            .flatMap {listOf(coords[it*2], coords[it*2+1])}
    }

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(coords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(coords)
                position(0)
            }
        }

    private val drawListBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(drawOrder.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }
        }

    fun draw(mvpMatrix: FloatArray, program: Int) {
        GLES20.glUseProgram(program)
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition").also {
            GLES20.glEnableVertexAttribArray(it)
            GLES20.glVertexAttribPointer(
                it,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * 4,
                vertexBuffer
            )

            mColorHandle = GLES20.glGetUniformLocation(program, "vColor").also { colorHandle ->
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            vPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
            GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                drawOrder.size,
                GLES20.GL_UNSIGNED_SHORT,
                drawListBuffer
            )
            GLES20.glDisableVertexAttribArray(it)
        }
    }
}