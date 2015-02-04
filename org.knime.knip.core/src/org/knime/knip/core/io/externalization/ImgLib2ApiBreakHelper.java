/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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
 * ---------------------------------------------------------------------
 *
 * Created on Jan 17, 2015 by Christian Dietz
 */
package org.knime.knip.core.io.externalization;

import java.io.IOException;

/**
 * This class provides helper methods to convert BitType and UnsignedByteType Images which were saved with ImgLib2 Beta
 * to proper ImgLib2 2.x.x Images
 *
 * @author Christian Dietz
 */
public class ImgLib2ApiBreakHelper {

    /**
     * Converts an int[] array which stores BitTypes to the new long[]
     *
     * @param numPix image size
     * @param stream containing the int[] array
     * @throws IOException
     */
    public static void bitTypeConvert(final long[] res, final int numPix, final BufferedDataInputStream stream)
            throws IOException {

        int[] tmp;
        if (numPix % Integer.SIZE == 0) {
            tmp = new int[numPix / Integer.SIZE];
        } else {
            tmp = new int[numPix / Integer.SIZE + 1];
        }

        stream.readLArray(tmp);

        // iterate over pixels
        for (int i = 0; i < numPix; i++) {
            longValueHelperBitType(res, i, bitValueHelper(i, tmp));
        }
    }

    /**
     * @param currentStorageArray
     * @param size
     * @param in
     * @throws IOException
     */
    public static void
            unsigned12BitTypeConvert(final long[] res, final int numPix, final BufferedDataInputStream stream)
                    throws IOException {

        long n = numPix * 12;
        if (n % 32 != 0) {
            n = n / 32 + 1;
        } else {
            n /= 32;
        }

        int[] tmp = new int[(int)n];

        stream.readLArray(tmp);

        // iterate over pixels
        for (int i = 0; i < numPix; i++) {
            int j1 = i * 12;
            int j2 = j1 + 1;
            int j3 = j1 + 2;
            int j4 = j1 + 3;
            int j5 = j1 + 4;
            int j6 = j1 + 5;
            int j7 = j1 + 6;
            int j8 = j1 + 7;
            int j9 = j1 + 8;
            int j10 = j1 + 9;
            int j11 = j1 + 10;
            int j12 = j1 + 11;

            short value = 0;

            if (bitValueHelper(j1, tmp)) {
                ++value;
            }
            if (bitValueHelper(j2, tmp)) {
                value += 2;
            }
            if (bitValueHelper(j3, tmp)) {
                value += 4;
            }
            if (bitValueHelper(j4, tmp)) {
                value += 8;
            }
            if (bitValueHelper(j5, tmp)) {
                value += 16;
            }
            if (bitValueHelper(j6, tmp)) {
                value += 32;
            }
            if (bitValueHelper(j7, tmp)) {
                value += 64;
            }
            if (bitValueHelper(j8, tmp)) {
                value += 128;
            }
            if (bitValueHelper(j9, tmp)) {
                value += 256;
            }
            if (bitValueHelper(j10, tmp)) {
                value += 512;
            }
            if (bitValueHelper(j11, tmp)) {
                value += 1024;
            }
            if (bitValueHelper(j12, tmp)) {
                value += 2048;
            }
            longValueHelperUnsigned12BitType(res, i, value);
        }
    }

    /**
     * @param res
     * @param i
     */
    private static long getValueAtPosition(final long[] res, final int i) {
        final long k = i * 12;
        final int i1 = (int)(k >>> 6); // k / 64;
        final long shift = k & 63; // k % 64;
        final long v = res[i1];
        final long antiShift = 64 - shift;
        if (antiShift < 12) {
            // Number split between two adjacent long
            final long v1 = (v >>> shift) & (4095 >>> (12 - antiShift)); // lower part, stored at the upper end
            final long v2 = (res[i1 + 1] & (4095 >>> antiShift)) << antiShift; // upper part, stored at the lower end
            return v1 | v2;
        } else {
            // Number contained inside a single long
            return (v >>> shift) & 4095;
        }
    }

    private static void longValueHelperBitType(final long[] res, final int i, final boolean value) {
        final int i1 = (i >>> 6);
        final long bit = 1l << (i & 63);
        // Clear or set the bit
        if (value) {
            res[i1] = res[i1] | bit;
        } else {
            res[i1] = res[i1] & ~bit;
        }
    }

    private static void longValueHelperUnsigned12BitType(final long[] res, final int i, final short value) {
        final long mask = 4095;
        final long k = i * 12;
        final int i1 = (int)(k >>> 6); // k / 64;
        final long shift = k & 63; // k % 64;
        final long safeValue = value & mask;
        final long antiShift = 64 - shift;
        final long v = res[i1];
        if (antiShift < 12) {
            // Number split between two adjacent longs
            // 1. Store the lower bits of safeValue at the upper bits of v1
            final long v1 = (v & (0xffffffffffffffffL >>> antiShift)) // clear upper bits, keep other values
                    | ((safeValue & (mask >>> (12 - antiShift))) << shift); // the lower part of safeValue, stored at the upper end
            res[i1] = v1;
            // 2. Store the upper bits of safeValue at the lower bits of v2
            final long v2 = (res[i1 + 1] & (0xffffffffffffffffL << (12 - antiShift))) // other
                    | (safeValue >>> antiShift); // upper part of safeValue, stored at the lower end
            res[i1 + 1] = v2;
        } else {
            // Number contained inside a single long
            if (0 == v) {
                // Trivial case
                res[i1] = safeValue << shift;
            } else {
                // Clear the bits first
                res[i1] = ((v & ~(mask << shift)) | (safeValue << shift));
            }
        }
    }
    private static boolean bitValueHelper(final int index, final int[] data) {
        final int arrayIndex = index / Integer.SIZE;
        final int arrayOffset = index % Integer.SIZE;
        final int entry = data[arrayIndex];
        final int value = (entry & (1 << arrayOffset));
        return value != 0;
    }

}
