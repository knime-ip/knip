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
import java.util.Collection;

import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.ops.operation.iterableinterval.unary.Centroid;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.core.algorithm.PolarImageFactory;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class Centralize<T extends RealType<T>, L extends Comparable<L>> implements
        BinaryOutputOperation<Img<T>, Labeling<L>, Labeling<L>> {

    private final int m_radius;

    private final int m_numAngles;

    private final int m_maxIterations;

    private final RulebasedLabelFilter<L> m_filter;

    public Centralize(final RulebasedLabelFilter<L> filter, final int radius, final int numAngles,
                      final int maxIterations) {
        m_radius = radius;
        m_numAngles = numAngles;
        m_maxIterations = maxIterations;
        m_filter = filter;
    }

    @Override
    public Labeling<L> compute(final Img<T> img, final Labeling<L> labeling, final Labeling<L> r) {
        if (img.numDimensions() != 2) {
            throw new IllegalArgumentException("Only labelings / images with dimensionality = 2  are allowed");
        }

        final T val = img.firstElement().createVariable();
        val.setReal(val.getMinValue());

        final Centroid centroidOp = new Centroid();
        final CentralizeOnePoint<T> centralizeOnePointOp =
                new CentralizeOnePoint<T>(new PolarImageFactory<T>(Views.extendMirrorDouble(img)), m_maxIterations,
                        m_radius, m_numAngles);

        final RandomAccess<LabelingType<L>> resAccess = r.randomAccess();
        final RandomAccess<LabelingType<L>> srcAccess = labeling.randomAccess();

        final long[] posBuffer = new long[resAccess.numDimensions()];

        final Collection<L> labels = labeling.getLabels();
        for (final L label : labels) {
            if (!m_filter.isValid(label)) {
                continue;
            }

            final IterableInterval<T> labelRoi =
                    labeling.getIterableRegionOfInterest(label).getIterableIntervalOverROI(img);

            final double[] centroid = centroidOp.compute(labelRoi, new double[labelRoi.numDimensions()]);

            final long[] centroidAsLong = new long[centroid.length];
            for (int d = 0; d < centroid.length; d++) {
                centroidAsLong[d] = Math.round(centroid[d]);
            }

            Arrays.fill(posBuffer, 0);
            srcAccess.setPosition(centroidAsLong);
            centralizeOnePointOp.compute(centroidAsLong, posBuffer);

            resAccess.setPosition(posBuffer);
            resAccess.get().set(srcAccess.get());
        }
        return r;
    }

    public Labeling<L> createType(final Img<T> src, final Labeling<L> src2, final long[] dims) {

        return src2.<L> factory().create(dims);
    }

    public long[] resultDims(final Interval srcOp1, final Interval srcOp2) {
        final long[] dims = new long[srcOp1.numDimensions()];
        srcOp1.dimensions(dims);

        return dims;
    }

    @Override
    public BinaryOutputOperation<Img<T>, Labeling<L>, Labeling<L>> copy() {
        return new Centralize<T, L>(m_filter, m_radius, m_numAngles, m_maxIterations);
    }

    @Override
    public BinaryObjectFactory<Img<T>, Labeling<L>, Labeling<L>> bufferFactory() {
        return new BinaryObjectFactory<Img<T>, Labeling<L>, Labeling<L>>() {

            @Override
            public Labeling<L> instantiate(final Img<T> op0, final Labeling<L> op1) {
                return createType(op0, op1, resultDims(op0, op1));
            }
        };
    }
}
