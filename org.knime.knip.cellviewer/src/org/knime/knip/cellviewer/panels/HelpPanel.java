package org.knime.knip.cellviewer.panels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class HelpPanel extends JPanel {

	public HelpPanel() {
		super(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets(10, 15, 0, 15);

		JLabel dText = new JLabel(
				"<html>" + "In order to facilitate navigation through the table, the cell viewer provides hotkeys that control"
						+ " the selection." + "</html>");
		dText.setVerticalAlignment(SwingConstants.TOP);
		add(dText, gbc);

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.LINE_START;
		++gbc.gridy;
		gbc.weighty = 0;
		gbc.weightx = 0;
		gbc.insets = new Insets(10, 15, 5, 0);

		JLabel headerLabel = new JLabel("<html><b>Hotkeys</b></html>");
		headerLabel.setHorizontalAlignment(SwingConstants.LEFT);
		add(headerLabel, gbc);

		++gbc.gridy;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(0, 25, 0, 0);
		JLabel first = new JLabel("<html><b>[w]</b>:</html>");
		first.setHorizontalAlignment(SwingConstants.RIGHT);
		add(first, gbc);

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.weightx = 1;
		++gbc.gridx;
		JLabel firstDesc = new JLabel("Moves the current table selection up.");
		firstDesc.setHorizontalAlignment(SwingConstants.LEFT);
		add(firstDesc, gbc);

		++gbc.gridy;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.gridx = 0;
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(0, 25, 0, 0);
		JLabel second = new JLabel("<html><b>[a]</b>:</html>");
		second.setHorizontalAlignment(SwingConstants.RIGHT);
		add(second, gbc);

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.weightx = 1;
		++gbc.gridx;
		JLabel secondDesc = new JLabel("Moves the current table selection to the left.");
		firstDesc.setHorizontalAlignment(SwingConstants.LEFT);
		add(secondDesc, gbc);

		++gbc.gridy;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.gridx = 0;
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(0, 25, 0, 0);
		JLabel third = new JLabel("<html><b>[s]</b>:</html>");
		second.setHorizontalAlignment(SwingConstants.RIGHT);
		add(third, gbc);

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.weightx = 1;
		++gbc.gridx;
		JLabel thirdDesc = new JLabel("Moves the current table selection down.");
		firstDesc.setHorizontalAlignment(SwingConstants.LEFT);
		add(thirdDesc, gbc);

		++gbc.gridy;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.gridx = 0;
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(0, 25, 0, 0);
		JLabel fourth = new JLabel("<html><b>[d]</b>:</html>");
		second.setHorizontalAlignment(SwingConstants.RIGHT);
		add(fourth, gbc);

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.weightx = 1;
		++gbc.gridx;
		JLabel fourthDesc = new JLabel("Moves the current table selection to the right.");
		firstDesc.setHorizontalAlignment(SwingConstants.LEFT);
		add(fourthDesc, gbc);

		++gbc.gridy;
		gbc.gridx = 0;

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		add(new JPanel(), gbc);
		setPreferredSize(new Dimension(800, 600));
		setVisible(true);

	}

}
