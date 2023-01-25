package com.example.osmrenderer

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var gLView: MapView
    private var value = "school"
    private var k = 1
    private lateinit var mMenu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        System.loadLibrary("sqliteX")
        val db = DBHelper(this)

        gLView = MapView(this, db)
        setContentView(gLView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        mMenu = menu

        val vSpinner = menu.findItem(R.id.value).actionView as Spinner
        val vList = listOf("school", "university", "hospital", "hotel")
        val vAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vList)
        vAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vSpinner.adapter = vAdapter
        vSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, id: Long) {
                value = vList[position]
            }
            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        val kSpinner = menu.findItem(R.id.k).actionView as Spinner
        val kList = listOf(1, 2, 3, 4, 5)
        val kAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kList)
        kAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        kSpinner.adapter = kAdapter
        kSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, id: Long) {
                k = kList[position]
            }
            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.search) {
            gLView.searchNearest(value, k)
            mMenu.findItem(R.id.cancel).isVisible = true
            mMenu.findItem(R.id.search).isVisible = false
        }
        if (id == R.id.cancel) {
            gLView.cancelKNN()
            mMenu.findItem(R.id.cancel).isVisible = false
            mMenu.findItem(R.id.search).isVisible = true
        }
        return super.onOptionsItemSelected(item)
    }
}