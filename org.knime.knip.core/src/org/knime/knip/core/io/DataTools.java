package org.knime.knip.core.io;

//
// DataTools.java
//

/*
 LOCI Bio-Formats package for reading and converting biological file formats.
 Copyright (C) 2005-@year@ Melissa Linkert, Curtis Rueden, Chris Allan,
 Eric Kjellman and Brian Loranger.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU Library General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Library General Public License for more details.

 You should have received a copy of the GNU Library General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A utility class with convenience methods for reading, writing and decoding words.
 * 
 * @author Curtis Rueden ctrueden at wisc.edu
 * @author Chris Allan callan at blackcat.ca
 */
public final class DataTools {

    // -- Constants --

    /** Timestamp formats. */
    public static final int UNIX = 0; // January 1, 1970

    public static final int COBOL = 1; // January 1, 1601

    /** Milliseconds until UNIX epoch. */
    public static final long UNIX_EPOCH = 0;

    public static final long COBOL_EPOCH = 11644444800000L;

    // -- Static fields --

    /**
     * Persistent byte array for calling {@link java.io.DataInput#readFully(byte[], int, int)} efficiently.
     */
    private static ThreadLocal<Object> eightBytes = new ThreadLocal<Object>() {
        @Override
        protected synchronized Object initialValue() {
            return new byte[8];
        }
    };

    // -- Constructor --

    private DataTools() {
    }

    // -- Data reading --

    /** Reads 1 signed byte [-128, 127]. */
    public static byte readSignedByte(final DataInput in) throws IOException {
        final byte[] b = (byte[])eightBytes.get();
        in.readFully(b, 0, 1);
        return b[0];
    }

    /** Reads 1 unsigned byte [0, 255]. */
    public static short readUnsignedByte(final DataInput in) throws IOException {
        short q = readSignedByte(in);
        if (q < 0) {
            q += 256;
        }
        return q;
    }

    /** Reads 2 signed bytes [-32768, 32767]. */
    public static short read2SignedBytes(final DataInput in, final boolean little) throws IOException {
        final byte[] b = (byte[])eightBytes.get();
        in.readFully(b, 0, 2);
        return bytesToShort(b, little);
    }

    /** Reads 2 unsigned bytes [0, 65535]. */
    public static int read2UnsignedBytes(final DataInput in, final boolean little) throws IOException {
        int q = read2SignedBytes(in, little);
        if (q < 0) {
            q += 65536;
        }
        return q;
    }

    /** Reads 4 signed bytes [-2147483648, 2147483647]. */
    public static int read4SignedBytes(final DataInput in, final boolean little) throws IOException {
        final byte[] b = (byte[])eightBytes.get();
        in.readFully(b, 0, 4);
        return bytesToInt(b, little);
    }

    /** Reads 4 unsigned bytes [0, 4294967296]. */
    public static long read4UnsignedBytes(final DataInput in, final boolean little) throws IOException {
        long q = read4SignedBytes(in, little);
        if (q < 0) {
            q += 4294967296L;
        }
        return q;
    }

    /** Reads 8 signed bytes [-9223372036854775808, 9223372036854775807]. */
    public static long read8SignedBytes(final DataInput in, final boolean little) throws IOException {
        final byte[] b = (byte[])eightBytes.get();
        in.readFully(b, 0, 8);
        return bytesToLong(b, little);
    }

    /** Reads 4 bytes in single precision IEEE format. */
    public static float readFloat(final DataInput in, final boolean little) throws IOException {
        return Float.intBitsToFloat(read4SignedBytes(in, little));
    }

    /** Reads 8 bytes in double precision IEEE format. */
    public static double readDouble(final DataInput in, final boolean little) throws IOException {
        return Double.longBitsToDouble(read8SignedBytes(in, little));
    }

    // -- Data writing --

    /** Writes a string to the given data output destination. */
    public static void writeString(final DataOutput out, final String s) throws IOException {
        final byte[] b = s.getBytes("UTF-8");
        out.write(b);
    }

    /** Writes an integer to the given data output destination. */
    public static void writeInt(final DataOutput out, final int v, final boolean little) throws IOException {
        if (little) {
            out.write(v & 0xFF);
            out.write((v >>> 8) & 0xFF);
            out.write((v >>> 16) & 0xFF);
            out.write((v >>> 24) & 0xFF);
        } else {
            out.write((v >>> 24) & 0xFF);
            out.write((v >>> 16) & 0xFF);
            out.write((v >>> 8) & 0xFF);
            out.write(v & 0xFF);
        }
    }

