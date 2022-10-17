package com.example.osmrenderer

abstract class Geometry(
    private val coords: FloatArray,
    private val color: FloatArray
) {
    abstract fun draw(mvpMatrix: FloatArray, program: Int)
    abstract fun release()
}