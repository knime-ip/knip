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
package org.knime.knip.io.nodes.annotation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.imglib2.img.NativeImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;

/**
 * This Node reads images.
 * 
 * @param <T>
 * @param <L>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class AnnotatorNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends NodeModel implements BufferedDataTableHolder {

	/**
	 * Key to store the "add segment id" option
	 */
	public static final String CFG_ADD_SEGMENT_ID = "add_segment_id";

	/**
	 * Key to store the "factory type" option
	 */
	public static final String CFG_FACTORY_TYPE = "factory_type";

	/**
	 * Key to store the "factory type" option
	 */
	public static final String CFG_LABELING_TYPE = "labeling_type";

	/**
	 * Key to store the points
	 */
	public static final String CFG_POINTS = "points";

	/**
	 * The image out port of the Node.
	 */
	public static final int IMAGEOUTPORT = 0;

	private final SettingsModelBoolean m_addSegmentID = new SettingsModelBoolean(
			CFG_ADD_SEGMENT_ID, true);

	/*
	 * Settings for the Overlay
	 */
	private final SettingsModelAnnotator<String> m_annotatorModel = new SettingsModelAnnotator<String>(
			CFG_POINTS);

	/* data table for the table cell viewer */
	private BufferedDataTable m_data;

	private final SettingsModelString m_factoryType = new SettingsModelString(
			CFG_FACTORY_TYPE, ImgFactoryTypes.NTREE_IMG_FACTORY.toString());

	/*
	 * The image-output-table DataSpec
	 */
	private DataTableSpec m_imageOutSpec;

	private final SettingsModelString m_labelingType = new SettingsModelString(
			CFG_LABELING_TYPE, NativeTypes.SHORTTYPE.toString());

	private DataTableSpec m_labelsOutSpec;

	/*
	 * Collection of all settings.
	 */
	private final Collection<SettingsModel> m_settingsCollection;

	/**
	 * Initializes the ImageReader
	 */
	public AnnotatorNodeModel() {
		super(0, 2);
		m_settingsCollection = new ArrayList<SettingsModel>();
		m_settingsCollection.add(m_annotatorModel);
		m_settingsCollection.add(m_addSegmentID);
		m_settingsCollection.add(m_labelingType);
		m_settingsCollection.add(m_factoryType);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		m_imageOutSpec = new DataTableSpec(
				new DataColumnSpec[] {
						new DataColumnSpecCreator("Image", ImgPlusCell.TYPE)
								.createSpec(),
						new DataColumnSpecCreator("Segments", LabelingCell.TYPE)
								.createSpec() });

		m_labelsOutSpec = new DataTableSpec(
				new DataColumnSpec[] { new DataColumnSpecCreator("Image",
						StringCell.TYPE).createSpec() });

		return new DataTableSpec[] { m_imageOutSpec, m_labelsOutSpec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		final BufferedDataContainer imgCon = exec
				.createDataContainer(m_imageOutSpec);
		final BufferedDataContainer labelCon = exec
				.createDataContainer(m_labelsOutSpec);

		final Map<String, Overlay<String>> map = m_annotatorModel
				.getOverlayMap();
		final Set<String> labels = new HashSet<String>();
		final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);
		final LabelingCellFactory labCellFactory = new LabelingCellFactory(exec);
		for (final Entry<String, Overlay<String>> entry : map.entrySet()) {

			final ImgPlus<T> imgPlus = SettingsModelAnnotator.loadImgPlus(entry
					.getKey());

			final long[] dimensions = new long[imgPlus.numDimensions()];
			imgPlus.dimensions(dimensions);

			final Overlay<String> o = entry.getValue();

			if ((o != null) && (o.getElements().length > 0)) {
				final Labeling<String> labeling = o.renderSegmentationImage(
						(NativeImgFactory<?>) ImgFactoryTypes.getImgFactory(
								m_factoryType.getStringValue(), imgPlus),
						m_addSegmentID.getBooleanValue(), NativeTypes
								.valueOf(m_labelingType.getStringValue()));
				imgCon.addRowToTable(new DefaultRow(new RowKey(entry.getKey()),
						imgCellFactory.createCell(imgPlus),
						labCellFactory.createCell(labeling,
								new DefaultLabelingMetadata(imgPlus, imgPlus,
										imgPlus,
										new DefaultLabelingColorTable()))));

				for (final String s : labeling.firstElement().getMapping()
						.getLabels()) {
					if (!s.contains("Segment:")) {
						labels.add(s);
					}
				}

			}

		}

		for (final String s : labels) {
			pushFlowVariableString("Label_" + s, s);
			labelCon.addRowToTable(new DefaultRow(s, new StringCell(s)));
		}

		labelCon.close();
		imgCon.close();

		final BufferedDataTable[] out = new BufferedDataTable[] {
				imgCon.getTable(), labelCon.getTable() };

		// data table for the table cell viewer
		m_data = out[0];

		return out;
	}

	@Override
	public BufferedDataTable[] getInternalTables() {
		return new BufferedDataTable[] { m_data };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.loadSettingsFrom(settings);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		m_data = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		for (final SettingsModel sm : m_settingsCollection) {
			sm.saveSettingsTo(settings);
		}

	}

	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {
		m_data = tables[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.validateSettings(settings);
		}
	}
}
