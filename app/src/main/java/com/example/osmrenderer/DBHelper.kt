package com.example.osmrenderer

import android.content.Context
import android.util.Log
import org.sqlite.database.sqlite.SQLiteDatabase
import org.sqlite.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream

const val dbName = "SQLITE_DB"
const val dbVersionNumber = 1

class DBHelper(private val context: Context) : SQLiteOpenHelper(context, dbName, null, dbVersionNumber) {

    private var dataBase: SQLiteDatabase? = null

    init {
        val dbExist = checkDatabase()
        if (dbExist) {
            Log.e("-----", "Database exist")
        } else {
            Log.e("-----", "Database doesn't exist")
            createDatabase()
        }
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

    private fun openDatabase() {
        dataBase = SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).path, null, SQLiteDatabase.OPEN_READWRITE)
    }

    override fun close() {
        dataBase?.close()
        super.close()
    }

    fun getNode() {
        openDatabase()
        val cursor = dataBase?.rawQuery("select * from nodes where node_id = 25264054", null)

        cursor?.moveToFirst()
        val id = cursor?.getInt(0)
        val lon = cursor?.getDouble(1)
        val lat = cursor?.getDouble(2)

        println("nesto $lon $lat")

        cursor?.close()
        close()
    }
}