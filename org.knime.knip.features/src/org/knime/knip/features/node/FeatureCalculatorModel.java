/*
 * ------------------------------------------------------------------------
 *
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
  ---------------------------------------------------------------------
 *
 */

package org.knime.knip.features.node;

import java.io.File;
import java.io.IOException;

import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.DataContainer;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.features.FeatureSetGroup;
import org.knime.knip.features.FeaturesGateway;
import org.knime.knip.features.node.model.SettingsModelFeatureSet;

/**
 * This is the model implementation of FeatureNode.
 *
 * @author Daniel Seebacher, University of Konstanz
 * @author Tim-Oliver Buchholz, University of Konstanz
 * @author Christian Dietz, University of Konstanz
 */
public class FeatureCalculatorModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> extends NodeModel {

	static final String[] COL_CREATION_MODES = new String[] { "Append", "New Table" };

	/**
	 * Helper to create various {@link SettingsModel}s
	 */

	public static SettingsModelFeatureSet createFeatureSetsModel() {
		return new SettingsModelFeatureSet("feature_node_featuresets");
	}

	public static SettingsModelString createImgColumnModel() {
		return new SettingsModelString("feature_node_img_column_selection", "");
	}

	public static SettingsModelString createLabelingColumnModel() {
		return new SettingsModelString("feature_node_labeling_column_selection", "");
	}

	public static SettingsModelString createColumnCreationModeModel() {
		return new SettingsModelString("feature_node_column_creation_mode", "");
	}

	public static SettingsModelDimSelection createDimSelectionModel() {
		return new SettingsModelDimSelection("feature_node_dim_selection", "X", "Y");
	}

	public static SettingsModelBoolean createAppendLabelsOfOverlappingSegments() {
		return new SettingsModelBoolean("m_appendLabelsOfOverlappingSegmentsModel", false);
	}

	public static SettingsModelBoolean createIntersectionModeModel() {
		return new SettingsModelBoolean("m_labelIntersectionModel", false);
	}

	public static SettingsModelBoolean createAppendSegmentInfoModel() {
		return new SettingsModelBoolean("m_appendSegmentInfoModel", false);
	}

	public static <K extends Comparable<K>> SettingsModelFilterSelection<K> createIncludeLabelModel() {
		return new SettingsModelFilterSelection<K>("segment_filter");
	}

	/**
	 * Setting Models
	 */
	private final SettingsModelString m_imgColumn = createImgColumnModel();
	private final SettingsModelString m_labelingColumn = createLabelingColumnModel();
	private final SettingsModelString m_columnCreationModeModel = createColumnCreationModeModel();
	private final SettingsModelDimSelection m_dimselectionModel = createDimSelectionModel();
	private final SettingsModelBoolean m_appendLabelsOfOverlappingSegmentsModel = createAppendLabelsOfOverlappingSegments();
	private final SettingsModelBoolean m_labelIntersectionModeModel = createIntersectionModeModel();
	private final SettingsModelBoolean m_appendSegmentInfoModel = createAppendSegmentInfoModel();
	private final SettingsModelFilterSelection<L> m_includeLabelModel = createIncludeLabelModel();
	private final SettingsModelFeatureSet m_featureSetsModel = createFeatureSetsModel();

	/**
	 * Constructor for the node model.
	 */
	protected FeatureCalculatorModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		final int imgColIndex = getColIdx(inData[0].getDataTableSpec(), ImgPlusValue.class, m_imgColumn);
		final int labColIdx = getColIdx(inData[0].getDataTableSpec(), LabelingValue.class, m_labelingColumn);

		final FeatureSetGroup group = FeaturesGateway.fs().getFeatureGroup(m_featureSetsModel.getFeatureSetInfos(),
				imgColIndex, labColIdx, doAppend(), doAppendOverlappingSegments(), doAppendSegmentInformation(),
				getIntersectionMode(), m_includeLabelModel.getRulebasedFilter(), exec, m_dimselectionModel);

		final DataTableSpec outSpec = group.createSpec(inData[0].getDataTableSpec());
		final BufferedDataContainer container = exec.createDataContainer(outSpec);

		// check for empty table
		if (inData[0].size() == 0) {
			KNIPGateway.log().warn("Empty input table. Creating empty output table.");
			container.close();
			return new BufferedDataTable[] { container.getTable() };
		}

