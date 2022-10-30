package com.example.osmrenderer

import android.content.Context
import android.util.Log
import org.sqlite.database.sqlite.SQLiteDatabase
import org.sqlite.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.ln
import kotlin.math.tan

const val dbName = "SQLITE_DB"
const val dbVersionNumber = 1

class DBHelper(
    private val context: Context
) : SQLiteOpenHelper(context, dbName, null, dbVersionNumber) {

    @Volatile
    private var dataBase: SQLiteDatabase? = null
    private val valueTags = listOf(
        "water", "sand", "beach", "cemetery", "commercial", "bridge",
        "residential", "retail", "farmland", "industrial", "forest", "meadow", "flowerbed",
        "grass", "orchard", "garages", "construction", "recreation_ground", "pitch", "track",
    )

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

    fun getIdsForExtent(extent: Extent, scale: Int): List<Triple<FloatArray, String, IntArray?>>{
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        Log.e("call", sdf.format(Date()))
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
        val arrays = mutableListOf<Triple<FloatArray, String, IntArray?>>()

        while (cursor!!.moveToNext()) {
            val id = cursor.getInt(0)
            val lon = cursor.getFloat(1)
            val lat = cursor.getFloat(2)
            var tag = cursor.getString(3)
            val value = cursor.getString(4)

            if (value in valueTags && tag != "highway")
                tag = value

            if (value in listOf("footway", "bridleway", "steps", "path", "corridor", "cycleway"))
                tag = "path"

            if (tag == "highway" && value in listOf("pedestrian", "service", "track"))
                tag = "way"

            if (tag == "highway" && value in listOf("motorway", "motorway_link"))
                tag = "motorway"

            if (tag == "highway" && value in listOf("primary", "primary_link", "secondary", "secondary_link"))
                tag = "street"

            if (value in listOf("proposed", "construction", "elevator"))
                continue

            if (cursor.isFirst) {
                currentId = id
                currentTag = tag
            }

            if (cursor.isLast) {
                coords.add(lon)
                coords.add(lat)
            }

            if (id != currentId || cursor.isLast) {
                if ((currentTag != "man_made" || isPolygon(coords)) &&
                    (currentTag != "way" || !isPolygon(coords)) &&
                    (scale != 2 || currentTag !in listOf("building", "man_made")))
                    arrays.add(Triple(coords.toFloatArray(), currentTag, null))
                currentId = id
                currentTag = tag
                coords.clear()
            }
            coords.add(lon)
            coords.add(lat)
        }
        cursor.close()

        Log.e("query1", sdf.format(Date()))

        cursor = dataBase?.rawQuery(
            "select r.relation_id, lon, lat, key, value, role, w.way_id from rtree_relation$scale r " +
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
        val outer = mutableListOf<MutableList<Pair<Float, Float>>>()
        val inner = mutableListOf<List<Pair<Float, Float>>>()

        while (cursor!!.moveToNext()) {
            val id = cursor.getInt(0)
            val lon = cursor.getFloat(1)
            val lat = cursor.getFloat(2)
            var tag = cursor.getString(3)
            val value = cursor.getString(4)
            val role = cursor.getString(5)
            val way = cursor.getInt(6)

            if (value in valueTags)
                tag = value

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
                        outer.add(currentCoordsWay.toList() as MutableList<Pair<Float, Float>>)
                    currentCoordsWay.clear()
                }
                currentWay = way
                currentRole = role
                coordsWay.clear()
            }

            if (id != currentId || cursor.isLast){
                outer.forEach { out ->
                    val maxX = out.map { it.first }.max()
                    val maxY = out.map { it.second }.max()
                    val minX = out.map { it.first }.min()
                    val minY = out.map { it.second }.min()
                    val holes = mutableListOf<Int>()
                    inner.forEach { inn ->
                        if (maxX >= inn.map { it.first }.max() &&
                            maxY >= inn.map { it.second }.max() &&
                            minX <= inn.map { it.first }.min() &&
                            minY <= inn.map { it.second }.min()
                        ) {
                            holes.add(out.size)
                            out.addAll(inn)
                        }
                    }
                    if (scale != 2 || currentTag !in listOf("building", "man_made")) {
                        arrays.add(
                            Triple(
                                out.flatMap { e -> listOf(e.first, e.second) }.toFloatArray(),
                                currentTag,
                                if (holes.isEmpty()) null else holes.toIntArray()
                            )
                        )
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


        Log.e("query2", sdf.format(Date()))

        cursor.close()
        arrays.sortBy { tagSort(it.second) }
        Log.e("sort", sdf.format(Date()))
        return arrays
    }

    private fun isPolygon(coords: List<Float>): Boolean {
        return coords[0] == coords[coords.size-2] && coords[1] == coords[coords.lastIndex]
    }

    private fun tagSort(tag: String): Int{
        return when(tag){
            "boundary" -> 17
            in listOf("building", "man_made") -> 16
            "motorway" -> 15
            "street" -> 14
            "highway" -> 13
            "way" -> 12
            "path" -> 11
            "bridge" -> 10
            in listOf("beach", "sand") -> 9
            "water" -> 8
            in listOf("cemetery", "natural", "forest") -> 7
            in listOf("farmland", "meadow", "orchard", "pitch", "track") -> 6
            in listOf("leisure", "flowerbed", "grass", "recreation_ground") -> 5
            in listOf("commercial", "residential", "retail") -> 4
            in listOf("industrial", "garages", "construction") -> 3
            "landuse" -> 2
            else -> 1
        }
    }
}