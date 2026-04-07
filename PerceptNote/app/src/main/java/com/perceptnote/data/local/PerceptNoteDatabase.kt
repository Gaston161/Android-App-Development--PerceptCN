// 📄 app/src/main/java/com/perceptnote/data/local/PerceptNoteDatabase.kt
package com.perceptnote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.perceptnote.data.local.dao.AudioDao
import com.perceptnote.data.local.dao.NoteDao
import com.perceptnote.data.local.dao.SessionDao
import com.perceptnote.data.local.entities.AudioEntity
import com.perceptnote.data.local.entities.NoteEntity
import com.perceptnote.data.local.entities.SessionEntity

/**
 * Base de données Room principale de PerceptNote.
 * Version 1 — pour les migrations futures, incrémenter version et ajouter une Migration object.
 */
@Database(
    entities = [
        SessionEntity::class,
        NoteEntity::class,
        AudioEntity::class
    ],
    version = 1,
    exportSchema = false // TODO : passer à true + ajouter schemaLocation pour prod
)
abstract class PerceptNoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun sessionDao(): SessionDao
    abstract fun audioDao(): AudioDao
}
