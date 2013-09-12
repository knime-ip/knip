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

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.nodes.io.kernel.ImgConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;
import org.knime.knip.core.algorithm.convolvers.filter.linear.CurvedGabor;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class GaborCurvedConfiguration extends ImgConfiguration<FloatType> {

    private JCheckBox m_cbComplex;

    private JFormattedTextField m_jtfCurveRadius;

    private JFormattedTextField m_jtfPhaseOffset;

    private JFormattedTextField m_jtfSigmaX;

    private JFormattedTextField m_jtfSigmaY;

    private JFormattedTextField m_jtfSupportRadius;

    private JFormattedTextField m_jtfTheta;

    private JFormattedTextField m_jtfWaveLength;

    private JPanel m_panel;

    private SerializableSetting<Img<FloatType>[]> m_setting;

    public GaborCurvedConfiguration() {
        initializeGui();
    }

    public GaborCurvedConfiguration(final GaborCurvedSetting setting) {
        this();
        loadFromSetting(setting);
    }

    private void addCheckBox(final JPanel p, final GridBagConstraints c, final String label, final JCheckBox box) {
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel(label), c);
        c.gridx++;
        box.addActionListener(m_updatePreviewListener);
        p.add(box, c);
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
        return "Gabor Curved";
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

        gbc.insets = new Insets(5, 5, 0, 5);

        gbc.gridy = 0;
        m_jtfSupportRadius = new JFormattedTextField(intForamet);
        m_jtfSupportRadius.setText("20");
        addTextField(m_panel, gbc, "Support Radius", m_jtfSupportRadius);
        m_jtfTheta = new JFormattedTextField(doubleFormat);
        m_jtfTheta.setText(".1, .2 ... 1");
        addTextField(m_panel, gbc, "Theta", m_jtfTheta);
        m_jtfWaveLength = new JFormattedTextField(doubleFormat);
        m_jtfWaveLength.setText("10");
        addTextField(m_panel, gbc, "Wave Length", m_jtfWaveLength);
        m_jtfPhaseOffset = new JFormattedTextField(doubleFormat);
        m_jtfPhaseOffset.setText("0");
        addTextField(m_panel, gbc, "Phase Offset", m_jtfPhaseOffset);
        m_jtfCurveRadius = new JFormattedTextField(doubleFormat);
        m_jtfCurveRadius.setText("8, 10");
        addTextField(m_panel, gbc, "Curve Radius", m_jtfCurveRadius);
        m_jtfSigmaX = new JFormattedTextField(doubleFormat);
        m_jtfSigmaX.setText("8");
        addTextField(m_panel, gbc, "Sigma X", m_jtfSigmaX);
        m_jtfSigmaY = new JFormattedTextField(doubleFormat);
        m_jtfSigmaY.setText("30");
        addTextField(m_panel, gbc, "Sigma Y", m_jtfSigmaY);

        m_cbComplex = new JCheckBox();
        m_cbComplex.setSelected(false);
        addCheckBox(m_panel, gbc, "Complex Part?", m_cbComplex);
    }

    protected void loadFromSetting(final GaborCurvedSetting setting) {
        m_jtfSupportRadius.setValue(setting.m_supportRadius);
        m_jtfTheta.setValue(setting.m_theta);
        m_jtfWaveLength.setValue(setting.m_waveLength);
        m_jtfPhaseOffset.setValue(setting.m_phaseOffset);
        m_jtfCurveRadius.setValue(setting.m_curveRadius);
        m_jtfSigmaX.setValue(setting.m_sigmaxSqrt);
        m_jtfSigmaY.setValue(setting.m_sigmaySqrt);
        m_cbComplex.setSelected(setting.m_complexPart);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        try {
            m_jtfSupportRadius.commitEdit();
            m_jtfTheta.commitEdit();
            m_jtfWaveLength.commitEdit();
            m_jtfPhaseOffset.commitEdit();
            m_jtfCurveRadius.commitEdit();
            m_jtfSigmaX.commitEdit();
            m_jtfSigmaY.commitEdit();
            m_setting =
                    new GaborCurvedSetting((int[])m_jtfSupportRadius.getValue(), (double[])m_jtfTheta.getValue(),
                            (double[])m_jtfWaveLength.getValue(), (double[])m_jtfPhaseOffset.getValue(),
                            (double[])m_jtfCurveRadius.getValue(), (double[])m_jtfSigmaX.getValue(),
                            (double[])m_jtfSigmaY.getValue(), m_cbComplex.isSelected());
        } catch (final ParseException e) {
            throw new InvalidSettingsException(e);
        }
    }
}

class GaborCurvedSetting extends SerializableSetting<Img<FloatType>[]> {

    private static final long serialVersionUID = 1L;

    public boolean m_complexPart;

    final double[] m_curveRadius;

    final double[] m_phaseOffset;

    final double[] m_sigmaxSqrt;

    final double[] m_sigmaySqrt;

    final int[] m_supportRadius;

    final double[] m_theta;

    final double[] m_waveLength;

    public GaborCurvedSetting(final int[] supportRadius, final double[] theta, final double[] waveLength,
                              final double[] phaseOffset, final double[] curveRadius, final double[] sigmaxSqrt,
                              final double[] sigmaySqrt, final boolean complexPart) {
        super();
        this.m_supportRadius = supportRadius;
        this.m_theta = theta;
        this.m_waveLength = waveLength;
        this.m_phaseOffset = phaseOffset;
        this.m_curveRadius = curveRadius;
        this.m_sigmaxSqrt = sigmaxSqrt;
        this.m_sigmaySqrt = sigmaySqrt;
        this.m_complexPart = complexPart;
    }

    @Override
    protected SerializableConfiguration<Img<FloatType>[]> createConfiguration() {
        return new GaborCurvedConfiguration(this);
    }

    @Override
    public Img<FloatType>[] get() {
        final Img<FloatType>[] kernels =
                new Img[m_supportRadius.length * m_theta.length * m_waveLength.length * m_phaseOffset.length
                        * m_curveRadius.length * m_sigmaxSqrt.length * m_sigmaySqrt.length];
        int i = 0;
        for (final int r : m_supportRadius) {
            for (final double t : m_theta) {
                for (final double s : m_waveLength) {
                    for (final double o : m_phaseOffset) {
                        for (final double c : m_curveRadius) {
                            for (final double x : m_sigmaxSqrt) {
                                for (final double y : m_sigmaySqrt) {
                                    final CurvedGabor g =
                                            new CurvedGabor(r, t * 2 * Math.PI, s, o, c, x, y, m_complexPart);
                                    kernels[i] = g;
                                    ++i;
                                }
                            }
                        }
                    }
                }
            }
        }
        return kernels;
    }
}
