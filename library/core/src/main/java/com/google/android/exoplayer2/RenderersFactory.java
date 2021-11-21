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

import android.os.Handler;

import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

/**
 * Builds {@link Renderer} instances for use by an {@link ExoPlayer}.
 */
public interface RenderersFactory {
    
    /**
     * Builds the {@link Renderer} instances for an {@link ExoPlayer}.
     *
     * @param eventHandler               A handler to use when invoking event listeners and outputs.
     * @param videoRendererEventListener An event listener for video renderers.
     * @param audioRendererEventListener An event listener for audio renderers.
     * @param textRendererOutput         An output for text renderers.
     * @param metadataRendererOutput     An output for metadata renderers.
     * @return The {@link Renderer instances}.
     */
    /**
     * 创建exoplayer的渲染器实例
     *
     * @param eventHandler               事件handler，exoplayer线程模型中的应用线程，用于事件回调，大部分都在主线程
     * @param videoRendererEventListener 视频回调事件监听
     * @param audioRendererEventListener 音频回调事件监听
     * @param textRendererOutput         文本输出接收器
     * @param metadataRendererOutput     metadata输出接收器
     * @return The {@link Renderer instances}.
     */
    Renderer[] createRenderers(
            Handler eventHandler,
            VideoRendererEventListener videoRendererEventListener,
            AudioRendererEventListener audioRendererEventListener,
            TextOutput textRendererOutput,
            MetadataOutput metadataRendererOutput);
}
