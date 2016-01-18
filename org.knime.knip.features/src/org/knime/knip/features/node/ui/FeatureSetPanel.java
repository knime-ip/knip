/*
 * ------------------------------------------------------------------------
 *
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
  ---------------------------------------------------------------------
 *
 */

package org.knime.knip.features.node.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.knime.knip.features.FeaturesGateway;
import org.knime.knip.features.node.model.FeatureSetInfo;
import org.knime.knip.features.sets.FeatureSet;
import org.scijava.InstantiableException;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.ui.swing.widget.SwingInputPanel;

//TODO ONE PANEL OF FEATURESET
@SuppressWarnings("rawtypes")
public class FeatureSetPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 5766985553194363328L;

	private final Module module;
	private final FeatureCalculatorSwingInputPanel inputPanel;

	/*****************************************************************
	 ******* LOAD ICONS
	 ******************************************************************/
	private final ImageIcon maximizeIcon = new ImageIcon(
			getClass().getClassLoader().getResource("resources/Down16.gif"));
	private final ImageIcon minimizeIcon = new ImageIcon(getClass().getClassLoader().getResource("resources/Up16.gif"));
	private final JButton btnHelp = new JButton(
			new ImageIcon(getClass().getClassLoader().getResource("resources/info_small.gif")));
	private final JButton btnClose = new JButton(
			new ImageIcon(getClass().getClassLoader().getResource("resources/trash_small.gif")));

	/*****************************************************************
	 ******* INIT BUTTONS & CHECKBOXES
	 ******************************************************************/
	private final JButton btnMinimize = new JButton(this.minimizeIcon);
	private boolean shouldMaximize = true;

	private final JCheckBox chkbSelectAll = new JCheckBox("Select All", true);

	/**
	 * Input to create a {@link FeatureSetPanel} from the class and the
	 * parameters of the {@link FeatureSet}
	 *
	 * @param fsi
	 *            A {@link FeatureSetInfo}
	 *
	 * @throws InstantiableException
	 *             if the featureset can't be instantiated
	 * @throws ModuleException
	 *             if no inputpanel can be built from the module
	 */
	public FeatureSetPanel(final FeatureSetInfo fsi) throws InstantiableException, ModuleException {

		// TODO: We actually need only the suitable FeatureSets for the current
		// FeatureGroup (depending on numDims and inputs)

		// create an instance of the feature set
		this.module = fsi.load();

		// inject harvester and get input panel
		final FeatureCalculatorSwingInputHarvester builder = new FeatureCalculatorSwingInputHarvester();
		FeaturesGateway.getInstance().getContext().inject(builder);
		this.inputPanel = builder.createInputPanel();

		try {

			builder.buildPanel(this.inputPanel, this.module);
			this.inputPanel.refresh();
		} catch (final ModuleException e) {
			e.printStackTrace();
			throw new ModuleException("Couldn't create SwingInputPanel", e);
		}

		build();
	}

	private void build() {
		// set jpanel settings
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createLineBorder(new Color(189, 189, 189), 5));

		// checkbox background
		this.chkbSelectAll.setBackground(new Color(189, 189, 189));

		// title box
		final JPanel menuPanel = new JPanel();
		menuPanel.setBackground(new Color(189, 189, 189));
		menuPanel.setLayout(new MigLayout("", "[]push[]push[][][][]", "[26px]"));

		// set visiblity
		this.chkbSelectAll.setVisible(false);
		this.btnMinimize.setVisible(false);

		final JLabel lblFeatureSetName = new JLabel(this.module.getInfo().getLabel());
		menuPanel.add(lblFeatureSetName, "cell 0 0");
		menuPanel.add(this.chkbSelectAll, "cell 2 0");
		menuPanel.add(this.btnMinimize, "cell 3 0");
		menuPanel.add(this.btnHelp, "cell 4 0");
		menuPanel.add(this.btnClose, "cell 5 0");

		this.add(menuPanel, BorderLayout.NORTH);
		
		if (!getUnresolvedParameterNames().isEmpty()) {
			this.chkbSelectAll.setVisible(false);
			this.btnMinimize.setVisible(true);
			this.add(this.inputPanel.getComponent());
		}

	}

	public Module getModule() {
		return this.module;
	}

	public JButton getInfoButton() {
		return this.btnHelp;
	}

	public JButton getRemoveButton() {
		return this.btnClose;
	}

	public JButton getMinimizeButton() {
		return this.btnMinimize;
	}

	public JCheckBox getSelectAllCheckbox() {
		return this.chkbSelectAll;
	}

	public void toggleMinimizeMaximize() {

		this.shouldMaximize = !this.shouldMaximize;

		if (this.shouldMaximize) {
			this.btnMinimize.setIcon(this.minimizeIcon);
		} else {
			this.btnMinimize.setIcon(this.maximizeIcon);

		}

		if (!getUnresolvedParameterNames().isEmpty()) {
			this.inputPanel.getComponent().setVisible(this.shouldMaximize);
		}
	}
	

	public void selectAll() {
		//this.inputPanel.getComponent().
	}

	/**
	 * @return The names of all unresolved parameters
	 */
	public Set<String> getUnresolvedParameterNames() {
		final Set<String> parameterNames = new HashSet<String>();

		// only return unresolved fields since the resolved fields are not
		// necessary
		for (final String parameterName : this.module.getInputs().keySet()) {
			if (!this.module.isResolved(parameterName)) {
				parameterNames.add(parameterName);
			}
		}

		return parameterNames;
	}

	/**
	 * @return returns the class of the input {@link FeatureSet} and a map of
	 *         the user set parameters
	 */
	public FeatureSetInfo getSerializableInfos() {

		@SuppressWarnings("unchecked")
		final Class<? extends FeatureSet> featuresetClass = (Class<? extends FeatureSet>) this.module
				.getDelegateObject().getClass();

		final Map<String, Object> parameterValues = new HashMap<String, Object>();
		for (final String parameterName : getUnresolvedParameterNames()) {
			parameterValues.put(parameterName, this.inputPanel.getValue(parameterName));
		}

		return new FeatureSetInfo(featuresetClass, parameterValues);
	}

}
