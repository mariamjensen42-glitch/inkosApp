package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookByIdFlow(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    fun getChaptersForBookFlow(bookId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersForBook(bookId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND chapterIndex = :index")
    suspend fun getChapterByIndex(bookId: Long, index: Int): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: Long)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)
}

@Dao
interface PlayStateDao {
    @Query("SELECT * FROM play_states WHERE bookId = :bookId LIMIT 1")
    fun getPlayStateForBookFlow(bookId: Long): Flow<PlayStateEntity?>

    @Query("SELECT * FROM play_states WHERE bookId = :bookId LIMIT 1")
    suspend fun getPlayStateForBook(bookId: Long): PlayStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayState(playState: PlayStateEntity): Long

    @Update
    suspend fun updatePlayState(playState: PlayStateEntity)
}
