package org.knime.knip.io.nodes.annotation;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;

public abstract class SettingsModelAnnotatorView<A> extends SettingsModel {
	
	public abstract void setAnnotationMap(HashMap<RowColKey, A> map);

	public abstract Map<RowColKey, A> getAnnotationMap();
	
	protected abstract void saveSettings(NodeSettingsWO settings);
	
	protected abstract void loadSettings(NodeSettingsRO settings);
	
	//
	// standard methods
	//

	@Override
	protected void loadSettingsForDialog(NodeSettingsRO settings,
			PortObjectSpec[] specs) throws NotConfigurableException {
		loadSettings(settings);
	}

	@Override
	protected void saveSettingsForDialog(NodeSettingsWO settings)
			throws InvalidSettingsException {
		saveSettings(settings);
	}

	@Override
	protected void loadSettingsForModel(NodeSettingsRO settings)
			throws InvalidSettingsException {
		loadSettings(settings);
	}

	@Override
	protected void saveSettingsForModel(NodeSettingsWO settings) {
		saveSettings(settings);
	}

	@Override
	protected void validateSettingsForModel(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// Nothing to do here
	}
}
