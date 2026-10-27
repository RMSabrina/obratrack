package com.unaj.appmoviles.obratrack.core.firebase.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseAuthDataSource
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseAuthDataSourceImpl
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreObraDataSource
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreObraDataSourceImpl
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreRelevamientoDataSource
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreRelevamientoDataSourceImpl
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreTareaDataSource
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreTareaDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseProvidesModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Caché en disco explícito: la app lee y escribe sin red y sincroniza al volver la conexión.
        firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { setSizeBytes(CACHE_BYTES) })
        }
    }

    private const val CACHE_BYTES = 100L * 1024 * 1024
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseBindsModule {

    @Binds
    abstract fun bindFirebaseAuthDataSource(
        impl: FirebaseAuthDataSourceImpl
    ): FirebaseAuthDataSource

    @Binds
    abstract fun bindFirestoreObraDataSource(
        impl: FirestoreObraDataSourceImpl
    ): FirestoreObraDataSource

    @Binds
    abstract fun bindFirestoreRelevamientoDataSource(
        impl: FirestoreRelevamientoDataSourceImpl
    ): FirestoreRelevamientoDataSource

    @Binds
    abstract fun bindFirestoreTareaDataSource(
        impl: FirestoreTareaDataSourceImpl
    ): FirestoreTareaDataSource
}
