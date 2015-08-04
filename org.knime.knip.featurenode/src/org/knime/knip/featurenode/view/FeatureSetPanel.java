package org.knime.knip.featurenode.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.knip.featurenode.OpsGateway;
import org.knime.knip.featurenode.model.FeatureSetInfo;
import org.scijava.InstantiableException;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.process.InitPreprocessor;
import org.scijava.module.process.ValidityPreprocessor;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.ui.swing.widget.SwingInputPanel;

import net.imagej.ops.OpRef;
import net.imagej.ops.features.AbstractAutoResolvingFeatureSet;
import net.imagej.ops.features.FeatureSet;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("rawtypes")
public class FeatureSetPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 5766985553194363328L;

	private final PluginInfo<FeatureSet> pluginInfo;
	private final Module module;
	private final SwingInputPanel inputpanel;
	private FeatureSelectionPanel fsp;

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
	private final JLabel numSelectedFeaturesLabel = new JLabel();

	/*****************************************************************
	 ******* INIT BUTTONS & CHECKBOXES
	 ******************************************************************/
	private final JButton btnMinimize = new JButton(this.minimizeIcon);
	private boolean shouldMaximize = true;

	private final JCheckBox chkbSelectAll = new JCheckBox("Select All", true);
	private boolean selectAll = true;

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
		// create plugininfo from the class of the feature set
		final PluginInfo<FeatureSet> plugin = OpsGateway.getPluginService().getPlugin(fsi.getFeatureSetClass(),
				FeatureSet.class);

		this.pluginInfo = plugin;

		// create an instance of the feature set
		FeatureSet<?, ?> op;
		try {
			op = this.pluginInfo.createInstance();
		} catch (final InstantiableException e) {
			throw new InstantiableException("Couldn't instantiate feature set", e);
		}

		// create a module
		this.module = OpsGateway.getCommandService().getModuleService()
				.createModule(new CommandInfo(op.getClass(), op.getClass().getAnnotation(Plugin.class)));

		validateAndInitialize(this.module, fsi.getFieldNameAndValues());

		// inject harvester and get input panel
		final SwingInputHarvester builder = new SwingInputHarvester();
		OpsGateway.getContext().inject(builder);
		this.inputpanel = builder.createInputPanel();

		// if this feature set consists of a set of features
		this.fsp = null;
		if (AbstractAutoResolvingFeatureSet.class.isAssignableFrom(op.getClass())) {
			this.fsp = new FeatureSelectionPanel(fsi.getSelectedFeatures());
		}

		try {
			builder.buildPanel(this.inputpanel, this.module);
			this.inputpanel.refresh();
		} catch (final ModuleException e) {
			e.printStackTrace();
			throw new ModuleException("Couldn't create SwingInputPanel", e);
		}

		build();
	}

	/**
	 * Input to create a {@link FeatureSetPanel} from a {@link PluginInfo}
	 *
	 * @param pluginInfo
	 *            A {@link PluginInfo} of a {@link FeatureSet}
	 *
	 * @throws InstantiableException
	 *             if the featureset can't be instantiated
	 * @throws ModuleException
	 *             if no inputpanel can be built from the module
	 */
	public FeatureSetPanel(final PluginInfo<FeatureSet> pluginInfo) throws InstantiableException, ModuleException {
		this.pluginInfo = pluginInfo;

		FeatureSet<?, ?> op;
		try {
			op = pluginInfo.createInstance();
		} catch (final InstantiableException e) {
			throw new InstantiableException("Couldn't instantiate feature set", e);
		}

		this.module = OpsGateway.getCommandService().getModuleService()
				.createModule(new CommandInfo(op.getClass(), op.getClass().getAnnotation(Plugin.class)));

		// validate and initialize module
		validateAndInitialize(this.module, null);

		// inject harvester and get input panel
		final SwingInputHarvester builder = new SwingInputHarvester();
		OpsGateway.getContext().inject(builder);
		this.inputpanel = builder.createInputPanel();

		// if this feature set consists of a set of features
		this.fsp = null;
		if (AbstractAutoResolvingFeatureSet.class.isAssignableFrom(op.getClass())) {
			final AbstractAutoResolvingFeatureSet<?, ?> autoOp = (AbstractAutoResolvingFeatureSet<?, ?>) op;

			final Set<OpRef<?>> outputOps = autoOp.getOutputOps();
			if ((outputOps != null) && !outputOps.isEmpty()) {
				this.fsp = new FeatureSelectionPanel(outputOps);
			}
		}

		try {
			builder.buildPanel(this.inputpanel, this.module);
			this.inputpanel.refresh();
		} catch (final ModuleException e) {
			e.printStackTrace();
			throw new ModuleException("Couldn't create SwingInputPanel", e);
		}

		build();
	}

	private void validateAndInitialize(final Module someModule, final Map<String, Object> fieldNamesAndValues) {
		// resolve default input and output
		someModule.setResolved("input", true);
		someModule.setResolved("output", true);

		// set parameters, if available
		if (fieldNamesAndValues != null) {
			for (final Entry<String, Object> entry : fieldNamesAndValues.entrySet()) {
				someModule.setInput(entry.getKey(), entry.getValue());
			}
		}

		// ensure the module is well-formed
		final ValidityPreprocessor validater = new ValidityPreprocessor();
		validater.process(someModule);
		if (validater.isCanceled()) {
			final String cancelReason = validater.getCancelReason();
			throw new IllegalArgumentException("Couldn't validate given module. " + cancelReason);
		}

		// run the module initializers
		new InitPreprocessor().process(someModule);
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

		final JLabel lblFeatureSetName = new JLabel(this.pluginInfo.getLabel());
		menuPanel.add(lblFeatureSetName, "cell 0 0");
		menuPanel.add(this.chkbSelectAll, "cell 2 0");
		menuPanel.add(this.btnMinimize, "cell 3 0");
		menuPanel.add(this.btnHelp, "cell 4 0");
		menuPanel.add(this.btnClose, "cell 5 0");

		this.add(menuPanel, BorderLayout.NORTH);

		if (this.fsp != null) {
			this.add(this.fsp, BorderLayout.SOUTH);
			menuPanel.add(this.numSelectedFeaturesLabel, "cell 1 0");

			int num = 0;
			int numSelected = 0;
			for (final Entry<Class<?>, Boolean> selectedOps : this.fsp.getSelectedOps().entrySet()) {
				++num;
				if (selectedOps.getValue()) {
					++numSelected;
				}
			}

			this.numSelectedFeaturesLabel.setText(numSelected + "/" + num + " Features");
		} else {
			this.chkbSelectAll.setVisible(false);
		}

		if (!getUnresolvedParameterNames().isEmpty()) {
			this.add(this.inputpanel.getComponent(), BorderLayout.CENTER);
		}
	}

	public PluginInfo<FeatureSet> getPluginInfo() {
		return this.pluginInfo;
	}

	public Module getModule() {
		return this.module;
	}

	public SwingInputPanel getInputpanel() {
		return this.inputpanel;
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

		if (this.fsp != null) {
			this.fsp.setVisible(this.shouldMaximize);
		}

		if (!getUnresolvedParameterNames().isEmpty()) {
			this.inputpanel.getComponent().setVisible(this.shouldMaximize);
		}
	}

	/**
	 * Toggle whether all features should be selected or not
	 */
	public void toggleSelectDeselectAll() {
		this.selectAll = !this.selectAll;

		for (final Class<?> key : this.fsp.getSelectedOps().keySet()) {
			this.fsp.getSelectedOps().put(key, this.selectAll);
		}

		this.fsp.updateCheckboxes();

		int num = 0;
		int numSelected = 0;
		for (final Entry<Class<?>, Boolean> entry : this.fsp.getSelectedOps().entrySet()) {
			++num;
			if (entry.getValue()) {
				++numSelected;
			}
		}

		this.numSelectedFeaturesLabel.setText(numSelected + "/" + num + " Features");
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

		final Class<? extends FeatureSet> featuresetClass = this.pluginInfo.getPluginClass();

		final Map<String, Object> parameterValues = new HashMap<String, Object>();
		for (final String parameterName : getUnresolvedParameterNames()) {
			parameterValues.put(parameterName, this.inputpanel.getValue(parameterName));
		}

		Map<Class<?>, Boolean> selectedFeatures = new HashMap<Class<?>, Boolean>();
		if (this.fsp != null) {
			selectedFeatures = this.fsp.getSelectedOps();
		}

		return new FeatureSetInfo(featuresetClass, parameterValues, selectedFeatures);
	}

	/**
	 * JPanel to display a Checkbox for each Feature from a {@link FeatureSet}.
	 *
	 * @author Daniel Seebacher, University of Konstanz.
	 */
	private class FeatureSelectionPanel extends JPanel {

		/**
		 * serialVersionUID.
		 */
		private static final long serialVersionUID = 706108386333175144L;
		private final Map<Class<?>, Boolean> selectedOps;
		private final Map<Class<?>, JCheckBox> addedCheckboxes;

		public FeatureSelectionPanel(final Set<OpRef<?>> ops) {
			this.selectedOps = new HashMap<Class<?>, Boolean>();
			this.addedCheckboxes = new HashMap<Class<?>, JCheckBox>();
			this.setLayout(new GridLayout(0, 3));

			final List<OpRef<?>> sortedOps = new ArrayList<OpRef<?>>(ops);
			Collections.sort(sortedOps, new Comparator<OpRef<?>>() {
				@Override
				public int compare(final OpRef<?> o1, final OpRef<?> o2) {
					return o1.getType().getSimpleName().compareTo(o2.getType().getSimpleName());
				}
			});

			for (final OpRef<?> opRef : sortedOps) {
				this.selectedOps.put(opRef.getType(), true);

				final JCheckBox checkBox = new JCheckBox(getFeatureLabel(opRef.getType()));
				checkBox.setSelected(true);
				checkBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {

						FeatureSelectionPanel.this.selectedOps.put(opRef.getType(), checkBox.isSelected());

						int num = 0;
						int numSelected = 0;
						for (final Entry<Class<?>, Boolean> entry : getSelectedOps().entrySet()) {
							++num;
							if (entry.getValue()) {
								++numSelected;
							}
						}

						FeatureSetPanel.this.numSelectedFeaturesLabel.setText(numSelected + "/" + num + " Features");
					}
				});

				this.add(checkBox);
				this.addedCheckboxes.put(opRef.getType(), checkBox);
			}
		}

		public FeatureSelectionPanel(final Map<Class<?>, Boolean> input) {
			this.selectedOps = new HashMap<Class<?>, Boolean>();
			this.addedCheckboxes = new HashMap<Class<?>, JCheckBox>();

			this.setLayout(new GridLayout(0, 3));

			final List<Class<?>> sortedOps = new ArrayList<Class<?>>(input.keySet());
			Collections.sort(sortedOps, new Comparator<Class<?>>() {
				@Override
				public int compare(final Class<?> o1, final Class<?> o2) {
					return o1.getSimpleName().compareTo(o2.getSimpleName());
				}
			});

			for (final Class<?> op : sortedOps) {
				final boolean isSelected = input.get(op);
				this.selectedOps.put(op, isSelected);

				final JCheckBox checkBox = new JCheckBox(getFeatureLabel(op));
				checkBox.setSelected(isSelected);
				checkBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						FeatureSelectionPanel.this.selectedOps.put(op, !FeatureSelectionPanel.this.selectedOps.get(op));

						int num = 0;
						int numSelected = 0;
						for (final Entry<Class<?>, Boolean> entry : getSelectedOps().entrySet()) {
							++num;
							if (entry.getValue()) {
								++numSelected;
							}
						}

						FeatureSetPanel.this.numSelectedFeaturesLabel.setText(numSelected + "/" + num + " Features");
					}
				});

				this.add(checkBox);
				this.addedCheckboxes.put(op, checkBox);
			}
		}

		public void updateCheckboxes() {
			for (final Entry<Class<?>, Boolean> entry : this.selectedOps.entrySet()) {
				this.addedCheckboxes.get(entry.getKey()).setSelected(entry.getValue());
			}
		}

		/**
		 * Pretty dirty hack to get the LABEL name of a feature, quietly fail
		 * and use the class name if we can't read it.
		 *
		 * @param feature
		 *            the class of a feature
		 * @return the feature label or the simple class name if the feature has
		 *         no label or we can't read it
		 */
		private String getFeatureLabel(final Class<?> feature) {

			try {
				final Class<?> class1 = feature.getInterfaces()[1];
				final Field field = class1.getDeclaredField("LABEL");

				final String[] token = ((String) field.get(class1)).split("\\.");

				return token[token.length - 1];

			} catch (final Exception ex) {
				// ignore
			}

			return feature.getSimpleName();
		}

		public Map<Class<?>, Boolean> getSelectedOps() {
			return this.selectedOps;
		}
	}
}
