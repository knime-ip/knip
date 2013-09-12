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
package org.knime.knip.core.ops.spotdetection.icybased;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/*
 * This OP uses the two classes B3SplineUDWT and WaveletConfigException from icy
 */

/**
 * Op to create a a trous wavelet decomposition of a 2D image as stack on the z axis.
 * 
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ATrousWaveletCreatorIcyBased<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> {

    private final Integer[] m_skipLevels;

    public ATrousWaveletCreatorIcyBased() {
        m_skipLevels = new Integer[]{};
    }

    /**
     * @param skipLevels at these indices the output will only contain temporary results and not the wavelet plane. This
     *            can be used to speed up the computations if not all wavelet planes are required.
     */
    public ATrousWaveletCreatorIcyBased(final Integer... skipLevels) {
        m_skipLevels = skipLevels;
    }

    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> copy() {
        return new ATrousWaveletCreatorIcyBased<T>(m_skipLevels);
    }

    /**
     * Computes a a trous wavelet representation with output.z - 1 wavelet planes W_i and one residual plane A_z.
     * Depending on the {@link #m_skipLevels} parameter some of the planes may contain only temporary results.<br>
     * <br>
     * The image can be recomputed as follows A_z + W_i | i � (0..z-1) <br>
     * sum(output_0 .. output_z-1) + output_z<br>
     * This works only if no skip levels are specified.
     */
    @Override
    public RandomAccessibleInterval<FloatType> compute(final RandomAccessibleInterval<T> input,
                                                       final RandomAccessibleInterval<FloatType> output) {

        if ((input.numDimensions() != 2) || (output.numDimensions() != 3)) {
            throw new RuntimeException(new IncompatibleTypeException(input,
                    "input has to be a 2D image, output a 3D image"));
        }

        if (output.dimension(2) < 2) {
            throw new RuntimeException(new IncompatibleTypeException(input,
                    "output requires at least 2 XY planes i.e.  {[0..sizeX], [0..sizeY], [0..a] | a >= 1}"));
        }

        final long shortSize = Math.min(input.dimension(0), input.dimension(1));
        if (shortSize < getMinSize(output.dimension(2) - 1)) {
            throw new RuntimeException("image to small (to many wavelet levels)");
        }

        return createCoefficients(input, output);
    }

    /**
     * uses B3SplineUDWT from icy to generate the wavelet coefficients.
     * 
     * @param input2D a 2d image slice
     * @return RandomAccessibleInterval with x,y dimension according to the input and i+1 slices in dimension unknown
     *         holding i wavelet coefficients and the residual level.
     */
    private RandomAccessibleInterval<FloatType>
            createCoefficients(final RandomAccessibleInterval<T> input2D,
                               final RandomAccessibleInterval<FloatType> outputStack) {

        // make it 1D
        final int sizeX = (int)input2D.dimension(0);
        final int sizeY = (int)input2D.dimension(1);

        final float dataIn[] = new float[sizeX * sizeY];
        final Cursor<T> curso = Views.flatIterable(input2D).cursor();

        int k = 0;
        while (curso.hasNext()) {
            dataIn[k] = curso.next().getRealFloat();
            k++;
        }

        // decompose the image
        final B3SplineUDWT waveletTransform = new B3SplineUDWT();

        try {

            // calculate coefficients (wavelet scaleX - wavelet scaleY)
            final float[][] coefficients =
                    waveletTransform
                            .b3WaveletCoefficients2D(waveletTransform
                                                             .b3WaveletScales2D(dataIn, sizeX, sizeY,
                                                                                (int)(outputStack.dimension(2) - 1)),
                                                     dataIn, (int)(outputStack.dimension(2) - 1), sizeX * sizeY);

            // write it back into an image
            final RandomAccess<FloatType> ra = outputStack.randomAccess();

            for (int i = 0; i < coefficients.length; i++) {
                for (int j = 0; j < coefficients[0].length; j++) {

                    final int x = j % sizeX;
                    final int y = j / sizeX;

                    ra.setPosition(new int[]{x, y, i});
                    ra.get().setReal(coefficients[i][j]);
                }
            }
        } catch (final WaveletConfigException e1) {
            throw new RuntimeException(e1);
        }

        return outputStack;
    }

    private long getMinSize(final long levels) {
        return 5 + ((long)(Math.pow(2, levels - 1)) * 4);
    }
}
