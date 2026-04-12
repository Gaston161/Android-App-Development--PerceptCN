// 📄 app/src/main/java/com/perceptnote/di/AppModule.kt — VERSION CLOUD
package com.perceptnote.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.perceptnote.auth.AuthManager
import com.perceptnote.data.local.PerceptNoteDatabase
import com.perceptnote.data.local.dao.AudioDao
import com.perceptnote.data.local.dao.NoteDao
import com.perceptnote.data.local.dao.SessionDao
import com.perceptnote.data.remote.AIApiService
import com.perceptnote.data.remote.AIClient
import com.perceptnote.data.remote.firebase.UserRepository
import com.perceptnote.data.repository.NoteRepositoryImpl
import com.perceptnote.data.repository.SessionRepositoryImpl
import com.perceptnote.domain.repository.NoteRepository
import com.perceptnote.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PerceptNoteDatabase =
        Room.databaseBuilder(context, PerceptNoteDatabase::class.java, "perceptnote_db")
            .fallbackToDestructiveMigration().build()

    @Provides fun provideNoteDao(db: PerceptNoteDatabase) = db.noteDao()
    @Provides fun provideSessionDao(db: PerceptNoteDatabase) = db.sessionDao()
    @Provides fun provideAudioDao(db: PerceptNoteDatabase) = db.audioDao()

    @Provides @Singleton fun provideAIApiService(): AIApiService = AIClient.create()

    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton
    fun provideAuthManager(auth: FirebaseAuth, @ApplicationContext ctx: Context) =
        AuthManager(auth, ctx)

    @Provides @Singleton
    fun provideUserRepository(auth: FirebaseAuth, fs: FirebaseFirestore) =
        UserRepository(auth, fs)

    @Provides @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
    @Binds @Singleton abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
