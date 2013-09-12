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
package org.knime.knip.base.node.dialog;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.meta.TypedAxis;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.node2012.OptionDocument.Option;

/**
 * Dialog component to select image axis.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DialogComponentDimSelection extends DialogComponent implements ItemListener {

    class MyToogleButton extends JToggleButton {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public MyToogleButton(final String text) {
            super(text);
        }

        @Override
        public void paint(final Graphics g) {
            super.paint(g);

            final Color oldC = g.getColor();
            if (isSelected()) {
                g.setColor(new Color(0, 109, 44));
            } else {
                g.setColor(new Color(204, 76, 2));
            }
            g.fillRect(getWidth() - 10, getHeight() - 10, 6, 6);
            g.setColor(oldC);
        }
    }

    /**
     * max length of points for a dimension to be selected
     */
    public static final int MAX_DIM_LENGTH = 200;

    private final LinkedList<JToggleButton> m_activeToogleButtonsQueue;

    /*
     * strings containing the selected variable names
     */
    private final List<JToggleButton> m_dimLabelButtonList;

    private final TypedAxis[] m_dimLabels = KNIMEKNIPPlugin.parseDimensionLabelsAsAxis();

    /*
     * An error message.
     */
    private final JLabel m_errorMessage;

    private final String m_label;

    private final int m_maxNumDims;

    private final int m_minNumDims;

    private final JPanel m_panel;

    /**
     * 
     * @param model
     */
    public DialogComponentDimSelection(final SettingsModelDimSelection model, final String label) {
        this(model, label, 1, 100);
    }

    /**
     * 
     * @param model
     */
    public DialogComponentDimSelection(final SettingsModelDimSelection model, final String label, final int minNumDims,
                                       final int maxNumDims) {
        super(model);

        m_activeToogleButtonsQueue = new LinkedList<JToggleButton>();

        m_label = label;
        m_minNumDims = minNumDims;
        m_maxNumDims = maxNumDims;
        getComponentPanel().setLayout(new BoxLayout(getComponentPanel(), BoxLayout.Y_AXIS));

        m_dimLabelButtonList = new ArrayList<JToggleButton>(m_dimLabels.length);
        m_panel = new JPanel();
        m_panel.add(new JLabel(m_label));
        for (int i = 0; i < m_dimLabels.length; i++) {
            final JToggleButton button = new MyToogleButton(m_dimLabels[i].type().getLabel());
            button.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    toogleButtonChanged(e);
                }

            });
            button.setSelected(true);
            m_dimLabelButtonList.add(button);
            m_panel.add(button);
        }

        getComponentPanel().add(m_panel);
        m_errorMessage = new JLabel("");
        getComponentPanel().add(m_errorMessage);
        getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent arg0) {
                updateComponent();
            }
        });
        updateComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void itemStateChanged(final ItemEvent arg0) {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void setEnabledComponents(final boolean enabled) {
        for (final JToggleButton button : m_dimLabelButtonList) {
            button.setEnabled(enabled);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setToolTipText(final String text) {
        // Nothing to do here
    }

    private void toogleButtonChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (m_activeToogleButtonsQueue.size() == m_maxNumDims) {
                m_activeToogleButtonsQueue.poll().setSelected(false);
            }
            m_activeToogleButtonsQueue.add((JToggleButton)e.getSource());
        } else {
            m_activeToogleButtonsQueue.remove(e.getSource());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void updateComponent() {
        // updateModel();
        final SettingsModelDimSelection model = ((SettingsModelDimSelection)getModel());
        final int[] selectedIndices = model.getSelectedDimIndices(m_dimLabels.length, m_dimLabels);
        for (final JToggleButton button : m_dimLabelButtonList) {
            button.setSelected(false);
        }
        for (final int index : selectedIndices) {
            m_dimLabelButtonList.get(index).setSelected(true);
        }

        // also update the enable status
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     *
     */
    private final void updateModel() {
        final SettingsModelDimSelection model = (SettingsModelDimSelection)getModel();
        final Set<String> selectedValues = new HashSet<String>(m_dimLabelButtonList.size());
        for (final JToggleButton button : m_dimLabelButtonList) {
            if (button.isSelected()) {
                selectedValues.add(button.getText());
            }
        }
        model.setDimSelectionValue(selectedValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void validateSettingsBeforeSave() throws InvalidSettingsException {
        int numSel = 0;
        for (final JToggleButton button : m_dimLabelButtonList) {
            if (button.isSelected()) {
                numSel++;
            }
        }

        if ((numSel < m_minNumDims) || (numSel > m_maxNumDims)) {
            final String mess =
                    "Wrong number of selected dimensions! Allowed are " + m_minNumDims + "-" + m_maxNumDims + ".";
            m_errorMessage.setText(mess);
            throw new InvalidSettingsException(mess);
        } else {
            m_errorMessage.setText("");
        }
        updateModel();
    }

    /**
     *
     */
    public static void createNodeDescription(final Option opt) {
        opt.setName("Dimension Selection");
        opt.addNewP()
                .newCursor()
                .setTextValue("This component allows the selection of dimensions of interest."
                                      + System.getProperty("line.separator")
                                      + "If an algorithm cannot, as it only supports fewer dimensions than the number of dimensions of the image, or shouldnot, as one wants to run the algorithm only on subsets of the image, be applied on the complete image, the dimension selection can be used to define the plane/cube/hypercube on which the algorithm is applied."
                                      + System.getProperty("line.separator")
                                      + "Example 1 with three dimensional Image (X,Y,Time): An algorithm cannot be applied to the complete image, as it only supports two dimensional images. If you select e.g. X,Y then the algorithm will be applied to all X,Y planes individually."
                                      + System.getProperty("line.separator")
                                      + "Example 2 with three dimensional Image (X,Y,Time): The algorithm can be applied in two, three or even more dimensions. Select the dimensions to define your plane/cube/hypercube on which the algorithm will be applied.");

    }
}
