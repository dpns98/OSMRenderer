package com.example.osmrenderer

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Line(
    private val coords: FloatArray,
    private val color: FloatArray
) : Geometry(coords, color) {
    private var positionHandle: Int = 0
    private var vPMatrixHandle: Int = 0
    private var mColorHandle: Int = 0
    private val buffer = IntArray(1)

    init {
        var vertexBuffer: FloatBuffer? =
            ByteBuffer.allocateDirect(coords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(coords)
                    position(0)
                }
            }

        GLES20.glGenBuffers(1, buffer, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexBuffer!!.capacity() * 4,
            vertexBuffer,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        vertexBuffer.limit(0)
        vertexBuffer = null
    }

    override fun draw(mvpMatrix: FloatArray, program: Int) {
        GLES20.glUseProgram(program)
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        mColorHandle = GLES20.glGetUniformLocation(program, "vColor").also { colorHandle ->
            GLES20.glUniform4fv(colorHandle, 1, color, 0)
        }
        vPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

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
            GLES20.GL_LINE_STRIP,
            0,
            coords.size/2
        )
    }

    override fun release() {
        GLES20.glDeleteBuffers(1, buffer, 0)
    }
}