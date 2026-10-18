package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PdfDocumentEntity::class,
        BookmarkEntity::class,
        PageAnnotationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun pdfDao(): PdfDao

    companion object {
        @Volatile
        private var INSTANCE: FolioDatabase? = null

        fun getInstance(context: Context): FolioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FolioDatabase::class.java,
                    "folio_pdf_studio.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
