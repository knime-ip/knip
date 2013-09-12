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

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author schoenen
 */
public final class ApacheMathTools {

    private ApacheMathTools() {
        // Utility
    }

    /**
     * Creates a numDims dimensions image where all dimensions d<numDims are of size 1 and the last dimensions contains
     * the vector.
     * 
     * @param <R>
     * @param ar
     * @param type
     * @param numDims number of dimensions
     * @return
     */
    public static <R extends RealType<R>> Img<R> vectorToImage(final RealVector ar, final R type, final int numDims,
                                                               final ImgFactory<R> fac) {
        final long[] dims = new long[numDims];

        for (int i = 0; i < (dims.length - 1); i++) {
            dims[i] = 1;
        }
        dims[dims.length - 1] = ar.getDimension();
        final Img<R> res = fac.create(dims, type);
        final Cursor<R> c = res.cursor();
        while (c.hasNext()) {
            c.fwd();
            c.get().setReal(ar.getEntry(c.getIntPosition(numDims - 1)));
        }

        return res;
    }

    /**
     * 
     * @param <R>
     * @param img A two dimensional image.
     * @return
     */
    public static <R extends RealType<R>> RealMatrix toMatrix(final Img<R> img) {

        assert img.numDimensions() == 2;
        final RealMatrix ret = new BlockRealMatrix((int)img.dimension(0), (int)img.dimension(1));
        final Cursor<R> c = img.cursor();
        while (c.hasNext()) {
            c.fwd();
            ret.setEntry(c.getIntPosition(0), c.getIntPosition(1), c.get().getRealDouble());
        }
        return ret;
    }

}
