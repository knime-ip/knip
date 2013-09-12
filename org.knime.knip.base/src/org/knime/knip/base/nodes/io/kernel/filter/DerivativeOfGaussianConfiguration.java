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
package org.knime.knip.base.nodes.io.kernel.filter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.nodes.io.kernel.ImgConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;
import org.knime.knip.core.algorithm.convolvers.filter.linear.DerivativeOfGaussian;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DerivativeOfGaussianConfiguration extends ImgConfiguration<DoubleType> {

    private JFormattedTextField m_jtfOrd;

    private JFormattedTextField m_jtfScale;

    private JFormattedTextField m_jtfSupportRadius;

    private JFormattedTextField m_jtfTheta;

    private JPanel m_panel;

    private SerializableSetting<Img<DoubleType>[]> m_setting;

    public DerivativeOfGaussianConfiguration() {
        initializeGui();
    }

    public DerivativeOfGaussianConfiguration(final DerivativeOfGaussianSetting setting) {
        this();
        loadFromSetting(setting);
    }

    private void addTextField(final JPanel p, final GridBagConstraints c, final String label, final JTextField t) {
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel(label), c);
        c.gridx++;
        t.addActionListener(m_updatePreviewListener);
        t.setColumns(STANDARD_TEXTFIELD_COLUMNS);
        p.add(t, c);
    }

    @Override
    public JPanel getConfigContentPanel() {
        return m_panel;
    }

    @Override
    public String getName() {
        return "Derivative of Gaussian";
    }

    @Override
    public SerializableSetting<Img<DoubleType>[]> getSetting() {
        try {
            validate();
        } catch (InvalidSettingsException e) {
        }
        return m_setting;
    }

    protected void initializeGui() {
        final DoubleRangeFormat doubleFormat = new DoubleRangeFormat();
        final IntegerRangeFormat intForamet = new IntegerRangeFormat();
        m_panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 0, 5);

        m_jtfSupportRadius = new JFormattedTextField(intForamet);
        m_jtfSupportRadius.setText("20");
        addTextField(m_panel, gbc, "Support Radius", m_jtfSupportRadius);
        m_jtfTheta = new JFormattedTextField(doubleFormat);
        m_jtfTheta.setText(".1, .2 ... 1");
        addTextField(m_panel, gbc, "Theta", m_jtfTheta);
        m_jtfScale = new JFormattedTextField(doubleFormat);
        m_jtfScale.setText("1");
        addTextField(m_panel, gbc, "Scale", m_jtfScale);
        m_jtfOrd = new JFormattedTextField(intForamet);
        m_jtfOrd.setText("1");
        addTextField(m_panel, gbc, "Order", m_jtfOrd);
    }

    protected void loadFromSetting(final DerivativeOfGaussianSetting setting) {
        m_jtfSupportRadius.setValue(setting.m_supportRadius);
        m_jtfTheta.setValue(setting.m_theta);
        m_jtfScale.setValue(setting.m_scale);
        m_jtfOrd.setValue(setting.m_order);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        try {
            m_jtfSupportRadius.commitEdit();
            m_jtfTheta.commitEdit();
            m_jtfScale.commitEdit();
            m_jtfOrd.commitEdit();
            m_setting =
                    new DerivativeOfGaussianSetting((int[])m_jtfSupportRadius.getValue(),
                            (double[])m_jtfTheta.getValue(), (double[])m_jtfScale.getValue(),
                            (int[])m_jtfOrd.getValue());
            m_setting.get();
        } catch (final Exception e) {
            throw new InvalidSettingsException(e);
        }
    }
}

class DerivativeOfGaussianSetting extends SerializableSetting<Img<DoubleType>[]> {

    private static final long serialVersionUID = 1L;

    final int[] m_order;

    final double[] m_scale;

    final int[] m_supportRadius;

    final double[] m_theta;

    public DerivativeOfGaussianSetting(final int[] supportRadius, final double[] theta, final double[] scale,
                                       final int[] order) {
        super();
        this.m_supportRadius = supportRadius;
        this.m_theta = theta;
        this.m_scale = scale;
        this.m_order = order;
    }

    @Override
    protected SerializableConfiguration<Img<DoubleType>[]> createConfiguration() {
        return new DerivativeOfGaussianConfiguration(this);
    }

    @Override
    public Img<DoubleType>[] get() {
        @SuppressWarnings("unchecked")
        final Img<DoubleType>[] kernels =
                new Img[m_supportRadius.length * m_theta.length * m_scale.length * m_order.length];
        int i = 0;
        for (final int r : m_supportRadius) {
            for (final double t : m_theta) {
                for (final double s : m_scale) {
                    for (final int o : m_order) {
                        kernels[i] = new DerivativeOfGaussian(r, t * 2 * Math.PI, s, o);
                        ++i;

                    }
                }
            }
        }
        return kernels;
    }
}
