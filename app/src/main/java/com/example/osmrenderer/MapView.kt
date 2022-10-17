package com.example.osmrenderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.sqrt

class MapView(context: Context, val db: DBHelper) : GLSurfaceView(context) {

    private val renderer: MapRenderer
    private val density = (resources.displayMetrics.density * 160)
    private val screenWidth = (resources.displayMetrics.widthPixels / density) * 0.065f
    private val screenHeight = (resources.displayMetrics.heightPixels / density) * 0.065f

    init {
        setEGLContextClientVersion(2)
        renderer = MapRenderer(db, screenWidth, screenHeight)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    var prevX = 0.0f
    var prevY = 0.0f
    var prevX1 = 0.0f
    var prevY1 = 0.0f
    var zooming = false

    override fun onTouchEvent(e: MotionEvent): Boolean {

        val x = e.getX(0)
        val y = e.getY(0)
        var x1 = 0f
        var y1 = 0f
        if (e.pointerCount > 1) {
            x1 = e.getX(1)
            y1 = e.getY(1)
        }

        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                zooming = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!zooming) {
                    renderer.positionX += (prevX - x) * 0.0003f * renderer.scale
                    renderer.positionY += (y - prevY) * 0.0003f * renderer.scale
                } else if (e.pointerCount > 1) {
                    val d1 = sqrt((prevX-prevX1) * (prevX-prevX1) + (prevY-prevY1) * (prevY-prevY1))
                    val d2 = sqrt((x-x1) * (x-x1) + (y-y1) * (y-y1))
                    val ratio = d1/d2

                    if (renderer.scale * ratio > 3f && renderer.scale * ratio < 100000f) {
                        renderer.scale *= ratio
                    }
                }
                requestRender()
            }
            MotionEvent.ACTION_UP -> {
                zooming = false
            }
        }

        prevX = x
        prevY = y
        if (e.pointerCount > 1) {
            prevX1 = x1
            prevY1 = y1
        }
        return true
    }
}