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
package org.knime.knip.core.algorithm.convolvers.filter.linear;

import net.imglib2.Cursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.DoubleAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;

/**
 * Creates n-dimensional gaussian kernel.
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Stephan Sellien, University of Konstanz
 */
public final class Gaussian extends ArrayImg<DoubleType, DoubleAccess> {

    /**
     * Factory method to make calculations before super() possible.
     * 
     * @param sigma sigma parameter
     * @param nrDimensions number of dimensions
     * @return a new {@link Gaussian} with given parameters.
     */
    public static Gaussian create(final double sigma, final int nrDimensions) {
        final int size = Math.max(3, (2 * (int)(3 * sigma + 0.5) + 1));
        long[] dims = new long[nrDimensions];
        for (int d = 0; d < nrDimensions; d++) {
            dims[d] = size;
        }
        return new Gaussian(sigma, dims);
    }

    /**
     * Creates a gaussian kernel of given size.
     * 
     * @param sigma sigma
     */
    private Gaussian(final double sigma, final long[] dims) {
        super(new DoubleArray(ArrayImgFactory.numEntitiesRangeCheck(dims, 1)), dims, 1);
        int nrDimensions = dims.length;

        double[] kernel = Util.createGaussianKernel1DDouble(sigma, true);
        // create a Type that is linked to the container
        final DoubleType linkedType = new DoubleType(this);

        // pass it to the native container
        setLinkedType(linkedType);

        Cursor<DoubleType> cursor = localizingCursor();
        while (cursor.hasNext()) {
            cursor.fwd();
            double result = 1.0f;
            for (int d = 0; d < nrDimensions; d++) {
                result *= kernel[cursor.getIntPosition(d)];
            }
            cursor.get().set(result);
        }

    }

}
