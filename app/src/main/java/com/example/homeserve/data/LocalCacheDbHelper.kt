package com.example.homeserve.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.homeserve.data.model.Category
import com.example.homeserve.data.model.ServiceModel

class LocalCacheDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "homeserve_cache.db"
        private const val DATABASE_VERSION = 1

        // Categories table
        private const val TABLE_CATEGORIES = "categories"
        private const val COL_CAT_ID = "categoryId"
        private const val COL_CAT_NAME = "name"
        private const val COL_CAT_ICON = "icon"
        private const val COL_CAT_ACTIVE = "isActive"

        // Services table
        private const val TABLE_SERVICES = "services"
        private const val COL_SRV_ID = "serviceId"
        private const val COL_SRV_CAT_ID = "categoryId"
        private const val COL_SRV_NAME = "name"
        private const val COL_SRV_DESC = "description"
        private const val COL_SRV_PRICE = "price"
        private const val COL_SRV_ACTIVE = "isActive"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID TEXT PRIMARY KEY,
                $COL_CAT_NAME TEXT,
                $COL_CAT_ICON TEXT,
                $COL_CAT_ACTIVE INTEGER
            )
        """.trimIndent()

        val createServicesTable = """
            CREATE TABLE $TABLE_SERVICES (
                $COL_SRV_ID TEXT PRIMARY KEY,
                $COL_SRV_CAT_ID TEXT,
                $COL_SRV_NAME TEXT,
                $COL_SRV_DESC TEXT,
                $COL_SRV_PRICE INTEGER,
                $COL_SRV_ACTIVE INTEGER
            )
        """.trimIndent()

        db.execSQL(createCategoriesTable)
        db.execSQL(createServicesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SERVICES")
        onCreate(db)
    }

    fun saveCategories(categories: List<Category>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_CATEGORIES, null, null)
            for (category in categories) {
                val values = ContentValues().apply {
                    put(COL_CAT_ID, category.categoryId)
                    put(COL_CAT_NAME, category.name)
                    put(COL_CAT_ICON, category.icon)
                    put(COL_CAT_ACTIVE, if (category.isActive) 1 else 0)
                }
                db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun saveServices(services: List<ServiceModel>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_SERVICES, null, null)
            for (service in services) {
                val values = ContentValues().apply {
                    put(COL_SRV_ID, service.serviceId)
                    put(COL_SRV_CAT_ID, service.categoryId)
                    put(COL_SRV_NAME, service.name)
                    put(COL_SRV_DESC, service.description)
                    put(COL_SRV_PRICE, service.price)
                    put(COL_SRV_ACTIVE, if (service.isActive) 1 else 0)
                }
                db.insertWithOnConflict(TABLE_SERVICES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCategories(): List<Category> {
        val categories = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getString(it.getColumnIndexOrThrow(COL_CAT_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(COL_CAT_NAME))
                    val icon = it.getString(it.getColumnIndexOrThrow(COL_CAT_ICON))
                    val isActive = it.getInt(it.getColumnIndexOrThrow(COL_CAT_ACTIVE)) == 1
                    categories.add(Category(id, name, icon, isActive))
                } while (it.moveToNext())
            }
        }
        return categories
    }

    fun getAllServices(): List<ServiceModel> {
        val services = mutableListOf<ServiceModel>()
        val db = readableDatabase
        val cursor = db.query(TABLE_SERVICES, null, null, null, null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getString(it.getColumnIndexOrThrow(COL_SRV_ID))
                    val catId = it.getString(it.getColumnIndexOrThrow(COL_SRV_CAT_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(COL_SRV_NAME))
                    val desc = it.getString(it.getColumnIndexOrThrow(COL_SRV_DESC))
                    val price = it.getInt(it.getColumnIndexOrThrow(COL_SRV_PRICE))
                    val isActive = it.getInt(it.getColumnIndexOrThrow(COL_SRV_ACTIVE)) == 1
                    services.add(ServiceModel(id, catId, name, desc, price, isActive))
                } while (it.moveToNext())
            }
        }
        return services
    }

    fun getServicesByCategory(categoryId: String): List<ServiceModel> {
        val services = mutableListOf<ServiceModel>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SERVICES,
            null,
            "$COL_SRV_CAT_ID = ?",
            arrayOf(categoryId),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getString(it.getColumnIndexOrThrow(COL_SRV_ID))
                    val catId = it.getString(it.getColumnIndexOrThrow(COL_SRV_CAT_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(COL_SRV_NAME))
                    val desc = it.getString(it.getColumnIndexOrThrow(COL_SRV_DESC))
                    val price = it.getInt(it.getColumnIndexOrThrow(COL_SRV_PRICE))
                    val isActive = it.getInt(it.getColumnIndexOrThrow(COL_SRV_ACTIVE)) == 1
                    services.add(ServiceModel(id, catId, name, desc, price, isActive))
                } while (it.moveToNext())
            }
        }
        return services
    }
}
