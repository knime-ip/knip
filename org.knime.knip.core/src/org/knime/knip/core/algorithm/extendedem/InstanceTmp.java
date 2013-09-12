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
public class InstanceTmp {

    private InstancesTmp m_dataset;

    private double[] m_attValues;

    private double m_weight;

    public InstanceTmp(final double weight, final double[] attValues) {

        m_attValues = attValues;
        m_weight = weight;
        m_dataset = null;
    }

    public boolean isMissingValue(final double val) {

        return Double.isNaN(val);
    }

    public boolean isMissing(final int attIndex) {

        if (isMissingValue(value(attIndex))) {
            return true;
        }
        return false;
    }

    public final double weight() {
        return m_weight;
    }

    public double value(final int attIndex) {
        return m_attValues[attIndex];
    }

    public AttributeTmp attribute(final int index) {
        return m_dataset.attribute(index);
    }

    public void setDataset(final InstancesTmp instances) {
        m_dataset = instances;

    }

    public double missingValue() {

        return Double.NaN;
    }

    public InstanceTmp(final int numAttributes) {

        m_attValues = new double[numAttributes];
        for (int i = 0; i < m_attValues.length; i++) {
            m_attValues[i] = missingValue();
        }
        m_weight = 1;
        m_dataset = null;
    }

    public void setValue(final AttributeTmp att, final double value) {
        setValue(att.index(), value);

    }

    public void setWeight(final double weight) {
        m_weight = weight;

    }

    private void freshAttributeVector() {

        m_attValues = toDoubleArray();
    }

    public void setValue(final int attIndex, final double value) {
        freshAttributeVector();
        m_attValues[attIndex] = value;
    }

    public double[] toDoubleArray() {
        final double[] newValues = new double[m_attValues.length];
        System.arraycopy(m_attValues, 0, newValues, 0, m_attValues.length);
        return newValues;
    }
}