    /** Writes a short to the given data output destination. */
    public static void writeShort(final DataOutput out, final int v, final boolean little) throws IOException {
        if (little) {
            out.write(v & 0xFF);
            out.write((v >>> 8) & 0xFF);
        } else {
            out.write((v >>> 8) & 0xFF);
            out.write(v & 0xFF);
        }
    }

    // -- Word decoding --

    /**
     * Translates up to the first len bytes of a byte array beyond the given offset to a short. If there are fewer than
     * 2 bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static short bytesToShort(final byte[] bytes, final int off, int len, final boolean little) {
        if ((bytes.length - off) < len) {
            len = bytes.length - off;
        }
        short total = 0;
        for (int i = 0, ndx = off; i < len; i++, ndx++) {
            total |= (bytes[ndx] < 0 ? 256 + bytes[ndx] : (int)bytes[ndx]) << ((little ? i : len - i - 1) * 8);
        }
        return total;
    }

    /**
     * Translates up to the first 2 bytes of a byte array beyond the given offset to a short. If there are fewer than 2
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static short bytesToShort(final byte[] bytes, final int off, final boolean little) {
        return bytesToShort(bytes, off, 2, little);
    }

    /**
     * Translates up to the first 2 bytes of a byte array to a short. If there are fewer than 2 bytes in the array, the
     * MSBs are all assumed to be zero (regardless of endianness).
     */
    public static short bytesToShort(final byte[] bytes, final boolean little) {
        return bytesToShort(bytes, 0, 2, little);
    }

    /**
     * Translates up to the first len bytes of a byte array byond the given offset to a short. If there are fewer than 2
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static short bytesToShort(final short[] bytes, final int off, int len, final boolean little) {
        if ((bytes.length - off) < len) {
            len = bytes.length - off;
        }
        short total = 0;
        for (int i = 0, ndx = off; i < len; i++, ndx++) {
            total |= (bytes[ndx]) << ((little ? i : len - i - 1) * 8);
        }
        return total;
    }

    /**
     * Translates up to the first 2 bytes of a byte array byond the given offset to a short. If there are fewer than 2
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static short bytesToShort(final short[] bytes, final int off, final boolean little) {
        return bytesToShort(bytes, off, 2, little);
    }

    /**
     * Translates up to the first 2 bytes of a byte array to a short. If there are fewer than 2 bytes in the array, the
     * MSBs are all assumed to be zero (regardless of endianness).
     */
    public static short bytesToShort(final short[] bytes, final boolean little) {
        return bytesToShort(bytes, 0, 2, little);
    }

    /**
     * Translates up to the first len bytes of a byte array beyond the given offset to an int. If there are fewer than 4
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static int bytesToInt(final byte[] bytes, final int off, int len, final boolean little) {
        if ((bytes.length - off) < len) {
            len = bytes.length - off;
        }
        int total = 0;
        for (int i = 0, ndx = off; i < len; i++, ndx++) {
            total |= (bytes[ndx] < 0 ? 256 + bytes[ndx] : (int)bytes[ndx]) << ((little ? i : len - i - 1) * 8);
        }
        return total;
    }

    /**
     * Translates up to the first 4 bytes of a byte array beyond the given offset to an int. If there are fewer than 4
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static int bytesToInt(final byte[] bytes, final int off, final boolean little) {
        return bytesToInt(bytes, off, 4, little);
    }

    /**
     * Translates up to the first 4 bytes of a byte array to an int. If there are fewer than 4 bytes in the array, the
     * MSBs are all assumed to be zero (regardless of endianness).
     */
    public static int bytesToInt(final byte[] bytes, final boolean little) {
        return bytesToInt(bytes, 0, 4, little);
    }

    /**
     * Translates up to the first len bytes of a byte array beyond the given offset to an int. If there are fewer than 4
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static int bytesToInt(final short[] bytes, final int off, int len, final boolean little) {
        if ((bytes.length - off) < len) {
            len = bytes.length - off;
        }
        int total = 0;
        for (int i = 0, ndx = off; i < len; i++, ndx++) {
            total |= (bytes[ndx]) << ((little ? i : len - i - 1) * 8);
        }
        return total;
    }

    /**
     * Translates up to the first 4 bytes of a byte array beyond the given offset to an int. If there are fewer than 4
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static int bytesToInt(final short[] bytes, final int off, final boolean little) {
        return bytesToInt(bytes, off, 4, little);
    }

    /**
     * Translates up to the first 4 bytes of a byte array to an int. If there are fewer than 4 bytes in the array, the
     * MSBs are all assumed to be zero (regardless of endianness).
     */
    public static int bytesToInt(final short[] bytes, final boolean little) {
        return bytesToInt(bytes, 0, 4, little);
    }

