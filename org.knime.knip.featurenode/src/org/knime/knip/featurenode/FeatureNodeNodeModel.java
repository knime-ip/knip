package org.knime.knip.featurenode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.imagej.ops.features.FeatureResult;
import net.imagej.ops.features.FeatureSet;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.roi.IterableRegionOfInterest;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.plugin.PluginInfo;

/**
 * This is the model implementation of FeatureNode.
 * 
 *
 * @author Daniel Seebacher
 * @author Tim-Oliver Buchholz
 */
public class FeatureNodeNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends NodeModel {
	/**
	 * The logger instance.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FeatureNodeNodeModel.class);

	/**
	 * @return Settings model for selected labeling.
	 */
	static SettingsModelString createLabelingSelectionModel() {
		return new SettingsModelString("m_labeling_settings_model", null);
	}

	/**
	 * @return Settings model for selected image.
	 */
	static SettingsModelString createImgSelectionModel() {
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
	private SettingsModelString IMG_SELECTION_MODEL = createImgSelectionModel();

	/**
	 * Labeling selection model.
	 */
	private SettingsModelString LABELING_SELECTION_MODEL = createLabelingSelectionModel();

	/**
	 * Feature set model.
	 */
	private SettingsModelFeatureSet FEATURE_SET_MODEL = createFeatureSetsModel();

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
	protected FeatureNodeNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		
		if (inData[0].getRowCount() == 0) {
			LOGGER.warn("Empty input table. No other columns created.");
			return inData;
		}

		List<FeatureSet<IterableInterval<?>>> compiledFeatureSets = compileFeatureSets(
				m_imgPlusCol, m_labelingCol, FEATURE_SET_MODEL.getFeatureSets());

		DataTableSpec outSpec = null;
		DataContainer container = null;

		CloseableRowIterator iterator = inData[0].iterator();
		while (iterator.hasNext()) {
			exec.checkCanceled();
			DataRow row = iterator.next();

			boolean imgMissing = m_imgPlusCol > -1
					&& row.getCell(m_imgPlusCol).isMissing();
			boolean labMissing = m_labelingCol > -1
					&& row.getCell(m_labelingCol).isMissing();

			if (imgMissing || labMissing) {
				LOGGER.warn("Missing input value in "
						+ row.getKey().getString()
						+ ". Skipped and deleted this row "
						+ "from output table.");
				continue;
			}
			// for each input iterableinterval
			for (IterableInterval<?> input : getIterableIntervals(row,
					m_imgPlusCol, m_labelingCol)) {

				// results for this iterable interval
				List<FeatureResult> results = new ArrayList<FeatureResult>();

				// calcuate the features from every featureset
				for (FeatureSet<IterableInterval<?>> featureSet : compiledFeatureSets) {
					exec.checkCanceled();
					featureSet.setInput(input);
					featureSet.run();
					List<FeatureResult> compute = featureSet.getOutput();

					results.addAll(compute);
				}

				// first row, if outspec is null create one from scratch and
				// create container
				if (outSpec == null) {
					outSpec = createOutputSpec(inData[0].getDataTableSpec(),
							results);
					container = exec.createDataContainer(outSpec);
				}

				// save results of this iterableinterval in a row
				List<DataCell> cells = new ArrayList<DataCell>();

				// store previous data
				for (int i = 0; i < row.getNumCells(); i++) {
					cells.add(row.getCell(i));
				}

				// store new results
				for (FeatureResult featureResult : results) {
					cells.add(new DoubleCell(featureResult.getValue()));
				}

				// add row
				container.addRowToTable(new DefaultRow(row.getKey(), cells
						.toArray(new DataCell[cells.size()])));
			}
		}

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
	 * @param results
	 *            a list of {@link FeatureResult}, each result will result in
	 *            one extra column
	 * @return a new {@link DataTableSpec} existing of all columns from the
	 *         {@link DataTableSpec} and a column for each given
	 *         {@link FeatureResult}
	 */
	private DataTableSpec createOutputSpec(DataTableSpec inSpec,
			List<FeatureResult> results) {

		List<DataColumnSpec> outcells = new ArrayList<DataColumnSpec>();

		// add all old columns
		for (int i = 0; i < inSpec.getNumColumns(); i++) {
			outcells.add(inSpec.getColumnSpec(i));
		}

		// add a new column for each given feature result
		for (int i = 0; i < results.size(); i++) {
			outcells.add(new DataColumnSpecCreator(DataTableSpec
					.getUniqueColumnName(inSpec, results.get(i).getName() + "_"
							+ i), DoubleCell.TYPE).createSpec());
		}

		return new DataTableSpec(outcells.toArray(new DataColumnSpec[outcells
				.size()]));
	}

	/**
	 * Creates the {@link FeatureSet} which were added in the
	 * {@link FeatureNodeNodeDialog}
	 * 
	 * @param imgCol
	 *            the index of the image column
	 * @param labelingCol
	 *            the index of the labeling column
	 * @param featureSetsData
	 * @return
	 * @throws ModuleException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<FeatureSet<IterableInterval<?>>> compileFeatureSets(
			int imgCol, int labelingCol,
			List<Pair<Class<?>, Map<String, Object>>> featureSetsData)
			throws ModuleException {

		if (-1 == imgCol && -1 == labelingCol) {
			return new ArrayList<FeatureSet<IterableInterval<?>>>();
		}

		List<FeatureSet<IterableInterval<?>>> compiledFeatureSets = new ArrayList<FeatureSet<IterableInterval<?>>>();

		// if img is set and not a labeling we can use FeatureSet<Img>
		// otherwise FeatureSet<IterableInterval>
		for (Pair<Class<?>, Map<String, Object>> featureSetData : featureSetsData) {
			if (-1 != imgCol && -1 == labelingCol) {

				FeatureSet<IterableInterval<?>> createInstance = OpsGateway
						.getPluginService().createInstance(
								new PluginInfo<FeatureSet>(
										(Class<FeatureSet<Img>>) featureSetData
												.getA(), FeatureSet.class));

				Module module = OpsGateway.getOpService().info(createInstance)
						.createModule();
				module.setInputs(featureSetData.getB());

				compiledFeatureSets.add(createInstance);
			} else {
				FeatureSet<IterableInterval<?>> createInstance = OpsGateway
						.getPluginService()
						.createInstance(
								new PluginInfo<FeatureSet>(
										(Class<FeatureSet<IterableInterval>>) featureSetData
												.getA(), FeatureSet.class));

				Module module = OpsGateway.getOpService().info(createInstance)
						.createModule();
				module.setInputs(featureSetData.getB());

				compiledFeatureSets.add(createInstance);
			}
		}

		return compiledFeatureSets;
	}

	/**
	 * Extracts every {@link IterableInterval} from the given input
	 * {@link DataRow}
	 * 
	 * @param row
	 *            the input {@link DataRow}
	 * @param imgCol
	 *            the index of the image column
	 * @param labelingCol
	 *            the index of the labeling column
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<IterableInterval<?>> getIterableIntervals(DataRow row,
			int imgCol, int labelingCol) {

		if (-1 == imgCol && -1 == labelingCol) {
			return new ArrayList<IterableInterval<?>>();
		}

		List<IterableInterval<?>> inputs = new ArrayList<IterableInterval<?>>();

		// both set
		if (-1 != imgCol && -1 != labelingCol) {
			Img<T> img = ((ImgPlusCell<T>) row.getCell(imgCol)).getImgPlus();
			Labeling<L> labeling = ((LabelingCell<L>) row.getCell(labelingCol))
					.getLabeling();

			for (L label : labeling.getLabels()) {
				IterableRegionOfInterest iiROI = labeling
						.getIterableRegionOfInterest(label);

				IterableInterval<?> ii = iiROI.getIterableIntervalOverROI(img);
				inputs.add(ii);
			}
		}
		// only labeling set
		else if (-1 != labelingCol) {
			Labeling<L> labeling = ((LabelingCell<L>) row.getCell(labelingCol))
					.getLabeling();

			for (L label : labeling.getLabels()) {
				IterableInterval<?> ii = labeling.getIterableRegionOfInterest(
						label).getIterableIntervalOverROI(labeling);
				inputs.add(ii);
			}
		}
		// only image set
		else {
			Img<T> img = ((ImgPlusCell<T>) row.getCell(imgCol)).getImgPlus();
			inputs.add(img);
		}

		return inputs;
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

		DataTableSpec spec = inSpecs[0];

		if (!(spec.containsCompatibleType(ImgPlusValue.class) || spec
				.containsCompatibleType(LabelingValue.class))) {
			throw new InvalidSettingsException(
					"Invalid input spec. At least one image or labeling column must be provided.");
		}

		String selectedImgCol = IMG_SELECTION_MODEL.getStringValue();
		String selectedLabelingCol = LABELING_SELECTION_MODEL.getStringValue();
		if (selectedImgCol == null && selectedLabelingCol == null) {
			// both empty == first configure
			for (DataColumnSpec dcs : spec) {
				if (imgPlusFilter().includeColumn(dcs)) {
					IMG_SELECTION_MODEL.setStringValue(dcs.getName());
					selectedImgCol = dcs.getName();
					break;
				}
			}

			for (DataColumnSpec dcs : spec) {
				if (labelingFilter().includeColumn(dcs)) {
					LABELING_SELECTION_MODEL.setStringValue(dcs.getName());
					selectedLabelingCol = dcs.getName();
					break;
				}
			}
		}

		m_imgPlusCol = selectedImgCol == null ? -1 : spec
				.findColumnIndex(selectedImgCol);
		m_labelingCol = selectedLabelingCol == null ? -1 : spec
				.findColumnIndex(selectedLabelingCol);

		if (-1 == m_imgPlusCol && -1 == m_labelingCol) {
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
		IMG_SELECTION_MODEL.saveSettingsTo(settings);
		LABELING_SELECTION_MODEL.saveSettingsTo(settings);
		FEATURE_SET_MODEL.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		IMG_SELECTION_MODEL.loadSettingsFrom(settings);
		LABELING_SELECTION_MODEL.loadSettingsFrom(settings);
		FEATURE_SET_MODEL.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		IMG_SELECTION_MODEL.validateSettings(settings);
		LABELING_SELECTION_MODEL.validateSettings(settings);
		FEATURE_SET_MODEL.validateSettings(settings);
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
			public boolean includeColumn(DataColumnSpec colSpec) {
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
			public boolean includeColumn(DataColumnSpec colSpec) {
				return colSpec.getType().equals(LabelingCell.TYPE);
			}

			@Override
			public String allFilteredMsg() {
				return "No labeling column available.";
			}
		};
	}
}
