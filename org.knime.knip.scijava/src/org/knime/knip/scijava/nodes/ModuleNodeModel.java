package org.knime.knip.scijava.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.scijava.SciJavaGateway;
import org.knime.knip.scijava.adapters.ModuleAdapter;
import org.knime.knip.scijava.adapters.ModuleAdapterFactory;
import org.knime.knip.scijava.adapters.ModuleItemAdapter;
import org.knime.knip.scijava.dialog.SettingsModelModuleDialog;
import org.scijava.Context;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleRunner;
import org.scijava.module.process.InitPreprocessor;
import org.scijava.module.process.ModulePreprocessor;

public class ModuleNodeModel extends NodeModel {

	/**
	 * @return the settings model for the SciJava Dialog.
	 */
	public static SettingsModelModuleDialog createSciJavaDialogModel() {
		return new SettingsModelModuleDialog("scijava_dlg_settings_model");
	}

	private ModuleAdapterFactory adapterFactory;

	private ModuleInfo info;

	private SettingsModelModuleDialog sciJavaDialogModel = createSciJavaDialogModel();

	private Map<ModuleItem<?>, SettingsModel> additionalModels;

	private ModuleAdapter configurationAdapter;

	protected ModuleNodeModel(final ModuleInfo info,
			final ModuleAdapterFactory adapterFactory) {
		super(1, 1);

		this.info = info;
		this.adapterFactory = adapterFactory;
		this.additionalModels = new HashMap<ModuleItem<?>, SettingsModel>();
		try {
			this.configurationAdapter = adapterFactory.createAdapter(info
					.createModule());
		} catch (final ModuleException e) {
			// TODO
			e.printStackTrace();
		}

		final Map<ModuleItem<?>, ModuleItemAdapter<?>> itemAdapters = configurationAdapter
				.getInputAdapters();
		for (final ModuleItem<?> item : info.inputs()) {
			final ModuleItemAdapter<?> itemAdapter = itemAdapters.get(item);

			if (itemAdapter != null) {
				additionalModels.put(item,
						itemAdapter.createSettingsModel(item));
			}
		}
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		return outSpec();
	}

	private DataTableSpec[] outSpec() {
		sciJavaDialogModel.configureModule(configurationAdapter.getModule());
		return new DataTableSpec[] { configurationAdapter.getOutSpec() };
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		final BufferedDataContainer container = exec
				.createDataContainer(outSpec()[0]);
		final CloseableRowIterator rowIterator = inData[0].iterator();

		final Context context = SciJavaGateway.getInstance().getContext();

		int i = 0;
		while (rowIterator.hasNext()) {
			final DataRow row = rowIterator.next();

			// configure module to use values from automatically generated
			// dialog
			final Module module = info.createModule();
			final ModuleAdapter adapter = adapterFactory.createAdapter(module);

			sciJavaDialogModel.configureModule(module);
			adapter.configureModule(inData[0].getDataTableSpec(), row,
					additionalModels);

			context.inject(module);
			final List<ModulePreprocessor> pre = new ArrayList<ModulePreprocessor>();
			final InitPreprocessor ip = new InitPreprocessor();
			ip.setContext(context);
			pre.add(ip);

			// TODO potentially: ModuleService.run(...) for ...
			final ModuleRunner runner = new ModuleRunner(context, module, pre,
					null);

			runner.run();
			final Iterator<DataCell[]> resIterator = adapter.resultRows()
					.iterator();
			while (resIterator.hasNext()) {
				final DataCell[] resRow = resIterator.next();

				// Todo better row key?
				container.addRowToTable(new DefaultRow("Row" + i++, resRow));
			}
		}

		container.close();

		// TODO Auto-generated method stub
		return new BufferedDataTable[] { container.getTable() };
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		sciJavaDialogModel.saveSettingsTo(settings);

		for (final SettingsModel model : additionalModels.values()) {
			model.saveSettingsTo(settings);
		}
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		sciJavaDialogModel.validateSettings(settings);

		for (final SettingsModel model : additionalModels.values()) {
			model.validateSettings(settings);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		sciJavaDialogModel.loadSettingsFrom(settings);

		for (final SettingsModel model : additionalModels.values()) {
			model.loadSettingsFrom(settings);
		}
	}

	@Override
	protected void reset() {
		// nothing to do here
	}

	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Nothing to do here
	}

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do here
	}

}
