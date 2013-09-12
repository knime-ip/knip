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
package org.knime.knip.core.ui.imgviewer.panels.transfunc;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import javax.swing.event.EventListenerList;

import org.knime.knip.core.ui.event.EventService;

/**
 * A Panel used to control the Transferfunctions and the actually drawn values of the image to be shown.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens M�thing (clemens.muething@uni-konstanz.de)
 */
public class TransferFunctionControlPanel extends JPanel implements TransferFunctionChgListener {

    public final class Memento {

        private TransferFunctionBundle m_currentBundle;

        private final HistogramPainter.Scale m_scale;

        private final Map<TransferFunctionBundle, TransferFunctionColor> m_map =
                new HashMap<TransferFunctionBundle, TransferFunctionColor>();

        private final Histogram m_histogram;

        public Memento(final HistogramPainter.Scale s, final TransferFunctionBundle cb, final Histogram hist) {
            m_scale = s;
            m_currentBundle = cb;
            m_histogram = hist;
        }
    }

    public static final String CMD_NORMALIZE = "normalize";

    public static final int ID_NORMALIZE = 1;

    public static final String CMD_APPLY = "apply";

    public static final int ID_APPLY = 2;

    public static final String CMD_ONLYONE = "only_one";

    public static final int ID_ONLYONE = 3;

