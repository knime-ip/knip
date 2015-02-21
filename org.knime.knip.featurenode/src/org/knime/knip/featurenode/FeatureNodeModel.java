package org.knime.knip.featurenode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ConstantUtils;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.featurenode.model.FeatureComputationTask;
import org.knime.knip.featurenode.model.FeatureSetInfo;
import org.knime.knip.featurenode.model.FeatureTaskInput;
import org.knime.knip.featurenode.model.FeatureTaskOutput;
import org.knime.knip.featurenode.model.SettingsModelFeatureSet;

/**
 * This is the model implementation of FeatureNode.
 *
 *
 * @author Daniel Seebacher
 * @author Tim-Oliver Buchholz
 */
@SuppressWarnings("deprecation")
public class FeatureNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends NodeModel {

	private final CompletionService<List<FeatureTaskOutput<T, L>>> m_completionService = new ExecutorCompletionService<List<FeatureTaskOutput<T, L>>>(
			Executors.newFixedThreadPool(KNIPConstants.THREADS_PER_NODE));

	/**
	 *
	 * The logger instance.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FeatureNodeModel.class);

	/**
	 * @return Settings model for selected feature sets.
	 */
	public static SettingsModelFeatureSet createFeatureSetsModel() {
		return new SettingsModelFeatureSet("m_featuresets");
	}

	public static SettingsModelString createImgColumnModel() {
		return new SettingsModelString("img_column_selection", "");
	}

	public static SettingsModelString createLabelingColumnModel() {
		return new SettingsModelString("labeling_column_selection", "");
	}

	public static SettingsModelString createColumnCreationModeModel() {
		return new SettingsModelString("column_creation_mode", "");
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
	@SuppressWarnings("unchecked")
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		// check for empty table
		if (0 == inData[0].getRowCount()) {
			LOGGER.warn("Empty input table. No other columns created.");
			return inData;
		}

		// get data from models
		final List<FeatureSetInfo> inputFeatureSets = m_featureSets
				.getFeatureSets();
		final int imgColumnIndex = getImgColIdx(inData[0].getDataTableSpec());
		final int labelingColumnIndex = getLabelingColIdx(inData[0]
				.getDataTableSpec());

		// create cellfactories
		ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);

		final CloseableRowIterator iterator = inData[0].iterator();
		double rowCount = 0;
		int numTasks = 0;
		while (iterator.hasNext()) {
			final DataRow row = iterator.next();

			// create FeatureTaskInput, submit the FeatureTask and increment the
			// number of submitted tasks
			FeatureTaskInput<T, L> featureTaskInput = new FeatureTaskInput<T, L>(
					imgColumnIndex, labelingColumnIndex, row);
			m_completionService.submit(new FeatureComputationTask<T, L>(
					inputFeatureSets, featureTaskInput));
			numTasks++;

			exec.checkCanceled();
			exec.setProgress((++rowCount / inData[0].getRowCount()) / 2);
		}

		DataTableSpec outSpec = null;
		DataContainer container = null;

		for (int currentTask = 0; currentTask < numTasks; currentTask++) {

			List<FeatureTaskOutput<T, L>> taskResults = m_completionService
					.take().get();

			for (int currentResult = 0; currentResult < taskResults.size(); currentResult++) {

				FeatureTaskOutput<T, L> featureTaskOutput = taskResults
						.get(currentResult);

				// first row, if outspec is null create one from scratch and
				// create container
				if (outSpec == null) {
					outSpec = createOutputSpec(inData[0].getDataTableSpec(),
							featureTaskOutput);
					container = exec.createDataContainer(outSpec);
				}

				// save results of this iterableinterval in a row
				final List<DataCell> cells = new ArrayList<DataCell>();

				if ("Append".equalsIgnoreCase(m_columnCreationModeModel
						.getStringValue())) {

					DataRow dataRow = featureTaskOutput.getDataRow();
					for (int i = 0; i < dataRow.getNumCells(); i++) {
						cells.add(dataRow.getCell(i));
					}
				}

				L label = featureTaskOutput.getResultsWithPossibleLabel()
						.getB();
				if (label != null) {

					Labeling<L> labeling = ((LabelingValue<L>) featureTaskOutput
							.getDataRow().getCell(
									featureTaskOutput.getLabelingColumnIndex()))
							.getLabeling();

					IterableRegionOfInterest labelRoi = labeling
							.getIterableRegionOfInterest(label);

					final Img<BitType> bitMask = new ImgView<BitType>(
							Views.zeroMin(Views.interval(
									Views.raster(labelRoi),
									labelRoi.getIterableIntervalOverROI(ConstantUtils
											.constantRandomAccessible(
													new BitType(),
													labeling.numDimensions())))),
							new ArrayImgFactory<BitType>());

					cells.add(imgCellFactory.createCell(new ImgPlus<BitType>(
							bitMask)));
					cells.add(new StringCell(label.toString()));
				}

				// store new results
				Pair<List<Pair<String, T>>, L> resultsWithPossibleLabel = featureTaskOutput
						.getResultsWithPossibleLabel();
				for (final Pair<String, T> results : resultsWithPossibleLabel
						.getA()) {
					cells.add(new DoubleCell(results.getB().getRealDouble()));
				}

				RowKey newRowKey = (currentResult == 0) ? featureTaskOutput
						.getDataRow().getKey() : new RowKey(featureTaskOutput
						.getDataRow().getKey().getString()
						+ "_#" + currentResult);

				// add row
				container.addRowToTable(new DefaultRow(newRowKey, cells
						.toArray(new DataCell[cells.size()])));
			}

			exec.checkCanceled();
			exec.setProgress(0.5 + ((currentTask / (double) numTasks) / 2d));
		}

