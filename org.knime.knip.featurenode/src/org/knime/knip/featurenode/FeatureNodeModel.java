package org.knime.knip.featurenode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.ThreadPoolExecutorService;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.featurenode.model.FeatureRowInput;
import org.knime.knip.featurenode.model.FeatureRowResult;
import org.knime.knip.featurenode.model.FeatureTask;
import org.knime.knip.featurenode.model.SettingsModelFeatureSet;

/**
 * This is the model implementation of FeatureNode.
 *
 *
 * @author Daniel Seebacher
 * @author Tim-Oliver Buchholz
 */
public class FeatureNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends NodeModel {

	private final ThreadPoolExecutorService m_executor = new ThreadPoolExecutorService(
			KNIMEConstants.GLOBAL_THREAD_POOL
					.createSubPool(KNIPConstants.THREADS_PER_NODE));

	/**
	 *
	 * The logger instance.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FeatureNodeModel.class);

	/**
	 * @return Settings model for selected labeling.
	 */
	public static SettingsModelString createLabelingSelectionModel() {
		return new SettingsModelString("m_labeling_settings_model", null);
	}

	/**
	 * @return Settings model for selected image.
	 */
	public static SettingsModelString createImgSelectionModel() {
		return new SettingsModelString("m_imgage_settings_model", null);
	}

	/**
	 * @return Settings model for selected feature sets.
	 */
	static SettingsModelFeatureSet createFeatureSetsModel() {
		return new SettingsModelFeatureSet("m_featuresets");
	}

	/**
	 * Image selection model.
	 */
	private final SettingsModelString IMG_SELECTION_MODEL = createImgSelectionModel();

	/**
	 * Labeling selection model.
	 */
	private final SettingsModelString LABELING_SELECTION_MODEL = createLabelingSelectionModel();

	/**
	 * Feature set model.
	 */
	private final SettingsModelFeatureSet FEATURE_SET_MODEL = createFeatureSetsModel();

	/**
	 * The index of the image column in the {@link DataTableSpec} of the input
	 * table.
	 */
	private int m_imgPlusCol = -1;

	/**
	 * The index of the labeling column in the {@link DataTableSpec} of the
	 * input table.
	 */
	private int m_labelingCol = -1;

	/**
	 * Constructor for the node model.
	 */
	protected FeatureNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		// check for empty table
		final int numRows = inData[0].getRowCount();
		if (numRows == 0) {
			LOGGER.warn("Empty input table. No other columns created.");
			return inData;
		}

		DataTableSpec outSpec = null;
		DataContainer container = null;

		final CloseableRowIterator iterator = inData[0].iterator();
		double rowCount = 0;
		final List<Future<List<FeatureRowResult<T, L>>>> futureTasks = new ArrayList<Future<List<FeatureRowResult<T, L>>>>();

		while (iterator.hasNext()) {
			final DataRow row = iterator.next();

			final ImgPlusValue<T> inputImgValue = (this.m_imgPlusCol != -1) ? ((ImgPlusValue<T>) row
					.getCell(this.m_imgPlusCol)) : null;
			final LabelingValue<L> inputLabelingValue = (this.m_labelingCol != -1) ? ((LabelingValue<L>) row
					.getCell(this.m_labelingCol)) : null;

			final FeatureRowInput<T, L> fri = new FeatureRowInput<T, L>(
					inputImgValue, inputLabelingValue);

			final FeatureTask<T, L> featureTask = new FeatureTask<T, L>(
					this.FEATURE_SET_MODEL.getFeatureSets(), fri);
			futureTasks.add(this.m_executor.submit(featureTask));

			exec.checkCanceled();
			exec.setProgress((++rowCount / inData[0].getRowCount()) / 2);
		}

