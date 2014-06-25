package org.knime.knip.scijava.module.nodes;

import java.util.LinkedList;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.knip.scijava.module.ModuleGateway;
import org.knime.knip.scijava.module.adapters.ModuleAdapter;
import org.knime.knip.scijava.module.adapters.ModuleAdapterFactory;
import org.knime.knip.scijava.module.adapters.ModuleItemAdapter;
import org.knime.knip.scijava.module.dialog.AbstractModuleNodeDialog;
import org.knime.knip.scijava.module.dialog.DialogComponentGroup;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

public class ModuleNodeDialog extends AbstractModuleNodeDialog {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(ModuleNodeDialog.class);

	/**
	 * Creates a {@link StandardIJNodeDialog}
	 * 
	 * @param moduleInfo
	 *            {@link ModuleInfo} which will be used to create the dialog
	 *            panel
	 * @throws ModuleException
	 */
	public ModuleNodeDialog(final ModuleInfo info,
			final ModuleAdapterFactory adapterFactory) throws ModuleException {
		super(info, adapterFactory);

		boolean useOptionTab = false;
		if (hasNoneEmptyFreeImageJDialog()) {
			createNewGroup("ImageJ Dialog");
			addImageJDialogIfNoneEmpty();
			closeCurrentGroup();
			useOptionTab = true;
		}

		final ModuleAdapter adapter = adapterFactory.createAdapter(info
				.createModule());

		// add KNIME settings if additional input components exist
		final LinkedList<DialogComponentGroup> additionalInputDialogGroups = new LinkedList<DialogComponentGroup>();
		final Map<ModuleItem<?>, ModuleItemAdapter<?>> itemAdapters = adapter
				.getInputAdapters();

		for (final ModuleItem<?> item : adapter.getModule().getInfo().inputs()) {

			final ModuleItemAdapter<?> itemAdapter = itemAdapters.get(item);
			if (itemAdapter != null) {
				additionalInputDialogGroups.add(itemAdapter
						.getDialogComponentGroup(item));
			} else if (!ModuleGateway.isSciJavaDialogInputType(item.getType())) {
				LOGGER.error("GenericModuleNodeDialog: can not create dialog components for an adapter.");
			}
		}
		if (additionalInputDialogGroups.size() > 0) {
			useOptionTab = true;
			createNewGroup("KNIME Settings");
			for (final DialogComponentGroup dcg : additionalInputDialogGroups) {
				addComponents(dcg);
			}
			closeCurrentGroup();
		}

		if (!useOptionTab) {
			removeTab("Options");
		}
	}
}
