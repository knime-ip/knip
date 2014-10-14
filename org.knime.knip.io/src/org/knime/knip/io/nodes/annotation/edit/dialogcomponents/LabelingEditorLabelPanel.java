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
package org.knime.knip.io.nodes.annotation.edit.dialogcomponents;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.ui.imgviewer.annotator.create.AnnotatorLabelPanel;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsColResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsSelChgEvent;
import org.knime.knip.core.ui.imgviewer.events.HilitedLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelPanelIsHiliteModeEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorResetEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorResetRowEvent;

/**
 * A list for selecting labels used in the InteractiveLabelingEditor.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 */
public class LabelingEditorLabelPanel extends AnnotatorLabelPanel {

	private static final int PANEL_WIDTH = 150;

	private static final int BUTTON_HEIGHT = 25;

	private static final long serialVersionUID = 1L;

	private boolean m_highlight = false;

	public LabelingEditorLabelPanel(final String... defaultLabels) {

		setTitle("Labels");

		setPreferredSize(new Dimension(PANEL_WIDTH, 200));

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		setLayout(new BorderLayout());

		m_labels = new Vector<String>();
		if (defaultLabels != null) {
			for (final String s : defaultLabels) {
				m_labels.add(s);
			}
		}

		m_jLabelList = new JList<String>(m_labels);
		m_jLabelList.setSelectedIndex(0);

		m_jLabelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		m_jLabelList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {

				if (m_isAdjusting || e.getValueIsAdjusting()) {
					return;
				}

				m_eventService.publish(new AnnotatorLabelsSelChgEvent(
						m_jLabelList.getSelectedValuesList().toArray(
								new String[0])));
				if (m_highlight) {
					m_eventService.publish(new HilitedLabelsChgEvent(
							new HashSet<String>(m_jLabelList
									.getSelectedValuesList())));
					m_eventService.publish(new ImgRedrawEvent());
				}
			}
		});

		add(new JScrollPane(m_jLabelList), BorderLayout.CENTER);
		JButton jb;

		// jb = new JButton("Delete label");
		// setButtonIcon(jb, "icons/tool-clean.png");
		// jb.setMinimumSize(new Dimension(140, 30));
		// jb.addActionListener(new ActionListener() {
		// @Override
		// public void actionPerformed(final ActionEvent e) {
		//
		// if (JOptionPane
		// .showConfirmDialog(
		// m_parent,
		// "Do you really want to delete all selected labels selection?",
		// "Confirm", JOptionPane.OK_CANCEL_OPTION) == 0) {
		//
		// if (m_jLabelList.isSelectionEmpty()) {
		// return;
		// }
		//
		// if (JOptionPane.showConfirmDialog(
		// m_parent,
		// "Do you really want to delete your complete selection of the class(es)  \'"
		// + Arrays.toString(m_jLabelList
		// .getSelectedValuesList().toArray(
		// new String[0])) + " \'?",
		// "Confirm", JOptionPane.OK_CANCEL_OPTION) == 0) {
		//
		// m_eventService.publish(new AnnotatorLabelsDelEvent(
		// m_jLabelList.getSelectedValuesList().toArray(
		// new String[0])));
		//
		// for (final String s : m_jLabelList
		// .getSelectedValuesList()) {
		// m_labels.remove(s);
		// }
		//
		// m_jLabelList.setListData(m_labels);
		// m_jLabelList.setSelectedIndex(Math.max(0,
		// m_labels.size() - 1));
		// }
		// }
		//
		// }
		// });
		// jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		// jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		// buttonPanel.add(jb);

		jb = new JButton("Create label");
		setButtonIcon(jb, "icons/tool-class.png");
		jb.setMinimumSize(new Dimension(140, 30));
		jb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final String name = JOptionPane.showInputDialog(m_parent,
						"Class name:");
				if ((name != null) && (name.length() > 0)) {
					m_labels.add(name);
					Collections.sort(m_labels);
					m_jLabelList.setListData(m_labels);
					m_jLabelList.setSelectedIndex(m_jLabelList.getNextMatch(
							name, 0, javax.swing.text.Position.Bias.Forward));
				}
			}
		});
		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		// jb = new JButton("Rename label");
		// setButtonIcon(jb, "icons/tool-rename.png");
		// jb.setMinimumSize(new Dimension(140, 30));
		// jb.addActionListener(new ActionListener() {
		// @Override
		// public void actionPerformed(final ActionEvent e) {
		// final List<String> selectedLabels = m_jLabelList
		// .getSelectedValuesList();
		// if ((selectedLabels.size() == 0) || (selectedLabels.size() > 1)) {
		// JOptionPane.showMessageDialog(m_parent,
		// "Please select ONE label",
		// "Wrong number of selected labels",
		// JOptionPane.ERROR_MESSAGE);
		// return;
		// } else {
		//
		// final int selIndex = m_jLabelList.getSelectedIndex();
		// final String oldName = selectedLabels.get(0);
		// final String res = JOptionPane.showInputDialog(m_parent,
		// "New unique label name:", selectedLabels.get(0));
		//
		// if (res == null) {
		// return;
		// }
		// for (int i = 0; i < m_jLabelList.getModel().getSize(); ++i) {
		// String currlabel = m_jLabelList.getModel()
		// .getElementAt(i);
		// if (currlabel.equals(res)) {
		// JOptionPane.showMessageDialog(m_parent,
		// "Please enter a UNIQUE name",
		// "Label already exists",
		// JOptionPane.ERROR_MESSAGE);
		// return;
		// }
		// if (currlabel.equals("")) {
		// JOptionPane.showMessageDialog(m_parent,
		// "A Labeling has to be at least 1 character",
		// "Empty name entered",
		// JOptionPane.ERROR_MESSAGE);
		// return;
		// }
		// }
		//
		// m_isAdjusting = true;
		// m_labels.set(m_jLabelList.getSelectedIndex(), res);
		// m_jLabelList.setListData(m_labels);
		// m_jLabelList.setSelectedIndex(selIndex);
		// m_isAdjusting = false;
		// m_eventService.publish(new AnnotatorLabelEditEvent(oldName,
		// res));
		//
		// }
		//
		// }
		// });
		// jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		// jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		// buttonPanel.add(jb);

		buttonPanel.add(Box.createVerticalStrut(10));

		jb = new JButton("Hilite selected");
		setButtonIcon(jb, "icons/tool-clean.png");
		jb.setMinimumSize(new Dimension(140, 30));
		jb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {

				m_eventService.publish(new LabelPanelIsHiliteModeEvent(true));
				m_highlight = true;
				m_eventService.publish(new HilitedLabelsChgEvent(
						new HashSet<String>(m_jLabelList
								.getSelectedValuesList())));
				m_eventService.publish(new ImgRedrawEvent());

			}
		});
		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		jb = new JButton("Stop hiliting");
		setButtonIcon(jb, "icons/tool-clean.png");
		jb.setMinimumSize(new Dimension(140, 30));
		jb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {

				m_eventService.publish(new HilitedLabelsChgEvent(
						new HashSet<String>()));
				m_highlight = false;
				m_eventService.publish(new LabelPanelIsHiliteModeEvent(false));
				m_eventService.publish(new ImgRedrawEvent());

			}
		});
		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		buttonPanel.add(Box.createVerticalStrut(10));

		jb = new JButton("Reset to input");
		setButtonIcon(jb, "icons/tool-setlabels.png");
		jb.setMinimumSize(new Dimension(140, 30));
		jb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				m_eventService.publish(new LabelingEditorResetEvent());
			}
		});
		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		jb = new JButton("Reset current image");
		setButtonIcon(jb, "icons/tool-setlabels.png");
		jb.setMinimumSize(new Dimension(140, 30));
		jb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				m_eventService.publish(new LabelingEditorResetRowEvent());
			}
		});
		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		jb = new JButton("Randomize color");
		setButtonIcon(jb, "icons/tool-colorreset.png");
		jb.setMinimumSize(new Dimension(140, 30));
		jb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_jLabelList.updateUI();
				for (final String s : m_jLabelList.getSelectedValuesList()) {
					RandomMissingColorHandler.resetColor(s);
				}

				m_eventService.publish(new AnnotatorLabelsColResetEvent(
						m_jLabelList.getSelectedValuesList().toArray(
								new String[0])));

			}
		});
		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		add(buttonPanel, BorderLayout.SOUTH);

	}

}
