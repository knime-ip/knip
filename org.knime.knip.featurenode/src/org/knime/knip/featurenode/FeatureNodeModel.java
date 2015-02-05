package org.knime.knip.featurenode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import net.imagej.ops.OpRef;
import net.imagej.ops.features.AutoResolvingFeatureSet;
import net.imagej.ops.features.FeatureSet;
import net.imglib2.IterableInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
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
import org.knime.knip.featurenode.model.FeatureSetInfo;
import org.knime.knip.featurenode.model.SettingsModelFeatureSet;
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
public class FeatureNodeModel<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends NodeModel {
	/**
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
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		final int numRows = inData[0].getRowCount();
		if (numRows == 0) {
			LOGGER.warn("Empty input table. No other columns created.");
			return inData;
		}

		final List<FeatureSet<IterableInterval<?>, DoubleType>> compiledFeatureSets = compileFeatureSets(
				this.m_imgPlusCol, this.m_labelingCol,
				this.FEATURE_SET_MODEL.getFeatureSets());

		DataTableSpec outSpec = null;
		DataContainer container = null;

		final CloseableRowIterator iterator = inData[0].iterator();
		double rowCount = 0;
		while (iterator.hasNext()) {
			exec.setProgress(rowCount++ / numRows);
			exec.checkCanceled();
			final DataRow row = iterator.next();

			final boolean imgMissing = (this.m_imgPlusCol > -1)
					&& row.getCell(this.m_imgPlusCol).isMissing();
			final boolean labMissing = (this.m_labelingCol > -1)
					&& row.getCell(this.m_labelingCol).isMissing();

			if (imgMissing || labMissing) {
				LOGGER.warn("Missing input value in "
						+ row.getKey().getString()
						+ ". Skipped and deleted this row "
						+ "from output table.");
				continue;
			}
			// for each input iterableinterval
			final List<IterableInterval<?>> iterableIntervals = getIterableIntervals(
					row, this.m_imgPlusCol, this.m_labelingCol);
			for (int i = 0; i < iterableIntervals.size(); i++) {
				final IterableInterval<?> input = iterableIntervals.get(i);
				// results for this iterable interval
				final List<Pair<String, DoubleType>> results = new ArrayList<Pair<String, DoubleType>>();

				// calcuate the features from every featureset
				for (final FeatureSet<IterableInterval<?>, DoubleType> featureSet : compiledFeatureSets) {
					exec.checkCanceled();
					final List<Pair<String, DoubleType>> compute = featureSet
							.getFeatures(input);
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
				final List<DataCell> cells = new ArrayList<DataCell>();

				// store previous data
				for (int k = 0; k < row.getNumCells(); k++) {
					cells.add(row.getCell(k));
				}

				// store new results
				for (final Pair<String, DoubleType> featureResult : results) {
					cells.add(new DoubleCell(featureResult.getB()
							.getRealDouble()));
				}

				// add row
				container.addRowToTable(new DefaultRow(row.getKey() + "_" + i,
						cells.toArray(new DataCell[cells.size()])));
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
	private DataTableSpec createOutputSpec(final DataTableSpec inSpec,
			final List<Pair<String, DoubleType>> results) {

		final List<DataColumnSpec> outcells = new ArrayList<DataColumnSpec>();

		// add all old columns
		for (int i = 0; i < inSpec.getNumColumns(); i++) {
			outcells.add(inSpec.getColumnSpec(i));
		}

		// add a new column for each given feature result
		for (int i = 0; i < results.size(); i++) {
			outcells.add(new DataColumnSpecCreator(DataTableSpec
					.getUniqueColumnName(inSpec, results.get(i).getA() + "_"
							+ i), DoubleCell.TYPE).createSpec());
		}

		return new DataTableSpec(outcells.toArray(new DataColumnSpec[outcells
				.size()]));
	}

	/**
	 * Creates the {@link FeatureSet} which were added in the
	 * {@link FeatureNodeDialog}
	 *
	 * @param imgCol
	 *            the index of the image column
	 * @param labelingCol
	 *            the index of the labeling column
	 * @param list
	 * @return
	 * @throws ModuleException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<FeatureSet<IterableInterval<?>, DoubleType>> compileFeatureSets(
			final int imgCol, final int labelingCol,
			final List<FeatureSetInfo> list) throws ModuleException {

		if ((-1 == imgCol) && (-1 == labelingCol)) {
			return new ArrayList<FeatureSet<IterableInterval<?>, DoubleType>>();
		}

		final List<FeatureSet<IterableInterval<?>, DoubleType>> compiledFeatureSets = new ArrayList<FeatureSet<IterableInterval<?>, DoubleType>>();

		// if img is set and not a labeling we can use FeatureSet<Img>
		// otherwise FeatureSet<IterableInterval>
		for (final FeatureSetInfo fsi : list) {
			if ((-1 != imgCol) && (-1 == labelingCol)) {

				final FeatureSet<IterableInterval<?>, DoubleType> createInstance = OpsGateway
						.getPluginService()
						.createInstance(
								new PluginInfo<FeatureSet>(fsi
										.getFeatureSetClass(), FeatureSet.class));

				if (AutoResolvingFeatureSet.class
						.isAssignableFrom(createInstance.getClass())) {
					final AutoResolvingFeatureSet<?, ?> arfs = (AutoResolvingFeatureSet<?, ?>) createInstance;

					final Set<OpRef<?>> ops = new HashSet<OpRef<?>>();
					for (final Entry<Class<?>, Boolean> entry : fsi
							.getSelectedFeatures().entrySet()) {
						if (!entry.getValue()) {
							continue;
						}

						ops.add(new OpRef(entry.getKey()));
					}

					arfs.setOutputOps(ops);
				}

				final Module module = OpsGateway.getOpService()
						.info(createInstance).createModule();

				for (final Entry<String, Object> fieldNameAndValue : fsi
						.getFieldNamesAndValues().entrySet()) {
					module.setInput(fieldNameAndValue.getKey(),
							fieldNameAndValue.getValue());
				}

				compiledFeatureSets.add(createInstance);
			} else {
				final FeatureSet<IterableInterval<?>, DoubleType> createInstance = OpsGateway
						.getPluginService()
						.createInstance(
								new PluginInfo<FeatureSet>(fsi
										.getFeatureSetClass(), FeatureSet.class));

				if (AutoResolvingFeatureSet.class
						.isAssignableFrom(createInstance.getClass())) {
					final AutoResolvingFeatureSet<?, ?> arfs = (AutoResolvingFeatureSet<?, ?>) createInstance;

					final Set<OpRef<?>> ops = new HashSet<OpRef<?>>();
					for (final Entry<Class<?>, Boolean> entry : fsi
							.getSelectedFeatures().entrySet()) {
						if (!entry.getValue()) {
							continue;
						}

						ops.add(new OpRef(entry.getKey()));
					}

					arfs.setOutputOps(ops);
				}

				final Module module = OpsGateway.getOpService()
						.info(createInstance).createModule();

				for (final Entry<String, Object> fieldNameAndValue : fsi
						.getFieldNamesAndValues().entrySet()) {
					module.setInput(fieldNameAndValue.getKey(),
							fieldNameAndValue.getValue());
				}

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
	private List<IterableInterval<?>> getIterableIntervals(final DataRow row,
			final int imgCol, final int labelingCol) {

		if ((-1 == imgCol) && (-1 == labelingCol)) {
			return new ArrayList<IterableInterval<?>>();
		}

		final List<IterableInterval<?>> inputs = new ArrayList<IterableInterval<?>>();

		// both set
		if ((-1 != imgCol) && (-1 != labelingCol)) {
			final Img<T> img = ((ImgPlusCell<T>) row.getCell(imgCol))
					.getImgPlus();
			final Labeling<L> labeling = ((LabelingCell<L>) row
					.getCell(labelingCol)).getLabeling();

			for (final L label : labeling.getLabels()) {
				final IterableRegionOfInterest iiROI = labeling
						.getIterableRegionOfInterest(label);

				final IterableInterval<?> ii = iiROI
						.getIterableIntervalOverROI(img);
				inputs.add(ii);
			}
		}
		// only labeling set
		else if (-1 != labelingCol) {
			final Labeling<L> labeling = ((LabelingCell<L>) row
					.getCell(labelingCol)).getLabeling();

			for (final L label : labeling.getLabels()) {
				final IterableInterval<LabelingType<L>> ii = labeling
						.getIterableRegionOfInterest(label)
						.getIterableIntervalOverROI(labeling);

				final IterableInterval<BitType> convert = Converters.convert(
						ii, new Converter<LabelingType<L>, BitType>() {

							@Override
							public void convert(final LabelingType<L> arg0,
									final BitType arg1) {
								arg1.set(!arg0.getLabeling().isEmpty());
							}
						}, new BitType());

				inputs.add(convert);
			}
		}
		// only image set
		else {
			final Img<T> img = ((ImgPlusCell<T>) row.getCell(imgCol))
					.getImgPlus();
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
