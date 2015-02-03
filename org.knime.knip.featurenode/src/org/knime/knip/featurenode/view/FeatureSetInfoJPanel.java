package org.knime.knip.featurenode.view;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.imagej.ops.OpRef;
import net.imagej.ops.features.AutoResolvingFeatureSet;
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

/**
 * Class used for extracting and displaying the information from a
 * {@link FeatureSet}
 *
 * @author Daniel Seebacher, University of Konstanz.
 */
@SuppressWarnings("rawtypes")
public class FeatureSetInfoJPanel {

	private final PluginInfo<FeatureSet> pluginInfo;
	private final Module module;
	private final SwingInputPanel inputpanel;
	private FeatureSelectionPanel fsp;

	/**
	 * Input to create a {@link FeatureSetInfoJPanel} from the class and
	 * the parameters of the {@link FeatureSet}
	 *
	 * @param clzz
	 *            the class of the {@link FeatureSet}
	 * @param parameterValues
	 *            a map of the parameter names and values
	 * @throws InstantiableException
	 *             if the featureset can't be instantiated
	 * @throws ModuleException
	 *             if no inputpanel can be built from the module
	 */
	public FeatureSetInfoJPanel(final FeatureSetInfo fsi)
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
		if (AutoResolvingFeatureSet.class.isAssignableFrom(op.getClass())) {
			this.fsp = new FeatureSelectionPanel(fsi.getSelectedFeatures());
		}

		if (this.fsp != null) {
			this.inputpanel.getComponent().add(this.fsp);
			this.inputpanel.getComponent().add(new JLabel());
		}

		try {
			builder.buildPanel(this.inputpanel, this.module);
			this.inputpanel.refresh();
		} catch (final ModuleException e) {
			e.printStackTrace();
			throw new ModuleException("Couldn't create SwingInputPanel", e);
		}
	}

	/**
	 * Input to create a {@link FeatureSetInfoJPanel} from a
	 * {@link PluginInfo}
	 *
	 * @throws InstantiableException
	 *             if the featureset can't be instantiated
	 * @throws ModuleException
	 *             if no inputpanel can be built from the module
	 */
	public FeatureSetInfoJPanel(final PluginInfo<FeatureSet> pluginInfo)
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
		if (AutoResolvingFeatureSet.class.isAssignableFrom(op.getClass())) {
			final AutoResolvingFeatureSet<?, ?> autoOp = (AutoResolvingFeatureSet<?, ?>) op;

			final Set<OpRef<?>> outputOps = autoOp.getOutputOps();
			if ((outputOps != null) && !outputOps.isEmpty()) {
				this.fsp = new FeatureSelectionPanel(outputOps);
			}
		}

		if (this.fsp != null) {
			this.inputpanel.getComponent().add(this.fsp);
			this.inputpanel.getComponent().add(new JLabel());
		}

		try {
			builder.buildPanel(this.inputpanel, this.module);
			this.inputpanel.refresh();
		} catch (final ModuleException e) {
			e.printStackTrace();
			throw new ModuleException("Couldn't create SwingInputPanel", e);
		}
	}

	/**
	 * Validates and initializes the given module. Resolves the default input
	 * and outputs and sets the given parameters.
	 *
	 * @param module
	 *            a module
	 * @param parameterValues
	 *            a set of parameters which should be set
	 */
	private void validateAndInitialize(final Module module,
			final Map<String, Object> parameterValues) {
		// resolve default input and output
		module.setResolved("input", true);
		module.setResolved("output", true);

		// set parameters, if available
		if (parameterValues != null) {
			for (final Entry<String, Object> entry : parameterValues.entrySet()) {
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

	public PluginInfo<FeatureSet> getPluginInfo() {
		return this.pluginInfo;
	}

	public Module getModule() {
		return this.module;
	}

	public SwingInputPanel getInputpanel() {
		return this.inputpanel;
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

			for (final Entry<Class<?>, Boolean> opRef : input.entrySet()) {
				this.selectedOps.put(opRef.getKey(), opRef.getValue());

				final JCheckBox checkBox = new JCheckBox(opRef.getKey()
						.getSimpleName());
				checkBox.setSelected(opRef.getValue());
				checkBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						FeatureSelectionPanel.this.selectedOps.put(opRef
								.getKey(),
								!FeatureSelectionPanel.this.selectedOps
										.get(opRef.getKey()));
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