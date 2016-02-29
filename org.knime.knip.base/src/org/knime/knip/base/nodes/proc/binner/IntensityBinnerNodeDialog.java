/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2016
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
 * ---------------------------------------------------------------------
 *
 * Created on Feb 28, 2016 by hornm
 */
package org.knime.knip.base.nodes.proc.binner;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.base.node.preproc.pmml.binner.BinnerNodeDialogPane;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;

import net.imglib2.type.numeric.RealType;

/**
 *
 * NB: A lot of code has been copied from {@link BinnerNodeDialogPane}.
 *
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 */
public class IntensityBinnerNodeDialog<T extends RealType<T>> extends ValueToCellNodeDialog<ImgPlusValue<T>> {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(BinnerNodeDialogPane.class);

    private final IntervalPanel m_intervalPanel;

    private final JLabel m_info;

    /**
     * Creates a new binner dialog.
     */
    public IntensityBinnerNodeDialog() {
        super(true);

        // panel in tab
        final JPanel intervalTabPanel = new JPanel(new BorderLayout());
        m_intervalPanel = new IntervalPanel(intervalTabPanel, DoubleCell.TYPE);

        // numeric column intervals
        m_intervalPanel.setBorder(BorderFactory.createTitledBorder("Intervals"));
        m_intervalPanel.setMinimumSize(new Dimension(350, 300));
        m_intervalPanel.setPreferredSize(new Dimension(350, 300));
        intervalTabPanel.add(m_intervalPanel, BorderLayout.CENTER);

        final JPanel infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createTitledBorder("Result Pixel Type"));
        m_info = new JLabel("");
        infoPanel.add(m_info);
        intervalTabPanel.add(infoPanel, BorderLayout.SOUTH);

