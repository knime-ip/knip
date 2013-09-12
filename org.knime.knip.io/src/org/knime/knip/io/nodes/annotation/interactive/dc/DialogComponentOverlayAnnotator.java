/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
package org.knime.knip.io.nodes.annotation.interactive.dc;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.knime.knip.io.nodes.annotation.interactive.AnnotatorView;
import org.knime.knip.io.nodes.annotation.interactive.OverlayAnnotatorView;

/**
 * Adapter class that ties a SettingsModel and a {@link AnnotatorView} together.
 * Which allows you to use the {@link AnnotatorView} as Dialogcomponent.
 * 
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class DialogComponentOverlayAnnotator<T extends RealType<T> & NativeType<T>>
		extends DialogComponent {

	/* wrapped by this component */
	private AnnotatorView<String> m_annotatorView = new OverlayAnnotatorView<T>();

	public DialogComponentOverlayAnnotator(
			final SettingsModelOverlayAnnotator<String> model) {
		super(model);

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
		SettingsModelOverlayAnnotator<String> model = (SettingsModelOverlayAnnotator<String>) getModel();
		Map<String, Overlay<String>> map = model.getOverlayMap();

		m_annotatorView.reset();
		for (String srcName : map.keySet()) {
			m_annotatorView.setOverlay(srcName, map.get(srcName));
		}
	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		SettingsModelOverlayAnnotator<String> model = (SettingsModelOverlayAnnotator<String>) getModel();

		HashMap<String, Overlay<String>> map = new HashMap<String, Overlay<String>>();
		for (String srcName : m_annotatorView.getOverlaySrcNames()) {
			map.put(srcName, m_annotatorView.getOverlay(srcName));
		}

		model.setOverlayMap(map);
	}

	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs)
			throws NotConfigurableException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setEnabledComponents(boolean enabled) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setToolTipText(String text) {
		// TODO Auto-generated method stub
	}

	public void reset() {
		m_annotatorView.reset();
	}

}
