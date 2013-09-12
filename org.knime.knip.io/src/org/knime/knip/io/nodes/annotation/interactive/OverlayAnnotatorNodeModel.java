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
package org.knime.knip.io.nodes.annotation.interactive;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.imglib2.img.NativeImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;
import org.knime.knip.io.nodes.annotation.interactive.dc.SettingsModelOverlayAnnotator;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class OverlayAnnotatorNodeModel<T extends RealType<T> & NativeType<T>>
		extends ValueToCellNodeModel<ImgPlusValue<T>, LabelingCell<String>>
		implements BufferedDataTableHolder {

	static String LABEL_SETTINGS_KEY = "annotatedLabels";

	static SettingsModelOverlayAnnotator<String> createAnnotatorSM() {
		return new SettingsModelOverlayAnnotator<String>(LABEL_SETTINGS_KEY);
	}

	static SettingsModelBoolean createWithSegmentidSM() {
		return new SettingsModelBoolean("add_segment_id", true);
	}

	static SettingsModelString creatFactoryTypeSM() {
		return new SettingsModelString("factory_type",
				ImgFactoryTypes.NTREE_IMG_FACTORY.toString());
	}

	static SettingsModelString createLabelingTypeSM() {
		return new SettingsModelString("labeling_type",
				NativeTypes.SHORTTYPE.toString());
	}

	private SettingsModelOverlayAnnotator<String> m_annotationsSM = createAnnotatorSM();

	private final SettingsModelBoolean m_addSegmentID = createWithSegmentidSM();

	private final SettingsModelString m_factoryType = creatFactoryTypeSM();

	private final SettingsModelString m_labelingType = createLabelingTypeSM();

	private LabelingCellFactory m_labelingCellFactory;

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		settingsModels.add(m_annotationsSM);
		settingsModels.add(m_addSegmentID);
		settingsModels.add(m_factoryType);
		settingsModels.add(m_labelingType);
	}

	@Override
	protected void prepareExecute(ExecutionContext exec) {
		m_labelingCellFactory = new LabelingCellFactory(exec);
	}

	@Override
	protected LabelingCell<String> compute(ImgPlusValue<T> cellValue)
			throws Exception {

		Map<String, Overlay<String>> overlayMap = m_annotationsSM
				.getOverlayMap();
		ImgPlus<?> imgPlus = cellValue.getImgPlus();

		final Overlay<String> overlay = overlayMap.get(imgPlus.getSource());

		if ((overlay != null) && (overlay.getElements().length > 0)) {
			final Labeling<String> labeling = overlay.renderSegmentationImage(
					(NativeImgFactory<?>) ImgFactoryTypes.getImgFactory(
							m_factoryType.getStringValue(), imgPlus),
					m_addSegmentID.getBooleanValue(), NativeTypes
							.valueOf(m_labelingType.getStringValue()));

			try {
				return m_labelingCellFactory.createCell(labeling,
						new DefaultLabelingMetadata(imgPlus, imgPlus, imgPlus,
								new DefaultLabelingColorTable()));
			} catch (IOException e) {
				throw new KNIPRuntimeException(
						"error while creating new labeling", e);
			}
		} else {
			// => missing cell
			return null;
		}
	}
}
