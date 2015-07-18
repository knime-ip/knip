//
//package org.knime.knip.io.nodes.annotation.edit.dialogcomponents;
//
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.knime.core.data.DataTable;
//import org.knime.core.node.InvalidSettingsException;
//import org.knime.core.node.NotConfigurableException;
//import org.knime.core.node.defaultnodesettings.DialogComponent;
//import org.knime.core.node.port.PortObjectSpec;
//import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
//import org.knime.knip.io.nodes.annotation.AnnotatorView;
//import org.knime.knip.io.nodes.annotation.edit.SettingsModelLabelEditor;
//import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorChangeTracker;
//
//
//public class DialogComponentLabelingEditorView
//		extends DialogComponent {
//
//	/* wrapped by this component */
//	private AnnotatorView<LabelingEditorChangeTracker> m_annotatorView;
//
//	public DialogComponentLabelingEditorView(final AnnotatorView<LabelingEditorChangeTracker> annotatorView,
//			final SettingsModelLabelEditor model) {
//		super(model);
//
//		m_annotatorView = annotatorView;
//		
//		// set the view panel
//		getComponentPanel().setLayout(new GridBagLayout());
//		GridBagConstraints gbc = new GridBagConstraints();
//		gbc.fill = GridBagConstraints.BOTH;
//		gbc.weightx = 1.0d;
//		gbc.weighty = 1.0d;
//
//		getComponentPanel().add(m_annotatorView.getAnnotatorPanel(), gbc);
//	}
//
//	public void updateDataTable(DataTable inputTable) {
//		// note that the order in which updateDataTable and updateComponent are
//		// called should
//		// ideally be the other way around. However the forgiving implementation
//		// of the OverlayAnnotatorView
//		// makes it possible to add the overlays before the input table.
//		m_annotatorView.setInputTable(inputTable);
//	}
//
//	@Override
//	protected void updateComponent() {
//		// note that the order in which updateDataTable and updateComponent are
//		// called should
//		// ideally be the other way around. However the forgiving implementation
//		// of the OverlayAnnotatorView
//		// makes it possible to add the overlays before the input table.
//		SettingsModelLabelEditor model = (SettingsModelLabelEditor)getModel();
//		Map<RowColKey, LabelingEditorChangeTracker> map = new HashMap<RowColKey, LabelingEditorChangeTracker>(model.getTrackerMap());
//
//		m_annotatorView.reset();
//		for (RowColKey key : map.keySet()) {
//			m_annotatorView.setAnnotation(key, map.get(key));
//		}
//	}
//
//	@Override
//	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
//		SettingsModelLabelEditor model = (SettingsModelLabelEditor) getModel();
//
//		Map<RowColKey, LabelingEditorChangeTracker> map = new HashMap<RowColKey, LabelingEditorChangeTracker>();
//		for (RowColKey key : m_annotatorView.getIdentifiersOfManagedSources()) {
//			map.put(key, (LabelingEditorChangeTracker) m_annotatorView.getAnnotation(key));
//		}
//
//		model.setAnnotationMap(map);
//	}
//
//	@Override
//	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs)
//			throws NotConfigurableException {
//		// Nothing to do here
//	}
//
//	@Override
//	protected void setEnabledComponents(boolean enabled) {
//		// Nothing to do here
//	}
//
//	@Override
//	public void setToolTipText(String text) {
//		// Nothing to do here
//	}
//
//	public void reset() {
//		m_annotatorView.reset();
//	}
//
//}
