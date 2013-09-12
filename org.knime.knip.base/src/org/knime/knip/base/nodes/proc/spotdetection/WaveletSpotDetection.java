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
package org.knime.knip.base.nodes.proc.spotdetection;

import java.util.ArrayList;
import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.knip.core.ops.misc.MAD;
import org.knime.knip.core.ops.misc.MeanAbsoluteDeviation;
import org.knime.knip.core.ops.spotdetection.icybased.ATrousWaveletCreatorIcyBased;

/*
 * Implementation status:
 * Currently using B3SplineUDWT and WaveletConfigException from ICY both could be replaced by the imglib implementation
 * but the array specific code runs faster than standard convolution. *
 */

/**
 * Implementation of Extraction of spots in biological images using multiscale products (Pattern Recognition 35)<br>
 * Jean-Christophe Olivo-Marin<br>
 * <br>
 * Allows the detection of bright spots over dark background using a multiscale wavelet partition of the image.
 * 
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class WaveletSpotDetection<T extends RealType<T>> implements UnaryOperation<ImgPlus<T>, ImgPlus<BitType>> {

    private boolean[] m_enabled;

    private double[] m_factors;

    private final boolean m_useMeanMAD;

    /**
     * Wavelet based spot detection (e.g. for biological images). The enabled and factor parameters have to be of the
     * same length and specify the nr of used wavelet planes, which planes are used and a multiplication factor for fine
     * tuning of the auto threshold of each plane.
     * 
     * @param enabled true if the wavelet plane i should be used for spot detection
     * @param factor threshold factor for the wavelet plane i
     * @param useMeanMAD true => use {@link MeanAbsoluteDeviation} false => use {@link MAD}
     */
    public WaveletSpotDetection(final boolean[] enabled, final double[] factor, final boolean useMeanMAD) {
        m_factors = factor.clone();
        m_enabled = enabled.clone();
        m_useMeanMAD = useMeanMAD;

        // speed up if the last x levels are not enabled we can ignore
        // them
        if (m_enabled[m_enabled.length - 1] != true) {
            int maxIndex = 0;
            for (int i = 0; i < m_enabled.length; i++) {
                if (m_enabled[i]) {
                    maxIndex = i;
                }
            }

            m_enabled = new boolean[maxIndex + 1];
            m_factors = new double[maxIndex + 1];

            for (int i = 0; i <= maxIndex; i++) {
                m_enabled[i] = enabled[i];
                m_factors[i] = factor[i];
            }
        }
    }

    /**
     * combines the enabled wavelet levels by multiplication of their respective thresholded values. Returns one for
     * values > 0.
     * 
     * @param thresholdedCoefficients
     * @return a bitmask with the detected spots
     */
    private RandomAccessibleInterval<BitType>
            combine(final RandomAccessibleInterval<FloatType> thresholdedCoefficients,
                    final RandomAccessibleInterval<BitType> output) {

        // prepare random access and output
        final long sizeX = thresholdedCoefficients.dimension(0);
        final long sizeY = thresholdedCoefficients.dimension(1);

        final RandomAccess<FloatType> in = thresholdedCoefficients.randomAccess();
        final RandomAccess<BitType> out = output.randomAccess();

        // get all enabled indices
        final ArrayList<Integer> indiceList = new ArrayList<Integer>();
        for (int i = 0; i < m_enabled.length; i++) {
            if (m_enabled[i]) {
                indiceList.add(i);
            }
        }
        final Integer[] indices = indiceList.toArray(new Integer[]{});

        // calculate result
        for (long y = 0; y < sizeY; y++) {
            for (long x = 0; x < sizeX; x++) {
                float val = 1;
                for (int i = 0; i < indices.length; i++) {
                    in.setPosition(new long[]{x, y, indices[i]});
                    val *= in.get().getRealFloat();
                }
                out.setPosition(new long[]{x, y});

                if (val > 0) {
                    out.get().setOne();
                }
            }
        }

        return output;
    }

    @Override
    public ImgPlus<BitType> compute(final ImgPlus<T> input, final ImgPlus<BitType> output) {

        if ((input.numDimensions() != 2) || (output.numDimensions() != 2)) {
            throw new RuntimeException(new IncompatibleTypeException(input, "input and output have to be a 2D image"));
        }

        // create the wavelete coefficients for the required levels
        final Img<FloatType> coefficients =
                new ArrayImgFactory<FloatType>().create(new long[]{input.dimension(0), input.dimension(1),
                        (m_enabled.length + 1)}, new FloatType());

        final ArrayList<Integer> skip = new ArrayList<Integer>();
        skip.add(m_enabled.length); // don't need the residual level
        for (int i = 0; i < m_enabled.length; i++) {
            if (!m_enabled[i]) {
                skip.add(i);
            }
        }

        final ATrousWaveletCreatorIcyBased<T> twc = new ATrousWaveletCreatorIcyBased<T>(skip.toArray(new Integer[]{}));
        twc.compute(input, coefficients);

        // thresholding according to selected mean crit and combine
        filter(coefficients, skip.toArray(new Integer[]{}));
        combine(coefficients, output);

        return output;
    }

    @Override
    public UnaryOperation<ImgPlus<T>, ImgPlus<BitType>> copy() {
        return new WaveletSpotDetection<T>(m_enabled, m_factors, m_useMeanMAD);
    }

    /**
     * filters the enabled wavelet levels according to the calculated auto threshold. Threshold is determined as (3 *
     * avg / 0.67) * threhsold_factor where avg is the result of the selected avg method e.g. the MAD.
     * 
     * @param coefficients the coefficients that should be processed (they are altered)
     */
    private void filter(final RandomAccessibleInterval<FloatType> coefficients, final Integer... skipLevels) {
        // filter

        final long maxX = coefficients.max(0);
        final long maxY = coefficients.max(1);
        final long minX = coefficients.min(0);
        final long minY = coefficients.min(1);

        for (int i = 0; i < coefficients.dimension(2); i++) {
            if (!Arrays.asList(skipLevels).contains(i)) {
                double avg;

                final long[] max = new long[]{maxX, maxY, i};
                final long[] min = new long[]{minX, minY, i};
                final IntervalView<FloatType> scale = Views.interval(coefficients, new FinalInterval(min, max));

                if (m_useMeanMAD) {
                    avg =
                            new MeanAbsoluteDeviation<FloatType, DoubleType>().compute(scale, new DoubleType())
                                    .getRealDouble();
                } else {
                    // MAD
                    avg = new MAD<FloatType, DoubleType>().compute(scale, new DoubleType()).getRealDouble();
                }

                float thresh = (float)((3.0 * avg) / 0.67);
                thresh *= m_factors[i];

                // apply threshold
                final Cursor<FloatType> c = scale.cursor();

                while (c.hasNext()) {
                    final FloatType current = c.next();
                    if (current.get() < thresh) {
                        current.setZero();
                    }
                }

            }
        }
    }

}
