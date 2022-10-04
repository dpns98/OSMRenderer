package com.example.osmrenderer

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var gLView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gLView = MapView(this)
        setContentView(gLView)

        System.loadLibrary("sqliteX")

        val db = DBHelper(this)
        db.getNode()
    }
}