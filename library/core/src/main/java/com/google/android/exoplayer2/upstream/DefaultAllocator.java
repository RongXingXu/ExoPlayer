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
package com.google.android.exoplayer2.upstream;

import static java.lang.Math.max;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import java.util.Arrays;

import org.checkerframework.checker.nullness.compatqual.NullableType;

/**
 * Default implementation of {@link Allocator}.
 */
public final class DefaultAllocator implements Allocator {
    
    private static final int AVAILABLE_EXTRA_CAPACITY = 100;
    
    // reset时，是否释放内存
    private final boolean trimOnReset;
    // 每个单独Allocation大小
    private final int individualAllocationSize;
    @Nullable
    // 初始缓存占位，取决于初始化时availableCount是否>0，<=0则为空
    private final byte[] initialAllocationBlock;
    // 纯打辅助，用途只有一个，release单个Allocation的时候，通过singleAllocationReleaseHolder组成一个数组
    private final Allocation[] singleAllocationReleaseHolder;
    
    private int targetBufferSize;
    // 已经分配的Allocation个数
    private int allocatedCount;
    // 池子里面还剩的Allocation个数
    private int availableCount;
    private @NullableType Allocation[] availableAllocations;
    
    /**
     * Constructs an instance without creating any {@link Allocation}s up front.
     *
     * @param trimOnReset              Whether memory is freed when the allocator is reset. Should be true unless
     *                                 the allocator will be re-used by multiple player instances.
     * @param individualAllocationSize The length of each individual {@link Allocation}.
     */
    public DefaultAllocator(boolean trimOnReset, int individualAllocationSize) {
        this(trimOnReset, individualAllocationSize, 0);
    }
    
    /**
     * Constructs an instance with some {@link Allocation}s created up front.
     *
     * <p>Note: {@link Allocation}s created up front will never be discarded by {@link #trim()}.
     *
     * @param trimOnReset              Whether memory is freed when the allocator is reset. Should be true unless
     *                                 the allocator will be re-used by multiple player instances.
     * @param individualAllocationSize The length of each individual {@link Allocation}.
     * @param initialAllocationCount   The number of allocations to create up front.
     */
    /*
    *
    * @param initialAllocationCount 预先创建的Allocation数量。
    * */
    public DefaultAllocator(
            boolean trimOnReset, int individualAllocationSize, int initialAllocationCount) {
        Assertions.checkArgument(individualAllocationSize > 0);
        Assertions.checkArgument(initialAllocationCount >= 0);
        this.trimOnReset = trimOnReset;
        this.individualAllocationSize = individualAllocationSize;
        this.availableCount = initialAllocationCount;
        this.availableAllocations = new Allocation[initialAllocationCount + AVAILABLE_EXTRA_CAPACITY];
        if (initialAllocationCount > 0) {
            initialAllocationBlock = new byte[initialAllocationCount * individualAllocationSize];
            for (int i = 0; i < initialAllocationCount; i++) {
                int allocationOffset = i * individualAllocationSize;
                availableAllocations[i] = new Allocation(initialAllocationBlock, allocationOffset);
            }
        } else {
            initialAllocationBlock = null;
        }
        singleAllocationReleaseHolder = new Allocation[1];
    }
    /*
    * 重置
    * trimOnReset未true则清理所有memory
    * */
    public synchronized void reset() {
        if (trimOnReset) {
            setTargetBufferSize(0);
        }
    }
    
    public synchronized void setTargetBufferSize(int targetBufferSize) {
        // 很明显了这个判断就是为了重新分配空间，比如：原来是100，现在只要80，下面trim就会清除掉多出来的20
        boolean targetBufferSizeReduced = targetBufferSize < this.targetBufferSize;
        this.targetBufferSize = targetBufferSize;
        if (targetBufferSizeReduced) {
            trim();
        }
    }
    
    /*
    * 池子里面有就从池子里面拿，没有就重新new一个Allocation
    * */
    @Override
    public synchronized Allocation allocate() {
        allocatedCount++;
        Allocation allocation;
        if (availableCount > 0) {
            allocation = Assertions.checkNotNull(availableAllocations[--availableCount]);
            availableAllocations[availableCount] = null;
        } else {
            allocation = new Allocation(new byte[individualAllocationSize], 0);
        }
        return allocation;
    }
    
    @Override
    public synchronized void release(Allocation allocation) {
        singleAllocationReleaseHolder[0] = allocation;
        release(singleAllocationReleaseHolder);
    }
    
    /*
    * 这里不难看出调用DefaultAllocator.release(Allocation[] allocations)，
    * 并不是将allocations释放，而是回收等待下次使用，所以外面一旦调用release接口，
    * 外部一定要将release的allocations置空清理掉
    * */
    @Override
    public synchronized void release(Allocation[] allocations) {
        if (availableCount + allocations.length >= availableAllocations.length) {
            // availableAllocations 扩容，扩到原来的两倍或者【availableCount + allocations.length】大小，哪个大，扩到哪个
            availableAllocations =
                    Arrays.copyOf(
                            availableAllocations,
                            max(availableAllocations.length * 2, availableCount + allocations.length));
        }
        // 回收release的allocations
        for (Allocation allocation : allocations) {
            availableAllocations[availableCount++] = allocation;
        }
        
        // 从已经分配的allocatedCount中删除
        allocatedCount -= allocations.length;
        // Wake up threads waiting for the allocated size to drop.
        notifyAll();
    }
    
    @Override
    public synchronized void trim() {
        int targetAllocationCount = Util.ceilDivide(targetBufferSize, individualAllocationSize);
        int targetAvailableCount = max(0, targetAllocationCount - allocatedCount);
        
        // 这里可以看出availableCount如果==0或者targetAvailableCount > availableCount，
        // 代表没有多余空间可以挤出来或者多余的空间不够，直接return
        if (targetAvailableCount >= availableCount) {
            // We're already at or below the target.
            return;
        }
        
        if (initialAllocationBlock != null) {
            // Some allocations are backed by an initial block. We need to make sure that we hold onto all
            // such allocations. Re-order the available allocations so that the ones backed by the initial
            // block come first.
            int lowIndex = 0;
            int highIndex = availableCount - 1;
            while (lowIndex <= highIndex) {
                Allocation lowAllocation = Assertions.checkNotNull(availableAllocations[lowIndex]);
                if (lowAllocation.data == initialAllocationBlock) {
                    lowIndex++;
                } else {
                    Allocation highAllocation = Assertions.checkNotNull(availableAllocations[highIndex]);
                    if (highAllocation.data != initialAllocationBlock) {
                        highIndex--;
                    } else {
                        availableAllocations[lowIndex++] = highAllocation;
                        availableAllocations[highIndex--] = lowAllocation;
                    }
                }
            }
            // lowIndex is the index of the first allocation not backed by an initial block.
            targetAvailableCount = max(targetAvailableCount, lowIndex);
            if (targetAvailableCount >= availableCount) {
                // We're already at or below the target.
                return;
            }
        }
        
        // Discard allocations beyond the target.
        // 清理末尾的memory，这里多余的Allocation才真正释放
        Arrays.fill(availableAllocations, targetAvailableCount, availableCount, null);
        availableCount = targetAvailableCount;
    }
    
    @Override
    public synchronized int getTotalBytesAllocated() {
        return allocatedCount * individualAllocationSize;
    }
    
    @Override
    public int getIndividualAllocationLength() {
        return individualAllocationSize;
    }
}
