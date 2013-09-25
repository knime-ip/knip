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
package org.knime.knip.core.ui.imgviewer.annotator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelEditEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsColResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsDelEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsSelChgEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsSetEvent;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class AnnotatorLabelPanel extends ViewerComponent {

    private static final int PANEL_WIDTH = 150;

    private static final int BUTTON_HEIGHT = 25;

    private static final long serialVersionUID = 1L;

    private Vector<String> m_labels;

    private JList m_jLabelList;

    private boolean m_isAdjusting;

    private EventService m_eventService;

    private Component m_parent;

    public AnnotatorLabelPanel(final String... defaultLabels) {
        super("Labels", false);

        setPreferredSize(new Dimension(PANEL_WIDTH, 200));

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        setLayout(new BorderLayout());

        m_labels = new Vector<String>();
        if ((defaultLabels == null) || (defaultLabels.length == 0)) {
            m_labels.add("Unknown");
        } else {
            for (final String s : defaultLabels) {
                m_labels.add(s);
            }
        }

        m_jLabelList = new JList(m_labels);
        m_jLabelList.setSelectedIndex(0);

        m_jLabelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        m_jLabelList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent e) {

                if (m_isAdjusting || e.getValueIsAdjusting()) {
                    return;
                }

                m_eventService.publish(new AnnotatorLabelsSelChgEvent(objectArrayAsStringArray(m_jLabelList
                        .getSelectedValues())));
            }
        });

        add(new JScrollPane(m_jLabelList), BorderLayout.CENTER);

        JButton jb = new JButton("Delete labels");
        setButtonIcon(jb, "icons/tool-clean.png");
        jb.setMinimumSize(new Dimension(140, 30));
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {

                if (JOptionPane.showConfirmDialog(m_parent,
                                                  "Do you really want to delete all selected labels selection?",
                                                  "Confirm", JOptionPane.OK_CANCEL_OPTION) == 0) {

                    if (m_jLabelList.isSelectionEmpty()) {
                        return;
                    }

                    if (JOptionPane.showConfirmDialog(m_parent,
                                                      "Do you really want to delete your complete selection of the class(es)  \'"
                                                              + Arrays.toString(m_jLabelList.getSelectedValues())
                                                              + " \'?", "Confirm", JOptionPane.OK_CANCEL_OPTION) == 0) {

                        m_eventService.publish(new AnnotatorLabelsDelEvent(objectArrayAsStringArray(m_jLabelList
                                .getSelectedValues())));

                        for (final Object o : m_jLabelList.getSelectedValues()) {
                            m_labels.remove(o);
                        }

                        if (m_labels.size() == 0) {
                            m_labels.add("Unknown");
                        }

                        m_jLabelList.setListData(m_labels);
                        m_jLabelList.setSelectedIndex(Math.max(0, m_labels.size() - 1));
                    }
                }

            }
        });
        jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
        jb.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(jb);

        jb = new JButton("Add label");
        setButtonIcon(jb, "icons/tool-class.png");
        jb.setMinimumSize(new Dimension(140, 30));
        jb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                final String name = JOptionPane.showInputDialog(m_parent, "Class name:");
                if ((name != null) && (name.length() > 0)) {
                    m_labels.add(name);
                    m_jLabelList.setListData(m_labels);
                    m_jLabelList.setSelectedIndex(m_labels.size() - 1);
                }
            }
        });
        jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
        jb.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(jb);

        jb = new JButton("Set labels");
        setButtonIcon(jb, "icons/tool-setlabels.png");
        jb.setMinimumSize(new Dimension(140, 30));
        jb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_eventService.publish(new AnnotatorLabelsSetEvent(objectArrayAsStringArray(m_jLabelList
                        .getSelectedValues())));
            }
        });
        jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
        jb.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(jb);

        jb = new JButton("Reset colors");
        setButtonIcon(jb, "icons/tool-colorreset.png");
        jb.setMinimumSize(new Dimension(140, 30));
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_jLabelList.updateUI();
                for (final Object o : m_jLabelList.getSelectedValues()) {
                    RandomMissingColorHandler.resetColor((String)o);
                }

                m_eventService.publish(new AnnotatorLabelsColResetEvent(objectArrayAsStringArray(m_jLabelList
                        .getSelectedValues())));

            }
        });
        jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
        jb.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(jb);

        jb = new JButton("Rename label");
        setButtonIcon(jb, "icons/tool-rename.png");
        jb.setMinimumSize(new Dimension(140, 30));
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final String[] selectedLabels = objectArrayAsStringArray(m_jLabelList.getSelectedValues());
                if ((selectedLabels.length == 0) || (selectedLabels.length > 1)) {
                    JOptionPane.showMessageDialog(m_parent, "Please select ONE label",
                                                  "Wrong number of selected labels", JOptionPane.ERROR_MESSAGE);
                    return;
                } else {

                    final int selIndex = m_jLabelList.getSelectedIndex();
                    final String oldName = selectedLabels[0];
                    final String res = JOptionPane.showInputDialog(m_parent, "New label name:", selectedLabels[0]);

                    if (res == null) {
                        return;
                    }

                    m_isAdjusting = true;
                    m_labels.set(m_jLabelList.getSelectedIndex(), res);
                    m_jLabelList.setListData(m_labels);
                    m_jLabelList.setSelectedIndex(selIndex);
                    m_isAdjusting = false;
                    m_eventService.publish(new AnnotatorLabelEditEvent(oldName, res));

                }

            }
        });
        jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
        jb.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(jb);

        add(buttonPanel, BorderLayout.SOUTH);

    }

    private final void setButtonIcon(final AbstractButton jb, final String path) {
        final URL icon =
                getClass().getClassLoader().getResource(getClass().getPackage().getName().replace('.', '/') + "/"
                                                                + path);
        jb.setHorizontalAlignment(SwingConstants.LEFT);
        if (icon != null) {
            jb.setIcon(new ImageIcon(icon));
        }
    }

    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
    }

    @Override
    public Position getPosition() {
        return Position.EAST;
    }

    public static String[] objectArrayAsStringArray(final Object[] o) {
        final String[] s = new String[o.length];

        for (int i = 0; i < o.length; i++) {
            s[i] = (String)o[i];
        }

        return s;
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        out.writeInt(m_labels.size());
        for (int s = 0; s < m_labels.size(); s++) {
            out.writeUTF(m_labels.get(s));
        }

        out.writeInt(m_jLabelList.getSelectedIndices().length);
        for (int i = 0; i < m_jLabelList.getSelectedIndices().length; i++) {
            out.writeInt(m_jLabelList.getSelectedIndices()[i]);
        }

    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException {
        m_labels.clear();

        int num = in.readInt();
        for (int s = 0; s < num; s++) {
            m_labels.add(in.readUTF());
        }

        num = in.readInt();
        final int[] selected = new int[num];

        for (int i = 0; i < num; i++) {
            selected[i] = in.readInt();
        }

        m_isAdjusting = true;
        m_jLabelList.setListData(m_labels);
        m_jLabelList.setSelectedIndices(selected);
        m_jLabelList.updateUI();
        m_isAdjusting = false;
    }

}