    /**
     * Translates up to the first len bytes of a byte array beyond the given offset to a long. If there are fewer than 8
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static long bytesToLong(final byte[] bytes, final int off, int len, final boolean little) {
        if ((bytes.length - off) < len) {
            len = bytes.length - off;
        }
        long total = 0;
        for (int i = 0, ndx = off; i < len; i++, ndx++) {
            total |= (bytes[ndx] < 0 ? 256L + bytes[ndx] : (long)bytes[ndx]) << ((little ? i : len - i - 1) * 8);
        }
        return total;
    }

    /**
     * Translates up to the first 8 bytes of a byte array beyond the given offset to a long. If there are fewer than 8
     * bytes in the array, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static long bytesToLong(final byte[] bytes, final int off, final boolean little) {
        return bytesToLong(bytes, off, 8, little);
    }

    /**
     * Translates up to the first 8 bytes of a byte array to a long. If there are fewer than 8 bytes in the array, the
     * MSBs are all assumed to be zero (regardless of endianness).
     */
    public static long bytesToLong(final byte[] bytes, final boolean little) {
        return bytesToLong(bytes, 0, 8, little);
    }

    /**
     * Translates up to the first len bytes of a byte array beyond the given offset to a long. If there are fewer than 8
     * bytes to be translated, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static long bytesToLong(final short[] bytes, final int off, int len, final boolean little) {
        if ((bytes.length - off) < len) {
            len = bytes.length - off;
        }
        long total = 0;
        for (int i = 0, ndx = off; i < len; i++, ndx++) {
            total |= ((long)bytes[ndx]) << ((little ? i : len - i - 1) * 8);
        }
        return total;
    }

    /**
     * Translates up to the first 8 bytes of a byte array beyond the given offset to a long. If there are fewer than 8
     * bytes to be translated, the MSBs are all assumed to be zero (regardless of endianness).
     */
    public static long bytesToLong(final short[] bytes, final int off, final boolean little) {
        return bytesToLong(bytes, off, 8, little);
    }

    /**
     * Translates up to the first 8 bytes of a byte array to a long. If there are fewer than 8 bytes in the array, the
     * MSBs are all assumed to be zero (regardless of endianness).
     */
    public static long bytesToLong(final short[] bytes, final boolean little) {
        return bytesToLong(bytes, 0, 8, little);
    }

    // -- Byte swapping --

    public static short swap(final short x) {
        return (short)((x << 8) | ((x >> 8) & 0xFF));
    }

    public static char swap(final char x) {
        return (char)((x << 8) | ((x >> 8) & 0xFF));
    }

    public static int swap(final int x) {
        return ((swap((short)x) << 16) | (swap((short)(x >> 16)) & 0xFFFF));
    }

    public static long swap(final long x) {
        return (((long)swap((int)x) << 32) | (swap((int)(x >> 32)) & 0xFFFFFFFFL));
    }

    // -- Miscellaneous --

    /** Remove null bytes from a string. */
    public static String stripString(String toStrip) {
        final char[] toRtn = new char[toStrip.length()];
        int counter = 0;
        for (int i = 0; i < toRtn.length; i++) {
            if (toStrip.charAt(i) != 0) {
                toRtn[counter] = toStrip.charAt(i);
                counter++;
            }
        }
        toStrip = new String(toRtn);
        toStrip = toStrip.trim();
        return toStrip;
    }

    /** Check if two filenames have the same prefix. */
    public static boolean samePrefix(final String s1, final String s2) {
        if ((s1 == null) || (s2 == null)) {
            return false;
        }
        final int n1 = s1.indexOf(".");
        final int n2 = s2.indexOf(".");
        if ((n1 == -1) || (n2 == -1)) {
            return false;
        }

        final int slash1 = s1.lastIndexOf(File.pathSeparator);
        final int slash2 = s2.lastIndexOf(File.pathSeparator);

        final String sub1 = s1.substring((slash1 == -1) ? 0 : slash1 + 1, n1);
        final String sub2 = s2.substring((slash2 == -1) ? 0 : slash2 + 1, n2);
        return sub1.equals(sub2) || sub1.startsWith(sub2) || sub2.startsWith(sub1);
    }

