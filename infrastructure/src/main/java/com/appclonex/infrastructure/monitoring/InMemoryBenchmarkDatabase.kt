package com.appclonex.infrastructure.monitoring

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/**
 * Lightweight in-memory SQLite database used exclusively for benchmarking
 * database operations (insert, query, update, delete) without touching the
 * real app database.
 */
internal class InMemoryBenchmarkDatabase(context: Context) {

    private val db: SQLiteDatabase = SQLiteDatabase.create(null).also {
        it.version = 1
        it.execSQL(
            """
            CREATE TABLE IF NOT EXISTS benchmark_users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    fun insertUser(name: String, email: String): Long {
        db.execSQL(
            "INSERT INTO benchmark_users (name, email) VALUES (?, ?)",
            arrayOf(name, email)
        )
        // Return last inserted row id via raw query
        val cursor = db.rawQuery("SELECT last_insert_rowid()", null)
        return cursor.use {
            if (it.moveToFirst()) it.getLong(0) else -1L
        }
    }

    fun queryUserById(id: Long): Map<String, Any?>? {
        val cursor = db.rawQuery(
            "SELECT id, name, email FROM benchmark_users WHERE id = ?",
            arrayOf(id.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) {
                mapOf(
                    "id" to it.getLong(0),
                    "name" to it.getString(1),
                    "email" to it.getString(2)
                )
            } else {
                null
            }
        }
    }

    fun updateUserEmail(id: Long, newEmail: String) {
        db.execSQL(
            "UPDATE benchmark_users SET email = ? WHERE id = ?",
            arrayOf(newEmail, id.toString())
        )
    }

    fun deleteUser(id: Long) {
        db.execSQL(
            "DELETE FROM benchmark_users WHERE id = ?",
            arrayOf(id.toString())
        )
    }

    fun close() {
        db.close()
    }
}
