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
package org.knime.knip.core.util;

import java.util.Arrays;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Lee Kamentsky
 */
public class NeighborhoodUtils {

    /**
     * Return an array of offsets to the 8-connected (or N-d equivalent) structuring element for the dimension space.
     * The structuring element is the list of offsets from the center to the pixels to be examined.
     * 
     * @param dimensions
     * @return the structuring element.
     */
    public static long[][] get8ConStructuringElement(final int dimensions) {
        int nElements = 1;
        for (int i = 0; i < dimensions; i++) {
            nElements *= 3;
        }
        nElements--;
        final long[][] result = new long[nElements][dimensions];
        final long[] position = new long[dimensions];
        Arrays.fill(position, -1);
        for (int i = 0; i < nElements; i++) {
            System.arraycopy(position, 0, result[i], 0, dimensions);
            /*
             * Special case - skip the center element.
             */
            if (i == ((nElements / 2) - 1)) {
                position[0] += 2;
            } else {
                for (int j = 0; j < dimensions; j++) {
                    if (position[j] == 1) {
                        position[j] = -1;
                    } else {
                        position[j]++;
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Return an array of offsets to the -connected (or N-d equivalent) structuring element for the dimension space. The
     * structuring element is the list of offsets from the center to the pixels to be examined.
     * 
     * @param dimensions
     * @return the structuring element.
     */
    public static long[][] get4ConStructuringElement(final int dimensions) {
        final int nElements = dimensions * 2;

        final long[][] result = new long[nElements][dimensions];
        for (int d = 0; d < dimensions; d++) {
            result[d * 2] = new long[dimensions];
            result[(d * 2) + 1] = new long[dimensions];
            result[d * 2][d] = -1;
            result[(d * 2) + 1][d] = 1;

        }
        return result;
    }

    /**
     * Rework the structuring element into a series of consecutive offsets so we can use Positionable.move to scan the
     * image array.
     */
    public static long[][] reworkStructuringElement(final long[][] structuringElement) {

        final int numDimensions = structuringElement[0].length;
        final long[][] strelMoves = new long[structuringElement.length][];
        final long[] currentOffset = new long[numDimensions];
        for (int i = 0; i < structuringElement.length; i++) {
            strelMoves[i] = new long[numDimensions];
            for (int j = 0; j < numDimensions; j++) {
                strelMoves[i][j] = structuringElement[i][j] - currentOffset[j];
                if (i > 0) {
                    currentOffset[j] += structuringElement[i][j] - structuringElement[i - 1][j];
                } else {
                    currentOffset[j] += structuringElement[i][j];
                }
            }
        }
        return strelMoves;
    }

}
