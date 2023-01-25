package com.example.osmrenderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlinx.coroutines.*
import kotlin.math.cos
import kotlin.math.sqrt

class MapView(context: Context, private val db: DBHelper) : GLSurfaceView(context) {

    private val renderer: MapRenderer
    private val metrics = resources.displayMetrics
    private var pixels2meters = 0.0f
    //half width and half height of screen
    private var screenWidth = metrics.widthPixels.toFloat() / 2f
    private var screenHeight = metrics.heightPixels.toFloat() / 2f

    init {
        setEGLContextClientVersion(2)
        val initialPosition = db.getInitialPosition()
        //distortion from cylindrical projection https://en.wikipedia.org/wiki/Mercator_projection#Scale_factor
        val distortion = cos(Math.toRadians(initialPosition.first.toDouble()))
        //converting pixels to meters by dividing with pixel density per inch and distortion and multiplying with number of meters in inch
        pixels2meters = 0.0254f / (metrics.xdpi * distortion).toFloat()
        screenHeight *= pixels2meters
        screenWidth *= pixels2meters

        renderer = MapRenderer(
            screenWidth,
            screenHeight,
            initialPosition.first,
            initialPosition.second
        )

        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        GlobalScope.launch(Dispatchers.IO) {
            getGeometriesForExtent()
        }
    }

    //previous touch pixel coordinates
    var prevX = 0.0f
    var prevY = 0.0f
    var prevX1 = 0.0f
    var prevY1 = 0.0f
    var zooming = false

    override fun onTouchEvent(e: MotionEvent): Boolean {

        //touch pixel coordinates
        val x = e.getX(0)
        val y = e.getY(0)
        var x1 = 0f
        var y1 = 0f
        //touch with 2 fingers
        if (e.pointerCount > 1) {
            x1 = e.getX(1)
            y1 = e.getY(1)
        }

        when (e.action and MotionEvent.ACTION_MASK) {
            //2 fingers
            MotionEvent.ACTION_POINTER_DOWN -> {
                zooming = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!zooming) {
                    //translate camera
                    renderer.positionX += (prevX - x) * renderer.scale * pixels2meters
                    renderer.positionY += (y - prevY) * renderer.scale * pixels2meters
                } else if (e.pointerCount > 1) {
                    //scale and translate camera around midpoint between 2 finger pixel coordinates
                    val d1 = sqrt((prevX-prevX1) * (prevX-prevX1) + (prevY-prevY1) * (prevY-prevY1))
                    val d2 = sqrt((x-x1) * (x-x1) + (y-y1) * (y-y1))
                    val ratio = d1/d2

                    val xOffset = (metrics.widthPixels/2 - (x+x1)/2) * pixels2meters * renderer.scale*(1-ratio)
                    val yOffset = (metrics.heightPixels/2 - (y+y1)/2) * pixels2meters * renderer.scale*(1-ratio)

                    if (renderer.scale * ratio > 1000f && renderer.scale * ratio < 20000f) {
                        renderer.scale *= ratio
                        renderer.positionX -= xOffset
                        renderer.positionY += yOffset
                    }
                }
                renderer.create = false
                requestRender()
            }
            MotionEvent.ACTION_UP -> {
                zooming = false
                //on finger up query db
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
        //get data for screen bounding box
        val extent = Extent(
            renderer.positionX - (screenWidth*renderer.scale),
            renderer.positionX + (screenWidth*renderer.scale),
            renderer.positionY - (screenHeight*renderer.scale),
            renderer.positionY + (screenHeight*renderer.scale)
        )
        renderer.arrays = db.getGeometriesForExtent(extent, if (renderer.scale <= 8000) 1 else 2)
        renderer.create = true
        requestRender()
    }

    fun searchNearest(value: String, k: Int) {
        renderer.knnArrays = db.knn(renderer.positionX, renderer.positionY, k, value)
        requestRender()
    }

    fun cancelKNN() {
        renderer.knnArrays = emptyList()
        requestRender()
    }
}