		double futureTasksCount = 0;
		for (int i = 0; i < futureTasks.size(); i++) {
			final List<FeatureRowResult<T, L>> result = futureTasks.get(i)
					.get();

			for (final FeatureRowResult<T, L> featureRowResult : result) {
				// first row, if outspec is null create one from scratch and
				// create container
				if (outSpec == null) {
					outSpec = createOutputSpec(inData[0].getDataTableSpec(),
							featureRowResult);
					container = exec.createDataContainer(outSpec);
				}

				// save results of this iterableinterval in a row
				final List<DataCell> cells = new ArrayList<DataCell>();

				// store new results
				for (final Pair<String, T> featureResult : featureRowResult
						.getResults()) {
					cells.add(new DoubleCell(featureResult.getB()
							.getRealDouble()));

				}

				// add row
				container.addRowToTable(new DefaultRow("Row_" + i, cells
						.toArray(new DataCell[cells.size()])));
			}

			exec.checkCanceled();
			exec.setProgress(0.5 + ((++futureTasksCount / inData[0]
					.getRowCount()) / 2));
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
			final FeatureRowResult<T, L> featureRowResult) {

		final List<DataColumnSpec> outcells = new ArrayList<DataColumnSpec>();

		// add a new column for each given feature result
		for (int i = 0; i < featureRowResult.getResults().size(); i++) {
			outcells.add(new DataColumnSpecCreator(DataTableSpec
					.getUniqueColumnName(inSpec, featureRowResult.getResults()
							.get(i).getA()
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

		if (!(spec.containsCompatibleType(ImgPlusValue.class) || spec
				.containsCompatibleType(LabelingValue.class))) {
			throw new InvalidSettingsException(
					"Invalid input spec. At least one image or labeling column must be provided.");
		}

		String selectedImgCol = this.IMG_SELECTION_MODEL.getStringValue();
		String selectedLabelingCol = this.LABELING_SELECTION_MODEL
				.getStringValue();
		if ((selectedImgCol == null) && (selectedLabelingCol == null)) {
			// both empty == first configure
			for (final DataColumnSpec dcs : spec) {
				if (imgPlusFilter().includeColumn(dcs)) {
					this.IMG_SELECTION_MODEL.setStringValue(dcs.getName());
					selectedImgCol = dcs.getName();
					break;
				}
			}

			for (final DataColumnSpec dcs : spec) {
				if (labelingFilter().includeColumn(dcs)) {
					this.LABELING_SELECTION_MODEL.setStringValue(dcs.getName());
					selectedLabelingCol = dcs.getName();
					break;
				}
			}
		}

		this.m_imgPlusCol = selectedImgCol == null ? -1 : spec
				.findColumnIndex(selectedImgCol);
		this.m_labelingCol = selectedLabelingCol == null ? -1 : spec
				.findColumnIndex(selectedLabelingCol);

		if ((-1 == this.m_imgPlusCol) && (-1 == this.m_labelingCol)) {
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
		this.IMG_SELECTION_MODEL.saveSettingsTo(settings);
		this.LABELING_SELECTION_MODEL.saveSettingsTo(settings);
		this.FEATURE_SET_MODEL.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		this.IMG_SELECTION_MODEL.loadSettingsFrom(settings);
		this.LABELING_SELECTION_MODEL.loadSettingsFrom(settings);
		this.FEATURE_SET_MODEL.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		this.IMG_SELECTION_MODEL.validateSettings(settings);
		this.LABELING_SELECTION_MODEL.validateSettings(settings);
		this.FEATURE_SET_MODEL.validateSettings(settings);
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

	/**
	 * Filter which filters all image plus columns from {@link DataTableSpec}.
	 *
	 * @return all image plus columns
	 */
	public static ColumnFilter imgPlusFilter() {
		return new ColumnFilter() {

			@Override
			public boolean includeColumn(final DataColumnSpec colSpec) {
				return colSpec.getType().equals(ImgPlusCell.TYPE);
			}

			@Override
			public String allFilteredMsg() {
				return "No image column available.";
			}
		};
	}

	/**
	 * Filter which filters all labling columns from {@link DataTableSpec}.
	 *
	 * @return all labeling columns
	 */
	public static ColumnFilter labelingFilter() {
		return new ColumnFilter() {

			@Override
			public boolean includeColumn(final DataColumnSpec colSpec) {
				return colSpec.getType().equals(LabelingCell.TYPE);
			}

			@Override
			public String allFilteredMsg() {
				return "No labeling column available.";
			}
		};
	}

}
