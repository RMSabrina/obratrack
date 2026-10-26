package com.unaj.appmoviles.obratrack.core.data.di

import com.unaj.appmoviles.obratrack.core.data.repository.AuthRepositoryImpl
import com.unaj.appmoviles.obratrack.core.data.repository.CloudinaryImageStorageImpl
import com.unaj.appmoviles.obratrack.core.data.repository.ObraRepositoryImpl
import com.unaj.appmoviles.obratrack.core.data.repository.RelevamientoRepositoryImpl
import com.unaj.appmoviles.obratrack.core.data.repository.TareaRepositoryImpl
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ImageStorageRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindObraRepository(
        impl: ObraRepositoryImpl
    ): ObraRepository

    @Binds
    abstract fun bindRelevamientoRepository(
        impl: RelevamientoRepositoryImpl
    ): RelevamientoRepository

    @Binds
    abstract fun bindTareaRepository(
        impl: TareaRepositoryImpl
    ): TareaRepository

    @Binds
    abstract fun bindImageStorageRepository(
        impl: CloudinaryImageStorageImpl
    ): ImageStorageRepository

    @Binds
    abstract fun bindGeocodingRepository(
        impl: com.unaj.appmoviles.obratrack.core.data.repository.GeocodingRepositoryImpl
    ): com.unaj.appmoviles.obratrack.core.domain.repository.GeocodingRepository

    @Binds
    abstract fun bindLanguagePreferencesRepository(
        impl: com.unaj.appmoviles.obratrack.core.data.repository.LanguagePreferencesRepositoryImpl
    ): com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository

    @Binds
    abstract fun bindNotificacionRepository(
        impl: com.unaj.appmoviles.obratrack.core.data.repository.NotificacionRepositoryImpl
    ): com.unaj.appmoviles.obratrack.core.domain.repository.NotificacionRepository

    @Binds
    abstract fun bindColaboradorRepository(
        impl: com.unaj.appmoviles.obratrack.core.data.repository.ColaboradorRepositoryImpl
    ): com.unaj.appmoviles.obratrack.core.domain.repository.ColaboradorRepository

    @Binds
    abstract fun bindIncidenteRepository(
        impl: com.unaj.appmoviles.obratrack.core.data.repository.IncidenteRepositoryImpl
    ): com.unaj.appmoviles.obratrack.core.domain.repository.IncidenteRepository
}
