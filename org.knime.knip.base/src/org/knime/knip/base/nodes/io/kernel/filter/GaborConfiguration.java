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
import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.img.UnaryOperationAssignment;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.complex.real.unary.ComplexRealToRealAdapter;
import net.imglib2.type.numeric.real.FloatType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.nodes.io.kernel.ImgConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;
import org.knime.knip.core.algorithm.convolvers.filter.linear.Gabor;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class GaborConfiguration extends ImgConfiguration<FloatType> {

    private JFormattedTextField m_jtfElongation;

    private JFormattedTextField m_jtfFrequency;

    private JFormattedTextField m_jtfScale;

    private JFormattedTextField m_jtfSupportRadius;

    private JFormattedTextField m_jtfTheta;

    private JPanel m_panel;

    private SerializableSetting<Img<FloatType>[]> m_setting;

    public GaborConfiguration() {
        initializeGui();
    }

    public GaborConfiguration(final GaborSetting setting) {
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
    protected JPanel getConfigContentPanel() {
        return m_panel;
    }

    @Override
    public String getName() {
        return "Gabor";
    }

    @Override
    public SerializableSetting<Img<FloatType>[]> getSetting() {
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
        m_jtfTheta.setText(".1, .2 ... .5");
        addTextField(m_panel, gbc, "Theta", m_jtfTheta);
        m_jtfScale = new JFormattedTextField(doubleFormat);
        m_jtfScale.setText("1");
        addTextField(m_panel, gbc, "Scale", m_jtfScale);
        m_jtfFrequency = new JFormattedTextField(doubleFormat);
        m_jtfFrequency.setText("1");
        addTextField(m_panel, gbc, "Frequency", m_jtfFrequency);
        m_jtfElongation = new JFormattedTextField(doubleFormat);
        m_jtfElongation.setText("1");
        addTextField(m_panel, gbc, "Elongation", m_jtfElongation);
    }

    protected void loadFromSetting(final GaborSetting setting) {
        m_jtfSupportRadius.setValue(setting.m_supportRadius);
        m_jtfTheta.setValue(setting.m_theta);
        m_jtfScale.setValue(setting.m_scale);
        m_jtfFrequency.setValue(setting.m_frequency);
        m_jtfElongation.setValue(setting.m_elongation);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        try {
            m_jtfSupportRadius.commitEdit();
            m_jtfTheta.commitEdit();
            m_jtfScale.commitEdit();
            m_jtfFrequency.commitEdit();
            m_jtfElongation.commitEdit();
            m_setting =
                    new GaborSetting((int[])m_jtfSupportRadius.getValue(), (double[])m_jtfTheta.getValue(),
                            (double[])m_jtfScale.getValue(), (double[])m_jtfFrequency.getValue(),
                            (double[])m_jtfElongation.getValue());
        } catch (final ParseException e) {
            throw new InvalidSettingsException(e);
        }
    }
}

class GaborSetting extends SerializableSetting<Img<FloatType>[]> {

    private static final long serialVersionUID = 1L;

    final double[] m_elongation;

    final double[] m_frequency;

    final double[] m_scale;

    final int[] m_supportRadius;

    final double[] m_theta;

    public GaborSetting(final int[] supportRadius, final double[] theta, final double[] scale,
                        final double[] frequency, final double[] elongation) {
        super();
        this.m_supportRadius = supportRadius;
        this.m_theta = theta;
        this.m_scale = scale;
        this.m_frequency = frequency;
        this.m_elongation = elongation;
    }

    @Override
    protected SerializableConfiguration<Img<FloatType>[]> createConfiguration() {
        return new GaborConfiguration(this);
    }

    @Override
    public Img<FloatType>[] get() {
        final Img<FloatType>[] kernels =
                new Img[m_supportRadius.length * m_theta.length * m_scale.length * m_frequency.length
                        * m_elongation.length];
        final UnaryOperation op = new UnaryOperationAssignment(new ComplexRealToRealAdapter());
        int i = 0;
        for (final int r : m_supportRadius) {
            for (final double t : m_theta) {
                for (final double s : m_scale) {
                    for (final double f : m_frequency) {
                        for (final double e : m_elongation) {
                            final Gabor g = new Gabor(r, t * 2 * Math.PI, s, f, e);
                            kernels[i] = new ArrayImgFactory().create(g, new FloatType());
                            op.compute(g, kernels[i]);
                            ++i;
                        }
                    }
                }
            }
        }
        return kernels;
    }
}
