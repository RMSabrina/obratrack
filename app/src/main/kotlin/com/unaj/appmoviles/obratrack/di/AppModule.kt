package com.unaj.appmoviles.obratrack.di

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.unaj.appmoviles.obratrack.BuildConfig
import com.unaj.appmoviles.obratrack.core.data.di.CloudinaryUploadPreset
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCloudinary(): Cloudinary {
        val config = ObjectUtils.asMap(
            "cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME
        )
        return Cloudinary(config)
    }

    @Provides
    @CloudinaryUploadPreset
    fun provideCloudinaryUploadPreset(): String {
        return BuildConfig.CLOUDINARY_UPLOAD_PRESET
    }
}
