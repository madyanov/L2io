/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.io;

import java.io.EOFException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class RandomAccessMemory implements RandomAccess {
    private static final int DEFAULT_CAPACITY = 1 << 16;

    private final String name;
    private final Charset charset;
    private ByteBuffer buffer;

    public RandomAccessMemory(String name, byte[] data, Charset charset) {
        this.name = name;
        this.buffer = ByteBuffer.wrap(data);
        this.charset = charset;
    }

    public RandomAccessMemory(String name, Charset charset) {
        this(name, new byte[DEFAULT_CAPACITY], charset);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public int getPosition() {
        return buffer.position();
    }

    @Override
    public void setPosition(int position) {
        ensureCapacity(position);

        buffer.position(position);
    }

    @Override
    public void trimToPosition() {
        buffer.limit(buffer.position());
    }

    @Override
    public int readUnsignedByte() throws UncheckedIOException {
        if (buffer.position() >= buffer.limit())
            throw new UncheckedIOException(new EOFException());

        return buffer.get() & 0xff;
    }

    @Override
    public void writeByte(int b) {
        ensureCapacity(buffer.position() + 1);

        buffer.put((byte) b);
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > buffer.capacity())
            grow(minCapacity);
        if (minCapacity > buffer.limit())
            buffer.limit(minCapacity);
    }

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buffer.capacity();
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        int limit = buffer.limit();
        int position = buffer.position();
        buffer = ByteBuffer.wrap(Arrays.copyOf(buffer.array(), newCapacity));
        buffer.limit(limit);
        buffer.position(position);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    @Override
    public RandomAccess openNewSession(boolean readOnly) {
        return this;
    }

    @Override
    public void close() {
    }

    public void writeTo(DataOutput output) throws UncheckedIOException {
        output.writeBytes(buffer.array(), 0, buffer.limit());
    }
}