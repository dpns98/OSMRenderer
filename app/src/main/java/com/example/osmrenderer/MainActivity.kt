package com.example.osmrenderer

import android.database.Cursor
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.sqlite.database.sqlite.SQLiteDatabase


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        println("nesto " + applicationInfo.nativeLibraryDir)

        System.loadLibrary("sqliteX")

        val query = "select sqlite_version() AS sqlite_version"
        val db = SQLiteDatabase.openOrCreateDatabase(":memory:", null)
        var cursor: Cursor = db.rawQuery(query, null)
        var sqliteVersion = ""
        if (cursor.moveToNext()) {
            sqliteVersion = cursor.getString(0)
        }

        db.execSQL("CREATE VIRTUAL TABLE demo_index USING rtree(id, minX, maxX, minY, maxY);")
        db.execSQL("INSERT INTO demo_index VALUES(1,-80.7749, -80.7747, 35.3776, 35.3778);")
        db.execSQL("INSERT INTO demo_index VALUES(2,-81.0, -79.6, 35.0, 36.2);")

        cursor = db.rawQuery(
            "SELECT id FROM demo_index WHERE minX>=-81.08 AND maxX<=-80.58 AND minY>=35.00  AND maxY<=35.44;",
            null
        )

        var id = -1
        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(0)
            } while (cursor.moveToNext())
        }
        db.close()
    }
}