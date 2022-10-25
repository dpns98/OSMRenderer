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

    fun getIdsForExtent(extent: Extent, scale: Int): List<Pair<FloatArray, String>>{
        var cursor = dataBase?.rawQuery(
            "select r.way_id, lon, lat, key, value from rtree_way$scale r " +
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
            val value = cursor.getString(4)

            if (cursor.isFirst) {
                currentId = id
                currentTag = tag
            }

            if (cursor.isLast) {
                coords.add(lon)
                coords.add(lat)
            }

            if (id != currentId || cursor.isLast) {
                arrays.add(Pair(coords.toFloatArray(), currentTag))
                currentId = id
                currentTag = tag
                coords.clear()
            }
            coords.add(lon)
            coords.add(lat)
        }
        cursor.close()

        cursor = dataBase?.rawQuery(
            "select r.relation_id, lon, lat, key, value, role, w.way_id from rtree_relation r " +
                "join relation_members m on m.relation_id = r.relation_id " +
                "join way_nodes w on m.ref = w.way_id " +
                "join nodes n on w.node_id = n.node_id " +
                "join relation_tags t on t.relation_id = r.relation_id " +
                "where min_lon < ${extent.maxX} and max_lon > ${extent.minX} " +
                "and min_lat < ${extent.maxY} and max_lat > ${extent.minY}",
            null
        )

        var currentWay = -1
        currentId = -1
        currentTag = ""
        var currentRole = ""
        val currentCoordsWay = mutableListOf<Pair<Float, Float>>()
        val coordsWay = mutableListOf<Pair<Float, Float>>()
        val outer = mutableListOf<List<Pair<Float, Float>>>()
        val inner = mutableListOf<List<Pair<Float, Float>>>()

        while (cursor!!.moveToNext()) {
            val id = cursor.getInt(0)
            val lon = cursor.getFloat(1)
            val lat = cursor.getFloat(2)
            val tag = cursor.getString(3)
            val value = cursor.getString(4)
            val role = cursor.getString(5)
            val way = cursor.getInt(6)

            if (cursor.isFirst) {
                currentRole = role
                currentId = id
                currentTag = tag
                currentWay = way
            }

            if (cursor.isLast)
                coordsWay.add(Pair(lon, lat))

            if ((way != currentWay && id == currentId) || id != currentId || cursor.isLast) {
                if (currentCoordsWay.isEmpty())
                    currentCoordsWay.addAll(coordsWay)
                else if (currentCoordsWay.last() == coordsWay.first())
                    currentCoordsWay.addAll(coordsWay.subList(1, coordsWay.size))
                else if (currentCoordsWay.last() == coordsWay.last())
                    currentCoordsWay.addAll(coordsWay.reversed().subList(1, coordsWay.size))
                else if (currentCoordsWay.first() == coordsWay.first())
                    currentCoordsWay.addAll(0, coordsWay.reversed().subList(0, coordsWay.lastIndex))
                else if (currentCoordsWay.first() == coordsWay.last())
                    currentCoordsWay.addAll(0, coordsWay.subList(0, coordsWay.lastIndex))

                if (currentCoordsWay.first() == currentCoordsWay.last()){
                    if (currentRole == "inner")
                        inner.add(currentCoordsWay.toList())
                    else
                        outer.add(currentCoordsWay.toList())
                    currentCoordsWay.clear()
                }
                currentWay = way
                currentRole = role
                coordsWay.clear()
            }

            if (id != currentId || cursor.isLast){
                if (inner.size == 0 && outer.size > 0) {
                    outer.forEach {
                        arrays.add(Pair(
                            it.flatMap { e -> listOf(e.first, e.second) }.toFloatArray(),
                            currentTag
                        ))
                    }
                }
                inner.clear()
                outer.clear()
                currentCoordsWay.clear()
                currentId = id
                currentTag = tag
            }

            coordsWay.add(Pair(lon, lat))
        }

        cursor.close()
        return arrays
    }
}