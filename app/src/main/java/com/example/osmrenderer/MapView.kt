package com.example.osmrenderer

import android.content.Context
import android.opengl.GLSurfaceView

class MapView(context: Context) : GLSurfaceView(context) {

    private val renderer: MapRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MapRenderer()
        setRenderer(renderer)
    }
}