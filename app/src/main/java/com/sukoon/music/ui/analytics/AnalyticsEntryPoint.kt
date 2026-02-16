package com.sukoon.music.ui.analytics

import com.sukoon.music.data.analytics.AnalyticsTracker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyticsEntryPoint {
    fun analyticsTracker(): AnalyticsTracker
}