		// iterate over input table
		double currentRow = 0;
		final UnaryComputerOp<DataRow, DataContainer> computer = group.createComputerOp();
		for (final DataRow row : inData[0]) {
			if (imgColIndex != -1 && row.getCell(imgColIndex).isMissing()) {
				logMissingCell(imgColIndex, row);
			} else if (labColIdx != -1 && row.getCell(labColIdx).isMissing()) {
				logMissingCell(labColIdx, row);
			} else {
				exec.setProgress(
						"Computing " + m_featureSetsModel.getFeatureSetNames() + " on row " + row.getKey() + ".");
				// magic happens here ;-)
				computer.compute1(row, container);
			}

			exec.checkCanceled();
			exec.setProgress(currentRow++ / inData[0].size());
		}

		container.close();
		return new BufferedDataTable[] { container.getTable() };
	}

	private void logMissingCell(final int imgColIndex, final DataRow row) {
		KNIPGateway.log().warn(
				"Row " + row.getKey() + " was ignored because the cell in column " + imgColIndex + " was missing.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Nothing to do here
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		final int imgColIndex = getColIdx(inSpecs[0], ImgPlusValue.class, m_imgColumn);
		final int labelingColIdx = getColIdx(inSpecs[0], LabelingValue.class, m_labelingColumn);

		if ((-1 == imgColIndex) && (-1 == labelingColIdx)) {
			throw new InvalidSettingsException("At least one image or labeling column must be selected!");
		}

		final FeatureSetGroup group = FeaturesGateway.fs().getFeatureGroup(m_featureSetsModel.getFeatureSetInfos(),
				imgColIndex, labelingColIdx, doAppend(), doAppendOverlappingSegments(), doAppendSegmentInformation(),
				getIntersectionMode(), m_includeLabelModel.getRulebasedFilter(), null, m_dimselectionModel);

		final DataTableSpec outSpec = group.createSpec(inSpecs[0]);

		return new DataTableSpec[] { outSpec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		this.m_imgColumn.saveSettingsTo(settings);
		this.m_labelingColumn.saveSettingsTo(settings);
		this.m_columnCreationModeModel.saveSettingsTo(settings);
		this.m_dimselectionModel.saveSettingsTo(settings);
		this.m_featureSetsModel.saveSettingsTo(settings);
		this.m_appendLabelsOfOverlappingSegmentsModel.saveSettingsTo(settings);
		this.m_labelIntersectionModeModel.saveSettingsTo(settings);
		this.m_appendSegmentInfoModel.saveSettingsTo(settings);
		this.m_includeLabelModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		this.m_imgColumn.loadSettingsFrom(settings);
		this.m_labelingColumn.loadSettingsFrom(settings);
		this.m_columnCreationModeModel.loadSettingsFrom(settings);
		this.m_dimselectionModel.loadSettingsFrom(settings);
		this.m_featureSetsModel.loadSettingsFrom(settings);
		this.m_appendLabelsOfOverlappingSegmentsModel.loadSettingsFrom(settings);
		this.m_labelIntersectionModeModel.loadSettingsFrom(settings);
		this.m_appendSegmentInfoModel.loadSettingsFrom(settings);
		this.m_includeLabelModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		this.m_imgColumn.validateSettings(settings);
		this.m_labelingColumn.validateSettings(settings);
		this.m_columnCreationModeModel.validateSettings(settings);
		this.m_dimselectionModel.validateSettings(settings);
		this.m_featureSetsModel.validateSettings(settings);
		this.m_appendLabelsOfOverlappingSegmentsModel.validateSettings(settings);
		this.m_labelIntersectionModeModel.validateSettings(settings);
		this.m_appendSegmentInfoModel.validateSettings(settings);
		this.m_includeLabelModel.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// do nothing
	}

	/**
	 * HELPER METHODS
	 */

	private int getColIdx(final DataTableSpec inSpec, final Class<? extends DataValue> type,
			final SettingsModelString colModel) throws InvalidSettingsException {
		int colIndex = -1;
		if (colModel.getStringValue() != null && colModel.getStringValue().equals("")) {
			colIndex = NodeUtils.silentOptionalAutoColumnSelection(inSpec, colModel, type);
			return colIndex;
		}
		colIndex = inSpec.findColumnIndex(colModel.getStringValue());
		if (-1 != colIndex && !inSpec.getColumnSpec(colIndex).getType().isCompatible(type)) {
			throw new InvalidSettingsException("No column selected!");

		}
		return colIndex;
	}

	private boolean doAppend() {
		return COL_CREATION_MODES[0].equalsIgnoreCase(this.m_columnCreationModeModel.getStringValue());
	}

	private boolean doAppendSegmentInformation() {
		return m_appendSegmentInfoModel.getBooleanValue();
	}

	private boolean doAppendOverlappingSegments() {
		return m_appendLabelsOfOverlappingSegmentsModel.getBooleanValue();
	}

	private boolean getIntersectionMode() {
		return m_labelIntersectionModeModel.getBooleanValue();
	}

}
