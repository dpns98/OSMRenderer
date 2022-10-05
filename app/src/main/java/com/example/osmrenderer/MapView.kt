package com.example.osmrenderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent

class MapView(context: Context, db: DBHelper) : GLSurfaceView(context) {

    private val renderer: MapRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MapRenderer(db)
        setRenderer(renderer)
    }

    var downX = 0.0f
    var downY = 0.0f

    override fun onTouchEvent(e: MotionEvent): Boolean {

        val x: Float = e.x
        val y: Float = e.y

        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                renderer.positionX += (downX - x) * 0.3f
                renderer.positionY += (-downY + y) * 0.3f
                requestRender()
            }
            MotionEvent.ACTION_UP -> {
                renderer.velocityX += (downX - x)
                renderer.velocityY += (-downY + y)
            }
        }

        downX = x
        downY = y
        return true
    }
}