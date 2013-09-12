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

import java.awt.Polygon;

import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.type.logic.BitType;

import org.knime.knip.core.algorithm.BresenhamAlgorithm;
import org.knime.knip.core.data.algebra.RealVector;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PolygonTools {

    private PolygonTools() {
        // hidden contructor as it's an utility class
    }

    /**
     * Retrieves all positions of a line at the given position, specified direction and radius.
     * 
     * @param pos the center position (2 dim!)
     * @param dir the direction (2 dim!)
     * @param radius the radius
     * @return the resulting positions, whereas the number of positions equals 2*radius+1
     */
    public static int[][] getLineAt(final int[] pos, final float[] dir, final int radius) {
        new RealVector(dir).norm2().mapMultiply(radius).localize(dir);

        int x = Math.abs(Math.round(dir[0]));
        int y = Math.abs(Math.round(dir[1]));

        // garanties that the number of pixels between the two IntPoints
        // is
        // twice
        // the radius (instead of the euclidean distance)
        if (x > y) {
            x += radius - x;
            y += (int)Math.round((dir[1] / dir[0]) * (double)(radius - x));
        } else if (x < y) {
            y += radius - y;
            x += (int)Math.round((dir[0] / dir[1]) * (double)(radius - y));
        } else {
            x += radius - x;
            y += radius - y;
        }

        y *= (int)Math.signum(dir[1]);
        x *= (int)Math.signum(dir[0]);

        final int[][] dir1 = BresenhamAlgorithm.rasterizeLine(new int[]{pos[0] - x, pos[1] - y}, pos);
        final int[][] dir2 = BresenhamAlgorithm.rasterizeLine(new int[]{pos[0] + x, pos[1] + y}, pos);
        final int[][] res = new int[(radius * 2) + 1][2];

        for (int i = 0; i < dir1.length; i++) {
            res[i] = dir1[i];
        }
        for (int i = 0; i < dir2.length; i++) {
            res[i + radius + 1] = dir2[dir2.length - i - 1];
        }
        res[radius] = pos;
        return res;

    }

    /**
     * Extracts a polygon of a 2D binary image using the Square Tracing Algorithm (be aware of its drawbacks, e.g. if
     * the pattern is 4-connected!)
     * 
     * @param img the image, note that only the first and second dimension are taken into account
     * @param offset an offset for the points to be set in the new polygon
     * @return
     */
    public static Polygon extractPolygon(final RandomAccessibleInterval<BitType> img, final int[] offset) {
        final RandomAccess<BitType> cur =
                new ExtendedRandomAccessibleInterval<BitType, RandomAccessibleInterval<BitType>>(img,
                        new OutOfBoundsConstantValueFactory<BitType, RandomAccessibleInterval<BitType>>(new BitType(
                                false))).randomAccess();
        boolean start = false;
        // find the starting point
        for (int i = 0; i < img.dimension(0); i++) {
            for (int j = 0; j < img.dimension(1); j++) {
                cur.setPosition(i, 0);
                cur.setPosition(j, 1);
                if (cur.get().get()) {
                    cur.setPosition(i, 0);
                    cur.setPosition(j, 1);
                    start = true;
                    break;
                }
            }
            if (start) {
                break;
            }
        }
        int dir = 1;
        int dim = 0;
        final int[] startPos = new int[]{cur.getIntPosition(0), cur.getIntPosition(1)};
        final Polygon p = new Polygon();
        while (!((cur.getIntPosition(0) == startPos[0]) && (cur.getIntPosition(1) == startPos[1]) && (dim == 0)
                && (dir == 1) && !start)) {
            if (cur.get().get()) {
                p.addPoint(offset[0] + cur.getIntPosition(0), offset[1] + cur.getIntPosition(1));
                cur.setPosition(cur.getIntPosition(dim) - dir, dim);
                if (((dim == 1) && (dir == 1)) || ((dim == 1) && (dir == -1))) {
                    dir *= -1;
                }
            } else {
                cur.setPosition(cur.getIntPosition(dim) + dir, dim);
                if (((dim == 0) && (dir == 1)) || ((dim == 0) && (dir == -1))) {
                    dir *= -1;
                }
            }

            dim = (dim + 1) % 2;
            start = false;
        }
        return p;
    }
}
