package com.example.osmrenderer

import android.content.Context
import android.opengl.GLSurfaceView

class MapView(context: Context, db: DBHelper) : GLSurfaceView(context) {

    private val renderer: MapRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MapRenderer(db)
        setRenderer(renderer)
    }
}