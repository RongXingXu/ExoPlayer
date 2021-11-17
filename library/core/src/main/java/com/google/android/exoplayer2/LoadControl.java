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
package com.google.android.exoplayer2;

import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.upstream.Allocator;

/**
 * Controls buffering of media.
 */
public interface LoadControl {
    
    static final String TAG = "LoadControl";
    
    /**
     * Called by the player when prepared with a new source.
     */
    void onPrepared();
    
    /**
     * Called by the player when a track selection occurs.
     *
     * @param renderers       The renderers.
     * @param trackGroups     The {@link TrackGroup}s from which the selection was made.
     * @param trackSelections The track selections that were made.
     */
    void onTracksSelected(
            Renderer[] renderers, TrackGroupArray trackGroups, ExoTrackSelection[] trackSelections);
    
    /**
     * Called by the player when stopped.
     */
    void onStopped();
    
    /**
     * Called by the player when released.
     */
    void onReleased();
    
    /**
     * Returns the {@link Allocator} that should be used to obtain media buffer allocations.
     */
    Allocator getAllocator();
    
    /**
     * Returns the duration of media to retain in the buffer prior to the current playback position,
     * for fast backward seeking.
     *
     * <p>Note: If {@link #retainBackBufferFromKeyframe()} is false then seeking in the back-buffer
     * will only be fast if the back-buffer contains a keyframe prior to the seek position.
     *
     * <p>Note: Implementations should return a single value. Dynamic changes to the back-buffer are
     * not currently supported.
     *
     * @return The duration of media to retain in the buffer prior to the current playback position,
     * in microseconds.
     */
    // 为了支持能够快速向后搜索，返回缓冲区中，当前播放位置到最早保留的视频position的时间段
    long getBackBufferDurationUs();
    
    /**
     * Returns whether media should be retained from the keyframe before the current playback position
     * minus {@link #getBackBufferDurationUs()}, rather than any sample before or at that position.
     *
     * <p>Warning: Returning true will cause the back-buffer size to depend on the spacing of
     * keyframes in the media being played. Returning true is not recommended unless you control the
     * media and are comfortable with the back-buffer size exceeding {@link
     * #getBackBufferDurationUs()} by as much as the maximum duration between adjacent keyframes in
     * the media.
     *
     * <p>Note: Implementations should return a single value. Dynamic changes to the back-buffer are
     * not currently supported.
     *
     * @return Whether media should be retained from the keyframe before the current playback position
     * minus {@link #getBackBufferDurationUs()}, rather than any sample before or at that
     * position.
     */
    /*
    * TODO
    * */
    boolean retainBackBufferFromKeyframe();
    
    /**
     * Called by the player to determine whether it should continue to load the source.
     *
     * @param playbackPositionUs The current playback position in microseconds, relative to the start
     *                           of the {@link Timeline.Period period} that will continue to be loaded if this method
     *                           returns {@code true}. If playback of this period has not yet started, the value will be
     *                           negative and equal in magnitude to the duration of any media in previous periods still to
     *                           be played.
     * @param bufferedDurationUs The duration of media that's currently buffered.
     * @param playbackSpeed      The current factor by which playback is sped up.
     * @return Whether the loading should continue.
     */
    /*
    * 用途：决策是否需要继续加载数据，Called by the player
    *
    * @param playbackPositionUs：当前播放位置（微秒μs），相对于 {@link Timeline.Period period} 的开始。如果该时段的播放尚未开始，则该值将为负数，其大小等于前一时段仍要播放的任何媒体的持续时间。（这里简单介绍下Period，ExoPlayer支持播放列表，一个播放任务可以包含多个视频，Period是播放任务中，其中一个视频）
    * @param bufferedDurationUs：当前已缓存的视频时间段（微秒μs）
    * @param playbackSpeed：当前播放速度（*1，*2等）
    *
    * @return：如果此方法返回 {@code true}将会继续加载数据。
    * */
    boolean shouldContinueLoading(
            long playbackPositionUs, long bufferedDurationUs, float playbackSpeed);
    
    /**
     * Called repeatedly by the player when it's loading the source, has yet to start playback, and
     * has the minimum amount of data necessary for playback to be started. The value returned
     * determines whether playback is actually started. The load control may opt to return {@code
     * false} until some condition has been met (e.g. a certain amount of media is buffered).
     *
     * @param bufferedDurationUs The duration of media that's currently buffered.
     * @param playbackSpeed      The current factor by which playback is sped up.
     * @param rebuffering        Whether the player is rebuffering. A rebuffer is defined to be caused by
     *                           buffer depletion rather than a user action. Hence this parameter is false during initial
     *                           buffering and when buffering as a result of a seek operation.
     * @param targetLiveOffsetUs The desired playback position offset to the live edge in
     *                           microseconds, or {@link C#TIME_UNSET} if the media is not a live stream or no offset is
     *                           configured.
     * @return Whether playback should be allowed to start or resume.
     */
    /*
    * 用途：用于决策是否开始真正播放（启播，seek缓冲成功后播放）。
    * 在尚未开始播放时，播放器在加载Source时会重复调用该接口。
    * LoadControl可以选择返回 {@code false} 直到满足某些条件
    * （例如，缓冲了一定数量的媒体）。
    *
    * @param bufferedDurationUs：当前已缓存的视频时间段
    * @param playbackSpeed：当前播放速度（*1，*2等）
    * @param rebuffering：播放器是否正在rebuffering。这里rebuffering是指缓冲自然耗尽，而不是用户操作引起。so，在初始缓冲期间和由于seek进行缓冲时，此参数为false。
    * @param targetLiveOffsetUs：当前直播位置的偏移量，如果不是直播流，此参数为{@link C#TIME_UNSET}
    *
    * @return 是否应允许开始或恢复播放。
    * */
    boolean shouldStartPlayback(
            long bufferedDurationUs, float playbackSpeed, boolean rebuffering, long targetLiveOffsetUs);
}
