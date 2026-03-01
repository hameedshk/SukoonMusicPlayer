package com.sukoon.music.regression

import com.sukoon.music.shouldShowGlobalMiniPlayer
import com.sukoon.music.ui.navigation.Routes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerRouteVisibilityTest {

    @Test
    fun audioEditorRouteHidesMiniPlayer() {
        assertFalse(shouldShowGlobalMiniPlayer(Routes.AudioEditor.route))
    }

    @Test
    fun nowPlayingRouteHidesMiniPlayer() {
        assertFalse(shouldShowGlobalMiniPlayer(Routes.NowPlaying.route))
    }

    @Test
    fun feedbackRouteHidesMiniPlayer() {
        assertFalse(shouldShowGlobalMiniPlayer(Routes.FeedbackReport.route))
    }

    @Test
    fun regularRouteShowsMiniPlayer() {
        assertTrue(shouldShowGlobalMiniPlayer(Routes.Songs.route))
    }
}
