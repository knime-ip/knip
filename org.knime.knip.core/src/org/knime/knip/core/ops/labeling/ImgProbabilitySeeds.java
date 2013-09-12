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
package org.knime.knip.core.ops.labeling;

import java.util.Arrays;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.iterableinterval.unary.MinMax;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

import org.knime.knip.core.data.LabelGenerator;

/**
 * Samples seeding points randomly according to the image pixel intensity values.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgProbabilitySeeds<T extends RealType<T>, L extends Comparable<L>> implements
        UnaryOperation<Img<T>, Labeling<L>> {

    private final int m_avgDistance;

    private final LabelGenerator<L> m_seedGen;

    public ImgProbabilitySeeds(final LabelGenerator<L> seedGen, final int avgDistance) {
        m_seedGen = seedGen;
        m_avgDistance = avgDistance;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Labeling<L> compute(final Img<T> input, final Labeling<L> output) {

        m_seedGen.reset();
        final Random rand = new Random();

        if (m_avgDistance == 1) {
            /*
             * if the seeding point probability is determined only
             * by the value of each single pixel
             */

            final RandomAccess<LabelingType<L>> out = output.randomAccess();
            final Cursor<T> in = input.cursor();
            final ValuePair<T, T> mm = Operations.compute(new MinMax<T>(), input);
            final double range = mm.b.getRealDouble() - mm.a.getRealDouble();
            final double min = mm.a.getRealDouble();

            while (in.hasNext()) {
                in.next();
                out.setPosition(in);
                if (rand.nextFloat() < ((min + in.get().getRealDouble()) / range)) {
                    out.get().setLabel(m_seedGen.nextLabel());
                }
            }

        } else {
            /*
             * if the seeding point probability is determined by the
             * individual pixel values, but a certain average
             * distance between labels should be garantied. TODO
             * here: improve efficiency
             */

            final long[] currentGridPos = new long[output.numDimensions()];
            final RandomAccess<LabelingType<L>> outLabRA =
                    Views.extendValue(output, output.firstElement().createVariable()).randomAccess();
            final RandomAccess<T> inImgRA =
                    Views.extendValue(input, input.firstElement().createVariable()).randomAccess();

            // cumulative distribution function (as
            // image)
            final long[] dim = new long[input.numDimensions()];
            Arrays.fill(dim, (m_avgDistance * 2) + 1);
            final Img<DoubleType> cdf = new ArrayImgFactory<DoubleType>().create(dim, new DoubleType());
            final Cursor<DoubleType> cdfCur = cdf.localizingCursor();

            while (currentGridPos[currentGridPos.length - 1] < input.dimension(currentGridPos.length - 1)) {

                /* regular grid */

                currentGridPos[0] += m_avgDistance;

                // select the position according
                // to the pixel intensity value
                // distribution in the
                // neighbourhood
                double sum = calcCdf(cdfCur, inImgRA, currentGridPos);
                sum = rand.nextDouble() * sum;
                retrieveRandPosition(cdfCur, outLabRA, sum, currentGridPos);
                outLabRA.get().setLabel(m_seedGen.nextLabel());

                // next position in the higher
                // dimensions than 0
                for (int i = 0; i < (output.numDimensions() - 1); i++) {
                    if (currentGridPos[i] > input.dimension(i)) {
                        currentGridPos[i] = 0;
                        currentGridPos[i + 1] += m_avgDistance;
                    }
                }

            }

        }

        return output;
    }

    private double calcCdf(final Cursor<DoubleType> cdfCur, final RandomAccess<T> inRA, final long[] currentGridPos) {

        cdfCur.reset();
        double sum = 0;
        while (cdfCur.hasNext()) {
            cdfCur.fwd();
            for (int i = 0; i < inRA.numDimensions(); i++) {
                inRA.setPosition((currentGridPos[i] - m_avgDistance) + cdfCur.getIntPosition(i), i);
            }

            // to avoid that there are equal entries in the cdf, we
            // add a small value for integer types (avoids a bias in
            // the random
            // position selection)

            if (inRA.get() instanceof IntegerType) {
                double val;
                if ((val = inRA.get().getRealDouble()) == 0) {
                    sum += Double.MIN_VALUE;
                } else {
                    sum += val;
                }
            } else {

                sum += inRA.get().getRealDouble();
            }
            cdfCur.get().set(sum);
        }

        return sum;

    }

    private void retrieveRandPosition(final Cursor<DoubleType> cdfCur, final RandomAccess<LabelingType<L>> labRA,
                                      final double randVal, final long[] currentGridPos) {

        cdfCur.reset();
        while (cdfCur.hasNext()) {
            cdfCur.fwd();
            if (cdfCur.get().getRealDouble() >= randVal) {
                for (int i = 0; i < labRA.numDimensions(); i++) {
                    labRA.setPosition((currentGridPos[i] - m_avgDistance) + cdfCur.getIntPosition(i), i);
                }
                return;
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<Img<T>, Labeling<L>> copy() {
        return new ImgProbabilitySeeds<T, L>(m_seedGen, m_avgDistance);
    }

}