        super.addTab(" Intervals ", intervalTabPanel);
        super.buildDialog();
    }

    private SpinnerNumberModel createNumberModel(final DataType type) {
        if (IntCell.TYPE.equals(type)) {
            return new SpinnerNumberModel(0, null, null, 1);
        }
        return new SpinnerNumberModel(0.0, NEGATIVE_INFINITY, POSITIVE_INFINITY, 0.1);
    }

    /**
     * Creates new panel holding the binning gui.
     */
    final class IntervalPanel extends JPanel {
        /** List of intervals. */
        private final JList m_intervalList;

        /** The intervals' model. */
        private final DefaultListModel m_intervalMdl;

        /**
         * Create new interval panel.
         *
         * @param column the current column name
         * @param parent used to refresh column list is number of bins has changed
         * @param type the type for the spinner model
         *
         */
        IntervalPanel(final Component parent, final DataType type) {
            super(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(" " + "Title" + " "));
            m_intervalMdl = new DefaultListModel();
            m_intervalList = new JList(m_intervalMdl);
            Font font = new Font("Monospaced", Font.PLAIN, 12);
            m_intervalList.setFont(font);
            final JButton addButton = new JButton("Add");
            addButton.addActionListener(new ActionListener() {
                /**
                 *
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final int size = m_intervalMdl.getSize();
                    // if the first interval is added
                    if (size == 0) {
                        m_intervalMdl.addElement(new IntervalItemPanel(IntervalPanel.this, null, null, 0.0, type));
                    } else {
                        // if the first interval needs to be split
                        if (size == 1) {
                            IntervalItemPanel p = new IntervalItemPanel(IntervalPanel.this, 0.0, null, 1.0, type);
                            m_intervalMdl.addElement(p);
                            p.updateInterval();
                        } else {
                            Object o = m_intervalList.getSelectedValue();
                            // if non is selected or the last one is selected
                            if (o == null || m_intervalMdl.indexOf(o) == size - 1) {
                                IntervalItemPanel p1 = (IntervalItemPanel)m_intervalMdl.getElementAt(size - 1);
                                double d = p1.getLeftValue(false);
                                IntervalItemPanel p = new IntervalItemPanel(IntervalPanel.this, d, POSITIVE_INFINITY,
                                        (size + 1.0), type);
                                m_intervalMdl.insertElementAt(p, size);
                                p.updateInterval();
                            } else {
                                IntervalItemPanel p1 = (IntervalItemPanel)o;
                                IntervalItemPanel p2 =
                                        (IntervalItemPanel)m_intervalMdl.getElementAt(m_intervalMdl.indexOf(p1) + 1);
                                double d1 = p1.getRightValue(false);
                                double d2 = p2.getLeftValue(false);
                                IntervalItemPanel p =
                                        new IntervalItemPanel(IntervalPanel.this, d1, d2, +(size + 1.0), type);
                                m_intervalMdl.insertElementAt(p, m_intervalMdl.indexOf(p1) + 1);
                                p.updateInterval();
                            }
                        }
                    }
                    updateInfo();
                    parent.validate();
                    parent.repaint();
                }
            });
            final JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(new ActionListener() {
                /**
                 *
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    IntervalItemPanel p = (IntervalItemPanel)m_intervalList.getSelectedValue();
                    if (p != null) {
                        int i = m_intervalMdl.indexOf(p);
                        m_intervalMdl.removeElement(p);
                        int size = m_intervalMdl.getSize();
                        if (size > 0) {
                            if (size == 1 || size == i) {
                                m_intervalList.setSelectedIndex(size - 1);
                            } else {
                                m_intervalList.setSelectedIndex(i);
                            }
                            ((IntervalItemPanel)m_intervalList.getSelectedValue()).updateInterval();
                        }
                        updateInfo();
                        parent.validate();
                        parent.repaint();
                    }
                }
            });
            final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
            buttonPanel.add(addButton);
            buttonPanel.add(removeButton);
            super.add(buttonPanel, BorderLayout.NORTH);

            //
            // interval list
            //

            final JPanel selInterval = new JPanel(new GridLayout(1, 1));
            selInterval.add(new IntervalItemPanel(this, null, null, null, type));
            selInterval.validate();
            selInterval.repaint();

            m_intervalList.addListSelectionListener(new ListSelectionListener() {
                /**
                 *
                 */
                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    selInterval.removeAll();
                    Object o = m_intervalList.getSelectedValue();
                    if (o == null) {
                        selInterval.add(new IntervalItemPanel(IntervalPanel.this, null, null, null, type));
                    } else {
                        selInterval.add((IntervalItemPanel)o);
                    }
                    m_intervalPanel.validate();
                    m_intervalPanel.repaint();
                }
            });
            m_intervalList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            final JScrollPane intervalScroll = new JScrollPane(m_intervalList);
            intervalScroll.setMinimumSize(new Dimension(200, 155));
            intervalScroll.setPreferredSize(new Dimension(200, 155));
            super.add(intervalScroll, BorderLayout.CENTER);

            JPanel southPanel = new JPanel(new BorderLayout());
            southPanel.add(selInterval, BorderLayout.CENTER);
            super.add(southPanel, BorderLayout.SOUTH);
        }

        private void addIntervalItem(final IntervalItemPanel item) {
            m_intervalMdl.addElement(item);
            // m_intervalMdl.insertElementAt(item, m_intervalMdl.getSize());
            // item.updateInterval();
        }

        private void removeAllIntervalItems() {
            m_intervalMdl.clear();
        }

        /**
         * @param item the current interval item
         * @return the previous one in the list or <code>null</code>
         */
        private IntervalItemPanel getPrevious(final IntervalItemPanel item) {
            int i = m_intervalMdl.indexOf(item);
            if (i > 0) {
                return (IntervalItemPanel)m_intervalMdl.getElementAt(i - 1);
            }
            return null;
        }

        /**
         * @param item the current interval item
         * @return the next one in the list or <code>null</code>
         */
        private IntervalItemPanel getNext(final IntervalItemPanel item) {
            int i = m_intervalMdl.indexOf(item);
            if (i >= 0 && i + 1 < m_intervalMdl.getSize()) {
                return (IntervalItemPanel)m_intervalMdl.getElementAt(i + 1);
            }
            return null;
        }

        /**
         * @return number of interval specified for binning
         */
        public int getNumIntervals() {
            return m_intervalMdl.getSize();
        }

        /**
         * @param i index for interval
         * @return the interval item
         */
        public IntervalItemPanel getInterval(final int i) {
            return (IntervalItemPanel)m_intervalMdl.get(i);
        }

    }

    /**
     * Creates a new panel holding one interval.
     */
    final class IntervalItemPanel extends JPanel {
        private final IntervalPanel m_parent;

        private final JComboBox m_borderLeft = new JComboBox();

        private final JSpinner m_left;

        private final JSpinner m_right;

        private final JComboBox m_borderRight = new JComboBox();

        private final JSpinner m_bin;

        /** Left/open or right/closed interval bracket. */
        static final String LEFT = "]";

        /** Right/open or left/closed interval bracket. */
        static final String RIGHT = "[";

        /**
         * @param parent the interval item's parent component
         * @param leftOpen initial left open
         * @param left initial left value
         * @param rightOpen initial right open
         * @param right initial right value
         * @param binValue the bin value
         * @param type the column type of this interval
         */
        IntervalItemPanel(final IntervalPanel parent, final boolean leftOpen, final Double left,
                          final boolean rightOpen, final Double right, final Double binValue, final DataType type) {
            this(parent, left, right, binValue, type);
            setLeftOpen(leftOpen);
            setRightOpen(rightOpen);
        }

        /**
         * @param parent the interval item's parent component
         * @param left initial left value
         * @param right initial right value
         * @param binValue the bin value
         * @param type the column type of this interval
         */
        IntervalItemPanel(final IntervalPanel parent, final Double left, final Double right, final Double binValue,
                          final DataType type) {
            this(parent, type);
            if (binValue == null) {
                m_bin.setValue(0);
                m_bin.setEnabled(false);
            } else {
                m_bin.setValue(binValue);
            }
            JPanel p1 = new JPanel(new BorderLayout());
            p1.add(m_bin, BorderLayout.CENTER);
            p1.add(new JLabel(" :  "), BorderLayout.EAST);
            super.add(p1);
            JPanel p2 = new JPanel(new BorderLayout());
            p2.add(m_borderLeft, BorderLayout.WEST);
            p2.add(m_left, BorderLayout.CENTER);
            p2.add(new JLabel(" ."), BorderLayout.EAST);
            setLeftValue(left);
            super.add(p2);
            JPanel p3 = new JPanel(new BorderLayout());
            p3.add(new JLabel(". "), BorderLayout.WEST);
            p3.add(m_right, BorderLayout.CENTER);
            p3.add(m_borderRight, BorderLayout.EAST);
            setRightValue(right);
            super.add(p3);
            initListener();
        }

        /*
         * @param parent the interval item's parent component
         */
        private IntervalItemPanel(final IntervalPanel parent, final DataType type) {
            super(new GridLayout(1, 0));
            m_parent = parent;

            m_bin = new JSpinner(createNumberModel(type));
            m_bin.setPreferredSize(new Dimension(50, 25));
            JSpinner.DefaultEditor editorBin = new JSpinner.NumberEditor(m_bin, "0.0##############");
            editorBin.getTextField().setColumns(15);
            m_bin.setEditor(editorBin);
            m_bin.setPreferredSize(new Dimension(125, 25));

            m_left = new JSpinner(createNumberModel(type));
            JSpinner.DefaultEditor editorLeft = new JSpinner.NumberEditor(m_left, "0.0##############");
            editorLeft.getTextField().setColumns(15);
            m_left.setEditor(editorLeft);
            m_left.setPreferredSize(new Dimension(125, 25));

            m_right = new JSpinner(createNumberModel(type));
            JSpinner.DefaultEditor editorRight = new JSpinner.NumberEditor(m_right, "0.0##############");
            editorRight.getTextField().setColumns(15);
            m_right.setEditor(editorRight);
            m_right.setPreferredSize(new Dimension(125, 25));

            m_borderLeft.setPreferredSize(new Dimension(50, 25));
            m_borderLeft.setLightWeightPopupEnabled(false);
            m_borderLeft.addItem(RIGHT);
            m_borderLeft.addItem(LEFT);

            m_borderRight.setPreferredSize(new Dimension(50, 25));
            m_borderRight.setLightWeightPopupEnabled(false);
            m_borderRight.addItem(LEFT);
            m_borderRight.addItem(RIGHT);
        }

        private void initListener() {
            m_left.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    repairLeft();
                }
            });
            final JSpinner.DefaultEditor editorLeft = (JSpinner.DefaultEditor)m_left.getEditor();
            editorLeft.getTextField().addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(final FocusEvent e) {
                    getLeftValue(true);
                    repairLeft();
                }

                @Override
                public void focusGained(final FocusEvent e) {
                }
            });

            m_right.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    repairRight();
                }
            });
            final JSpinner.DefaultEditor editorRight = (JSpinner.DefaultEditor)m_right.getEditor();
            editorRight.getTextField().addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(final FocusEvent e) {
                    getRightValue(true);
                    repairRight();
                }

                @Override
                public void focusGained(final FocusEvent e) {
                }
            });

            m_borderLeft.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    IntervalItemPanel prev = m_parent.getPrevious(IntervalItemPanel.this);
                    if (prev != null && prev.isRightOpen() == isLeftOpen()) {
                        prev.setRightOpen(!isLeftOpen());
                    }
                    myRepaint();
                }
            });

            m_borderRight.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    IntervalItemPanel next = m_parent.getNext(IntervalItemPanel.this);
                    if (next != null && next.isLeftOpen() == isRightOpen()) {
                        next.setLeftOpen(!isRightOpen());
                    }
                    myRepaint();
                }
            });

            m_bin.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    updateInfo();
                    myRepaint();
                }
            });
            final JSpinner.DefaultEditor editorBin = (JSpinner.DefaultEditor)m_bin.getEditor();
            editorRight.getTextField().addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(final FocusEvent e) {
                    getBinValue();
                    myRepaint();
                }

                @Override
                public void focusGained(final FocusEvent e) {
                }
            });

        }

        private void repairLeft() {
            double l = getLeftValue(false);
            double r = getRightValue(true);
            if (l > r) {
                setRightValue(l);
                repairNext(l);
            }
            repairPrev(l);
            myRepaint();
        }

        private void repairRight() {
            double r = getRightValue(false);
            double l = getLeftValue(true);
            if (l > r) {
                repairPrev(r);
                setLeftValue(r);
            }
            repairNext(r);
            myRepaint();

        }

        /**
         * @return the name for this interval bin
         */
        public Double getBinValue() {
            return (Double)m_bin.getValue();
        }

        /**
         * Checks the current, previous, and next interval for consistency; and updates the intervals if necessary.
         */
        public void updateInterval() {
            IntervalItemPanel prev = m_parent.getPrevious(this);
            IntervalItemPanel next = m_parent.getNext(this);
            if (prev == null && next == null) {
                this.setLeftValue(null);
                this.setRightValue(null);
                this.setLeftOpen(true);
                this.setRightOpen(true);
            } else {
                repairPrev(getLeftValue(true));
                repairNext(getRightValue(true));
            }
            myRepaint();
        }

        private void myRepaint() {
            m_intervalPanel.validate();
            m_intervalPanel.repaint();
        }

        private void repairPrev(final double value) {
            IntervalItemPanel prev = m_parent.getPrevious(this);
            if (prev != null) {
                if (prev.getRightValue(false) != value) {
                    prev.setRightValue(value);
                    if (prev.getLeftValue(false) > value) {
                        prev.setLeftValue(value);
                    }
                }
                if (prev.isRightOpen() == isLeftOpen()) {
                    prev.setRightOpen(!isLeftOpen());
                }
            } else {
                setLeftValue(null);
                setLeftOpen(true);
            }
        }

        private void repairNext(final double value) {
            IntervalItemPanel next = m_parent.getNext(this);
            if (next != null) {
                if (next.getLeftValue(false) != value) {
                    next.setLeftValue(value);
                    if (next.getRightValue(false) < value) {
                        next.setRightValue(value);
                    }
                }
                if (next.isLeftOpen() == isRightOpen()) {
                    next.setLeftOpen(!isRightOpen());
                }
            } else {
                setRightValue(null);
                setRightOpen(true);
            }
        }

        /**
         * @param left new left value
         */
        public void setLeftValue(final Double left) {
            if (left == null || left.doubleValue() == NEGATIVE_INFINITY) {
                m_borderLeft.setSelectedItem(LEFT);
                m_borderLeft.setEnabled(false);
                m_left.setValue(NEGATIVE_INFINITY);
                m_left.setEnabled(false);
            } else {
                m_left.setValue(left);
                m_left.setEnabled(true);
                m_borderLeft.setEnabled(true);
            }
        }

        /**
         * @return left value
         * @param commit if the value has to be committed first
         */
        public double getLeftValue(final boolean commit) {
            if (commit) {
                double old = ((Number)m_left.getValue()).doubleValue();
                try {
                    m_left.commitEdit();
                } catch (ParseException pe) {
                    return old;
                }
            }
            return ((Number)m_left.getValue()).doubleValue();

        }

        /**
         * @param left <code>true</code> if the left interval bound is open otherwise <code>false</code>
         */
        public void setLeftOpen(final boolean left) {
            if (left) {
                m_borderLeft.setSelectedItem(LEFT);
            } else {
                m_borderLeft.setSelectedItem(RIGHT);
            }
        }

        /**
         * @return <code>true</code> if left side open
         */
        public boolean isLeftOpen() {
            return LEFT.equals(m_borderLeft.getSelectedItem());
        }

        /**
         * @param right new right value
         */
        public void setRightValue(final Double right) {
            if (right == null || right.doubleValue() == POSITIVE_INFINITY) {
                m_borderRight.setSelectedItem(RIGHT);
                m_borderRight.setEnabled(false);
                m_right.setValue(POSITIVE_INFINITY);
                m_right.setEnabled(false);

            } else {
                m_right.setValue(right);
                m_right.setEnabled(true);
                m_borderRight.setEnabled(true);
            }
        }

        /**
         * @param right <code>true</code> if the right interval bound is open otherwise <code>false</code>
         */
        public void setRightOpen(final boolean right) {
            if (right) {
                m_borderRight.setSelectedItem(RIGHT);
            } else {
                m_borderRight.setSelectedItem(LEFT);
            }
        }

        /**
         * @return right value
         * @param commit if the value has to be committed first
         */
        public double getRightValue(final boolean commit) {
            if (commit) {
                double old = ((Number)m_right.getValue()).doubleValue();
                try {
                    m_right.commitEdit();
                } catch (ParseException pe) {
                    return old;
                }
            }
            return ((Number)m_right.getValue()).doubleValue();
        }

        /**
         * @return <code>true</code> if right open
         */
        public boolean isRightOpen() {
            return RIGHT.equals(m_borderRight.getSelectedItem());
        }

        /**
         * @return string containing left and right border, and open/not open
         */
        @Override
        public String toString() {
            double left = getLeftValue(false);
            double right = getRightValue(false);
            String leftString, rightString;
            JComponent editor = m_left.getEditor();
            if (editor instanceof JSpinner.NumberEditor) {
                JSpinner.NumberEditor numEdit = (JSpinner.NumberEditor)editor;
                leftString = numEdit.getFormat().format(left);
                rightString = numEdit.getFormat().format(right);
            } else {
                leftString = Double.toString(left);
                rightString = Double.toString(right);
            }
            String rightBorder = m_borderRight.getSelectedItem().toString();
            String leftBorder = m_borderLeft.getSelectedItem().toString();
            return getBinValue() + " : " + leftBorder + " " + leftString + " ... " + rightString + " " + rightBorder;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultSuffixForAppend() {
        return "_binned";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDialogComponents() {
        //don't compose the dialog via dialog components
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        IntensityBins bins = null;
        try {
            bins = new IntensityBins(settings.getNodeSettings(IntensityBinnerNodeModel.KEY_BIN_SETTINGS));
        } catch (InvalidSettingsException e) {
            // TODO
            throw new NotConfigurableException(e.getMessage(), e);
        }
        m_intervalPanel.removeAllIntervalItems();
        for (int i = 0; i < bins.getNumBins(); i++) {
            double binValue = bins.getBinValue(i);
            boolean leftOpen = bins.isLeftOpen(i);
            double left = bins.getLeftValue(i);
            boolean rightOpen = bins.isRightOpen(i);
            double right = bins.getRightValue(i);
            IntervalItemPanel item =
                    new IntervalItemPanel(m_intervalPanel, leftOpen, left, rightOpen, right, binValue, DoubleCell.TYPE);
            m_intervalPanel.addIntervalItem(item);
        }
        updateInfo();
        getPanel().validate();
        getPanel().repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_intervalPanel.getNumIntervals() == 0) {
            throw new InvalidSettingsException("No pixel bins have been specified.");
        }
        createPixelBins().saveToSettings(settings.addNodeSettings(IntensityBinnerNodeModel.KEY_BIN_SETTINGS));
    }

    /**
     * Helper method to create the {@link IntensityBins}-object from the IntervalPanel.
     *
     * @return
     */
    private IntensityBins createPixelBins() {
        IntensityBins bins = new IntensityBins(m_intervalPanel.getNumIntervals());
        for (int j = 0; j < m_intervalPanel.getNumIntervals(); j++) {
            IntervalItemPanel p = m_intervalPanel.getInterval(j);
            bins.setBinAtIndex(j, p.getBinValue(), p.isLeftOpen(), p.getLeftValue(false), p.isRightOpen(),
                               p.getRightValue(false));
        }
        return bins;
    }

    /**
     * Helper method that determines the result pixel type and updates the info label.
     */
    private void updateInfo() {
        if (m_intervalPanel.getNumIntervals() > 0) {
            //determine the result pixel type
            //and update the info label
            m_info.setText(createPixelBins().getPixelType().getClass().getSimpleName());
        } else {
            m_info.setText("No bins specified.");
        }
    }
}
