/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.socket.nio;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

public final class ReceiveBufferPool {

    private static final int POOL_SIZE = 8;

    @SuppressWarnings("unchecked")
    private final SoftReference<ByteBuffer>[] pool = new SoftReference[POOL_SIZE];

    public ByteBuffer acquire(int size) {
        final SoftReference<ByteBuffer>[] pool = this.pool;
        for (int i = 0; i < POOL_SIZE; i ++) {
            SoftReference<ByteBuffer> ref = pool[i];
            if (ref == null) {
                continue;
            }

            ByteBuffer buf = ref.get();
            if (buf == null) {
                pool[i] = null;
                continue;
            }

            if (buf.capacity() < size) {
                continue;
            }

            pool[i] = null;

            buf.clear();
            return buf;
        }

        ByteBuffer buf = ByteBuffer.allocateDirect(normalizeCapacity(size));
        return buf;
    }

    public void release(ByteBuffer buffer) {
        final SoftReference<ByteBuffer>[] pool = this.pool;
        for (int i = 0; i < POOL_SIZE; i ++) {
            SoftReference<ByteBuffer> ref = pool[i];
            if (ref == null || ref.get() == null) {
                pool[i] = new SoftReference<ByteBuffer>(buffer);
                return;
            }
        }

        // pool is full - replace one
        final int capacity = buffer.capacity();
        for (int i = 0; i < POOL_SIZE; i ++) {
            SoftReference<ByteBuffer> ref = pool[i];
            ByteBuffer pooled = ref.get();
            if (pooled == null) {
                pool[i] = null;
                continue;
            }

            if (pooled.capacity() < capacity) {
                pool[i] = new SoftReference<ByteBuffer>(buffer);
                return;
            }
        }
    }

    private static int normalizeCapacity(int capacity) {
        // Normalize to multiple of 1024
        int q = capacity >>> 10;
        int r = capacity & 1023;
        if (r != 0) {
            q ++;
        }
        return q << 10;
    }
}
