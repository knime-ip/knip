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

import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.nodes.io.kernel.ImgConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;
import org.knime.knip.core.algorithm.convolvers.filter.linear.LaplacianOfGaussian;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LaplacianOfGaussianConfiguration extends ImgConfiguration<DoubleType> {

    private JFormattedTextField m_jtfScale;

    private JFormattedTextField m_jtfSupportRadius;

    private JPanel m_panel;

    private LaplacianOfGaussianSetting m_setting;

    public LaplacianOfGaussianConfiguration() {
        initGUI();
    }

    public LaplacianOfGaussianConfiguration(final LaplacianOfGaussianSetting setting) {
        this();
        loadFromSetting(setting);
    }

    @Override
    protected JPanel getConfigContentPanel() {
        return m_panel;
    }

    @Override
    public String getName() {
        return "Laplacian Of Gaussian";
    }

    @Override
    public SerializableSetting<Img<DoubleType>[]> getSetting() {
        try {
            validate();
        } catch (InvalidSettingsException e) {
        }
        return m_setting;
    }

    private void initGUI() {
        m_panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.insets = new Insets(5, 5, 0, 5);

        gbc.gridy = 0;
        gbc.gridx = 0;
        m_panel.add(new JLabel("Support Radius"), gbc);
        gbc.gridx++;
        m_jtfSupportRadius = new JFormattedTextField();
        m_jtfSupportRadius.setValue(7);
        m_jtfSupportRadius.setColumns(STANDARD_TEXTFIELD_COLUMNS);
        m_panel.add(m_jtfSupportRadius, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        m_panel.add(new JLabel("Scale"), gbc);
        gbc.gridx++;
        m_jtfScale = new JFormattedTextField();
        m_jtfScale.setValue(1.6);
        m_jtfScale.setColumns(STANDARD_TEXTFIELD_COLUMNS);
        m_panel.add(m_jtfScale, gbc);
    }

    private void loadFromSetting(final LaplacianOfGaussianSetting setting) {
        m_jtfSupportRadius.setValue(setting.m_supportRadius);
        m_jtfScale.setValue(setting.m_scale);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        try {
            m_jtfSupportRadius.commitEdit();
            m_jtfScale.commitEdit();
            m_setting =
                    new LaplacianOfGaussianSetting(((Number)m_jtfSupportRadius.getValue()).intValue(),
                            ((Number)m_jtfScale.getValue()).doubleValue());
        } catch (final Exception e) {
            throw new InvalidSettingsException(e);
        }
    }

}

class LaplacianOfGaussianSetting extends SerializableSetting<Img<DoubleType>[]> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    final double m_scale;

    final int m_supportRadius;

    public LaplacianOfGaussianSetting(final int supportRadius, final double scale) {
        this.m_supportRadius = supportRadius;
        this.m_scale = scale;
    }

    @Override
    protected SerializableConfiguration<Img<DoubleType>[]> createConfiguration() {
        return new LaplacianOfGaussianConfiguration(this);
    }

    @Override
    public Img<DoubleType>[] get() {
        return new Img[]{new LaplacianOfGaussian(m_supportRadius, m_scale)};
    }

}
