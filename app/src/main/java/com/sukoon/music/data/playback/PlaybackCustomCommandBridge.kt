package com.sukoon.music.data.playback

import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackCustomCommandBridge @Inject constructor() {
    @Volatile
    private var handler: ((String, Bundle) -> Int?)? = null

    fun setHandler(handler: ((String, Bundle) -> Int?)?) {
        this.handler = handler
    }

    fun dispatch(action: String, args: Bundle): Int? {
        return handler?.invoke(action, args)
    }
}
