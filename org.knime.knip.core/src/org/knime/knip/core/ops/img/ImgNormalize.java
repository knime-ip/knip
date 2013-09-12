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
package org.knime.knip.core.ops.img;

import net.imglib2.img.Img;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.iterableinterval.unary.MinMaxWithSaturation;
import net.imglib2.ops.operation.real.unary.Normalize;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgNormalize<T extends RealType<T>> implements UnaryOutputOperation<Img<T>, Img<T>> {

    private final double m_saturation;

    private final T m_val;

    private ValuePair<T, T> m_minmaxtarget, m_minmaxsource;

    private final boolean m_isManual;

    private final boolean m_isTarget;

    public ImgNormalize(final double saturation, final T val, final ValuePair<T, T> minmax, final boolean isTarget) {
        m_saturation = saturation;
        m_val = val;
        m_isTarget = isTarget;
        if (isTarget) {
            m_minmaxtarget = minmax;
        } else {
            m_minmaxsource = minmax;
        }

        m_isManual = minmax != null;

    }

    @Override
    public Img<T> compute(final Img<T> input, final Img<T> output) {

        if (m_minmaxtarget == null) {
            final T min = m_val.createVariable();
            final T max = m_val.createVariable();
            min.setReal(m_val.getMinValue());
            max.setReal(m_val.getMaxValue());

            m_minmaxtarget = new ValuePair<T, T>(min, max);
        }

        if (m_minmaxsource == null) {
            m_minmaxsource = Operations.compute(new MinMaxWithSaturation<T>(m_saturation, m_val), input);
        }

        Operations.map(new Normalize<T>(m_minmaxsource.a, m_minmaxsource.b, m_minmaxtarget.a, m_minmaxtarget.b))
                .compute(input, output);

        if (!m_isManual) {
            m_minmaxsource = null;
            m_minmaxtarget = null;
        } else if (m_isTarget) {
            m_minmaxsource = null;
        } else {
            m_minmaxtarget = null;
        }

        return output;
    }

    @Override
    public UnaryObjectFactory<Img<T>, Img<T>> bufferFactory() {
        return new UnaryObjectFactory<Img<T>, Img<T>>() {
            @Override
            public Img<T> instantiate(final Img<T> a) {
                return a.factory().create(a, a.firstElement().createVariable());
            }
        };
    }

    @Override
    public UnaryOutputOperation<Img<T>, Img<T>> copy() {
        return new ImgNormalize<T>(m_saturation, m_val.createVariable(), m_isTarget ? m_minmaxtarget : m_minmaxsource,
                m_isTarget);
    }
}
