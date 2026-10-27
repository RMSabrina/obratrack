package com.unaj.appmoviles.obratrack.di

import com.unaj.appmoviles.obratrack.core.data.fotos.PlanificadorSubidas
import com.unaj.appmoviles.obratrack.worker.PlanificadorSubidasWorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlanificadorModule {

    @Binds
    abstract fun bindPlanificadorSubidas(impl: PlanificadorSubidasWorkManager): PlanificadorSubidas
}