    /**
     * Convert a byte array to the appropriate primitive type array. The 'bpp' parameter denotes the number of bytes in
     * the returned primitive type (e.g. if bpp == 2, we should return an array of type short). If 'fp' is set and bpp
     * == 4 or bpp == 8, then return floats or doubles.
     */
    public static void fillDataArray(final Object alloc, final byte[] b, final int bpp, final boolean fp,
                                     final boolean little) {

        if (alloc instanceof byte[]) {
            System.arraycopy(b, 0, alloc, 0, b.length);
            return;
        }

        if (alloc instanceof int[]) {
            if ((bpp != 4) || fp) {
                throw new RuntimeException("Incompatible types in makeDataArray");
            }

            final int[] i = (int[])alloc;
            for (int j = 0; j < i.length; j++) {
                i[j] = bytesToInt(b, j * 4, 4, little);
            }

            return;
        }

        if (alloc instanceof long[]) {
            if ((bpp != 8) || fp) {
                throw new RuntimeException("Incompatible types in makeDataArray");
            }

            final long[] l = (long[])alloc;
            for (int i = 0; i < l.length; i++) {
                l[i] = bytesToLong(b, i * 8, 8, little);
            }
            return;
        }

        if (alloc instanceof double[]) {
            if ((bpp != 8) || !fp) {
                throw new RuntimeException("Incompatible types in makeDataArray");
            }

            final double[] d = (double[])alloc;
            for (int i = 0; i < d.length; i++) {
                d[i] = Double.longBitsToDouble(bytesToLong(b, i * 8, 8, little));
            }

            return;
        }

        if (alloc instanceof float[]) {
            if ((bpp != 4) || !fp) {
                throw new RuntimeException("Incompatible types in makeDataArray");
            }

            final float[] f = (float[])alloc;
            for (int i = 0; i < f.length; i++) {
                f[i] = Float.intBitsToFloat(bytesToInt(b, i * 4, 4, little));
            }

            return;
        }

        if (alloc instanceof short[]) {
            if ((bpp != 2) || fp) {
                throw new RuntimeException("Incompatible types in makeDataArray");
            }

            final short[] s = (short[])alloc;
            for (int i = 0; i < s.length; i++) {
                s[i] = bytesToShort(b, i * 2, 2, little);
            }

            return;
        }

        throw new RuntimeException("No compatible type found in makeDataArray");
    }

    /**
     * Convert a byte array to the appropriate primitive type array. The 'bpp' parameter denotes the number of bytes in
     * the returned primitive type (e.g. if bpp == 2, we should return an array of type short). If 'fp' is set and bpp
     * == 4 or bpp == 8, then return floats or doubles.
     */
    public static Object makeDataArray(final byte[] b, final int bpp, final boolean fp, final boolean little) {
        if (bpp == 1) {
            return b;
        } else if (bpp == 2) {
            final short[] s = new short[b.length / 2];
            for (int i = 0; i < s.length; i++) {
                s[i] = bytesToShort(b, i * 2, 2, little);
            }
            return s;
        } else if ((bpp == 4) && fp) {
            final float[] f = new float[b.length / 4];
            for (int i = 0; i < f.length; i++) {
                f[i] = Float.intBitsToFloat(bytesToInt(b, i * 4, 4, little));
            }
            return f;
        } else if (bpp == 4) {
            final int[] i = new int[b.length / 4];
            for (int j = 0; j < i.length; j++) {
                i[j] = bytesToInt(b, j * 4, 4, little);
            }
            return i;
        } else if ((bpp == 8) && fp) {
            final double[] d = new double[b.length / 8];
            for (int i = 0; i < d.length; i++) {
                d[i] = Double.longBitsToDouble(bytesToLong(b, i * 8, 8, little));
            }
            return d;
        } else if (bpp == 8) {
            final long[] l = new long[b.length / 8];
            for (int i = 0; i < l.length; i++) {
                l[i] = bytesToLong(b, i * 8, 8, little);
            }
            return l;
        }
        return null;
    }

    /**
     * Normalize the given float array so that the minimum value maps to 0.0 and the maximum value maps to 1.0.
     */
    public static float[] normalizeFloats(final float[] data) {
        final float[] rtn = new float[data.length];

        // make a quick pass through to determine the real min and max
        // values

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (int i = 0; i < data.length; i++) {
            if (data[i] < min) {
                min = data[i];
            }
            if (data[i] > max) {
                max = data[i];
            }
        }

        // now normalize; min => 0.0, max => 1.0

        for (int i = 0; i < rtn.length; i++) {
            rtn[i] = data[i] / max;
            if (rtn[i] < 0f) {
                rtn[i] = 0f;
            }
            if (rtn[i] > 1f) {
                rtn[i] = 1f;
            }
        }

        return rtn;
    }

    // -- Date handling --

    /** Converts the given timestamp into an ISO 8061 date. */
    public static String convertDate(final long stamp, final int format) {
        // see http://www.merlyn.demon.co.uk/critdate.htm for more
        // information
        // on
        // dates than you will ever need (or want)

        long ms = stamp;

        switch (format) {
            case COBOL:
                ms -= COBOL_EPOCH;
                break;
        }

        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        final StringBuffer sb = new StringBuffer();

        final Date d = new Date(ms);

        fmt.format(d, sb, new FieldPosition(0));
        return sb.toString();
    }

}
