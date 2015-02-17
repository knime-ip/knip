package org.knime.knip.featurenode.view.featureset;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.imagej.ops.OpRef;
import net.imagej.ops.features.AbstractAutoResolvingFeatureSet;
import net.imagej.ops.features.FeatureSet;

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

@SuppressWarnings("rawtypes")
public class FeatureSetPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 5766985553194363328L;

	private PluginInfo<FeatureSet> pluginInfo;
	private final Module module;
	private final SwingInputPanel inputpanel;
	private FeatureSelectionPanel fsp;
	private JButton infoButton;
	private JButton removeButton;

	/**
	 * Input to create a {@link FeatureSetInfoJPanel} from the class and the
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
	public FeatureSetPanel(final FeatureSetInfo fsi)
			throws InstantiableException, ModuleException {
		// create plugininfo from the class of the feature set
		final PluginInfo<FeatureSet> plugin = OpsGateway.getPluginService()
				.getPlugin(fsi.getFeatureSetClass(), FeatureSet.class);

		this.pluginInfo = plugin;

		// create an instance of the feature set
		FeatureSet<?, ?> op;
		try {
			op = this.pluginInfo.createInstance();
		} catch (final InstantiableException e) {
			throw new InstantiableException("Couldn't instantiate feature set",
					e);
		}

		// create a module
		this.module = OpsGateway
				.getCommandService()
				.getModuleService()
				.createModule(
						new CommandInfo(op.getClass(), op.getClass()
								.getAnnotation(Plugin.class)));

		validateAndInitialize(this.module, fsi.getFieldNamesAndValues());

		// inject harvester and get input panel
		final SwingInputHarvester builder = new SwingInputHarvester();
		OpsGateway.getContext().inject(builder);
		this.inputpanel = builder.createInputPanel();

		// if this feature set consists of a set of features
		this.fsp = null;
		if (AbstractAutoResolvingFeatureSet.class.isAssignableFrom(op
				.getClass())) {
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
	 * Input to create a {@link FeatureSetInfoJPanel} from a {@link PluginInfo}
	 *
	 * @param pluginInfo
	 *            A {@link PluginInfo} of a {@link FeatureSet}
	 *
	 * @throws InstantiableException
	 *             if the featureset can't be instantiated
	 * @throws ModuleException
	 *             if no inputpanel can be built from the module
	 */
	public FeatureSetPanel(final PluginInfo<FeatureSet> pluginInfo)
			throws InstantiableException, ModuleException {
		this.pluginInfo = pluginInfo;

		FeatureSet<?, ?> op;
		try {
			op = pluginInfo.createInstance();
		} catch (final InstantiableException e) {
			throw new InstantiableException("Couldn't instantiate feature set",
					e);
		}

		this.module = OpsGateway
				.getCommandService()
				.getModuleService()
				.createModule(
						new CommandInfo(op.getClass(), op.getClass()
								.getAnnotation(Plugin.class)));

		// validate and initialize module
		validateAndInitialize(this.module, null);

		// inject harvester and get input panel
		final SwingInputHarvester builder = new SwingInputHarvester();
		OpsGateway.getContext().inject(builder);
		this.inputpanel = builder.createInputPanel();

		// if this feature set consists of a set of features
		this.fsp = null;
		if (AbstractAutoResolvingFeatureSet.class.isAssignableFrom(op
				.getClass())) {
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

	private void validateAndInitialize(final Module module,
			final Map<String, Object> fieldNamesAndValues) {
		// resolve default input and output
		module.setResolved("input", true);
		module.setResolved("output", true);

		// set parameters, if available
		if (fieldNamesAndValues != null) {
			for (final Entry<String, Object> entry : fieldNamesAndValues
					.entrySet()) {
				module.setInput(entry.getKey(), entry.getValue());
			}
		}

		// ensure the module is well-formed
		final ValidityPreprocessor validater = new ValidityPreprocessor();
		validater.process(module);
		if (validater.isCanceled()) {
			final String cancelReason = validater.getCancelReason();
			throw new IllegalArgumentException(
					"Couldn't validate given module. " + cancelReason);
		}

		// run the module initializers
		new InitPreprocessor().process(module);
	}

	private void build() {
		// set jpanel settings
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setBorder(BorderFactory.createTitledBorder(this.pluginInfo
				.getLabel() + ":"));

		// initialize jcomponents
		this.infoButton = new JButton(new ImageIcon(getClass().getClassLoader()
				.getResource("resources/info.png")));
		this.removeButton = new JButton(new ImageIcon(getClass()
				.getClassLoader().getResource("resources/remove_icon.png")));

		// set sizes

		this.infoButton.setMaximumSize(this.infoButton.getPreferredSize());
		this.removeButton.setMaximumSize(this.removeButton.getPreferredSize());

		// title box
		final Box buttonBox = Box.createHorizontalBox();
		buttonBox.add(Box.createHorizontalGlue());
		buttonBox.add(this.infoButton);
		buttonBox.add(Box.createRigidArea(new Dimension(5, 5)));
		buttonBox.add(this.removeButton);
		buttonBox.add(Box.createRigidArea(new Dimension(10, 5)));

		final Box inputPanelBox = Box.createHorizontalBox();
		inputPanelBox.add(Box.createHorizontalGlue());
		inputPanelBox.add(this.inputpanel.getComponent());
		inputPanelBox.add(Box.createHorizontalGlue());

		this.add(buttonBox);
		if (this.fsp != null) {
			this.add(Box.createRigidArea(new Dimension(5, 40)));
			this.add(this.fsp);
		}

		if (!getUnresolvedParameterNames().isEmpty()) {
			this.add(Box.createRigidArea(new Dimension(5, 40)));
			this.add(inputPanelBox);
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
		return this.infoButton;
	}

	public JButton getRemoveButton() {
		return this.removeButton;
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

		final Class<? extends FeatureSet> featuresetClass = this.pluginInfo
				.getPluginClass();

		final Map<String, Object> parameterValues = new HashMap<String, Object>();
		for (final String parameterName : getUnresolvedParameterNames()) {
			parameterValues.put(parameterName,
					this.inputpanel.getValue(parameterName));
		}

		Map<Class<?>, Boolean> selectedFeatures = new HashMap<Class<?>, Boolean>();
		if (this.fsp != null) {
			selectedFeatures = this.fsp.getSelectedOps();
		}

		return new FeatureSetInfo(featuresetClass, parameterValues,
				selectedFeatures);
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

		public FeatureSelectionPanel(final Set<OpRef<?>> ops) {
			this.selectedOps = new HashMap<Class<?>, Boolean>();

			this.setLayout(new GridLayout(0, 3));

			for (final OpRef<?> opRef : ops) {
				this.selectedOps.put(opRef.getType(), true);

				final JCheckBox checkBox = new JCheckBox(opRef.getType()
						.getSimpleName());
				checkBox.setSelected(true);
				checkBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {

						FeatureSelectionPanel.this.selectedOps.put(
								opRef.getType(), checkBox.isSelected());
					}
				});

				this.add(checkBox);
			}
		}

		public FeatureSelectionPanel(final Map<Class<?>, Boolean> input) {
			this.selectedOps = new HashMap<Class<?>, Boolean>();

			this.setLayout(new GridLayout(0, 3));

			final TreeSet<Class<?>> opSet = new TreeSet<Class<?>>(
					new Comparator<Class<?>>() {
						@Override
						public int compare(final Class<?> o1, final Class<?> o2) {
							return o1.getCanonicalName().compareTo(
									o2.getCanonicalName());
						}
					});

			opSet.addAll(input.keySet());

			for (final Class<?> op : opSet) {
				final boolean isSelected = input.get(op);
				this.selectedOps.put(op, isSelected);

				final JCheckBox checkBox = new JCheckBox(op.getSimpleName());
				checkBox.setSelected(isSelected);
				checkBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						FeatureSelectionPanel.this.selectedOps
								.put(op,
										!FeatureSelectionPanel.this.selectedOps
												.get(op));
					}
				});

				this.add(checkBox);
			}
		}

		public Map<Class<?>, Boolean> getSelectedOps() {
			return this.selectedOps;
		}
	}
}
