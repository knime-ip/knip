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
package org.knime.knip.core.ops.metadata;

import java.util.ArrayList;
import java.util.Collections;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class DimSwapper {

    /**
     * <pre>
     * mapping[0] = 1; // X &lt;- Y, Y becomes X
     * mapping[1] = 2; // Y &lt;- C, C becomes Y
     * mapping[2] = 0; // C &lt;- X, X becomes C
     * </pre>
     *
     * @param op
     * @param backMapping
     * @param minimum the minimum of the according cell (the offset of the given cell)
     * @return image with swapped dimensions
     */
    public synchronized static <T> RandomAccessibleInterval<T> swap(final RandomAccessibleInterval<T> op,
                                                                    final int[] backMapping, final long[] minimum) {

        final int nDims = op.numDimensions();
        for (int i = 0; i < nDims; i++) {
            if (backMapping[i] >= nDims) {
                throw new IllegalArgumentException("Dimension Mapping is out of bounds");
            }
        }

        int[] backMappingCopy = backMapping.clone();
        long[] permutedMinimum = minimum.clone();

        RandomAccessibleInterval<T> permuted = op;
        int[] mapping = backMappingCopy.clone();

        // Swapping of Dimensions to fulfill the mapping resulting in an ordered RandomAccessibleInterval
        ArrayList<Integer> swappingState = new ArrayList<Integer>(nDims);
        for (int i = 0; i < nDims; i++) {
            swappingState.add(i);
        }
        for (int d = 0; d < nDims; d++) {
            if (mapping[d] == swappingState.get(d)) {
                continue;
            }

            int dimIndex = swappingState.indexOf(mapping[d]);
            permuted = Views.permute(permuted, d, dimIndex);

            //TODO: Please test @gab1one
            permutedMinimum = swap(permutedMinimum, d, dimIndex);
            Collections.swap(swappingState, d, dimIndex);
        }

        return permuted;
    }

    /**
     * @param permutedMinimum
     * @param d
     * @param dimIndex
     * @return
     */
    private synchronized static long[] swap(final long[] permutedMinimum, final int d, final int dimIndex) {
        long tmp = permutedMinimum[d];
        permutedMinimum[d] = permutedMinimum[dimIndex];
        permutedMinimum[dimIndex] = tmp;
        return permutedMinimum;
    }
}
