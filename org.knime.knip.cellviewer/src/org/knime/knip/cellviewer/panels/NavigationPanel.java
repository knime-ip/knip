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
 * Created on Jul 16, 2015 by Andreas Burger
 */
package org.knime.knip.cellviewer.panels;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * This Class represents a Panel containing two sets of directional buttons with labels.
 * 
 * @author <a href="mailto:Andreas.Burger@uni-konstanz.de">Andreas Burger</a>
 */
@SuppressWarnings("serial")
public class NavigationPanel extends JPanel {

	private Box m_northPanel;

	private Box m_eastPanel;

	private Box m_southPanel;

	private Box m_westPanel;

	private JButton m_northButton;

	private JButton m_eastButton;

	private JButton m_southButton;

	private JButton m_westButton;

	private JLabel m_colLabel;

	private JLabel m_rowLabel;

	private String m_rowName = "";

	private String m_colName = "";

	public NavigationPanel() {

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(Box.createHorizontalGlue());
		createPanels();

		Box columnBox = new Box(BoxLayout.X_AXIS);

		m_colLabel = new JLabel("");
		columnBox.add(Box.createVerticalStrut(32));
		columnBox.add(m_colLabel);
		columnBox.add(Box.createHorizontalStrut(5));
		columnBox.add(m_westPanel);
		columnBox.add(Box.createHorizontalStrut(5));
		columnBox.add(m_eastPanel);

		add(columnBox);
		add(Box.createRigidArea(new Dimension(20, 0)));
		Box rowBox = new Box(BoxLayout.X_AXIS);

		m_rowLabel = new JLabel("");
		rowBox.add(Box.createVerticalStrut(32));
		rowBox.add(m_rowLabel);
		rowBox.add(Box.createHorizontalStrut(5));
		rowBox.add(m_northPanel);
		rowBox.add(Box.createHorizontalStrut(5));
		rowBox.add(m_southPanel);

		m_rowLabel.setToolTipText(m_rowName);
		m_colLabel.setToolTipText(m_colName);

		add(rowBox);

		add(Box.createHorizontalGlue());


		InputMap m = getInputMap(WHEN_IN_FOCUSED_WINDOW);

		getActionMap().put("scrollRight", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				m_eastButton.doClick();
			}
		});

		getActionMap().put("scrollDown", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				m_southButton.doClick();
			}
		});

		getActionMap().put("scrollLeft", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				m_westButton.doClick();
			}
		});

		getActionMap().put("scrollUp", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				m_northButton.doClick();
			}
		});

		m.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "scrollRight");
		m.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "scrollDown");
		m.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "scrollLeft");
		m.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "scrollUp");

		validate();
	}

	private void createPanels() {

		m_northPanel = Box.createHorizontalBox();

		m_northButton = new BasicArrowButton(SwingConstants.NORTH) {

			@Override
			public Dimension getMaximumSize() {
				return new Dimension(32, 32);
			}

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(32, 32);
			}
		};
		m_northButton.setAlignmentY(0.5f);
		m_northPanel.add(m_northButton);

		m_southPanel = Box.createHorizontalBox();

		m_southButton = new BasicArrowButton(SwingConstants.SOUTH) {

			@Override
			public Dimension getMaximumSize() {
				return new Dimension(32, 32);
			}

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(32, 32);
			}
		};

		m_southButton.setAlignmentY(0.5f);
		m_southPanel.add(m_southButton);

		m_eastPanel = Box.createVerticalBox();
		m_eastButton = new BasicArrowButton(SwingConstants.EAST) {

			@Override
			public Dimension getMaximumSize() {
				return new Dimension(32, 32);
			}

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(32, 32);
			}
		};

		m_eastButton.setAlignmentX(0.5f);
		m_eastPanel.add(m_eastButton);

		m_westPanel = Box.createVerticalBox();
		m_westButton = new BasicArrowButton(SwingConstants.WEST) {

			@Override
			public Dimension getMaximumSize() {
				return new Dimension(32, 32);
			}

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(32, 32);
			}
		};

		m_westButton.setAlignmentX(0.5f);
		m_westPanel.add(m_westButton);

	}

	public void updatePosition(int colCount, int rowCount, int col, int row, String colName, String rowName) {

		// Ensure that only buttons that provide a valid navigation option are
		// enabled
		if (colCount != -1 || col != -1) {
			if (rowCount != -1 || row != -1) {
				setButtonStatus(true, true);
			} else {
				setButtonStatus(true, false);
			}
		} else {
			if (rowCount != -1 || row != -1) {
				setButtonStatus(false, true);
			} else {
				setButtonStatus(false, false);
			}
		}

		// Update text

		if (col != -1 || colCount != -1) {
			m_colLabel.setText("Column [" + colName.substring(0, Math.min(12, colName.length())) + "] (" + col + "/"
					+ colCount + ")");
		}
		if (row != -1 || rowCount != -1) {
			m_rowLabel.setText("Row [" + rowName.substring(0, Math.min(12, rowName.length())) + "] (" + row + "/"
					+ rowCount + ")");
		}
		m_rowLabel.setToolTipText(rowName);
		m_colLabel.setToolTipText(colName);
	}

	private void setButtonStatus(final boolean columnStatus, final boolean rowStatus) {
		m_northButton.setEnabled(rowStatus);
		m_eastButton.setEnabled(columnStatus);
		m_westButton.setEnabled(columnStatus);
		m_southButton.setEnabled(rowStatus);

	}

	/**
	 * Disables all buttons.
	 */
	public void disableButtons() {
		setButtonStatus(false, false);
	}

	/**
	 * Returns the button with the ''up''-arrow
	 * @return The button for scrolling ''up''
	 */
	public JButton getUpButton() {
		return m_northButton;
	}

	/**
	 * Returns the button with the ''down''-arrow
	 * @return The button for scrolling ''down''
	 */
	public JButton getDownButton() {
		return m_southButton;
	}

	/**
	 * Returns the button with the ''left''-arrow
	 * @return The button for scrolling ''left''
	 */
	public JButton getLeftButton() {
		return m_westButton;
	}

	/**
	 * Returns the button with the ''right''-arrow
	 * @return The button for scrolling ''right''
	 */
	public JButton getRightButton() {
		return m_eastButton;
	}

}
