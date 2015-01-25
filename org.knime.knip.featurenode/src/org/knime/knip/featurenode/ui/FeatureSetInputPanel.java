package org.knime.knip.featurenode.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.imagej.ops.features.FeatureSet;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.knime.knip.featurenode.OpsGateway;
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
public class FeatureSetInputPanel {

	private PluginInfo<FeatureSet> pluginInfo;
	private Module module;
	private SwingInputPanel inputpanel;

	/**
	 * Input to create a {@link FeatureSetInputPanel} from the class and the
	 * parameters of the {@link FeatureSet}
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
	public FeatureSetInputPanel(Class<? extends FeatureSet<?>> clzz,
			Map<String, Object> parameterValues) throws InstantiableException,
			ModuleException {

		// create plugininfo from the class of the feature set
		PluginInfo<FeatureSet> plugin = OpsGateway.getPluginService()
				.getPlugin(clzz, FeatureSet.class);

		this.pluginInfo = plugin;

		// create an instance of the feature set
		FeatureSet<?> op;
		try {
			op = pluginInfo.createInstance();
		} catch (InstantiableException e) {
			throw new InstantiableException("Couldn't instantiate feature set",
					e);
		}

		// create a module
		module = OpsGateway
				.getCommandService()
				.getModuleService()
				.createModule(
						new CommandInfo(op.getClass(), op.getClass()
								.getAnnotation(Plugin.class)));

		// resolve default input and output
		module.setResolved("in", true);
		module.setResolved("out", true);

		for (Entry<String, Object> entry : parameterValues.entrySet()) {
			module.setInput(entry.getKey(), entry.getValue());
		}

		// ensure the module is well-formed
		final ValidityPreprocessor validater = new ValidityPreprocessor();
		validater.process(module);
		if (validater.isCanceled()) {
			String cancelReason = validater.getCancelReason();
			throw new IllegalArgumentException(
					"Couldn't validate given module. " + cancelReason);
		}

		// run the module initializers
		new InitPreprocessor().process(module);

		// inject harvester and get input panel
		SwingInputHarvester builder = new SwingInputHarvester();
		OpsGateway.getContext().inject(builder);
		inputpanel = builder.createInputPanel();
		try {
			builder.buildPanel(inputpanel, module);
			inputpanel.refresh();
		} catch (ModuleException e) {
			throw new ModuleException("Couldn't create SwingInputPanel", e);
		}
	}

	/**
	 * Input to create a {@link FeatureSetInputPanel} from a {@link PluginInfo}
	 * 
	 * @throws InstantiableException
	 *             if the featureset can't be instantiated
	 * @throws ModuleException
	 *             if no inputpanel can be built from the module
	 */
	public FeatureSetInputPanel(PluginInfo<FeatureSet> pluginInfo)
			throws InstantiableException, ModuleException {
		this.pluginInfo = pluginInfo;

		FeatureSet<?> op;
		try {
			op = pluginInfo.createInstance();
		} catch (InstantiableException e) {
			throw new InstantiableException("Couldn't instantiate feature set",
					e);
		}

		module = OpsGateway
				.getCommandService()
				.getModuleService()
				.createModule(
						new CommandInfo(op.getClass(), op.getClass()
								.getAnnotation(Plugin.class)));

		// resolve default input and output
		module.setResolved("in", true);
		module.setResolved("out", true);

		// ensure the module is well-formed
		final ValidityPreprocessor validater = new ValidityPreprocessor();
		validater.process(module);
		if (validater.isCanceled()) {
			String cancelReason = validater.getCancelReason();
			throw new IllegalArgumentException(
					"Couldn't validate given module. " + cancelReason);
		}

		// run the module initializers
		new InitPreprocessor().process(module);

		// inject harvester and get input panel
		SwingInputHarvester builder = new SwingInputHarvester();
		OpsGateway.getContext().inject(builder);
		inputpanel = builder.createInputPanel();
		try {
			builder.buildPanel(inputpanel, module);
			inputpanel.refresh();
		} catch (ModuleException e) {
			throw new ModuleException("Couldn't create SwingInputPanel", e);
		}
	}

	/**
	 * @return The names of all unresolved parameters
	 */
	public Set<String> getUnresolvedParameterNames() {
		Set<String> parameterNames = new HashSet<String>();

		// only return unresolved fields since the resolved fields are not
		// necessary
		for (String parameterName : module.getInputs().keySet()) {
			if (!module.isResolved(parameterName)) {
				parameterNames.add(parameterName);
			}
		}

		return parameterNames;
	}

	/**
	 * @return returns the class of the input {@link FeatureSet} and a map of
	 *         the user set parameters
	 */
	public Pair<Class<?>, Map<String, Object>> getSerializableInfos() {

		Class<?> featuresetClass = pluginInfo.getPluginClass();
		Map<String, Object> parameterValues = new HashMap<String, Object>();

		for (String parameterName : getUnresolvedParameterNames()) {
			parameterValues.put(parameterName,
					inputpanel.getValue(parameterName));
		}

		return new ValuePair<Class<?>, Map<String, Object>>(featuresetClass,
				parameterValues);
	}

	public PluginInfo<FeatureSet> getPluginInfo() {
		return pluginInfo;
	}

	public Module getModule() {
		return module;
	}

	public SwingInputPanel getInputpanel() {
		return inputpanel;
	}
}