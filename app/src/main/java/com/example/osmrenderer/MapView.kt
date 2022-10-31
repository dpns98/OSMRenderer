package com.example.osmrenderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import kotlinx.coroutines.*
import kotlin.math.sqrt

class MapView(context: Context, val db: DBHelper) : GLSurfaceView(context) {

    private val renderer: MapRenderer
    private val metrics = resources.displayMetrics
    private val screenWidth = (metrics.widthPixels.toFloat() / metrics.xdpi) * 0.5f * 0.0254f * 1.4f
    private val screenHeight = (metrics.heightPixels.toFloat() / metrics.ydpi) * 0.5f * 0.0254f * 1.4f

    init {
        setEGLContextClientVersion(2)
        renderer = MapRenderer(screenWidth, screenHeight)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        GlobalScope.launch(Dispatchers.IO) {
            getGeometriesForExtent()
        }
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
                    renderer.positionX += ((prevX - x)/metrics.xdpi) * renderer.scale * 0.0254f * 1.4f
                    renderer.positionY += ((y - prevY)/metrics.ydpi) * renderer.scale * 0.0254f * 1.4f
                } else if (e.pointerCount > 1) {
                    val d1 = sqrt((prevX-prevX1) * (prevX-prevX1) + (prevY-prevY1) * (prevY-prevY1))
                    val d2 = sqrt((x-x1) * (x-x1) + (y-y1) * (y-y1))
                    val ratio = d1/d2

                    if (renderer.scale * ratio > 1000f && renderer.scale * ratio < 20000f) {
                        renderer.scale *= ratio
                    }
                }
                renderer.create = false
                requestRender()
            }
            MotionEvent.ACTION_UP -> {
                zooming = false
                GlobalScope.launch(Dispatchers.IO) {
                    getGeometriesForExtent()
                }
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

    private fun getGeometriesForExtent() {
        val extent = Extent(
            renderer.positionX - (screenWidth*renderer.scale),
            renderer.positionX + (screenWidth*renderer.scale),
            renderer.positionY - (screenHeight*renderer.scale),
            renderer.positionY + (screenHeight*renderer.scale)
        )
        renderer.arrays = db.getIdsForExtent(extent, if (renderer.scale <= 8000) 1 else 2)
        renderer.create = true
        requestRender()
    }
}