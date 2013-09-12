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
import java.util.List;

import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.knime.knip.core.features.FeatureSet;
import org.knime.knip.core.features.FeatureTargetListener;
import org.knime.knip.core.features.ObjectCalcAndCache;
import org.knime.knip.core.features.SharesObjects;

/**
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class TamuraFeatureSet<T extends RealType<T>> implements FeatureSet, SharesObjects {

    public static final String[] FEATURES =
            new String[]{"TamuraGranularity", "TamuraContrast", "TamuraKurtosisOfDirectionality",
                    "TamuraStdDevDirectionality", "TamuraMaxDirectionality", "TamuraSkewness"};

    private Tamura<T> m_tamura;

    private DescriptiveStatistics m_stats;

    private double[] m_hist;

    private final List<String> m_enabledFeatures = new ArrayList<String>(FEATURES.length);

    private ObjectCalcAndCache m_ocac;

    private boolean m_valid;

    @FeatureTargetListener
    public final void iiUpdated(final IterableInterval<T> interval) {

        final ValuePair<Integer, Integer> validDims = getValidDims(interval);

        if (validDims == null) {
            m_valid = false;
        } else {
            m_valid = true;
            m_tamura =
                    new Tamura<T>(validDims.a, validDims.b, m_enabledFeatures.toArray(new String[m_enabledFeatures
                            .size()]));
            m_stats = m_ocac.descriptiveStatistics(interval);
            m_hist = m_tamura.updateROI(interval);
        }
    }

    private ValuePair<Integer, Integer> getValidDims(final IterableInterval<T> interval) {

        int dimX = -1;
        int dimY = -1;

        for (int d = 0; d < interval.numDimensions(); d++) {
            if (interval.dimension(d) > 1) {
                if (dimX < 0) {
                    dimX = d;
                } else if (dimY < 0) {
                    dimY = d;
                } else {
                    return null;
                }
            }
        }

        if ((dimX < 0) || (dimY < 0)) {
            return null;
        }

        return new ValuePair<Integer, Integer>(dimX, dimY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double value(final int id) {

        if (!m_valid) {
            return Double.NaN;
        }

        switch (id) {
            case 0:
                return m_hist[0];
            case 1:
                return m_hist[1];
            case 2:
                return m_stats.getKurtosis();
            case 3:
                return m_stats.getStandardDeviation();
            case 4:
                return m_stats.getMax();
            case 5:
                return m_stats.getSkewness();
        }

        throw new IllegalStateException("Feature doesn't exist in Tamura Feature Factory");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name(final int id) {
        return FEATURES[id];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enable(final int id) {
        m_enabledFeatures.add(FEATURES[id]);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numFeatures() {
        return FEATURES.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String featureSetId() {
        return "Tamura Features";
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
