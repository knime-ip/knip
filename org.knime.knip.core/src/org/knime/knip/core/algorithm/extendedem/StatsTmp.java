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
package org.knime.knip.core.algorithm.extendedem;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
class StatsTmp {

    private double m_count = 0;

    private double m_sum = 0;

    private double m_sumSq = 0;

    private double m_stdDev = Double.NaN;

    private double m_mean = Double.NaN;

    private double m_min = Double.NaN;

    private double m_max = Double.NaN;

    public void add(final double value, final double n) {

        m_sum += value * n;
        m_sumSq += value * value * n;
        m_count += n;
        if (Double.isNaN(m_min)) {
            m_min = m_max = value;
        } else if (value < m_min) {
            m_min = value;
        } else if (value > m_max) {
            m_max = value;
        }
    }

    public void calculateDerived() {

        m_mean = Double.NaN;
        setStdDev(Double.NaN);
        if (m_count > 0) {
            m_mean = m_sum / m_count;
            setStdDev(Double.POSITIVE_INFINITY);
            if (m_count > 1) {
                setStdDev(m_sumSq - ((m_sum * m_sum) / m_count));
                setStdDev(getStdDev() / (m_count - 1));
                if (getStdDev() < 0) {
                    setStdDev(0);
                }
                setStdDev(Math.sqrt(getStdDev()));
            }
        }
    }

    /**
     * @return the m_stdDev
     */
    public double getStdDev() {
        return m_stdDev;
    }

    /**
     * @param m_stdDev the m_stdDev to set
     */
    public void setStdDev(double m_stdDev) {
        this.m_stdDev = m_stdDev;
    }
}
