package com.unaj.appmoviles.obratrack.core.data.di

import android.content.Context
import com.unaj.appmoviles.obratrack.core.data.fotos.ColaFotosPendientesImpl
import com.unaj.appmoviles.obratrack.core.data.fotos.FotosPendientesStore
import com.unaj.appmoviles.obratrack.core.data.fotos.PreparadorFotos
import com.unaj.appmoviles.obratrack.core.data.fotos.PreparadorFotosAndroid
import com.unaj.appmoviles.obratrack.core.data.repository.FotoRelevamientoRepositoryImpl
import com.unaj.appmoviles.obratrack.core.domain.repository.ColaFotosPendientes
import com.unaj.appmoviles.obratrack.core.domain.repository.FotoRelevamientoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/** Cola de fotos pendientes de subir. El PlanificadorSubidas lo enlaza :app (usa WorkManager). */
@Module
@InstallIn(SingletonComponent::class)
abstract class FotosBindsModule {

    @Binds
    abstract fun bindColaFotosPendientes(impl: ColaFotosPendientesImpl): ColaFotosPendientes

    @Binds
    abstract fun bindFotoRelevamientoRepository(impl: FotoRelevamientoRepositoryImpl): FotoRelevamientoRepository

    @Binds
    abstract fun bindPreparadorFotos(impl: PreparadorFotosAndroid): PreparadorFotos
}

@Module
@InstallIn(SingletonComponent::class)
object FotosProvidesModule {

    /** En filesDir y no en la caché: el sistema no la vacía y la limpieza de temporales no la toca. */
    @Provides
    @Singleton
    fun provideFotosPendientesStore(@ApplicationContext context: Context): FotosPendientesStore =
        FotosPendientesStore(File(context.filesDir, "fotos_pendientes"))
}
