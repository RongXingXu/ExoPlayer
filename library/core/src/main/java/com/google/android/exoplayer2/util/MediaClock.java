/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.util;

import com.google.android.exoplayer2.PlaybackParameters;

/**
 * Tracks the progression of media time.
 */
/**
 *
 * 跟踪媒体时间进度。
 */
public interface MediaClock {
    
    /**
     * Returns the current media position in microseconds.
     */
    /**
     * 返回当前媒体位置（微秒）
     * */
    long getPositionUs();
    
    /**
     * Attempts to set the playback parameters. The media clock may override the speed if changing the
     * playback parameters is not supported.
     *
     * @param playbackParameters The playback parameters to attempt to set.
     */
    /**
     * 尝试设置播放参数。如果不支持更改播放参数，媒体时钟可能会覆盖速度。
     *
     * @param playbackParameters 期望设置的播放参数
     */
    void setPlaybackParameters(PlaybackParameters playbackParameters);
    
    /**
     * Returns the active playback parameters.
     */
    PlaybackParameters getPlaybackParameters();
}
