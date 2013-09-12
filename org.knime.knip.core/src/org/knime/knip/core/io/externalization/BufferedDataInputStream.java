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
package org.knime.knip.core.io.externalization;

/* Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */
// What do we use in here?
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is intended for high performance I/O in scientific applications. It combines the functionality of the
 * BufferedInputStream and the DataInputStream as well as more efficient handling of arrays. This minimizes the number
 * of method calls that are required to read data. Informal tests of this method show that it can be as much as 10 times
 * faster than using a DataInputStream layered on a BufferedInputStream for writing large arrays. The performance gain
 * on scalars or small arrays will be less but there should probably never be substantial degradation of performance.
 * <p>
 * Many new read calls are added to allow efficient reading off array data. The read(Object o) call provides for reading
 * a primitive array of arbitrary type or dimensionality. There are also reads for each type of one dimensional array.
 * <p>
 * Note that there is substantial duplication of code to minimize method invocations. E.g., the floating point read
 * routines read the data as integer values and then convert to float. However the integer code is duplicated rather
 * than invoked. There has been considerable effort expended to ensure that these routines are efficient, but they could
 * easily be superceded if an efficient underlying I/O package were ever delivered as part of the basic Java libraries.
 * [This has subsequently happened with the NIO package and in an ideal universe these classes would be rewritten to
 * take advantage of NIO.]
 * <p>
 * Testing and timing routines are provided in the nom.tam.util.test.BufferedFileTester class.
 * 
 * Version 1.1: October 12, 2000: Fixed handling of EOF to return partially read arrays when EOF is detected. Version
 * 1.2: July 20, 2009: Added handling of very large Object arrays. Additional work is required to handle very large
 * arrays generally.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class BufferedDataInputStream extends BufferedInputStream {

    private long m_primitiveArrayCount;

    private final byte[] m_bb = new byte[8];

    /**
     * Use the BufferedInputStream constructor
     */
    public BufferedDataInputStream(final InputStream o) {
        super(o, 32768);
    }

    /**
     * Use the BufferedInputStream constructor
     */
    public BufferedDataInputStream(final InputStream o, final int bufLength) {
        super(o, bufLength);
    }

    /**
     * Read a byte array. This is the only method for reading arrays in the fundamental I/O classes.
     * 
     * @param obuf The byte array.
     * @param offset The starting offset into the array.
     * @param len The number of bytes to read.
     * @return The actual number of bytes read.
     */
    @Override
    public int read(final byte[] obuf, int offset, int len) throws IOException {

        int total = 0;

        while (len > 0) {

            // Use just the buffered I/O to get needed info.

            final int xlen = super.read(obuf, offset, len);
            if (xlen <= 0) {
                if (total == 0) {
                    throw new EOFException();
                } else {
                    return total;
                }
            } else {
                len -= xlen;
                total += xlen;
                offset += xlen;
            }
        }
        return total;

    }

    /**
     * Read a boolean value.
     * 
     * @return b The value read.
     */
    public boolean readBoolean() throws IOException {

        final int b = read();
        if (b == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Read a byte value in the range -128 to 127.
     * 
     * @return The byte value as a byte (see read() to return the value as an integer.
     */
    public byte readByte() throws IOException {
        return (byte)read();
    }

    /**
     * Read a byte value in the range 0-255.
     * 
     * @return The byte value as an integer.
     */
    public int readUnsignedByte() throws IOException {
        return read() | 0x00ff;
    }

    /**
     * Read an integer.
     * 
     * @return The integer value.
     */
    public int readInt() throws IOException {

        if (read(m_bb, 0, 4) < 4) {
            throw new EOFException();
        }
        final int i = (m_bb[0] << 24) | ((m_bb[1] & 0xFF) << 16) | ((m_bb[2] & 0xFF) << 8) | (m_bb[3] & 0xFF);
        return i;
    }

    /**
     * Read a 2-byte value as a short (-32788 to 32767)
     * 
     * @return The short value.
     */
    public short readShort() throws IOException {

        if (read(m_bb, 0, 2) < 2) {
            throw new EOFException();
        }

        final short s = (short)((m_bb[0] << 8) | (m_bb[1] & 0xFF));
        return s;
    }

    /**
     * Read a 2-byte value in the range 0-65536.
     * 
     * @return the value as an integer.
     */
    public int readUnsignedShort() throws IOException {

        if (read(m_bb, 0, 2) < 2) {
            throw new EOFException();
        }

        return ((m_bb[0] & 0xFF) << 8) | (m_bb[1] & 0xFF);
    }

    /**
     * Read a 2-byte value as a character.
     * 
     * @return The character read.
     */
    public char readChar() throws IOException {
        final byte[] b = new byte[2];

        if (read(b, 0, 2) < 2) {
            throw new EOFException();
        }

        final char c = (char)((b[0] << 8) | (b[1] & 0xFF));
        return c;
    }

    /**
     * Read a long.
     * 
     * @return The value read.
     */
    public long readLong() throws IOException {

        // use two ints as intermediarys to
        // avoid casts of bytes to longs...
        if (read(m_bb, 0, 8) < 8) {
            throw new EOFException();
        }
        final int i1 = (m_bb[0] << 24) | ((m_bb[1] & 0xFF) << 16) | ((m_bb[2] & 0xFF) << 8) | (m_bb[3] & 0xFF);
        final int i2 = (m_bb[4] << 24) | ((m_bb[5] & 0xFF) << 16) | ((m_bb[6] & 0xFF) << 8) | (m_bb[7] & 0xFF);
        return (((long)i1) << 32) | (i2 & 0x00000000ffffffffL);
    }

    /**
     * Read a 4 byte real number.
     * 
     * @return The value as a float.
     */
    public float readFloat() throws IOException {

        if (read(m_bb, 0, 4) < 4) {
            throw new EOFException();
        }

        final int i = (m_bb[0] << 24) | ((m_bb[1] & 0xFF) << 16) | ((m_bb[2] & 0xFF) << 8) | (m_bb[3] & 0xFF);
        return Float.intBitsToFloat(i);

    }

    /**
     * Read an 8 byte real number.
     * 
     * @return The value as a double.
     */
    public double readDouble() throws IOException {

        if (read(m_bb, 0, 8) < 8) {
            throw new EOFException();
        }

        final int i1 = (m_bb[0] << 24) | ((m_bb[1] & 0xFF) << 16) | ((m_bb[2] & 0xFF) << 8) | (m_bb[3] & 0xFF);
        final int i2 = (m_bb[4] << 24) | ((m_bb[5] & 0xFF) << 16) | ((m_bb[6] & 0xFF) << 8) | (m_bb[7] & 0xFF);

        return Double.longBitsToDouble((((long)i1) << 32) | (i2 & 0x00000000ffffffffL));
    }

    /**
     * Read a buffer and signal an EOF if the buffer cannot be fully read.
     * 
     * @param b The buffer to be read.
     */
    public void readFully(final byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /**
     * Read a buffer and signal an EOF if the requested elements cannot be read.
     * 
     * This differs from read(b,off,len) since that call will not signal and end of file unless no bytes can be read.
     * However both of these routines will attempt to fill their buffers completely.
     * 
     * @param b The input buffer.
     * @param off The requested offset into the buffer.
     * @param len The number of bytes requested.
     */
    public void readFully(final byte[] b, final int off, final int len) throws IOException {

        if ((off < 0) || (len < 0) || ((off + len) > b.length)) {
            throw new IOException("Attempt to read outside byte array");
        }

        if (read(b, off, len) < len) {
            throw new EOFException();
        }
    }

    /**
     * Skip the requested number of bytes. This differs from the skip call in that it takes an long argument and will
     * throw an end of file if the full number of bytes cannot be skipped.
     * 
     * @param toSkip The number of bytes to skip.
     */
    private byte[] m_skipBuf = null;

    public int skipBytes(final int toSkip) throws IOException {
        return (int)skipBytes((long)toSkip);
    }

    public long skipBytes(final long toSkip) throws IOException {

        long need = toSkip;

        while (need > 0) {

            try {
                final long got = skip(need);
                if (got > 0) {
                    need -= got;
                } else {
                    break;
                }
            } catch (final IOException e) {
                // Some input streams (process outputs) don't
                // allow
                // skipping. The kludgy solution here is to
                // try to do a read when we get an error in the
                // skip....
                // Real IO errors will presumably casue an error
                // in these reads too.
                if (m_skipBuf == null) {
                    m_skipBuf = new byte[8192];
                }
                while (need > 8192) {
                    final int got = read(m_skipBuf, 0, 8192);
                    if (got <= 0) {
                        break;
                    }
                    need -= got;
                }
                while (need > 0) {
                    final int got = read(m_skipBuf, 0, (int)need);
                    if (got <= 0) {
                        break;
                    }
                    need -= got;
                }
            }

        }

        if (need > 0) {
            throw new EOFException();
        } else {
            return toSkip;
        }
    }

    /**
     * Read a String in the UTF format. The implementation of this is very inefficient and use of this class is not
     * recommended for applications which will use this routine heavily.
     * 
     * @return The String that was read.
     * @throws IOException
     */
    public String readUTF() throws IOException {
        final DataInputStream d = new DataInputStream(this);

        // Punt on this one and use DataInputStream routines.
        try {
            return d.readUTF();
        } finally {
            d.close();
        }
    }

    /**
     * Emulate the deprecated DataInputStream.readLine() method. Originally we used the method itself, but Alan Brighton
     * suggested using a BufferedReader to eliminate the deprecation warning. This method is slow regardless.
     * 
     * @return The String read.
     * @throws IOException
     * @deprecated Use BufferedReader methods.
     */
    @Deprecated
    public String readLine() throws IOException {
        // Punt on this and use BufferedReader routines.
        final BufferedReader d = new BufferedReader(new InputStreamReader(this));
        // Punt on this one and use DataInputStream routines.
        try {
            return d.readLine();
        } finally {
            d.close();
        }
    }

    /**
     * This routine provides efficient reading of arrays of any primitive type. It is an error to invoke this method
     * with an object that is not an array of some primitive type. Note that there is no corresponding capability to
     * writePrimitiveArray in BufferedDataOutputStream to read in an array of Strings.
     * 
     * @param o The object to be read. It must be an array of a primitive type, or an array of Object's.
     * @deprecated See readLArray(Object o).
     */
    @Deprecated
    public int readPrimitiveArray(final Object o) throws IOException {

        // Note that we assume that only a single thread is
        // doing a primitive Array read at any given time. Otherwise
        // primitiveArrayCount can be wrong and also the
        // input data can be mixed up.

        m_primitiveArrayCount = 0;
        return (int)readLArray(o);
    }

    /**
     * Read an object. An EOF will be signaled if the object cannot be fully read. The getPrimitiveArrayCount() method
     * may then be used to get a minimum number of bytes read.
     * 
     * @param o The object to be read. This object should be a primitive (possibly multi-dimensional) array.
     * 
     * @returns The number of bytes read.
     * @deprecated See readLArray(Object) which handles large arrays properly.
     */
    @Deprecated
    public int readArray(final Object o) throws IOException {
        return (int)readLArray(o);
    }

    /**
     * Read an object. An EOF will be signaled if the object cannot be fully read. The getPrimitiveArrayCount() method
     * may then be used to get a minimum number of bytes read.
     * 
     * @param o The object to be read. This object should be a primitive (possibly multi-dimensional) array.
     * 
     * @returns The number of bytes read.
     */
    public long readLArray(final Object o) throws IOException {
        m_primitiveArrayCount = 0;
        return primitiveArrayRecurse(o);
    }

    /**
     * Read recursively over a multi-dimensional array.
     * 
     * @return The number of bytes read.
     */
    protected long primitiveArrayRecurse(final Object o) throws IOException {

        if (o == null) {
            return m_primitiveArrayCount;
        }

        final String className = o.getClass().getName();

        if (className.charAt(0) != '[') {
            throw new IOException("Invalid object passed to BufferedDataInputStream.readArray:" + className);
        }

        // Is this a multidimensional array? If so process recursively.
        if (className.charAt(1) == '[') {
            for (int i = 0; i < ((Object[])o).length; i += 1) {
                primitiveArrayRecurse(((Object[])o)[i]);
            }
        } else {

            // This is a one-d array. Process it using our special
            // functions.
            switch (className.charAt(1)) {
                case 'Z':
                    m_primitiveArrayCount += read((boolean[])o, 0, ((boolean[])o).length);
                    break;
                case 'B':
                    final int len = read((byte[])o, 0, ((byte[])o).length);
                    m_primitiveArrayCount += len;

                    if (len < ((byte[])o).length) {
                        throw new EOFException();
                    }
                    break;
                case 'C':
                    m_primitiveArrayCount += read((char[])o, 0, ((char[])o).length);
                    break;
                case 'S':
                    m_primitiveArrayCount += read((short[])o, 0, ((short[])o).length);
                    break;
                case 'I':
                    m_primitiveArrayCount += read((int[])o, 0, ((int[])o).length);
                    break;
                case 'J':
                    m_primitiveArrayCount += read((long[])o, 0, ((long[])o).length);
                    break;
                case 'F':
                    m_primitiveArrayCount += read((float[])o, 0, ((float[])o).length);
                    break;
                case 'D':
                    m_primitiveArrayCount += read((double[])o, 0, ((double[])o).length);
                    break;
                case 'L':

                    // Handle an array of Objects by recursion.
                    // Anything
                    // else is an error.
                    if (className.equals("[Ljava.lang.Object;")) {
                        for (int i = 0; i < ((Object[])o).length; i += 1) {
                            primitiveArrayRecurse(((Object[])o)[i]);
                        }
                    } else {
                        throw new IOException("Invalid object passed to BufferedDataInputStream.readArray: "
                                + className);
                    }
                    break;
                default:
                    throw new IOException("Invalid object passed to BufferedDataInputStream.readArray: " + className);
            }
        }
        return m_primitiveArrayCount;
    }

    /**
     * Ensure that the requested number of bytes are available in the buffer or throw an EOF if they cannot be obtained.
     * Note that this routine will try to fill the buffer completely.
     * 
     * @param The required number of bytes.
     */
    private void fillBuf(int need) throws IOException {

        if (count > pos) {
            System.arraycopy(buf, pos, buf, 0, count - pos);
            count -= pos;
            need -= count;
            pos = 0;
        } else {
            count = 0;
            pos = 0;
        }

        while (need > 0) {

            final int len = in.read(buf, count, buf.length - count);
            if (len <= 0) {
                throw new EOFException();
            }
            count += len;
            need -= len;
        }
    }

    /** Read a boolean array */
    public int read(final boolean[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Read a boolean array.
     */
    public int read(final boolean[] b, final int start, final int len) throws IOException {

        int i = start;
        try {
            for (; i < (start + len); i += 1) {

                if (pos >= count) {
                    fillBuf(1);
                }

                if (buf[pos] == 1) {
                    b[i] = true;
                } else {
                    b[i] = false;
                }
                pos += 1;
            }
        } catch (final EOFException e) {
            return eofCheck(e, i, start, 1);
        }
        return len;
    }

    /** Read a short array */
    public int read(final short[] s) throws IOException {
        return read(s, 0, s.length);
    }

    /** Read a short array */
    public int read(final short[] s, final int start, final int len) throws IOException {

        int i = start;
        try {
            for (; i < (start + len); i += 1) {
                if ((count - pos) < 2) {
                    fillBuf(2);
                }
                s[i] = (short)((buf[pos] << 8) | (buf[pos + 1] & 0xFF));
                pos += 2;
            }
        } catch (final EOFException e) {
            return eofCheck(e, i, start, 2);
        }
        return 2 * len;
    }

    /** Read a character array */
    public int read(final char[] c) throws IOException {
        return read(c, 0, c.length);
    }

    /** Read a character array */
    public int read(final char[] c, final int start, final int len) throws IOException {

        int i = start;
        try {
            for (; i < (start + len); i += 1) {
                if ((count - pos) < 2) {
                    fillBuf(2);
                }
                c[i] = (char)((buf[pos] << 8) | (buf[pos + 1] & 0xFF));
                pos += 2;
            }
        } catch (final EOFException e) {
            return eofCheck(e, i, start, 2);
        }
        return 2 * len;
    }

    /** Read an integer array */
    public int read(final int[] i) throws IOException {
        return read(i, 0, i.length);
    }

    /** Read an integer array */
    public int read(final int[] i, final int start, final int len) throws IOException {

        int ii = start;
        try {
            for (; ii < (start + len); ii += 1) {

                if ((count - pos) < 4) {
                    fillBuf(4);
                }

                i[ii] =
                        (buf[pos] << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8)
                                | (buf[pos + 3] & 0xFF);
                pos += 4;
            }
        } catch (final EOFException e) {
            return eofCheck(e, ii, start, 4);
        }
        return i.length * 4;
    }

    /** Read a long array */
    public int read(final long[] l) throws IOException {
        return read(l, 0, l.length);
    }

    /** Read a long array */
    public int read(final long[] l, final int start, final int len) throws IOException {

        int i = start;
        try {
            for (; i < (start + len); i += 1) {
                if ((count - pos) < 8) {
                    fillBuf(8);
                }
                final int i1 =
                        (buf[pos] << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8)
                                | (buf[pos + 3] & 0xFF);
                final int i2 =
                        (buf[pos + 4] << 24) | ((buf[pos + 5] & 0xFF) << 16) | ((buf[pos + 6] & 0xFF) << 8)
                                | (buf[pos + 7] & 0xFF);
                l[i] = (((long)i1) << 32) | (i2 & 0x00000000FFFFFFFFL);
                pos += 8;
            }

        } catch (final EOFException e) {
            return eofCheck(e, i, start, 8);
        }
        return 8 * len;
    }

    /** Read a float array */
    public int read(final float[] f) throws IOException {
        return read(f, 0, f.length);
    }

    /** Read a float array */
    public int read(final float[] f, final int start, final int len) throws IOException {

        int i = start;
        try {
            for (; i < (start + len); i += 1) {
                if ((count - pos) < 4) {
                    fillBuf(4);
                }
                final int t =
                        (buf[pos] << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8)
                                | (buf[pos + 3] & 0xFF);
                f[i] = Float.intBitsToFloat(t);
                pos += 4;
            }
        } catch (final EOFException e) {
            return eofCheck(e, i, start, 4);
        }
        return 4 * len;
    }

    /** Read a double array */
    public int read(final double[] d) throws IOException {
        return read(d, 0, d.length);
    }

    /** Read a double array */
    public int read(final double[] d, final int start, final int len) throws IOException {

        int i = start;
        try {
            for (; i < (start + len); i += 1) {

                if ((count - pos) < 8) {
                    fillBuf(8);
                }
                final int i1 =
                        (buf[pos] << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8)
                                | (buf[pos + 3] & 0xFF);
                final int i2 =
                        (buf[pos + 4] << 24) | ((buf[pos + 5] & 0xFF) << 16) | ((buf[pos + 6] & 0xFF) << 8)
                                | (buf[pos + 7] & 0xFF);
                d[i] = Double.longBitsToDouble((((long)i1) << 32) | (i2 & 0x00000000FFFFFFFFL));
                pos += 8;
            }
        } catch (final EOFException e) {
            return eofCheck(e, i, start, 8);
        }
        return 8 * len;
    }

    /**
     * For array reads return an EOF if unable to read any data.
     */
    private int eofCheck(final EOFException e, final int i, final int start, final int length) throws EOFException {

        if (i == start) {
            throw e;
        } else {
            return (i - start) * length;
        }
    }

    /** Represent the stream as a string */
    @Override
    public String toString() {
        return super.toString() + "[count=" + count + ",pos=" + pos + "]";
    }
}
