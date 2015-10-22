/*
 * ------------------------------------------------------------------------
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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsColResetEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsSelChgEvent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorAddEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorDeleteAllEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorDeleteEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorHighlightEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorRenameAddedEvent;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorResetRowEvent;

/**
 * A list for selecting labels used in the InteractiveLabelingEditor.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 */
public class LabelingEditorLabelPanel extends ViewerComponent {

	private static final long serialVersionUID = 1L;

	private EventService m_eventService;

	private Vector<String> m_newLabels = new Vector<String>();

	private final Vector<String> m_labels = new Vector<String>();

	private JList<String> m_newLabelList;

	private JList<String> m_labelList;

	private boolean m_highlight = false;

	private JToggleButton m_highlightButton;

	private final Container m_parent;

	public LabelingEditorLabelPanel(Vector<String> initialLabels) {
		super("", true);

		m_parent = this;
		m_newLabels = initialLabels;

		this.setLayout(new GridBagLayout());

		this.setPreferredSize(new Dimension(250, 500));

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;

		JLabel newLabelsLabel = new JLabel("New Labels");
		Box newLabelsLabelBox = new Box(BoxLayout.X_AXIS);
		newLabelsLabelBox.add(newLabelsLabel);
		newLabelsLabelBox.add(Box.createHorizontalGlue());
		add(newLabelsLabelBox, gbc);

		gbc.weighty = 0.5;
		add(createNewLabelPanel(), gbc);
		gbc.weighty = 0;

		JLabel currentLabelsLabel = new JLabel("Current Labels");
		Box currentLabelsLabelBox = new Box(BoxLayout.X_AXIS);
		currentLabelsLabelBox.add(currentLabelsLabel);
		currentLabelsLabelBox.add(Box.createHorizontalGlue());
		add(currentLabelsLabelBox, gbc);

		gbc.weighty = 1;
		add(createCurrentLabelPanel(), gbc);
		gbc.weighty = 0;

		Box buttonBox = new Box(BoxLayout.X_AXIS);
		buttonBox.add(Box.createHorizontalGlue());
		JButton labelRecolorButton = new JButton("Recolor");
		buttonBox.add(labelRecolorButton);
		buttonBox.add(Box.createHorizontalStrut(10));
		JButton resetButton = new JButton("Reset Labels");

		labelRecolorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				for (final String s : m_labelList.getSelectedValuesList()) {
					RandomMissingColorHandler.resetColor(s);
				}
				for (final String s : m_newLabelList.getSelectedValuesList()) {
					RandomMissingColorHandler.resetColor(s);
				}

				m_eventService.publish(
						new AnnotatorLabelsColResetEvent(m_labelList.getSelectedValuesList().toArray(new String[0])));
				m_eventService.publish(new ImgRedrawEvent());

			}
		});

		resetButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				m_eventService.publish(new LabelingEditorResetRowEvent());
			}
		});

		buttonBox.add(resetButton);
		buttonBox.add(Box.createHorizontalGlue());

		gbc.gridheight = GridBagConstraints.REMAINDER;

		add(buttonBox, gbc);

	}

	@Override
	public Position getPosition() {
		return Position.ADDITIONAL;
	}

	@EventListener
	public void onAnnotatorReset(final AnnotatorResetEvent event) {
		if (m_highlightButton != null) {
			m_highlightButton.setSelected(false);
			m_highlight = false;

		}
	}

	public void setCurrentLabels(Set<String> currentLabels) {
		m_labels.clear();
		m_labels.addAll(currentLabels);
		Collections.sort(m_labels);

		m_labelList.setListData(m_labels);
		m_labelList.updateUI();

	}

	public void setNewLabels(Set<String> newLabels) {
		m_newLabels.clear();
		m_newLabels.addAll(newLabels);
		Collections.sort(m_newLabels);

		m_newLabelList.setListData(m_newLabels);
		m_newLabelList.updateUI();
	}

	@Override
	public void setEventService(EventService eventService) {
		m_eventService = eventService;
		m_eventService.subscribe(this);

	}

	@Override
	public void saveComponentConfiguration(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadComponentConfiguration(ObjectInput in) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub

	}

	private JComponent createNewLabelPanel() {

		JPanel root = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridy = 0;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;

		m_newLabelList = new JList<String>(m_newLabels);

		m_newLabelList.setSelectedIndex(0);

		m_newLabelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		m_newLabelList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {

				if (e.getValueIsAdjusting())
					return;

				int i = m_newLabelList.getSelectedIndex();

				m_labelList.clearSelection();

				m_newLabelList.setSelectedIndex(i);

				m_eventService.publish(
						new AnnotatorLabelsSelChgEvent(m_newLabelList.getSelectedValuesList().toArray(new String[0])));
				if (m_highlight) {
					m_eventService.publish(new LabelingEditorHighlightEvent(m_newLabels, true));
				}
			}
		});

		root.add(new JScrollPane(m_newLabelList), gbc);

		Box buttonPanel = new Box(BoxLayout.Y_AXIS);
		JButton button1 = new JButton("Create");
		buttonPanel.add(button1);
		buttonPanel.add(Box.createVerticalStrut(5));
		JButton button2 = new JButton("Delete");
		buttonPanel.add(button2);
		buttonPanel.add(Box.createVerticalStrut(5));
		final JCheckBox highlight = new JCheckBox("Highlight new?");
		buttonPanel.add(highlight);
		buttonPanel.add(Box.createGlue());

		button1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final String name = JOptionPane.showInputDialog(m_parent, "Class name:");
				if ((name != null) && (name.length() > 0)) {
					m_newLabels.add(name);
					Collections.sort(m_newLabels);
					m_newLabelList.setListData(m_newLabels);
					m_newLabelList.setSelectedIndex(
							m_newLabelList.getNextMatch(name, 0, javax.swing.text.Position.Bias.Forward));
				}
			}
		});

		button2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_newLabelList.getSelectedValue() != null) {

					m_eventService.publish(new LabelingEditorDeleteAllEvent(m_newLabelList.getSelectedValuesList()));
					deleteAll(m_newLabelList.getSelectedValue());
					m_newLabelList.setListData(m_newLabels);
					m_eventService.publish(new ImgRedrawEvent());
				}
			}
		});

		highlight.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					m_highlight = true;

					m_eventService.publish(new LabelingEditorHighlightEvent(m_newLabels, true));

				} else {
					m_highlight = false;
					m_eventService.publish(new LabelingEditorHighlightEvent(null, false));
				}
				m_eventService.publish(new ImgRedrawEvent());
			}
		});

		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.VERTICAL;

		root.add(buttonPanel, gbc);
		return root;
	}

	private JComponent createCurrentLabelPanel() {

		JPanel root = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridy = 0;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;

		m_labelList = new JList<String>(m_labels);

		m_labelList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {

				if (e.getValueIsAdjusting())
					return;

				int i = m_labelList.getSelectedIndex();

				m_newLabelList.clearSelection();

				m_labelList.setSelectedIndex(i);

				m_eventService.publish(
						new AnnotatorLabelsSelChgEvent(m_labelList.getSelectedValuesList().toArray(new String[0])));
			}
		});

		root.add(new JScrollPane(m_labelList), gbc);

		Box ButtonPanel = new Box(BoxLayout.Y_AXIS);
		JButton button1 = new JButton("Rename");
		ButtonPanel.add(button1);
		ButtonPanel.add(Box.createVerticalStrut(5));
		JButton button2 = new JButton("Delete");
		ButtonPanel.add(button2);
		ButtonPanel.add(Box.createVerticalStrut(10));
		ButtonPanel.add(Box.createGlue());

		button1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_labelList.getSelectedValue() != null) {
					final String oldName = m_labelList.getSelectedValue();
					final String newName = JOptionPane.showInputDialog(m_parent, "New class name:");
					if ((newName != null) && (newName.length() > 0)) {

						m_eventService.publish(new LabelingEditorRenameAddedEvent(oldName, newName));
						renameLabel(oldName, newName);

						if (m_highlight) {
							m_eventService.publish(new LabelingEditorHighlightEvent(m_newLabels, true));
						}
						m_eventService.publish(new ImgRedrawEvent());
					}
				}

			}
		});

		button2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_labelList.getSelectedValue() != null) {

					// m_newLabelList.updateUI();
					m_eventService.publish(new LabelingEditorDeleteEvent(m_labelList.getSelectedValuesList()));
					m_labels.remove(m_labelList.getSelectedValue());
					m_labelList.setListData(m_labels);

				}
			}
		});

		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.VERTICAL;

		root.add(ButtonPanel, gbc);
		return root;
	}

	private void deleteAll(String label) {
		m_labels.remove(label);
		m_newLabels.remove(label);
		m_labelList.setListData(m_labels);
		m_newLabelList.setListData(m_newLabels);
	}

	private void renameLabel(String oldLabel, String newLabel) {
		if (m_labels.remove(oldLabel)) {
			m_labels.add(newLabel);
			Collections.sort(m_labels);
		}
		if (m_newLabels.remove(oldLabel)) {
			m_newLabels.add(newLabel);
			Collections.sort(m_newLabels);
		}
		m_labelList.setListData(m_labels);
		m_labelList.setSelectedIndex(m_labelList.getNextMatch(newLabel, 0, javax.swing.text.Position.Bias.Forward));
	}

	@EventListener
	public void onLabelAdd(final LabelingEditorAddEvent e) {

		boolean added = false;

		for (String label : e.getNewLabels())
			if (!m_labels.contains(label)) {
				m_labels.add(label);
				added = true;
			}

		if (added) {
			Collections.sort(m_labels);
			m_labelList.setListData(m_labels);
		}

	}

	public void clearLabels() {
		m_labels.clear();
		m_labelList.setListData(m_labels);
	}
}
