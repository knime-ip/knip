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
package org.knime.knip.core.ops.integralimage;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * Helper object for {@link IntegralImgND} that executes efficient region sum queries on the integral image.
 * 
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class IntegralImgSumAgent<T extends RealType<T>> {

    private final boolean[][] m_binaryRep;

    private final int[] m_signs;

    private final RandomAccess<T> m_iiRA;

    private final int m_dims;

    private final int m_points;

    /**
     * Initializes member variables that are needed for the nd sum calculation.
     * 
     * @param ii
     */
    public IntegralImgSumAgent(final RandomAccessibleInterval<T> ii) {
        m_iiRA = ii.randomAccess();

        // initialize the binary representation of the control points
        // (choose value from leftUpper (false) or rightLower (true))
        // and the respective signs for the points

        m_dims = ii.numDimensions();
        m_points = (int)Math.pow(2, m_dims);

        m_binaryRep = new boolean[m_points][m_dims];
        m_signs = new int[m_points];

        for (int i = 0; i < m_points; i++) {
            m_binaryRep[i] = getBinaryRep(i, m_dims);

            int ones = 0;
            for (int j = 0; j < m_dims; j++) {
                if (m_binaryRep[i][j]) {
                    ones++;
                }
            }

            m_signs[i] = (int)Math.pow(-1, m_dims - ones);
        }
    }

    /**
     * get the sum of the area including leftUpper and rightLower corner. The positions are defined with respect to the
     * original input image of the integral image. Therefore you can safely ignore the helper zeros of the integral
     * image.
     * 
     * @param leftUpper
     * @param rightLower
     * @return
     */
    public double getSum(final long[] leftUpper, final long[] rightLower) {

        // implemented according to
        // http://en.wikipedia.org/wiki/Summed_area_table high
        // dimensional variant

        final long[] position = new long[m_dims];
        double sum = 0;

        for (int i = 0; i < m_points; i++) {
            for (int j = 0; j < m_dims; j++) {
                if (m_binaryRep[i][j]) { // = 1
                    // +1 because the integral image
                    // contains a zero column
                    position[j] = rightLower[j] + 1l;
                } else { // = 0
                    // no +1 because integrating from 3..5
                    // inc. 3 & 5 means [5] - [2]
                    position[j] = leftUpper[j];
                }
            }

            m_iiRA.setPosition(position);
            sum += m_signs[i] * m_iiRA.get().getRealDouble();
        }

        return sum;
    }

    // gives as {0,1}^d all binary combinations 0,0,..,0 ...
    // 1,1,...,1
    private boolean[] getBinaryRep(final int i, final int d) {
        final char[] tmp = Long.toBinaryString(i).toCharArray();
        final boolean[] p = new boolean[d];
        for (int pos = 0; pos < tmp.length; pos++) {
            if (tmp[pos] == '1') {
                p[tmp.length - (pos + 1)] = true;
            }
        }

        return p;
    }
}
