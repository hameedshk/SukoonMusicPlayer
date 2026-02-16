package com.sukoon.music.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.sukoon.music.data.analytics.AnalyticsTracker
import com.sukoon.music.data.analytics.FirebaseAnalyticsTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context
    ): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        firebaseAnalytics: FirebaseAnalytics
    ): AnalyticsTracker = FirebaseAnalyticsTracker(firebaseAnalytics)
}

