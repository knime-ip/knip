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
package org.knime.knip.core.features.seg;

import java.util.ArrayList;
import java.util.BitSet;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.TypedAxis;
import net.imglib2.meta.TypedSpace;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.iterableinterval.unary.CalculateDiameter;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.ConvexHull2D;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.knip.core.features.CalculatePerimeter;
import org.knime.knip.core.features.FeatureSet;
import org.knime.knip.core.features.FeatureTargetListener;
import org.knime.knip.core.features.ObjectCalcAndCache;
import org.knime.knip.core.features.SharesObjects;
import org.knime.knip.core.util.ImgUtils;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SegmentFeatureSet implements FeatureSet, SharesObjects {

    private final String[] m_features;

    private double[] m_centroid = null;

    private final ExtractOutlineImg m_outlineOp;

    private IterableInterval<BitType> m_interval;

    private Img<BitType> m_outline;

    private final CalculatePerimeter m_calculatePerimeter;

    private final ConvexHull2D<Img<BitType>> m_convexityOp;

    private final CalculateDiameter m_calculateDiameter;

    private double m_perimeter;

    private double m_solidity;

    private double m_circularity;

    private double m_diameter;

    private final BitSet m_enabled = new BitSet();

    private ObjectCalcAndCache m_ocac;

    private TypedSpace<TypedAxis> m_cs;

    private final TypedAxis[] m_defaultAxis;

    /**
     * @param target
     */
    public SegmentFeatureSet(final TypedAxis[] defaultAxes) {
        super();
        m_calculatePerimeter = new CalculatePerimeter();
        m_outlineOp = new ExtractOutlineImg(false);
        m_convexityOp = new ConvexHull2D<Img<BitType>>(0, 1, false);
        m_calculateDiameter = new CalculateDiameter();
        m_defaultAxis = defaultAxes.clone();
        m_features = getFeatures(defaultAxes);
    }

    public static String[] getFeatures(final TypedAxis[] defaultAxes) {

        final ArrayList<String> features = new ArrayList<String>();

        for (final TypedAxis type : defaultAxes) {
            features.add("Centroid " + type.type().getLabel());
        }

        features.add("Num Pix");
        features.add("Circularity");
        features.add("Perimeter");
        features.add("Convexity");
        features.add("Extend");
        features.add("Diameter");

        for (final TypedAxis type : defaultAxes) {
            features.add("Dimension " + type.type().getLabel());
        }

        return features.toArray(new String[features.size()]);
    }

    /**
     * @param cs
     */
    @FeatureTargetListener
    public void calibratedSpaceUpdated(final CalibratedSpace cs) {
        m_cs = cs;
    }

    /**
     * @param interval
     */
    @FeatureTargetListener
    public void iiUpdated(final IterableInterval<BitType> interval) {
        m_interval = interval;
        m_centroid = null;

        int activeDims = 0;
        for (int d = 0; d < interval.numDimensions(); d++) {
            if (interval.dimension(d) > 1) {
                activeDims++;
            }
        }

        if (m_enabled.get(m_defaultAxis.length + 1) || m_enabled.get(m_defaultAxis.length + 2)
                || m_enabled.get(m_defaultAxis.length + 3) || m_enabled.get(m_defaultAxis.length + 5)) {
            if (activeDims > 2) {
                m_solidity = Double.NaN;
                m_perimeter = Double.NaN;
                m_circularity = Double.NaN;
                m_diameter = Double.NaN;

                throw new IllegalArgumentException(
                        "Perimeter, Convexity and Circularity and Diameter can only be calculated on two dimensional ROIs containing at least two pixels. Settings them to Double.NaN");

            } else {

                final Img<BitType> bitMask = m_ocac.binaryMask2D(interval);
                m_outline = m_outlineOp.compute(bitMask, ImgUtils.createEmptyImg(bitMask));
                m_perimeter = Operations.compute(m_calculatePerimeter, m_outline).get();

                if (m_enabled.get(1 + m_defaultAxis.length) || m_enabled.get(3 + m_defaultAxis.length)) {

                    if (activeDims == 2) {
                        m_convexityOp
                                .compute(new ImgView<BitType>(SubsetOperations.subsetview(bitMask, bitMask), null),
                                         new ImgView<BitType>(SubsetOperations.subsetview(bitMask, bitMask), null));
                        final Cursor<BitType> convexBitMaskCursor = bitMask.cursor();

                        double ctr = 0;
                        while (convexBitMaskCursor.hasNext()) {
                            convexBitMaskCursor.fwd();
                            ctr += convexBitMaskCursor.get().get() ? 1 : 0;
                        }

                        m_circularity = (4d * Math.PI * m_interval.size()) / Math.pow(m_perimeter, 2);
                        m_solidity = interval.size() / ctr;

                    } else {
                        m_circularity = Double.NaN;
                        m_solidity = Double.NaN;

                        throw new IllegalArgumentException(
                                "Perimeter, Convexity and Circularity and Diameter can only be calculated on two dimensional ROIs containing at least two pixels. Settings them to Double.NaN");
                    }

                }

                if (m_enabled.get(5 + m_defaultAxis.length)) {
                    m_diameter = m_calculateDiameter.compute(m_outline, new DoubleType()).get();
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double value(int id) {

        if (id < m_defaultAxis.length) {
            final int idx = m_cs.dimensionIndex(m_defaultAxis[id].type());
            if (idx != -1) {
                m_centroid = m_ocac.centroid(m_interval);
                return m_centroid[idx];
            } else {
                return 0;
            }
        }

        if (id > (m_defaultAxis.length + 5)) {
            final int idx = m_cs.dimensionIndex(m_defaultAxis[id - m_defaultAxis.length - 6].type());
            if (idx != -1) {
                return m_interval.dimension(idx);
            } else {
                return 0;
            }
        }

        id -= m_defaultAxis.length;
        switch (id) {
            case 0:
                return m_interval.size();
            case 1:
                return m_circularity;
            case 2:
                return m_perimeter;
            case 3:
                return m_solidity;
            case 4:
                double numPixBB = 1;
                for (int d = 0; d < m_interval.numDimensions(); d++) {
                    numPixBB *= m_interval.dimension(d);
                }
                return m_interval.size() / numPixBB;

            case 5:
                return m_diameter;
            default:
                return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name(final int id) {
        return m_features[id];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enable(final int id) {
        m_enabled.set(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numFeatures() {
        return m_features.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String featureSetId() {
        return "Segment Feature Factory";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?>[] getSharedObjectClasses() {
        return new Class[]{ObjectCalcAndCache.class};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSharedObjectInstances(final Object[] instances) {
        m_ocac = (ObjectCalcAndCache)instances[0];

    }

}
