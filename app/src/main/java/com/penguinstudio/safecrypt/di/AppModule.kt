package com.penguinstudio.safecrypt.di

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext

@Module
@InstallIn(ActivityComponent::class)
object AppModule{

    @Provides
    fun provideActivityResultRegistry(@ActivityContext activity: Context) =
        (activity as? AppCompatActivity)?.activityResultRegistry
            ?: throw IllegalArgumentException("You must use AppCompatActivity")
}