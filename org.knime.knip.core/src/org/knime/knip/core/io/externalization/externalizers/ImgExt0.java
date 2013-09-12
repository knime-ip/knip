/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.core.io.externalization.externalizers;

import net.imglib2.Cursor;
import net.imglib2.img.AbstractImg;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.Unsigned12BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.Externalizer;
import org.knime.knip.core.io.externalization.ExternalizerManager;
import org.knime.knip.core.types.NativeTypes;

/**
 * Naive img externalization.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgExt0 implements Externalizer<Img> {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Img> getType() {
        return Img.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Img read(final BufferedDataInputStream in) throws Exception {

        final Type<?> type = (Type<?>)ExternalizerManager.<Class> read(in).newInstance();

        final ImgFactory factory = (ImgFactory)ExternalizerManager.<Class> read(in).newInstance();

        final long[] dims = new long[in.readInt()];
        in.read(dims);

        @SuppressWarnings("unchecked")
        final AbstractImg<Type<?>> res = (AbstractImg<Type<?>>)factory.create(dims, type);

        final Cursor<? extends Type<?>> cur = res.cursor();
        final int totalSize = (int)res.size();
        final int buffSize = 8192;

        final NativeTypes nType = NativeTypes.getPixelType(cur.next());

        cur.reset();
        switch (nType) {
            case BITTYPE:

                final Cursor<BitType> bitTypeCursor = (Cursor<BitType>)cur;
                final boolean[] booleanBuf = new boolean[Math.min(buffSize, totalSize)];
                int currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(booleanBuf, 0, Math.min(booleanBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        bitTypeCursor.get().set(booleanBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case BYTETYPE:
                final Cursor<ByteType> byteTypeCursor = (Cursor<ByteType>)cur;
                byte[] byteBuf = new byte[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(byteBuf, 0, Math.min(byteBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        byteTypeCursor.get().set(byteBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case DOUBLETYPE:
                final Cursor<DoubleType> doubleTypeCursor = (Cursor<DoubleType>)cur;
                final double[] doubleBuf = new double[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(doubleBuf, 0, Math.min(doubleBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        doubleTypeCursor.get().set(doubleBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case FLOATTYPE:

                final Cursor<FloatType> floatTypeCursor = (Cursor<FloatType>)cur;
                final float[] floatBuf = new float[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(floatBuf, 0, Math.min(floatBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        floatTypeCursor.get().set(floatBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case INTTYPE:
                final Cursor<IntType> intTypeCursor = (Cursor<IntType>)cur;
                int[] intBuf = new int[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(intBuf, 0, Math.min(intBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        intTypeCursor.get().set(intBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case LONGTYPE:
                final Cursor<LongType> longTypeCursor = (Cursor<LongType>)cur;
                long[] longBuf = new long[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(longBuf, 0, Math.min(longBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        longTypeCursor.get().set(longBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case SHORTTYPE:
                final Cursor<ShortType> shortTypeCursor = (Cursor<ShortType>)cur;
                short[] shortBuf = new short[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(shortBuf, 0, Math.min(shortBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        shortTypeCursor.get().set(shortBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case UNSIGNED12BITTYPE:
                final Cursor<Unsigned12BitType> unsigned12BitTypeCursor = (Cursor<Unsigned12BitType>)cur;
                shortBuf = new short[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(shortBuf, 0, Math.min(shortBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        unsigned12BitTypeCursor.get().set(shortBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case UNSIGNEDBYTETYPE:
                final Cursor<UnsignedByteType> unsignedByteTypeCursor = (Cursor<UnsignedByteType>)cur;
                byteBuf = new byte[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(byteBuf, 0, Math.min(byteBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        unsignedByteTypeCursor.get().set(byteBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case UNSIGNEDINTTYPE:
                final Cursor<UnsignedIntType> unsignedIntTypeCursor = (Cursor<UnsignedIntType>)cur;
                longBuf = new long[Math.min(buffSize, totalSize)];

                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(longBuf, 0, Math.min(longBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        unsignedIntTypeCursor.get().set(longBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            case UNSIGNEDSHORTTYPE:
                final Cursor<UnsignedShortType> unsignedShortTypeCursor = (Cursor<UnsignedShortType>)cur;
                intBuf = new int[Math.min(buffSize, totalSize)];
                currIdx = 0;
                while (currIdx < totalSize) {
                    in.read(intBuf, 0, Math.min(intBuf.length, totalSize - currIdx));

                    int idx = 0;
                    while (cur.hasNext() && (idx < buffSize)) {
                        cur.fwd();
                        unsignedShortTypeCursor.get().set(intBuf[idx++]);
                    }
                    currIdx += idx;
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported Pixeltype.");
        }

        return res;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final BufferedDataOutputStream out, final Img obj) throws Exception {

        ExternalizerManager.<Class> write(out, obj.firstElement().getClass());
        ExternalizerManager.<Class> write(out, obj.factory().getClass());

        // write dimensions
        out.writeInt(obj.numDimensions());
        for (int i = 0; i < obj.numDimensions(); i++) {
            out.writeLong(obj.dimension(i));
        }

        final Cursor<? extends Type<?>> cur = obj.cursor();

        final NativeTypes type = NativeTypes.getPixelType(cur.next());
        cur.reset();

        switch (type) {
            case BITTYPE:
                final Cursor<BitType> bitTypeCursor = (Cursor<BitType>)cur;

                while (bitTypeCursor.hasNext()) {
                    bitTypeCursor.fwd();
                    out.writeBoolean(bitTypeCursor.get().get());
                }
                break;
            case BYTETYPE:
                final Cursor<ByteType> byteTypeCursor = (Cursor<ByteType>)cur;

                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeByte(byteTypeCursor.get().get());
                }
                break;
            case DOUBLETYPE:
                final Cursor<DoubleType> doubleTypeCursor = (Cursor<DoubleType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeDouble(doubleTypeCursor.get().get());
                }
                break;
            case FLOATTYPE:
                final Cursor<FloatType> floatTypeCursor = (Cursor<FloatType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeFloat(floatTypeCursor.get().get());
                }
                break;
            case INTTYPE:
                final Cursor<IntType> intTypeCursor = (Cursor<IntType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeInt(intTypeCursor.get().get());
                }
                break;
            case LONGTYPE:
                final Cursor<LongType> longTypeCursor = (Cursor<LongType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeLong(longTypeCursor.get().get());
                }
                break;
            case SHORTTYPE:
                final Cursor<ShortType> shortTypeCursor = (Cursor<ShortType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeShort(shortTypeCursor.get().get());
                }
                break;
            case UNSIGNED12BITTYPE:
                final Cursor<Unsigned12BitType> unsigned12BitTypeCursor = (Cursor<Unsigned12BitType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeShort(unsigned12BitTypeCursor.get().get());
                }
                break;
            case UNSIGNEDBYTETYPE:
                final Cursor<UnsignedByteType> unsignedByteTypeCursor = (Cursor<UnsignedByteType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeByte(unsignedByteTypeCursor.get().get());
                }
                break;
            case UNSIGNEDINTTYPE:
                final Cursor<UnsignedIntType> unsignedIntTypeCursor = (Cursor<UnsignedIntType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeLong(unsignedIntTypeCursor.get().get());
                }
                break;
            case UNSIGNEDSHORTTYPE:
                final Cursor<UnsignedShortType> unsignedShortTypeCursor = (Cursor<UnsignedShortType>)cur;
                while (cur.hasNext()) {
                    cur.fwd();
                    out.writeInt(unsignedShortTypeCursor.get().get());
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported Pixeltype.");
        }

    }

}
