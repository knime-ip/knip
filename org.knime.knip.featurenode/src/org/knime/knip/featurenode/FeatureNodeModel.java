package org.knime.knip.featurenode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.featurenode.model.FeatureComputationTask;
import org.knime.knip.featurenode.model.FeatureSetInfo;
import org.knime.knip.featurenode.model.FeatureTaskInput;
import org.knime.knip.featurenode.model.FeatureTaskOutput;
import org.knime.knip.featurenode.model.LabelSettings;
import org.knime.knip.featurenode.model.SettingsModelFeatureSet;
import org.knime.knip.featurenode.util.CountingCompletionService;

import net.imagej.ops.featuresets.StatsFeatureSet;
//import net.imagej.ops.features.sets.Geometric2DFeatureSet;
//import net.imagej.ops.features.sets.Geometric3DFeatureSet;
//import net.imagej.ops.features.sets.Haralick2DFeatureSet;
//import net.imagej.ops.features.sets.Haralick3DFeatureSet;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * This is the model implementation of FeatureNode.
 *
 *
 * @author Daniel Seebacher
 * @author Tim-Oliver Buchholz
 */
public class FeatureNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> extends NodeModel {

	public static final String CFG_KEY_FEATURE_SETS = "feature_node_featuresets";
	public static final String CFG_KEY_IMG_COLUMN = "feature_node_img_column_selection";
	public static final String CFG_KEY_LABELING_COLUMN = "feature_node_labeling_column_selection";
	public static final String CFG_KEY_COLUMN_CREATION_MODE = "feature_node_column_creation_mode";
	public static final String CFG_KEY_DIMENSION_SELECTION = "feature_node_dim_selection";
	private static final String CFG_KEY_APPEND_LABELS_MODEL = "m_appendLabelsModel";
	private static final String CFGK_KEY_LABEL_INTERSETION_MODE = "m_labelIntersectionModel";
	private static final String CFG_KEY_APPEND_LABEL_INFORMATION = "m_appendLabelInfoModel";
	private static final String CFG_KEY_SEGMENT_FILTER = "segment_filter";

	/**
	 *
	 * The logger instance.
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(FeatureNodeModel.class);

	/**
	 * @return Settings model for selected feature sets.
	 */
	public static SettingsModelFeatureSet createFeatureSetsModel() {
		return new SettingsModelFeatureSet(CFG_KEY_FEATURE_SETS);
	}

	public static SettingsModelString createImgColumnModel() {
		return new SettingsModelString(CFG_KEY_IMG_COLUMN, "");
	}

	public static SettingsModelString createLabelingColumnModel() {
		return new SettingsModelString(CFG_KEY_LABELING_COLUMN, "");
	}

	public static SettingsModelString createColumnCreationModeModel() {
		return new SettingsModelString(CFG_KEY_COLUMN_CREATION_MODE, "");
	}

	public static SettingsModelDimSelection createDimSelectionModel() {
		return new SettingsModelDimSelection(CFG_KEY_DIMENSION_SELECTION, "X", "Y");
	}

	public static SettingsModelBoolean createAppendDependenciesModel() {
		return new SettingsModelBoolean(CFG_KEY_APPEND_LABELS_MODEL, false);
	}

	public static SettingsModelBoolean createIntersectionModeModel() {
		return new SettingsModelBoolean(CFGK_KEY_LABEL_INTERSETION_MODE, false);
	}

	public static SettingsModelBoolean createAppendSegmentInfoModel() {
		return new SettingsModelBoolean(CFG_KEY_APPEND_LABEL_INFORMATION, false);
	}

	public static <K extends Comparable<K>> SettingsModelFilterSelection<K> createIncludeLabelModel() {
		return new SettingsModelFilterSelection<K>(CFG_KEY_SEGMENT_FILTER);
	}

	/**
	 * Image Column Selection Model.
	 */
	private final SettingsModelString m_imgColumn = createImgColumnModel();

	/**
	 * Labeling Column Selection Model.
	 */
	private final SettingsModelString m_labelingColumn = createLabelingColumnModel();

	/**
	 * Column Creation model.
	 */
	private final SettingsModelString m_columnCreationModeModel = createColumnCreationModeModel();

	/**
	 * Dimension Selection model.
	 */
	private final SettingsModelDimSelection m_dimselectionModel = createDimSelectionModel();

	private final SettingsModelBoolean m_appendLabelModelModel = createAppendDependenciesModel();
	private final SettingsModelBoolean m_labelIntersectionModeModel = createIntersectionModeModel();
	private final SettingsModelBoolean m_appendLabelInformationModel = createAppendSegmentInfoModel();
	private final SettingsModelFilterSelection<L> m_includeLabelModel = createIncludeLabelModel();

