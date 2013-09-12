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
package org.knime.knip.base.nodes.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class FileReadWriteTest {

    private static String DIR = "/home/hornm/tmp/filetest/";

    public static void writeMultipleFiles(final Iterator<long[]> data) throws IOException {
        int i = 0;
        while (data.hasNext()) {
            final long[] buf = data.next();
            final File f = new File(DIR + (i++));
            final BufferedDataOutputStream out = new BufferedDataOutputStream(new FileOutputStream(f));
            out.write(buf);
            out.flush();
            out.close();
        }

    }

    public static void writeSingleFile(final Iterator<long[]> data) throws IOException {
        final File f = new File(DIR + "000bigfile");
        f.delete();
        while (data.hasNext()) {
            final long[] buf = data.next();
            final BufferedDataOutputStream out = new BufferedDataOutputStream(new FileOutputStream(f, true));
            out.write(buf);
            out.flush();
            out.close();
            System.out.println(f.length());
        }

    }

    public static void readMultipleFiles(final long[] indices, final int entityLength) throws FileNotFoundException {

        final Iterator<long[]> it = new Iterator<long[]>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < indices.length;
            }

            @Override
            public long[] next() {
                final File f = new File(DIR + indices[i]);
                try {
                    final BufferedDataInputStream in = new BufferedDataInputStream(new FileInputStream(f));
                    final long[] buf = new long[entityLength];

                    in.read(buf);
                    in.close();
                    verifyArray(buf, indices[i++]);
                    return buf;
                } catch (final FileNotFoundException e) {
                    e.printStackTrace();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                return null;

            }

            @Override
            public void remove() {

            }
        };

        while (it.hasNext()) {
            it.next();
        }

    }

    public static void readSingleFile(final long[] indices, final int entityLength) {

        final Iterator<long[]> it = new Iterator<long[]>() {

            File f = new File(DIR + "000bigfile");

            int i = 0;

            @Override
            public boolean hasNext() {
                return i < indices.length;
            }

            @Override
            public long[] next() {

                try {
                    final BufferedDataInputStream in = new BufferedDataInputStream(new FileInputStream(f));
                    in.skip(indices[i] * entityLength * (Long.SIZE / 8));
                    final long[] buf = new long[entityLength];
                    in.read(buf);
                    in.close();
                    verifyArray(buf, indices[i++]);
                    return buf;
                } catch (final FileNotFoundException e) {
                    e.printStackTrace();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                return null;

            }

            @Override
            public void remove() {

            }
        };

        while (it.hasNext()) {
            it.next();
        }

    }

    private static void verifyArray(final long[] buf, final long idx) {
        if ((buf[0] != idx) || (buf[buf.length - 1] != idx)) {
            throw new RuntimeException("wrong array");
        }
    }

    private static class LongArrayIterator implements Iterator<long[]> {

        int i = 0;

        private final int m_numEntities;

        private final int m_entityLength;

        public LongArrayIterator(final int numEntities, final int entityLength) {
            m_numEntities = numEntities;
            m_entityLength = entityLength;

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return i < m_numEntities;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long[] next() {
            final long[] buf = new long[m_entityLength];
            Arrays.fill(buf, i++);
            return buf;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            // TODO Auto-generated method stub

        }

    }

    // private static long[] createLongArray(int numEntities) {
    // long[] buf = new long[numEntities];
    // for (int i = 0; i < buf.length; i++) {
    // buf[i] = i;
    // }
    // return buf;
    // }
    //
    // private static void permuteArray(long[] buf) {
    // Random rand = new Random();
    // long tmp;
    // for (int i = 0; i < buf.length; i++) {
    // int pos1 = rand.nextInt(buf.length);
    // int pos2 = rand.nextInt(buf.length);
    //
    // // swap
    // tmp = buf[pos1];
    // buf[pos1] = buf[pos2];
    // buf[pos2] = tmp;
    // }
    //
    // }

    public static void main(final String[] args) throws IOException {

        writeSingleFile(new LongArrayIterator(10000, 1));

        // for (int numEntities = 1000; numEntities < 1000000;
        // numEntities += 1000) {
        // for (int entityLength = 100; entityLength < 10000;
        // entityLength += 100) {
        // Iterator<long[]> longArrIt = new LongArrayIterator(
        // numEntities, entityLength);
        //
        // startTime();
        // writeMultipleFiles(longArrIt);
        // stopTime("writeMultipleFiles " + numEntities
        // + " " + entityLength);
        //
        // longArrIt = new LongArrayIterator(numEntities,
        // entityLength);
        //
        // startTime();
        // writeSingleFile(longArrIt);
        // stopTime("writeSingleFile " + numEntities + " "
        // + entityLength);
        //
        // long[] indices = createLongArray(numEntities);
        // permuteArray(indices);
        //
        // startTime();
        // readMultipleFiles(indices, entityLength);
        // stopTime("readMultipleFiles " + numEntities
        // + " " + entityLength);
        //
        // startTime();
        // readSingleFile(indices, entityLength);
        // stopTime("readSingleFile " + numEntities + " "
        // + entityLength);
        // }
        // }

    }

    // private static long time;

    // private static void startTime() {
    // time = System.currentTimeMillis();
    // }
    //
    // private static void stopTime(String event) {
    // long milisec = System.currentTimeMillis() - time;
    // System.out.println(event + " " + milisec);
    // }

}
