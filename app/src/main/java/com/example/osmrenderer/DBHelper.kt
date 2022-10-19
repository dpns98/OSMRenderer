package com.example.osmrenderer

import android.content.Context
import android.util.Log
import org.sqlite.database.sqlite.SQLiteDatabase
import org.sqlite.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.ln
import kotlin.math.tan

const val dbName = "SQLITE_DB"
const val dbVersionNumber = 1

class DBHelper(
    private val context: Context
) : SQLiteOpenHelper(context, dbName, null, dbVersionNumber) {

    @Volatile
    private var dataBase: SQLiteDatabase? = null

    init {
        val dbExist = checkDatabase()
        if (dbExist) {
            Log.e("-----", "Database exist")
        } else {
            Log.e("-----", "Database doesn't exist")
            createDatabase()
        }
        dataBase = SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).path, null, SQLiteDatabase.OPEN_READONLY)
    }

    override fun onCreate(db: SQLiteDatabase?) { }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) { }

    private fun createDatabase() {
        copyDatabase()
    }

    private fun checkDatabase(): Boolean {
        val dbFile = File(context.getDatabasePath(dbName).path)
        return dbFile.exists()
    }

    private fun copyDatabase() {
        val inputStream = context.assets.open("database/$dbName")
        val outputFile = File(context.getDatabasePath(dbName).path)
        val outputStream = FileOutputStream(outputFile)

        val bytesCopied = inputStream.copyTo(outputStream)
        Log.e("bytesCopied", "$bytesCopied")
        inputStream.close()

        outputStream.flush()
        outputStream.close()
    }

    fun getIdsForExtent(extent: Extent): List<Pair<FloatArray, String>>{
        val cursor = dataBase?.rawQuery(
            "select r.way_id, lon, lat, key from rtree_way r " +
                "join way_nodes w on r.way_id = w.way_id " +
                "join nodes n on w.node_id = n.node_id " +
                "join way_tags t on t.way_id = w.way_id " +
                "where min_lon < ${extent.maxX} and max_lon > ${extent.minX} " +
                "and min_lat < ${extent.maxY} and max_lat > ${extent.minY}",
            null
        )

        var currentId = -1
        var currentTag = ""
        val coords = mutableListOf<Float>()
        val arrays = mutableListOf<Pair<FloatArray, String>>()

        while (cursor!!.moveToNext()) {
            val id = cursor.getInt(0)
            val lon = cursor.getFloat(1)
            val lat = cursor.getFloat(2)
            val tag = cursor.getString(3)

            if (id != currentId) {
                if (currentId != -1)
                    arrays.add(Pair(coords.toFloatArray(), currentTag))
                currentId = id
                currentTag = tag
                coords.clear()
            }
            coords.add(lon)
            coords.add(lat)
        }
        arrays.add(Pair(coords.toFloatArray(), currentTag))

        cursor.close()
        return arrays
    }
}