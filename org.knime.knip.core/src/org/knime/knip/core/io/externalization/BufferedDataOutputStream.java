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
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is intended for high performance I/O in scientific applications. It combines the functionality of the
 * BufferedOutputStream and the DataOutputStream as well as more efficient handling of arrays. This minimizes the number
 * of method calls that are required to write data. Informal tests of this method show that it can be as much as 10
 * times faster than using a DataOutputStream layered on a BufferedOutputStream for writing large arrays. The
 * performance gain on scalars or small arrays will be less but there should probably never be substantial degradation
 * of performance.
 * <p>
 * Note that there is substantial duplication of code to minimize method invocations. However simple output methods were
 * used where empirical tests seemed to indicate that the simpler method did not cost any time. It seems likely that
 * most of these variations will be washed out across different compilers and users who wish to tune the method for
 * their particular system may wish to compare the the implementation of write(int[], int, int) with write(float[], int,
 * int).
 * <p>
 * Testing and timing for this class is peformed in the nom.tam.util.test.BufferedFileTester class.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class BufferedDataOutputStream extends BufferedOutputStream {

    /**
     * Use the BufferedOutputStream constructor
     * 
     * @param o An open output stream.
     */
    public BufferedDataOutputStream(final OutputStream o) {
        super(o, 32768);
    }

    /**
     * Use the BufferedOutputStream constructor
     * 
     * @param o An open output stream.
     * @param bufLength The buffer size.
     */
    public BufferedDataOutputStream(final OutputStream o, final int bufLength) {
        super(o, bufLength);
    }

    /**
     * Write a boolean value
     * 
     * @param b The value to be written. Externally true is represented as a byte of 1 and false as a byte value of 0.
     */
    public void writeBoolean(final boolean b) throws IOException {

        checkBuf(1);
        if (b) {
            buf[count++] = 1;
        } else {
            buf[count++] = 0;
        }
    }

    /**
     * Write a byte value.
     */
    public void writeByte(final int b) throws IOException {
        checkBuf(1);
        buf[count++] = (byte)b;
    }

    /**
     * Write an integer value.
     */
    public void writeInt(final int i) throws IOException {

        checkBuf(4);
        buf[count++] = (byte)(i >>> 24);
        buf[count++] = (byte)(i >>> 16);
        buf[count++] = (byte)(i >>> 8);
        buf[count++] = (byte)i;
    }

    /**
     * Write a short value.
     */
    public void writeShort(final int s) throws IOException {

        checkBuf(2);
        buf[count++] = (byte)(s >>> 8);
        buf[count++] = (byte)s;

    }

    /**
     * Write a char value.
     */
    public void writeChar(final int c) throws IOException {

        checkBuf(2);
        buf[count++] = (byte)(c >>> 8);
        buf[count++] = (byte)c;
    }

    /**
     * Write a long value.
     */
    public void writeLong(final long l) throws IOException {

        checkBuf(8);

        buf[count++] = (byte)(l >>> 56);
        buf[count++] = (byte)(l >>> 48);
        buf[count++] = (byte)(l >>> 40);
        buf[count++] = (byte)(l >>> 32);
        buf[count++] = (byte)(l >>> 24);
        buf[count++] = (byte)(l >>> 16);
        buf[count++] = (byte)(l >>> 8);
        buf[count++] = (byte)l;
    }

    /**
     * Write a float value.
     */
    public void writeFloat(final float f) throws IOException {

        checkBuf(4);

        final int i = Float.floatToIntBits(f);

        buf[count++] = (byte)(i >>> 24);
        buf[count++] = (byte)(i >>> 16);
        buf[count++] = (byte)(i >>> 8);
        buf[count++] = (byte)i;

    }

    /**
     * Write a double value.
     */
    public void writeDouble(final double d) throws IOException {

        checkBuf(8);
        final long l = Double.doubleToLongBits(d);

        buf[count++] = (byte)(l >>> 56);
        buf[count++] = (byte)(l >>> 48);
        buf[count++] = (byte)(l >>> 40);
        buf[count++] = (byte)(l >>> 32);
        buf[count++] = (byte)(l >>> 24);
        buf[count++] = (byte)(l >>> 16);
        buf[count++] = (byte)(l >>> 8);
        buf[count++] = (byte)l;

    }

    /**
     * Write a string using the local protocol to convert char's to bytes.
     * 
     * @param s The string to be written.
     */
    public void writeBytes(final String s) throws IOException {

        write(s.getBytes(), 0, s.length());
    }

    /**
     * Write a string as an array of chars.
     */
    public void writeChars(final String s) throws IOException {

        for (int i = 0; i < s.length(); i += 1) {
            writeChar(s.charAt(i));
        }
    }

    /**
     * Write a string as a UTF. Note that this class does not handle this situation efficiently since it creates new
     * DataOutputStream to handle each call.
     */
    public void writeUTF(final String s) throws IOException {

        // Punt on this one and use standard routines.
        final DataOutputStream d = new DataOutputStream(this);
        d.writeUTF(s);
        d.flush();
        d.close();
    }

    /**
     * This routine provides efficient writing of arrays of any primitive type. The String class is also handled but it
     * is an error to invoke this method with an object that is not an array of these types. If the array is
     * multidimensional, then it calls itself recursively to write the entire array. Strings are written using the
     * standard 1 byte format (i.e., as in writeBytes).
     * 
     * If the array is an array of objects, then writePrimitiveArray will be called for each element of the array.
     * 
     * @param o The object to be written. It must be an array of a primitive type, Object, or String.
     */
    public void writePrimitiveArray(final Object o) throws IOException {
        writeArray(o);
    }

    /**
     * This routine provides efficient writing of arrays of any primitive type. The String class is also handled but it
     * is an error to invoke this method with an object that is not an array of these types. If the array is
     * multidimensional, then it calls itself recursively to write the entire array. Strings are written using the
     * standard 1 byte format (i.e., as in writeBytes).
     * 
     * If the array is an array of objects, then writePrimitiveArray will be called for each element of the array.
     * 
     * @param o The object to be written. It must be an array of a primitive type, Object, or String.
     */
    public void writeArray(final Object o) throws IOException {
        final String className = o.getClass().getName();

        if (className.charAt(0) != '[') {
            throw new IOException("Invalid object passed to BufferedDataOutputStream.write" + className);
        }

        // Is this a multidimensional array? If so process recursively.
        if (className.charAt(1) == '[') {
            for (int i = 0; i < ((Object[])o).length; i += 1) {
                writeArray(((Object[])o)[i]);
            }
        } else {

            // This is a one-d array. Process it using our special
            // functions.
            switch (className.charAt(1)) {
                case 'Z':
                    write((boolean[])o, 0, ((boolean[])o).length);
                    break;
                case 'B':
                    write((byte[])o, 0, ((byte[])o).length);
                    break;
                case 'C':
                    write((char[])o, 0, ((char[])o).length);
                    break;
                case 'S':
                    write((short[])o, 0, ((short[])o).length);
                    break;
                case 'I':
                    write((int[])o, 0, ((int[])o).length);
                    break;
                case 'J':
                    write((long[])o, 0, ((long[])o).length);
                    break;
                case 'F':
                    write((float[])o, 0, ((float[])o).length);
                    break;
                case 'D':
                    write((double[])o, 0, ((double[])o).length);
                    break;
                case 'L':

                    // Handle two exceptions: an array of strings,
                    // or an
                    // array of objects. .
                    if (className.equals("[Ljava.lang.String;")) {
                        write((String[])o);
                    } else if (className.equals("[Ljava.lang.Object;")) {
                        for (int i = 0; i < ((Object[])o).length; i += 1) {
                            writeArray(((Object[])o)[i]);
                        }
                    } else {
                        throw new IOException("Invalid object passed to BufferedDataOutputStream.writeArray: "
                                + className);
                    }
                    break;
                default:
                    throw new IOException("Invalid object passed to BufferedDataOutputStream.writeArray: " + className);
            }
        }

    }

    /**
     * Write an array of booleans.
     */
    public void write(final boolean[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Write a segment of an array of booleans.
     */
    public void write(final boolean[] b, final int start, final int len) throws IOException {

        for (int i = start; i < (start + len); i += 1) {

            if ((count + 1) > buf.length) {
                checkBuf(1);
            }
            if (b[i]) {
                buf[count++] = 1;
            } else {
                buf[count++] = 0;
            }
        }
    }

    /**
     * Write an array of shorts.
     */
    public void write(final short[] s) throws IOException {
        write(s, 0, s.length);
    }

    /**
     * Write a segment of an array of shorts.
     */
    public void write(final short[] s, final int start, final int len) throws IOException {

        for (int i = start; i < (start + len); i += 1) {
            if ((count + 2) > buf.length) {
                checkBuf(2);
            }
            buf[count++] = (byte)(s[i] >> 8);
            buf[count++] = (byte)(s[i]);
        }
    }

    /**
     * Write an array of char's.
     */
    public void write(final char[] c) throws IOException {
        write(c, 0, c.length);
    }

    /**
     * Write a segment of an array of char's.
     */
    public void write(final char[] c, final int start, final int len) throws IOException {

        for (int i = start; i < (start + len); i += 1) {
            if ((count + 2) > buf.length) {
                checkBuf(2);
            }
            buf[count++] = (byte)(c[i] >> 8);
            buf[count++] = (byte)(c[i]);
        }
    }

    /**
     * Write an array of int's.
     */
    public void write(final int[] i) throws IOException {
        write(i, 0, i.length);
    }

    /**
     * Write a segment of an array of int's.
     */
    public void write(final int[] i, final int start, final int len) throws IOException {

        for (int ii = start; ii < (start + len); ii += 1) {
            if ((count + 4) > buf.length) {
                checkBuf(4);
            }

            buf[count++] = (byte)(i[ii] >>> 24);
            buf[count++] = (byte)(i[ii] >>> 16);
            buf[count++] = (byte)(i[ii] >>> 8);
            buf[count++] = (byte)(i[ii]);

        }

    }

    /**
     * Write an array of longs.
     */
    public void write(final long[] l) throws IOException {
        write(l, 0, l.length);
    }

    /**
     * Write a segement of an array of longs.
     */
    public void write(final long[] l, final int start, final int len) throws IOException {

        for (int i = start; i < (start + len); i += 1) {
            if ((count + 8) > buf.length) {
                checkBuf(8);
            }
            int t = (int)(l[i] >>> 32);

            buf[count++] = (byte)(t >>> 24);
            buf[count++] = (byte)(t >>> 16);
            buf[count++] = (byte)(t >>> 8);
            buf[count++] = (byte)(t);

            t = (int)(l[i]);

            buf[count++] = (byte)(t >>> 24);
            buf[count++] = (byte)(t >>> 16);
            buf[count++] = (byte)(t >>> 8);
            buf[count++] = (byte)(t);
        }
    }

    /**
     * Write an array of floats.
     */
    public void write(final float[] f) throws IOException {
        write(f, 0, f.length);
    }

    public void write(final float[] f, final int start, final int len) throws IOException {

        for (int i = start; i < (start + len); i += 1) {

            if ((count + 4) > buf.length) {
                checkBuf(4);
            }
            final int t = Float.floatToIntBits(f[i]);
            buf[count++] = (byte)(t >>> 24);
            buf[count++] = (byte)(t >>> 16);
            buf[count++] = (byte)(t >>> 8);
            buf[count++] = (byte)t;
        }
    }

    /**
     * Write an array of doubles.
     */
    public void write(final double[] d) throws IOException {
        write(d, 0, d.length);
    }

    public void write(final double[] d, final int start, final int len) throws IOException {

        for (int i = start; i < (start + len); i += 1) {
            if ((count + 8) > buf.length) {
                checkBuf(8);
            }
            final long t = Double.doubleToLongBits(d[i]);

            int ix = (int)(t >>> 32);

            buf[count++] = (byte)(ix >>> 24);
            buf[count++] = (byte)(ix >>> 16);
            buf[count++] = (byte)(ix >>> 8);
            buf[count++] = (byte)(ix);

            ix = (int)t;

            buf[count++] = (byte)(ix >>> 24);
            buf[count++] = (byte)(ix >>> 16);
            buf[count++] = (byte)(ix >>> 8);
            buf[count++] = (byte)ix;
        }

    }

    /**
     * Write a segment of an array of Strings. Equivalent to calling writeBytes for the selected elements.
     */
    public void write(final String[] s) throws IOException {

        // Do not worry about buffering this specially since the
        // strings may be of differing lengths.

        for (int i = 0; i < s.length; i += 1) {
            writeBytes(s[i]);
        }
    }

    /*
     * See if there is enough space to add something to the buffer.
     */
    protected void checkBuf(final int need) throws IOException {

        if ((count + need) > buf.length) {
            out.write(buf, 0, count);
            count = 0;
        }
    }
}
