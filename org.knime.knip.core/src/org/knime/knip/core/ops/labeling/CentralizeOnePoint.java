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

import java.util.ArrayList;
import java.util.Collections;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.algorithm.PolarImageFactory;
import org.knime.knip.core.ops.filters.DirectionalGradient;
import org.knime.knip.core.ops.filters.DirectionalGradient.GradientDirection;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class CentralizeOnePoint<T extends RealType<T>> implements UnaryOperation<long[], long[]> {

    private final int m_maxIterations;

    private final PolarImageFactory<T> m_factory;

    private final int m_samplingRate;

    private final DirectionalGradient<T, Img<T>> m_directionGradientOp;

    private Img<T> m_buffer;

    private final int m_radius;

    public CentralizeOnePoint(final PolarImageFactory<T> factory, final int numMaxIterations, final int radius,
                              final int samplingRate) {
        m_radius = radius;
        m_directionGradientOp = new DirectionalGradient<T, Img<T>>(GradientDirection.HORIZONTAL, false);
        m_maxIterations = numMaxIterations;
        m_factory = factory;
        m_samplingRate = samplingRate;
    }

    @Override
    public long[] compute(final long[] src, final long[] res) {

        System.arraycopy(centralizeOnePoint(src, m_maxIterations), 0, res, 0, res.length);

        return res;
    }

    public long[] compute(final double[] point, final long[] res) {
        final long[] intermediate = new long[point.length];

        for (int d = 0; d < point.length; d++) {
            intermediate[d] = Math.round(point[d]);
        }

        return compute(intermediate, res);
    }

    private long[] centralizeOnePoint(final long[] src, final int maxIterations) {

        if (src.length != 2) {
            throw new IllegalArgumentException("Must be 2 dimensional");
        }

        if (m_buffer == null) {
            m_buffer = m_factory.createPolarImage(src, m_radius, m_samplingRate);
        }

        final long[] res = new long[src.length];

        final Img<T> polarImg = m_factory.createPolarImage(src, m_radius, m_samplingRate);

        m_directionGradientOp.compute(polarImg, m_buffer);

        final RandomAccess<T> randomAccess = m_buffer.randomAccess();

        final T type = randomAccess.get();
        for (int y = 0; y < (m_buffer.dimension(1) / 2); y++) {
            randomAccess.setPosition(y, 1);
            final ArrayList<BaseVals> baseVals = new ArrayList<BaseVals>();

            for (int x = 0; x < m_buffer.dimension(0); x++) {
                randomAccess.setPosition(x, 0);
                baseVals.add(new BaseVals(x, type.getRealDouble()));
            }

            Collections.sort(baseVals);

            randomAccess.setPosition(y + (m_buffer.dimension(1) / 2), 1);
            final ArrayList<BaseVals> partnerVals = new ArrayList<BaseVals>();

            for (int x = 0; x < m_buffer.dimension(0); x++) {
                randomAccess.setPosition(x, 0);
                partnerVals.add(new BaseVals(x, type.getRealDouble()));
            }

            Collections.sort(partnerVals);

            double minDistBase = Double.MAX_VALUE;
            double minDistPartner = Double.MAX_VALUE;

            for (int f = partnerVals.size() - 1; f > (partnerVals.size() - 5); f--) {
                if (baseVals.get(f).getX() < minDistBase) {
                    minDistBase = baseVals.get(f).getX();
                }

                if (partnerVals.get(f).getX() < minDistPartner) {
                    minDistPartner = partnerVals.get(f).getX();
                }
            }

            // Calc
            final double difference = Math.abs(minDistBase - minDistPartner);
            double relY = 0;
            if (minDistBase > minDistPartner) {
                relY = y;
            } else {
                relY = y + (m_buffer.dimension(1) / 2);
            }

            res[0] += difference * (Math.cos((relY / m_buffer.dimension(1)) * 2 * Math.PI));

            res[1] += -(difference * Math.sin((relY / m_buffer.dimension(1)) * 2 * Math.PI));
        }

        final double newX = Math.round(res[0] / (m_buffer.dimension(1) / 2.0));
        final double newY = Math.round(res[1] / (m_buffer.dimension(1) / 2.0));

        res[0] = (long)(src[0] + newX);
        res[1] = (long)(src[1] + newY);

        if ((maxIterations > 0) && ((newX != 0) || (newY != 0))) {
            return centralizeOnePoint(res, maxIterations - 1);
        } else {
            return res;
        }
    }

    @Override
    public UnaryOperation<long[], long[]> copy() {
        return new CentralizeOnePoint<T>(m_factory, m_maxIterations, m_radius, m_samplingRate);

    }
}

/**
 * 
 * @author Christian Dietz, University of Konstanz
 */
class BaseVals implements Comparable<BaseVals> {

    private final int m_x;

    private final double m_d;

    public BaseVals(final int x, final double d) {
        this.m_x = x;
        this.m_d = d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final BaseVals o) {

        if ((m_d - o.m_d) > 0) {
            return 1;
        }

        if ((m_d - o.m_d) == 0) {
            return 0;
        }

        return -1;
    }

    /**
     * @return the x
     */
    public int getX() {
        return m_x;
    }

    /**
     * @return the d
     */
    public double getD() {
        return m_d;
    }

}
