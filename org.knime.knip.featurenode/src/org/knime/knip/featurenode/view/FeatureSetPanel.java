package org.knime.knip.featurenode.view;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FeatureSetPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -1772745996588303895L;

	/**
	 * Panel where all parameter panels are added.
	 */
	private final JPanel m_panel;

	/**
	 * {@link GridBagConstraints} to add parameter panels to
	 * {@link FeatureSetPanel#m_panel}
	 */
	private GridBagConstraints m_gbc;

	/**
	 * List of all added features.
	 */
	private List<FeatureSetInfoJPanel> m_featureList;

	private final JPanel m_featureNodeNodeDialogPanel;

	public FeatureSetPanel(final JPanel featureNodeNodeDialogPanel) {

		this.m_featureNodeNodeDialogPanel = featureNodeNodeDialogPanel;

		this.m_featureList = new ArrayList<FeatureSetInfoJPanel>();

		setLayout(new GridBagLayout());
		this.m_panel = new JPanel(new GridBagLayout());
		this.m_gbc = new GridBagConstraints();
		initGBC();

		add(this.m_panel, this.m_gbc);
	}

	/**
	 * Resets {@link FeatureSetPanel#m_gbc}.
	 */
	private void initGBC() {
		this.m_gbc.anchor = GridBagConstraints.NORTH;
		this.m_gbc.fill = GridBagConstraints.HORIZONTAL;
		this.m_gbc.gridheight = 1;
		this.m_gbc.gridwidth = 1;
		this.m_gbc.weighty = 1;
		this.m_gbc.weightx = 1;
		this.m_gbc.gridx = 0;
		this.m_gbc.gridy = 0;
	}

	/**
	 * Adds a the feature parameter dialogs to the panel and saves the feature
	 * in {@link FeatureSetPanel#getFeatureList()}
	 *
	 * @param feature
	 *            to be added
	 */
	public void addFeature(final FeatureSetInfoJPanel feature) {
		final JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder(feature.getPluginInfo()
				.getLabel()));

		// remove this feature
		final JButton remove = new JButton(new ImageIcon(getClass()
				.getResource("remove_icon.png")));

		remove.setMargin(new Insets(0, 0, 0, 0));

		remove.setBorderPainted(false);

		remove.setOpaque(true);
		remove.setContentAreaFilled(false);

		remove.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				FeatureSetPanel.this.m_featureList.remove(feature);
				FeatureSetPanel.this.m_panel.remove(p);
				FeatureSetPanel.this.m_panel.revalidate();
			}
		});

		// opens new dialog with feature description
		final JButton info = new JButton(new ImageIcon(getClass().getResource(
				"info.png")));
		info.setMargin(new Insets(0, 0, 0, 0));
		info.setBorderPainted(false);

		info.setOpaque(true);
		info.setContentAreaFilled(false);
		info.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				FeatureSetInfoDialog.openUserDialog(getFrame(), feature
						.getPluginInfo().getLabel(), feature.getPluginInfo()
						.getDescription());
			}
		});

		// group buttons in a panel to add both to the top right corner
		final JPanel buttons = new JPanel(new GridLayout(1, 2));
		final GridLayout g = new GridLayout(1, 2);
		g.setHgap(4);
		buttons.setLayout(g);
		buttons.add(info);
		buttons.add(remove);

		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 4, 4, 4);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weighty = 0;
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 0;

		p.add(buttons, c);

		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.weighty = 1;
		c.weightx = 1;
		c.gridwidth = 1;
		c.gridy++;

		p.add(feature.getInputpanel().getComponent(), c);

		c.gridy++;
		if (feature.getUnresolvedParameterNames().isEmpty()) {
			p.add(emptyParameters(), c);
		}

		this.m_featureList.add(feature);

		this.m_panel.add(p, this.m_gbc);
		this.m_gbc.gridy++;
	}

	/**
	 * Empty panel with the following message:
	 * "No parameters available for this feature."
	 *
	 * @return panel
	 */
	private JPanel emptyParameters() {
		final JPanel p = new JPanel();
		final JLabel l = new JLabel("No parameters available for this feature.");
		p.add(l);
		return p;
	}

	/**
	 * @return list of all added features
	 */
	public List<FeatureSetInfoJPanel> getFeatureList() {
		return this.m_featureList;
	}

	/**
	 * Removes all added features from the panel and the list.
	 */
	public void clear() {
		this.m_panel.removeAll();
		this.m_featureList = new ArrayList<FeatureSetInfoJPanel>();
		this.m_gbc = new GridBagConstraints();
		initGBC();
	}

	/**
	 * @return the parent frame
	 */
	protected Frame getFrame() {
		Frame f = null;
		Container c = this.m_featureNodeNodeDialogPanel.getParent();
		while (c != null) {
			if (c instanceof Frame) {
				f = (Frame) c;
			}
			c = c.getParent();
		}
		return f;
	}
}