    /**
     * The Adapter for the Scale ComboBox.
     */
    private class ScaleAdapter implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent event) {
            m_transferPanel.setScale((HistogramPainter.Scale)m_scaleBox.getSelectedItem());
        }
    }

    /**
     * The Adapter for the Focus ComboBox.
     */
    private class FocusAdapter implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent event) {

            final TransferFunctionColor color = (TransferFunctionColor)m_focusBox.getSelectedItem();

            m_transferPanel.setTransferFocus(color);
            m_memento.m_map.put(m_memento.m_currentBundle, color);
        }
    }

    /**
     * The Adapter for the Color ComboBox.
     */
    private class ColorModeAdapter implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent event) {

            final TransferFunctionBundle bundle = (TransferFunctionBundle)m_bundleBox.getSelectedItem();
            setActiveBundle(bundle);
        }
    }

    private Memento m_memento;

    private final TransferFunctionPanel m_transferPanel;

    private final EventListenerList m_listener = new EventListenerList();

    private final JComboBox m_bundleBox;

    private final JComboBox m_scaleBox;

    private final JComboBox m_focusBox;

    private final JCheckBox m_boxNormalize;

    private final JCheckBox m_boxOnlyOneFunc;

    private final JCheckBox m_boxAutoApply;

    private final JButton m_buttonApply;

    // Save this adapter to unset/set while changing content
    private final ActionListener m_focusAdapter;

    private final ActionListener m_bundleAdapter;

    private TransferFunction m_lastModified;

    /**
     * Sets up a new TransferPanel.
     * 
     * @param service The {@link EventService} to be used
     */
    public TransferFunctionControlPanel() {

        // Set up the Comboboxes
        m_scaleBox = new JComboBox(HistogramPainter.Scale.values());
        m_bundleBox = new JComboBox();
        m_focusBox = new JComboBox();

        // Set up the viewers
        m_transferPanel = new TransferFunctionPanel();

        // set up the checkboxes and the button
        m_boxOnlyOneFunc = new JCheckBox("One set of functions");
        m_boxOnlyOneFunc.setToolTipText("Unchecking this option results"
                + " in a unique set of transfer function for each " + "slice/volume. Usefull for rendering two volumes"
                + " simulatneously.");
        m_boxOnlyOneFunc.addActionListener(new ActionListener() {

            @Override
            public final void actionPerformed(final ActionEvent event) {
                fireActionEvent(ID_ONLYONE, CMD_ONLYONE);
            }
        });

        m_boxAutoApply = new JCheckBox("Autoapply changes");
        m_boxAutoApply.setSelected(true);

        m_boxNormalize = new JCheckBox("Normalize");
        m_boxNormalize.setSelected(false);
        m_boxNormalize.addActionListener(new ActionListener() {

            @Override
            public final void actionPerformed(final ActionEvent event) {
                applyNormalize(m_boxNormalize.isSelected());
                fireActionEvent(ID_NORMALIZE, CMD_NORMALIZE);
            }
        });

        m_buttonApply = new JButton("Apply");
        m_buttonApply.addActionListener(new ActionListener() {
            @Override
            public final void actionPerformed(final ActionEvent event) {
                fireActionEvent(ID_APPLY, CMD_APPLY);
            }
        });

        // create the layout
        final GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        final Component glue = Box.createHorizontalGlue();

        // Set up the layout
        // find the box with the longest Text
        final int width = getLongestComboBox();

        // the subgroups to be added to the main groups
        // numbers are colums/rows respectivly
        // use fixed size horizontally
        final GroupLayout.ParallelGroup box0 =
                layout.createParallelGroup().addComponent(m_boxOnlyOneFunc).addComponent(m_boxAutoApply);

        final GroupLayout.SequentialGroup boxgroup =
                layout.createSequentialGroup().addComponent(m_boxNormalize).addComponent(glue).addGroup(box0);

        final GroupLayout.ParallelGroup horizontal0 =
                layout.createParallelGroup().addComponent(m_transferPanel).addGroup(boxgroup);

        final GroupLayout.ParallelGroup horizontal1 =
                layout.createParallelGroup().addComponent(m_scaleBox, width, width, width)
                        .addComponent(m_bundleBox, width, width, width).addComponent(m_focusBox, width, width, width)
                        .addComponent(m_buttonApply);

        // do not stretch vertically
        final GroupLayout.SequentialGroup verticalButtons =
                layout.createSequentialGroup()
                        .addComponent(m_scaleBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_bundleBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(m_focusBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE);

        final GroupLayout.ParallelGroup vertical0 =
                layout.createParallelGroup().addComponent(m_transferPanel).addGroup(verticalButtons);

        final GroupLayout.ParallelGroup vertical1 =
                layout.createParallelGroup().addComponent(m_boxNormalize).addComponent(glue)
                        .addComponent(m_boxOnlyOneFunc);

        final GroupLayout.ParallelGroup vertical2 =
                layout.createParallelGroup().addComponent(m_boxAutoApply).addComponent(m_buttonApply);

        // Set up the main sequential layouts
        final GroupLayout.SequentialGroup horizontal =
                layout.createSequentialGroup().addGroup(horizontal0)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(horizontal1);

        final GroupLayout.SequentialGroup vertical =
                layout.createSequentialGroup().addGroup(vertical0)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(vertical1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(vertical2);

        // add everything to the layout
        layout.setHorizontalGroup(horizontal);
        layout.setVerticalGroup(vertical);

        // Listen to the events
        m_focusAdapter = new FocusAdapter();
        m_bundleAdapter = new ColorModeAdapter();
        m_scaleBox.addActionListener(new ScaleAdapter());
        m_focusBox.addActionListener(m_focusAdapter);
        m_bundleBox.addActionListener(m_bundleAdapter);

        // create an empty memento
        m_memento = new Memento((HistogramPainter.Scale)m_scaleBox.getSelectedItem(), null, null);

        m_transferPanel.addTransferFunctionChgListener(this);
    }

    /**
     * Used to determine the longest of the ComboBoxes.
     * 
     * @return the largest dimension
     */
    private int getLongestComboBox() {
        int max = (int)m_scaleBox.getPreferredSize().getWidth();

        if ((int)m_bundleBox.getPreferredSize().getWidth() > max) {
            max = (int)m_bundleBox.getPreferredSize().getWidth();
        }
        if ((int)m_focusBox.getPreferredSize().getWidth() > max) {
            max = (int)m_focusBox.getPreferredSize().getWidth();
        }

        return max;
    }

    /**
     * Get the current state of this control.
     * 
     * @return the current state
     */
    public final Memento getState() {
        return m_memento;
    }

    /**
     * Set the state of the control back.
     * 
     * @param memento the state to set back to
     * 
     * @return the current state
     */
    public final Memento setState(final Memento memento) {
        if (memento == null) {
            throw new NullPointerException();
        }

        final Memento oldMemento = m_memento;

        m_memento = memento;

        // data
        m_transferPanel.setHistogram(m_memento.m_histogram);

        // selected scale
        m_scaleBox.setSelectedItem(m_memento.m_scale);

        // list of bundles
        // remove the listener so that we do not get constant events
        m_bundleBox.removeActionListener(m_bundleAdapter);

        m_bundleBox.removeAllItems();
        for (final TransferFunctionBundle b : m_memento.m_map.keySet()) {
            m_bundleBox.addItem(b);
        }

        m_bundleBox.setSelectedItem(m_memento.m_currentBundle);

        m_bundleBox.addActionListener(m_bundleAdapter);

        // current bundle
        setActiveBundle(m_memento.m_currentBundle);

        return oldMemento;
    }

    /**
     * Convenience function to call getBundle with current mode.
     * 
     * @return the bundle of the current mode
     */
    public final TransferFunctionBundle getBundle() {
        return m_memento.m_currentBundle;
    }

    /**
     * Create a new memento with new histogram data, but keep the state of the current TransferFunctionBundle.<br>
     * 
     * To achieve this a deep copy of all currently set bundles is made and then put into this memento.
     * 
     * @param hist the data for the histogram background
     * 
     * @return the new memento
     */
    public final Memento createMemento(final Histogram hist) {
        return createMemento(m_memento, hist);
    }

    /**
     * Create a new memento using the passed data for the histogram, and copying the transfer functions from the passed
     * memento.<br>
     * 
     * This will perform a deep copy of the functions in the passed memento.
     * 
     * @param memento the memento used for copying the transfer functions
     * @param hist the new data
     * @return a new memento
     */
    public final Memento createMemento(final Memento memento, final Histogram hist) {

        final List<TransferFunctionBundle> bundles = new ArrayList<TransferFunctionBundle>();

        TransferFunctionBundle current = null;

        for (final TransferFunctionBundle b : memento.m_map.keySet()) {
            final TransferFunctionBundle copy = b.copy();

            if (b == memento.m_currentBundle) {
                current = copy;
            }

            bundles.add(copy);
        }

        return createMemento(bundles, hist, current);
    }

    /**
     * Create a new memento that can than be used to set the state.<br>
     * 
     * I.e. if you want to set different data, first create an memento and then put it in the control.
     * 
     * @param bundles a list of the bundles to display, the first element of this list will be active first
     * @param hist the data for the histogram
     * 
     * @return the new memento
     */
    public final Memento createMemento(final List<TransferFunctionBundle> bundles, final Histogram hist) {
        return createMemento(bundles, hist, bundles.get(0));
    }

    /**
     * Create a new memento for that can than be used to set the state.<br>
     * 
     * I.e. if you want to set different data, first create an memento and then put it in the control.
     * 
     * @param bundles a list of the bundles to display, the first element of this list will be active first
     * @param hist the data for the histogram
     * @param current the bundle from the bundles list that should be active when this memento is put to use
     * 
     * @return the new memento
     */
    public final Memento createMemento(final List<TransferFunctionBundle> bundles, final Histogram hist,
                                       final TransferFunctionBundle current) {

        if (!bundles.contains(current)) {
            throw new IllegalArgumentException("The current bundle must be part of the bundles list");
        }

        final Memento memento = new Memento((HistogramPainter.Scale)m_scaleBox.getSelectedItem(), current, hist);

        // set up the map
        for (final TransferFunctionBundle b : bundles) {
            memento.m_map.put(b, b.getKeys().iterator().next());
        }

        return memento;
    }

    /**
     * Sets the mode this panel operates in and that of all its children.
     * 
     * @param bundle the new active bundle
     */
    private void setActiveBundle(final TransferFunctionBundle bundle) {

        assert m_memento != null;

        m_memento.m_currentBundle = bundle;

        m_transferPanel.setBundle(m_memento.m_currentBundle);

        final Set<TransferFunctionColor> content = m_memento.m_currentBundle.getKeys();
        final TransferFunctionColor focus = m_memento.m_map.get(m_memento.m_currentBundle);

        // change contents or focus box
        // remove the adapter to not set focus to first inserted item
        m_focusBox.removeActionListener(m_focusAdapter);
        m_focusBox.removeAllItems();

        for (final TransferFunctionColor color : content) {
            m_focusBox.addItem(color);
        }

        m_focusBox.addActionListener(m_focusAdapter);

        m_focusBox.setSelectedItem(focus);

        fireActionEvent(ID_APPLY, CMD_APPLY);

        repaint();
    }

    public final void setAutoApply(final boolean value) {
        m_boxAutoApply.setSelected(value);
    }

    public final void setNormalize(final boolean value) {
        applyNormalize(value);
        m_boxNormalize.setSelected(value);
    }

    public final void setOnlyOneFunc(final boolean value) {
        m_boxOnlyOneFunc.setSelected(value);
    }

    public final TransferFunction getLastModifiedFunction() {
        return m_lastModified;
    }

    public final TransferFunctionBundle getCurrentBundle() {
        return m_memento.m_currentBundle;
    }

    /**
     * Check wheter the only one function option is selected.
     * 
     * @return true if force checkbox is selected.
     */
    public final boolean isOnlyOneFunc() {
        return m_boxOnlyOneFunc.isSelected();
    }

    public final boolean isNormalize() {
        return m_boxNormalize.isSelected();
    }

    /**
     * Check wheter the autoapply option is currently given.
     * 
     * @return true if autoapply checkbox is selected.
     */
    public final boolean isAutoApply() {
        return m_boxAutoApply.isSelected();
    }

    @Override
    public final void transferFunctionChg(final TransferFunctionChgEvent event) {
        m_lastModified = event.getFunction();

        if (m_boxAutoApply.isSelected()) {
            fireActionEvent(ID_APPLY, CMD_APPLY);
        }
    }

    private void applyNormalize(final boolean value) {
        m_transferPanel.normalize(value);
    }

    public void addActionListener(final ActionListener l) {
        m_listener.add(ActionListener.class, l);
    }

    public void removeActionListener(final ActionListener l) {
        m_listener.remove(ActionListener.class, l);
    }

    private void fireActionEvent(final int id, final String command) {
        for (final ActionListener l : m_listener.getListeners(ActionListener.class)) {
            l.actionPerformed(new ActionEvent(this, id, command));
        }
    }
}