	/**
	 * Feature set model.
	 */
	private final SettingsModelFeatureSet m_featureSets = createFeatureSetsModel();

	/**
	 * Constructor for the node model.
	 */
	protected FeatureNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		// check for empty table
		if (0 == inData[0].getRowCount()) {
			LOGGER.warn("Empty input table. No other columns created.");
			return inData;
		}

		if (this.m_featureSets.getFeatureSets().isEmpty()) {
			LOGGER.warn("No feature set selected. No other columns created.");
			return inData;
		}

		final int imgColumnIndex = getImgColIdx(inData[0].getDataTableSpec());
		final int labelingColumnIndex = getLabelingColIdx(inData[0].getDataTableSpec());

		// get computable feature sets
		final List<FeatureSetInfo> inputFeatureSets = getComputableFeatureSets(this.m_featureSets.getFeatureSets(),
				imgColumnIndex, labelingColumnIndex);
		if (inputFeatureSets.isEmpty()) {
			LOGGER.warn("No computable feature sets selected. No other columns created.");
			return inData;
		}

		// init stuff
		final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);
		final LabelSettings<L> labelSettings = new LabelSettings<L>(this.m_appendLabelModelModel.getBooleanValue(),
				this.m_labelIntersectionModeModel.getBooleanValue(),
				this.m_appendLabelInformationModel.getBooleanValue(), this.m_includeLabelModel.getRulebasedFilter());
		final boolean append = "Append".equalsIgnoreCase(this.m_columnCreationModeModel.getStringValue());
		final ExecutorService pool = Executors.newFixedThreadPool(KNIPConstants.THREADS_PER_NODE);
		final CountingCompletionService<List<FeatureTaskOutput<T, L>>> completionService = new CountingCompletionService<List<FeatureTaskOutput<T, L>>>(
				pool);

		// add tasks to completionservice
		final CloseableRowIterator iterator = inData[0].iterator();
		while (iterator.hasNext()) {
			final DataRow row = iterator.next();
			// create FeatureTaskInput, submit the FeatureTask and increment the
			// number of submitted tasks
			final FeatureTaskInput<T, L> featureTaskInput = new FeatureTaskInput<T, L>(imgColumnIndex,
					labelingColumnIndex, append, this.m_dimselectionModel, labelSettings, imgCellFactory,
					inData[0].getDataTableSpec(), row);
			completionService.submit(new FeatureComputationTask<T, L>(inputFeatureSets, featureTaskInput));

			exec.checkCanceled();
			exec.setProgress(((completionService.getNumberOfSubmittedTasks() / (double) inData[0].getRowCount()) * 0.2),
					"Processed row " + completionService.getNumberOfSubmittedTasks() + " of "
							+ inData[0].getRowCount());
		}

		// shutdown pool
		pool.shutdown();

		DataTableSpec outSpec = null;
		DataContainer container = null;

		while (completionService.hasUncompletedTasks()) {
			final List<FeatureTaskOutput<T, L>> results = completionService.take().get();
			for (final FeatureTaskOutput<T, L> fto : results) {
				if (outSpec == null) {
					outSpec = fto.getOutputSpec();
					container = exec.createDataContainer(outSpec);
				}

				if (container != null) {
					container.addRowToTable(fto.getDataRow());
				}
			}

			exec.checkCanceled();
			exec.setProgress(
					0.2d + ((completionService.getNumberOfCompletedTasks()
							/ (double) completionService.getNumberOfSubmittedTasks()) * 0.8),
					"Calculated features for row " + completionService.getNumberOfCompletedTasks() + " of "
							+ completionService.getNumberOfSubmittedTasks());
		}

		// no results, just return input data
		if (container == null) {
			LOGGER.warn("Empty data container, returning input data.");
			return inData;
		}

		// close container and get output table
		container.close();
		return new BufferedDataTable[] { (BufferedDataTable) container.getTable() };
	}

	private List<FeatureSetInfo> getComputableFeatureSets(final List<FeatureSetInfo> featureSets,
			final int imgColumnIndex, final int labelingColumnIndex) {

		final List<FeatureSetInfo> inputFeatureSets = new ArrayList<FeatureSetInfo>();

		for (final FeatureSetInfo fsi : featureSets) {
//			if (Geometric2DFeatureSet.class.isAssignableFrom(fsi.getFeatureSetClass()) && labelingColumnIndex == -1) {
//				LOGGER.warn("Geometric 2D Feature Set will be ignored, since no Labeling Column is selected.");
//			} else if (Geometric3DFeatureSet.class.isAssignableFrom(fsi.getFeatureSetClass())
//					&& labelingColumnIndex == -1) {
//				LOGGER.warn("Geometric 3D Feature Set will be ignored, since no Labeling Column is selected.");
//			} else
				if (StatsFeatureSet.class.isAssignableFrom(fsi.getFeatureSetClass()) && imgColumnIndex == -1) {
				LOGGER.warn("First Order Statistics Feature Set will be ignored, since no Image Column is selected.");
//			} else if (Haralick3DFeatureSet.class.isAssignableFrom(fsi.getFeatureSetClass())
//					&& this.m_dimselectionModel.getNumSelectedDimLabels() != 3) {
//				LOGGER.warn("Haralick 3D Feature Set will be ignored, since "
//						+ this.m_dimselectionModel.getNumSelectedDimLabels() + " dimensions are selected and not 3.");
//			} else if (Haralick2DFeatureSet.class.isAssignableFrom(fsi.getFeatureSetClass())
//					&& this.m_dimselectionModel.getNumSelectedDimLabels() != 2) {
//				LOGGER.warn("Haralick 2D Feature Set will be ignored, since "
//						+ this.m_dimselectionModel.getNumSelectedDimLabels() + " dimensions are selected and not 2.");
//			} else if (Geometric2DFeatureSet.class.isAssignableFrom(fsi.getFeatureSetClass())
//					&& this.m_dimselectionModel.getNumSelectedDimLabels() != 2) {
//				LOGGER.warn("Geometric 2D Feature Set will be ignored, since "
//						+ this.m_dimselectionModel.getNumSelectedDimLabels() + " dimensions are selected and not 2.");
//			} else if (Geometric3DFeatureSet.class.isAssignableFrom(fsi.getFeatureSetClass())
//					&& this.m_dimselectionModel.getNumSelectedDimLabels() != 3) {
//				LOGGER.warn("Geometric 3D Feature Set will be ignored, since "
//						+ this.m_dimselectionModel.getNumSelectedDimLabels() + " dimensions are selected and not 3.");
			} else {
				inputFeatureSets.add(fsi);
			}
		}

		return inputFeatureSets;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		final DataTableSpec spec = inSpecs[0];

		final int img_index = getImgColIdx(spec);
		final int labeling_index = getLabelingColIdx(spec);

		if ((-1 == img_index) && (-1 == labeling_index)) {
			throw new IllegalArgumentException("At least one image or labeling column must be selected!");
		}

		return new DataTableSpec[] { null };
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
		this.m_featureSets.saveSettingsTo(settings);
		this.m_appendLabelModelModel.saveSettingsTo(settings);
		this.m_labelIntersectionModeModel.saveSettingsTo(settings);
		this.m_appendLabelInformationModel.saveSettingsTo(settings);
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
		this.m_featureSets.loadSettingsFrom(settings);
		this.m_appendLabelModelModel.loadSettingsFrom(settings);
		this.m_labelIntersectionModeModel.loadSettingsFrom(settings);
		this.m_appendLabelInformationModel.loadSettingsFrom(settings);
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
		this.m_featureSets.validateSettings(settings);
		this.m_appendLabelModelModel.validateSettings(settings);
		this.m_labelIntersectionModeModel.validateSettings(settings);
		this.m_appendLabelInformationModel.validateSettings(settings);
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

	private int getImgColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
		int imgColIndex = -1;
		if (null == this.m_imgColumn.getStringValue()) {
			return imgColIndex;
		}
		imgColIndex = inSpec.findColumnIndex(this.m_imgColumn.getStringValue());
		if (-1 == imgColIndex) {
			if ((imgColIndex = NodeUtils.autoOptionalColumnSelection(inSpec, this.m_imgColumn,
					ImgPlusValue.class)) >= 0) {
				setWarningMessage("Auto-configure Image Column: " + this.m_imgColumn.getStringValue());
			} else {
				throw new InvalidSettingsException("No column selected!");
			}
		}
		return imgColIndex;
	}

	private int getLabelingColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
		int labelingColIndex = -1;
		if (null == this.m_labelingColumn.getStringValue()) {
			return labelingColIndex;
		}
		labelingColIndex = inSpec.findColumnIndex(this.m_labelingColumn.getStringValue());
		if (-1 == labelingColIndex) {
			if ((labelingColIndex = NodeUtils.autoOptionalColumnSelection(inSpec, this.m_labelingColumn,
					LabelingValue.class)) >= 0) {
				setWarningMessage("Auto-configure Labeling Column: " + this.m_labelingColumn.getStringValue());
			} else {
				throw new InvalidSettingsException("No column selected!");
			}
		}
		return labelingColIndex;
	}

}
