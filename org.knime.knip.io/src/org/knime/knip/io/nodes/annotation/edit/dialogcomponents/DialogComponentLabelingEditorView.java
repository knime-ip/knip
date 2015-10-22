/*
 * ------------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.io.nodes.annotation.edit.dialogcomponents;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.io.nodes.annotation.AnnotatorView;
import org.knime.knip.io.nodes.annotation.edit.SettingsModelLabelEditor;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorChangeTracker;


public class DialogComponentLabelingEditorView
		extends DialogComponent {

	/* wrapped by this component */
	private AnnotatorView<LabelingEditorChangeTracker> m_annotatorView;

	public DialogComponentLabelingEditorView(final AnnotatorView<LabelingEditorChangeTracker> annotatorView,
			final SettingsModelLabelEditor model) {
		super(model);

		m_annotatorView = annotatorView;
		
		// set the view panel
		getComponentPanel().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0d;
		gbc.weighty = 1.0d;

		getComponentPanel().add(m_annotatorView.getAnnotatorPanel(), gbc);
	}

	public void updateDataTable(DataTable inputTable) {
		// note that the order in which updateDataTable and updateComponent are
		// called should
		// ideally be the other way around. However the forgiving implementation
		// of the OverlayAnnotatorView
		// makes it possible to add the overlays before the input table.
		m_annotatorView.setInputTable(inputTable);
	}

	@Override
	protected void updateComponent() {
		// note that the order in which updateDataTable and updateComponent are
		// called should
		// ideally be the other way around. However the forgiving implementation
		// of the OverlayAnnotatorView
		// makes it possible to add the overlays before the input table.
		SettingsModelLabelEditor model = (SettingsModelLabelEditor)getModel();
		Map<RowColKey, LabelingEditorChangeTracker> map = new HashMap<RowColKey, LabelingEditorChangeTracker>(model.getTrackerMap());

		m_annotatorView.reset();
		for (RowColKey key : map.keySet()) {
			m_annotatorView.setAnnotation(key, map.get(key));
		}
	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		SettingsModelLabelEditor model = (SettingsModelLabelEditor) getModel();

		Map<RowColKey, LabelingEditorChangeTracker> map = new HashMap<RowColKey, LabelingEditorChangeTracker>();
		for (RowColKey key : m_annotatorView.getIdentifiersOfManagedSources()) {
			map.put(key, (LabelingEditorChangeTracker) m_annotatorView.getAnnotation(key));
		}

		model.setAnnotationMap(map);
	}

	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs)
			throws NotConfigurableException {
		// Nothing to do here
	}

	@Override
	protected void setEnabledComponents(boolean enabled) {
		// Nothing to do here
	}

	@Override
	public void setToolTipText(String text) {
		// Nothing to do here
	}

	public void reset() {
		m_annotatorView.reset();
	}

}
