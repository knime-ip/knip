package org.knime.knip.featurenode;

import java.awt.Color;
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
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.eclipse.draw2d.FlowLayout;
import org.knime.knip.featurenode.ui.FeatureSetInputPanel;

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
	private List<FeatureSetInputPanel> m_featureList;

	private JPanel m_featureNodeNodeDialogPanel;


	public FeatureSetPanel(JPanel featureNodeNodeDialogPanel) {
		
		m_featureNodeNodeDialogPanel = featureNodeNodeDialogPanel;
		
		m_featureList = new ArrayList<FeatureSetInputPanel>();

		setLayout(new GridBagLayout());
		m_panel = new JPanel(new GridBagLayout());
		m_gbc = new GridBagConstraints();
		initGBC();

		add(m_panel, m_gbc);
	}

	/**
	 * Resets {@link FeatureSetPanel#m_gbc}.
	 */
	private void initGBC() {
		m_gbc.anchor = GridBagConstraints.NORTH;
		m_gbc.fill = GridBagConstraints.HORIZONTAL;
		m_gbc.gridheight = 1;
		m_gbc.gridwidth = 1;
		m_gbc.weighty = 1;
		m_gbc.weightx = 1;
		m_gbc.gridx = 0;
		m_gbc.gridy = 0;
	}

	/**
	 * Adds a the feature parameter dialogs to the panel and saves the feature
	 * in {@link FeatureSetPanel#getFeatureList()}
	 * 
	 * @param feature
	 *            to be added
	 */
	public void addFeature(final FeatureSetInputPanel feature) {
		final JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder(feature.getPluginInfo()
				.getLabel()));
		
		// remove this feature
		JButton remove = new JButton(new ImageIcon(getClass().getResource(
				"remove_icon.png")));
		remove.setMargin(new Insets(0, 0, 0, 0));
		remove.setBorderPainted(false);
		remove.setOpaque(true);
		remove.setContentAreaFilled(false);
		remove.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_featureList.remove(feature);
				m_panel.remove(p);
				m_panel.revalidate();
			}
		});

		// opens new dialog with feature description
		JButton info = new JButton(new ImageIcon(getClass().getResource(
				"info.png")));
		info.setMargin(new Insets(0, 0, 0, 0));
		info.setBorderPainted(false);;
		info.setOpaque(true);
		info.setContentAreaFilled(false);
		info.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FeatureSetInfoDialog.openUserDialog(getFrame(), feature);
			}
		});
		
		// group buttons in a panel to add both to the top right corner
		JPanel buttons = new JPanel(new GridLayout(1, 2));
		GridLayout g = new GridLayout(1, 2);
		g.setHgap(4);
		buttons.setLayout(g);
		buttons.add(info);
		buttons.add(remove);

		GridBagConstraints c = new GridBagConstraints();
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
		if (!feature.getUnresolvedParameterNames().isEmpty()) {
			p.add(feature.getInputpanel().getComponent(), c);
		} else {
			p.add(emptyParameters(), c);
		}
		
		m_featureList.add(feature);

		m_panel.add(p, m_gbc);
		m_gbc.gridy++;
	}

	/**
	 * Empty panel with the following message:
	 * "No parameters available for this feature."
	 * 
	 * @return panel
	 */
	private JPanel emptyParameters() {
		JPanel p = new JPanel();
		JLabel l = new JLabel("No parameters available for this feature.");
		p.add(l);
		return p;
	}

	/**
	 * @return list of all added features
	 */
	public List<FeatureSetInputPanel> getFeatureList() {
		return m_featureList;
	}

	/**
	 * Removes all added features from the panel and the list.
	 */
	public void clear() {
		m_panel.removeAll();
		m_featureList = new ArrayList<FeatureSetInputPanel>();
		m_gbc = new GridBagConstraints();
		initGBC();
	}

	/**
	 * @return test panel with some input boxes. (No model behinde)
	 */
	static public JPanel createDummy() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weighty = 0;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;

		JLabel l1 = new JLabel("Parameter 1:");
		JLabel l2 = new JLabel("Parameter 2:");
		JLabel l3 = new JLabel("Parameter 3:");

		JTextField v1 = new JTextField("dummy text");
		JComboBox<Integer> v2 = new JComboBox<Integer>(new Integer[] { 1, 2, 3,
				4, 5, 6 });
		JTextField v3 = new JTextField("more dummy");

		p.add(l1, c);
		c.gridx++;

		p.add(v1, c);

		c.gridx = 0;
		c.gridy++;
		p.add(l2, c);

		c.gridx++;
		p.add(v2, c);

		c.gridx = 0;
		c.gridy++;
		p.add(l3, c);

		c.gridx++;
		p.add(v3, c);

		return p;
	}

    /**
     * @return the parent frame
     */
    protected Frame getFrame() {
        Frame f = null;
        Container c = m_featureNodeNodeDialogPanel.getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
            }
            c = c.getParent();
        }
        return f;
    }
}
