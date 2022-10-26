package com.example.osmrenderer

abstract class Geometry(
    private val coords: FloatArray,
    private val color: FloatArray
) {
    abstract fun draw(mvpMatrix: FloatArray, program: Int)
    abstract fun release()
    fun isPath(): Boolean {
        return color[0] == 0.96078f && color[1] == 0.54118f && color[2] == 0.67843f
    }
}