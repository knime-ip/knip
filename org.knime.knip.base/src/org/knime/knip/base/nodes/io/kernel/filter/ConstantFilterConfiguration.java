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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.nodes.io.kernel.ImgConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;
import org.knime.knip.core.algorithm.convolvers.filter.linear.ConstantFilter;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ConstantFilterConfiguration extends ImgConfiguration<DoubleType> {

    class VariantSelectionHandler implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                final ListSelectionModel lsm = (ListSelectionModel)e.getSource();

                if (lsm.isSelectionEmpty()) {
                    m_lFilter.setSelectedIndex(0);
                } else {
                    updatePreview();
                }
            }
        }
    }

    protected ConstantFilter m_filter;

    private JList m_lFilter;

    private JPanel m_panel;

    private ConstantFilterSetting m_setting;

    public ConstantFilterConfiguration(final ConstantFilter filter) {
        m_filter = filter;
        initGUI();
        m_lFilter.getSelectionModel().addListSelectionListener(new VariantSelectionHandler());
    }

    public ConstantFilterConfiguration(final ConstantFilterSetting setting) {
        m_filter = setting.m_filter;
        initGUI();
        loadFromSetting(setting);
        m_lFilter.getSelectionModel().addListSelectionListener(new VariantSelectionHandler());
    }

    @Override
    public JPanel getConfigContentPanel() {
        return m_panel;
    }

    @Override
    public String getName() {
        return m_filter.toString();
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
        gbc.gridy = 0;
        gbc.gridx = 0;
        m_panel.add(new JLabel("Variant Selection"), gbc);

        gbc.gridy++;
        m_lFilter = new JList(m_filter.getVariantNames());
        m_lFilter.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_lFilter.setSelectedIndex(0);

        final JScrollPane listScroller = new JScrollPane(m_lFilter);
        listScroller.setPreferredSize(new Dimension(STANDARD_PREF_WIDTH, 70));

        m_panel.add(listScroller, gbc);
    }

    private void loadFromSetting(final ConstantFilterSetting setting) {
        m_filter = setting.m_filter;

        // retrieve indices of selected values
        final ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
        final String[] vNames = m_filter.getVariantNames();
        for (final String selectedV : setting.m_selectedVariantNames) {
            for (int i = 0; i < vNames.length; i++) {
                if (vNames[i].equals(selectedV)) {
                    selectedIndices.add(i);
                    break;
                }
            }
        }

        final int[] indices = new int[selectedIndices.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = selectedIndices.get(i);
        }

        m_lFilter.setSelectedIndices(indices);
    }

    @Override
    public void validate() throws InvalidSettingsException {

        final int[] selection = m_lFilter.getSelectedIndices();
        final String[] selName = new String[selection.length];

        for (int i = 0; i < selection.length; i++) {
            selName[i] = m_filter.getVariantNames()[selection[i]];
        }

        m_setting = new ConstantFilterSetting(m_filter, selName);
    }
}

class ConstantFilterSetting extends SerializableSetting<Img<DoubleType>[]> {

    private static final long serialVersionUID = 1L;

    final ConstantFilter m_filter;

    final String m_selectedVariantNames[];

    public ConstantFilterSetting(final ConstantFilter filter, final String[] variantNames) {
        super();
        if (filter == null) {
            throw new IllegalArgumentException("Filter must not be null.");
        }
        this.m_filter = filter;
        m_selectedVariantNames = variantNames.clone();
    }

    @Override
    protected SerializableConfiguration<Img<DoubleType>[]> createConfiguration() {
        return new ConstantFilterConfiguration(this);
    }

    @Override
    public Img<DoubleType>[] get() {

        @SuppressWarnings("unchecked")
        final Img<DoubleType>[] images = new Img[m_selectedVariantNames.length];

        for (int i = 0; i < m_selectedVariantNames.length; i++) {
            images[i] = m_filter.createImage(m_selectedVariantNames[i]);
        }

        return images;
    }

}