		// close container and get output table
		container.close();
		return new BufferedDataTable[] { (BufferedDataTable) container
				.getTable() };
	}

	/**
	 * Creates a DataTableSpec from the given {@link DataTableSpec} and the a
	 * {@link List} of {@link FeatureResult}
	 *
	 * @param inSpec
	 *            an existing {@link DataTableSpec}
	 * @param featureRowResult
	 *            a list of {@link FeatureResult}, each result will result in
	 *            one extra column
	 * @return a new {@link DataTableSpec} existing of all columns from the
	 *         {@link DataTableSpec} and a column for each given
	 *         {@link FeatureResult}
	 */
	private DataTableSpec createOutputSpec(final DataTableSpec inSpec,
			final FeatureTaskOutput<T, L> featureTaskOutput) {

		final List<DataColumnSpec> outcells = new ArrayList<DataColumnSpec>();

		if ("Append".equalsIgnoreCase(m_columnCreationModeModel
				.getStringValue())) {
			for (int i = 0; i < inSpec.getNumColumns(); i++) {
				outcells.add(inSpec.getColumnSpec(i));
			}
		}

		// if labelings are present a column for the bitmasks and a column
		// for
		// the label name must be added
		if (-1 != featureTaskOutput.getLabelingColumnIndex()) {
			outcells.add(new DataColumnSpecCreator("Bitmask", ImgPlusCell.TYPE)
					.createSpec());
			outcells.add(new DataColumnSpecCreator("Label", StringCell.TYPE)
					.createSpec());
		}

		// add a new column for each given feature result
		List<Pair<String, T>> featureResults = featureTaskOutput
				.getResultsWithPossibleLabel().getA();

		for (int i = 0; i < featureResults.size(); i++) {
			outcells.add(new DataColumnSpecCreator(DataTableSpec
					.getUniqueColumnName(inSpec, featureResults.get(i).getA()
							+ "_" + i), DoubleCell.TYPE).createSpec());
		}

		return new DataTableSpec(outcells.toArray(new DataColumnSpec[outcells
				.size()]));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		final DataTableSpec spec = inSpecs[0];

		final int img_index = getImgColIdx(spec);
		final int labeling_index = getLabelingColIdx(spec);

		if (-1 == img_index && -1 == labeling_index) {
			throw new IllegalArgumentException(
					"At least one image or labeling column must be selected!");
		}

		return new DataTableSpec[] { null };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		this.m_featureSets.saveSettingsTo(settings);
		this.m_imgColumn.saveSettingsTo(settings);
		this.m_labelingColumn.saveSettingsTo(settings);
		this.m_columnCreationModeModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		this.m_featureSets.loadSettingsFrom(settings);
		this.m_imgColumn.loadSettingsFrom(settings);
		this.m_labelingColumn.loadSettingsFrom(settings);
		this.m_columnCreationModeModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		this.m_featureSets.validateSettings(settings);
		this.m_imgColumn.validateSettings(settings);
		this.m_labelingColumn.validateSettings(settings);
		this.m_labelingColumn.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

	}

	private int getImgColIdx(final DataTableSpec inSpec)
			throws InvalidSettingsException {
		int imgColIndex = -1;
		if (null == m_imgColumn.getStringValue()) {
			return imgColIndex;
		}
		imgColIndex = inSpec.findColumnIndex(m_imgColumn.getStringValue());
		if (-1 == imgColIndex) {
			if ((imgColIndex = NodeUtils.autoOptionalColumnSelection(inSpec,
					m_imgColumn, ImgPlusValue.class)) >= 0) {
				setWarningMessage("Auto-configure Image Column: "
						+ m_imgColumn.getStringValue());
			} else {
				throw new InvalidSettingsException("No column selected!");
			}
		}
		return imgColIndex;
	}

	private int getLabelingColIdx(final DataTableSpec inSpec)
			throws InvalidSettingsException {
		int labelingColIndex = -1;
		if (null == m_labelingColumn.getStringValue()) {
			return labelingColIndex;
		}
		labelingColIndex = inSpec.findColumnIndex(m_labelingColumn
				.getStringValue());
		if (-1 == labelingColIndex) {
			if ((labelingColIndex = NodeUtils.autoOptionalColumnSelection(
					inSpec, m_labelingColumn, LabelingValue.class)) >= 0) {
				setWarningMessage("Auto-configure Labeling Column: "
						+ m_labelingColumn.getStringValue());
			} else {
				throw new InvalidSettingsException("No column selected!");
			}
		}
		return labelingColIndex;
	}

}
