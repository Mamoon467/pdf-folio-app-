package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_documents ORDER BY lastModified DESC")
    fun observeAllDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents ORDER BY lastModified DESC")
    suspend fun getAllDocumentsOnce(): List<PdfDocumentEntity>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): PdfDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: PdfDocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: PdfDocumentEntity)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    // Bookmarks
    @Query("SELECT * FROM pdf_bookmarks ORDER BY createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM pdf_bookmarks WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun observeBookmarksForDocument(documentId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM pdf_bookmarks WHERE documentId = :documentId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getBookmarkForPage(documentId: Long, pageIndex: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM pdf_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM pdf_bookmarks WHERE documentId = :documentId")
    suspend fun deleteBookmarksForDocument(documentId: Long)

    // Annotations
    @Query("SELECT * FROM page_annotations WHERE documentId = :documentId ORDER BY createdAt ASC")
    fun observeAnnotationsForDocument(documentId: Long): Flow<List<PageAnnotationEntity>>

    @Query("SELECT * FROM page_annotations WHERE documentId = :documentId ORDER BY createdAt ASC")
    suspend fun getAnnotationsForDocumentOnce(documentId: Long): List<PageAnnotationEntity>

    @Query("SELECT * FROM page_annotations WHERE documentId = :documentId AND pageIndex = :pageIndex ORDER BY createdAt ASC")
    suspend fun getAnnotationsForPageOnce(documentId: Long, pageIndex: Int): List<PageAnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: PageAnnotationEntity): Long

    @Query("DELETE FROM page_annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: Long)

    @Query("DELETE FROM page_annotations WHERE documentId = :documentId AND pageIndex = :pageIndex")
    suspend fun clearAnnotationsForPage(documentId: Long, pageIndex: Int)

    @Query("DELETE FROM page_annotations WHERE documentId = :documentId")
    suspend fun clearAnnotationsForDocument(documentId: Long)
}
