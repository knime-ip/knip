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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.annotator.create.AnnotatorLabelPanel;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsColResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsSelChgEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.events.HilitedLabelsChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorDeleteAddedEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorHighlightEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorRenameAddedEvent;
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

	private Vector<String> m_newLabels;

	private JList<String> m_newLabelList;

	private boolean m_highlight = false;

	private JToggleButton m_highlightButton;

	public LabelingEditorLabelPanel(final Vector<String> newLabels,
			final String... defaultLabels) {

		add(Box.createVerticalStrut(10));
		m_newLabels = newLabels;
		setPreferredSize(new Dimension(PANEL_WIDTH, 200));

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		final JCheckBox highlight = new JCheckBox("Highlight new?");
		JPanel highlightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		highlightPanel
				.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		highlight.setAlignmentX(Component.LEFT_ALIGNMENT);
		highlight.setHorizontalTextPosition(SwingConstants.RIGHT);
		highlightPanel.add(highlight);
		add(highlightPanel);

		highlight.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					m_highlight = true;

					m_eventService.publish(new LabelingEditorHighlightEvent(
							m_newLabels, true));

				} else {
					m_highlight = false;
					m_eventService.publish(new LabelingEditorHighlightEvent(
							null, false));
				}
				m_eventService.publish(new ImgRedrawEvent());
			}
		});

		final JLabel newLabel = new JLabel("New Labels");
		m_labels = new Vector<String>();
		if (defaultLabels != null) {
			for (final String s : defaultLabels) {
				m_labels.add(s);
			}
		}

		m_newLabelList = new JList<String>(m_newLabels);
		m_newLabelList.setSelectedIndex(0);

		m_newLabelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		m_newLabelList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {

				if (m_isAdjusting || e.getValueIsAdjusting()) {
					m_jLabelList.clearSelection();
					return;
				}

				m_eventService.publish(new AnnotatorLabelsSelChgEvent(
						m_newLabelList.getSelectedValuesList().toArray(
								new String[0])));
				if (m_highlight) {
					m_eventService.publish(new LabelingEditorHighlightEvent(
							m_newLabels, true));
				}
			}
		});
		newLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(newLabel);

		add(new JScrollPane(m_newLabelList));

		JButton jb;

		jb = new JButton("Create Label");
		setButtonIcon(jb, "icons/tool-class.png");
		jb.setMinimumSize(new Dimension(140, 30));
		jb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final String name = JOptionPane.showInputDialog(m_parent,
						"Class name:");
				if ((name != null) && (name.length() > 0)) {
					m_newLabels.add(name);
					Collections.sort(m_newLabels);
					m_newLabelList.setListData(m_newLabels);
					m_newLabelList.setSelectedIndex(m_newLabelList
							.getNextMatch(name, 0,
									javax.swing.text.Position.Bias.Forward));
				}
			}
		});
		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		jb = new JButton("Delete label");
		setButtonIcon(jb, "icons/tool-clean.png");
		jb.setMinimumSize(new Dimension(140, 30));

		jb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_newLabelList.getSelectedValue() != null) {

					// m_newLabelList.updateUI();
					m_eventService.publish(new LabelingEditorDeleteAddedEvent(
							m_newLabelList.getSelectedValuesList()));
					m_newLabels.remove(m_newLabelList.getSelectedValue());
					m_newLabelList.setListData(m_newLabels);
					m_eventService.publish(new ImgRedrawEvent());
				}

			}
		});

		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		jb = new JButton("Rename label");
		setButtonIcon(jb, "icons/tool-clean.png");
		jb.setMinimumSize(new Dimension(140, 30));

		jb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_newLabelList.getSelectedValue() != null) {
					final String oldName = m_newLabelList.getSelectedValue();
					final String newName = JOptionPane.showInputDialog(
							m_parent, "New class name:");
					if ((newName != null) && (newName.length() > 0)) {
						m_newLabels.remove(oldName);
						m_newLabels.add(newName);
						m_eventService
								.publish(new LabelingEditorRenameAddedEvent(
										oldName, newName));
						Collections.sort(m_newLabels);
						m_newLabelList.setListData(m_newLabels);
						m_newLabelList.setSelectedIndex(m_newLabelList
								.getNextMatch(newName, 0,
										javax.swing.text.Position.Bias.Forward));
						m_eventService.publish(new ImgRedrawEvent());
					}
				}

			}
		});

		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		buttonPanel.add(Box.createVerticalStrut(10));

		jb = new JButton("Reset to Input");
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

		jb = new JButton("Recolor Label");
		setButtonIcon(jb, "icons/tool-colorreset.png");
		jb.setMinimumSize(new Dimension(140, 30));
		jb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_jLabelList.updateUI();
				for (final String s : m_jLabelList.getSelectedValuesList()) {
					RandomMissingColorHandler.resetColor(s);
				}
				for (final String s : m_newLabelList.getSelectedValuesList()) {
					RandomMissingColorHandler.resetColor(s);
				}

				m_eventService.publish(new AnnotatorLabelsColResetEvent(
						m_jLabelList.getSelectedValuesList().toArray(
								new String[0])));
				m_eventService.publish(new ImgRedrawEvent());

			}
		});
		jb.setMaximumSize(new Dimension(PANEL_WIDTH, BUTTON_HEIGHT));
		jb.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(jb);

		add(buttonPanel);
		add(Box.createVerticalStrut(10));

		final JLabel oldLabels = new JLabel("Old Labels");
		oldLabels.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(oldLabels);

		m_jLabelList = new JList<String>(m_labels);
		m_jLabelList.setSelectedIndex(0);

		m_jLabelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		m_jLabelList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {

				if (m_isAdjusting || e.getValueIsAdjusting()) {
					m_newLabelList.clearSelection();
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

		add(new JScrollPane(m_jLabelList));

	}

	@EventListener
	public void onAnnotatorReset(final AnnotatorResetEvent event) {
		if (m_highlightButton != null) {
			m_highlightButton.setSelected(false);
			m_highlight = false;

		}
	}

	// public void addNewLabel(String label) {
	// m_newLabels.add(label);
	// }

	@Override
	/**
	 * @param list
	 */
	public void addLabels(final Set<String> list) {

		for (String label : list) {
			if (!m_labels.contains(label) && !m_newLabels.contains(label)) {
				m_labels.add(label);
			}
		}

		updateLocalUI();
	}
}
