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
package org.knime.knip.base.nodes.io.kernel.structuring;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.imglib2.Cursor;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.roi.EllipseRegionOfInterest;
import net.imglib2.type.logic.BitType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.nodes.io.kernel.ImgConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SphereConfiguration extends ImgConfiguration<BitType> {

    private JTextField m_jtfNumDimensions;

    private JTextField m_jtfRadius;

    private JPanel m_panel;

    private SerializableSetting<Img<BitType>[]> m_setting;

    public SphereConfiguration() {
        initializeGui();
    }

    public SphereConfiguration(final SphereSetting setting) {
        this();
        loadFromSetting(setting);
    }

    @Override
    public JPanel getConfigContentPanel() {
        return m_panel;
    }

    @Override
    public String getName() {
        return "Sphere";
    }

    @Override
    public SerializableSetting<Img<BitType>[]> getSetting() {
        try {
            validate();
        } catch (InvalidSettingsException e) {
        }
        return m_setting;
    }

    protected void initializeGui() {
        m_panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        m_panel.add(new JLabel("Num Dimensions"), gbc);

        gbc.gridx++;
        m_jtfNumDimensions = new JTextField("2");
        m_jtfNumDimensions.addActionListener(m_updatePreviewListener);
        m_jtfNumDimensions.setColumns(STANDARD_TEXTFIELD_COLUMNS);
        m_panel.add(m_jtfNumDimensions, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        m_panel.add(new JLabel("Radius"), gbc);

        gbc.gridx++;
        m_jtfRadius = new JTextField("1");
        m_jtfRadius.addActionListener(m_updatePreviewListener);
        m_jtfRadius.setColumns(STANDARD_TEXTFIELD_COLUMNS);

        m_panel.add(m_jtfRadius, gbc);
    }

    protected void loadFromSetting(final SphereSetting setting) {
        m_jtfNumDimensions.setText("" + setting.m_numDimensions);
        m_jtfRadius.setText("" + setting.m_radius);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        try {
            final int numDimensions = Integer.parseInt(m_jtfNumDimensions.getText());
            final double radius = Double.parseDouble(m_jtfRadius.getText());
            m_setting = new SphereSetting(numDimensions, radius);
        } catch (final NumberFormatException e) {
            throw new InvalidSettingsException(e);
        }
    }
}

class SphereSetting extends SerializableSetting<Img<BitType>[]> {

    private static final long serialVersionUID = 1L;

    final int m_numDimensions;

    final double m_radius;

    public SphereSetting(final int numDimensions, final double radius) {
        super();
        m_numDimensions = numDimensions;
        m_radius = radius;
    }

    @Override
    protected SerializableConfiguration<Img<BitType>[]> createConfiguration() {
        return new SphereConfiguration(this);
    }

    @Override
    public Img<BitType>[] get() {
        final double[] origin = new double[m_numDimensions];
        Arrays.fill(origin, m_radius);
        final long[] dim = new long[m_numDimensions];
        for (int i = 0; i < origin.length; i++) {
            origin[i] = Math.round(origin[i]);
            dim[i] = (long)((2 * origin[i]) + 1);
        }
        final EllipseRegionOfInterest el = new EllipseRegionOfInterest(new RealPoint(origin), m_radius);
        final ArrayImgFactory<BitType> fac = new ArrayImgFactory<BitType>();
        final Img<BitType> img = fac.create(dim, new BitType());
        final Cursor<BitType> c = el.getIterableIntervalOverROI(img).cursor();
        while (c.hasNext()) {
            c.next();
            c.get().set(true);
        }
        return new Img[]{img};
    }
}
