package org.knime.knip.io.nodes.annotation.edit;

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorChangeTracker;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorManager;

/**
 * SettingsModel used in the InteractiveLabelingEditor node to store the
 * manager.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 */
public class SettingsModelLabelEditor extends SettingsModel {

	private String m_configName;

	private LabelingEditorManager m_manager = new LabelingEditorManager();

	public LabelingEditorManager getManager() {
		return m_manager;
	}

	public SettingsModelLabelEditor(final String configName) {
		m_configName = configName;
	}

	public Map<RowColKey, LabelingEditorChangeTracker> getTrackerMap() {
		return m_manager.getTrackerMap();
	}

	public void setAnnotationMap(
			final Map<RowColKey, LabelingEditorChangeTracker> map) {
		m_manager.setTrackerMap(map);
	}

	@Override
	protected <T extends SettingsModel> T createClone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getModelTypeID() {
		// TODO Auto-generated method stub
		return "SMID_LabelingEditor";
	}

	@Override
	protected String getConfigName() {
		return m_configName;
	}

	@Override
	protected void loadSettingsForDialog(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			loadSettingsForModel(settings);
		} catch (final InvalidSettingsException e) {
			throw new NotConfigurableException("Error when loadings Settings");
		}
	}

	@Override
	protected void saveSettingsForDialog(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		saveSettingsForModel(settings);

	}

	@Override
	protected void validateSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void loadSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_manager.loadSettingsFrom(settings);

	}

	@Override
	protected void saveSettingsForModel(final NodeSettingsWO settings) {
		m_manager.saveSettingsTo(settings);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
