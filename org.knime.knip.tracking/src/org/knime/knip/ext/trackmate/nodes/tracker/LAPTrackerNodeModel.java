package org.knime.knip.ext.trackmate.nodes.tracker;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.logic.BitType;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.ext.trackmate.data.GenericLapTracker;
import org.knime.knip.ext.trackmate.data.TrackedNode;

import fiji.plugin.trackmate.TrackableObjectCollection;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.TrackingUtils;

public class LAPTrackerNodeModel extends NodeModel implements
		BufferedDataTableHolder {

	public enum LAPTrackerAlgorithm {
		HUNGARIAN("Hungarian"), MUNKRESKUHN("Munkres Kuhn");

		private String name;

		private LAPTrackerAlgorithm(String describingName) {
			this.name = describingName;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/*
	 * KNIME SETTINGS MODELS
	 */
	private final SettingsModelString m_sourceLabelingColumn = LAPTrackerSettingsModels
			.createSourceLabelingSettingsModel();

	private final SettingsModelFilterString m_columns = LAPTrackerSettingsModels
			.createColumnSelectionModel();

	private final SettingsModelString m_timeAxisModel = LAPTrackerSettingsModels
			.createTimeAxisModel();

	private final SettingsModelString m_bitMaskColumnModel = LAPTrackerSettingsModels
			.createBitMaskModel();

	private final SettingsModelString m_labelColumnModel = LAPTrackerSettingsModels
			.createLabelModel();

	/*
	 * TRACKMATE SETTINGS
	 */

	private final SettingsModelString m_trackingAlgorithmModel = LAPTrackerSettingsModels
			.createTrackingAlgorithmModel();

	private final SettingsModelBoolean m_allowGapClosingModel = LAPTrackerSettingsModels
			.createAllowGapClosingModel();

	private final SettingsModelBoolean m_allowMergingModel = LAPTrackerSettingsModels
			.createAllowMergingModel();

	private final SettingsModelBoolean m_allowSplittingModel = LAPTrackerSettingsModels
			.createAllowSplittingModel();

	private final SettingsModelInteger m_gapClosingMaxFrameModel = LAPTrackerSettingsModels
			.createMaxFrameGapClosingModel();

	private final SettingsModelDouble m_alternativeLinkingCostFactor = LAPTrackerSettingsModels
			.createAlternativeLinkingCostFactor();

	private final SettingsModelDouble m_cutoffPercentileModel = LAPTrackerSettingsModels
			.createCutoffPercentileModel();

	private final SettingsModelDouble m_gapClosingMaxDistanceModel = LAPTrackerSettingsModels
			.createGapClosingMaxDistanceModel();

	private final SettingsModelDouble m_linkingMaxDistanceModel = LAPTrackerSettingsModels
			.createLinkingMaxDistanceModel();

	private final SettingsModelDouble m_mergingMaxDistanceModel = LAPTrackerSettingsModels
			.createMergingMaxDistance();

	private final SettingsModelDouble m_splittingMaxDistance = LAPTrackerSettingsModels
			.createSplittingMaxDistance();

	private BufferedDataTable m_resultTable;

	/*
	 * Node Begins
	 */

	protected LAPTrackerNodeModel() {
		super(1, 1);
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		// simply to check whether the input changed
		getSelectedColumnIndices(inSpecs[0]);
		getColIndices(
				m_labelColumnModel,
				StringValue.class,
				inSpecs[0],
				getColIndices(m_bitMaskColumnModel, ImgPlusValue.class,
						inSpecs[0]));

		getColIndices(m_sourceLabelingColumn, LabelingValue.class, inSpecs[0]);

		return createOutSpec();
	}

	private DataTableSpec[] createOutSpec() {
		return new DataTableSpec[] { new DataTableSpec(
				new DataColumnSpecCreator("Tracking", LabelingCell.TYPE)
						.createSpec()) };
	}

	@SuppressWarnings("unchecked")
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		// get all information needed from table
		final DataTableSpec spec = inData[0].getSpec();
		final String[] columnNames = spec.getColumnNames();
		final int[] featureIndices = getSelectedColumnIndices(spec);

		// get bitmask & time
		int bitMaskColumnIdx = getColIndices(m_bitMaskColumnModel,
				ImgPlusValue.class, spec);

		int labelIdx = getColIndices(m_labelColumnModel, StringValue.class,
				spec, bitMaskColumnIdx);

		int sourceLabelingIdx = getColIndices(m_sourceLabelingColumn,
				LabelingValue.class, spec);

		// time axis
		final AxisType timeAxis = Axes.get(m_timeAxisModel.getStringValue());

		// Source labeling. Important: Since now only one labeling is allowed.
		Labeling<?> sourceLabeling = null;
		String sourceLabelingName = "";
		LabelingMetadata sourceLabelingMetadata = null;

		// Set-Up the tracker
		GenericLapTracker<String> tracker = new GenericLapTracker<String>(
				EnumUtils.valueForName(
						m_trackingAlgorithmModel.getStringValue(),
						LAPTrackerAlgorithm.values()));
		initTracker(tracker);

		TrackableObjectCollection<TrackedNode<String>> trackedNodes = new TrackableObjectCollection<TrackedNode<String>>();
		for (DataRow row : inData[0]) {
			exec.checkCanceled();
			ImgPlusValue<BitType> bitMaskValue = ((ImgPlusValue<BitType>) row
					.getCell(bitMaskColumnIdx));
			final ImgPlus<BitType> bitMask = bitMaskValue.getImgPlus();
			final String label = ((StringValue) row.getCell(labelIdx))
					.getStringValue();

			// get time dimension
			final int timeIdx = bitMask.dimensionIndex(timeAxis);

			if (timeIdx == -1) {
				throw new IllegalArgumentException(
						"Tracking dimension doesn't exist in your BitMask. Please choose the correct tracking dimension!");
			}

			// here: if source labeling is null set it. only one source is
			// allowed since now
			if (sourceLabeling == null) {
				LabelingValue<?> labValue = ((LabelingValue<?>) row
						.getCell(sourceLabelingIdx));
				sourceLabeling = labValue.getLabeling();
				sourceLabelingName = labValue.getLabelingMetadata().getName();
				sourceLabelingMetadata = labValue.getLabelingMetadata();
			} else if (!sourceLabelingName
					.equalsIgnoreCase(((LabelingValue<?>) row
							.getCell(sourceLabelingIdx)).getLabelingMetadata()
							.getName())) {
				throw new IllegalArgumentException(
						"Since now only labels from one Labeling are allowed. Use KNIME Loops!");
			}

			final Map<String, Double> featureMap = new HashMap<String, Double>();
			for (int idx : featureIndices) {
				featureMap.put(columnNames[idx],
						((DoubleValue) row.getCell(idx)).getDoubleValue());
			}

			// add the node
			TrackedNode<String> trackedNode = new TrackedNode<String>(bitMask,
					bitMaskValue.getMinimum(), label, timeIdx, featureMap);

			trackedNodes.add(trackedNode, trackedNode.frame());
		}

		// Start tracking
		tracker.setTarget(trackedNodes, initTracker(tracker));
		tracker.setNumThreads(Runtime.getRuntime().availableProcessors());
		tracker.process();

		// use the results and create output labeling
		// create tracks
		final ConnectivityInspector<TrackedNode<String>, DefaultWeightedEdge> inspector = new ConnectivityInspector<TrackedNode<String>, DefaultWeightedEdge>(
				tracker.getResult());
		final List<Set<TrackedNode<String>>> unsortedSegments = inspector
				.connectedSets();
		final ArrayList<SortedSet<TrackedNode<String>>> trackSegments = new ArrayList<SortedSet<TrackedNode<String>>>(
				unsortedSegments.size());

		for (final Set<TrackedNode<String>> set : unsortedSegments) {
			final SortedSet<TrackedNode<String>> sortedSet = new TreeSet<TrackedNode<String>>(
					TrackingUtils.frameComparator());
			sortedSet.addAll(set);
			trackSegments.add(sortedSet);
		}

		int trackCtr = 0;
		Labeling<String> res = sourceLabeling.<String> factory().create(
				sourceLabeling);
		RandomAccess<LabelingType<String>> resAccess = res.randomAccess();
		final int numDims = resAccess.numDimensions();
		for (SortedSet<TrackedNode<String>> track : trackSegments) {
			for (TrackedNode<String> node : track) {
				ImgPlus<BitType> bitMask = node.bitMask();
				Cursor<BitType> bitMaskCursor = bitMask.cursor();
				while (bitMaskCursor.hasNext()) {
					if (!bitMaskCursor.next().get())
						continue;

					for (int d = 0; d < numDims; d++) {
						resAccess.setPosition(bitMaskCursor.getLongPosition(d)
								+ node.offset(d), d);
					}
					// set all the important information
					List<String> labeling = new ArrayList<String>(resAccess
							.get().getLabeling());

					// labeling.add(node.label());
					labeling.add("Track: " + trackCtr);
					resAccess.get().setLabeling(labeling);

				}
			}
			trackCtr++;
		}

		LabelingCellFactory labelingCellFactory = new LabelingCellFactory(exec);
		BufferedDataContainer container = exec
				.createDataContainer(createOutSpec()[0]);

		container.addRowToTable(new DefaultRow(sourceLabelingName,
				labelingCellFactory.createCell(res, sourceLabelingMetadata)));
		container.close();

		return new BufferedDataTable[] { m_resultTable = container.getTable() };
	}

	private Map<String, Object> initTracker(GenericLapTracker<String> tracker) {
		// Set the tracking settings
		final Map<String, Object> settings = LAPUtils
				.getDefaultLAPSettingsMap();
		settings.put(KEY_LINKING_MAX_DISTANCE,
				m_linkingMaxDistanceModel.getDoubleValue());
		settings.put(KEY_ALLOW_GAP_CLOSING,
				m_allowGapClosingModel.getBooleanValue());
		settings.put(KEY_ALLOW_TRACK_MERGING,
				m_allowMergingModel.getBooleanValue());
		settings.put(KEY_ALLOW_TRACK_MERGING,
				m_allowMergingModel.getBooleanValue());
		settings.put(KEY_ALTERNATIVE_LINKING_COST_FACTOR,
				m_alternativeLinkingCostFactor.getDoubleValue());
		settings.put(KEY_CUTOFF_PERCENTILE,
				m_cutoffPercentileModel.getDoubleValue());
		settings.put(KEY_GAP_CLOSING_MAX_FRAME_GAP,
				m_gapClosingMaxFrameModel.getIntValue());
		settings.put(KEY_GAP_CLOSING_MAX_DISTANCE,
				m_gapClosingMaxDistanceModel.getDoubleValue());
		settings.put(KEY_LINKING_MAX_DISTANCE,
				m_linkingMaxDistanceModel.getDoubleValue());
		settings.put(KEY_MERGING_MAX_DISTANCE,
				m_mergingMaxDistanceModel.getDoubleValue());
		settings.put(KEY_SPLITTING_MAX_DISTANCE,
				m_splittingMaxDistance.getDoubleValue());

		return settings;
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_bitMaskColumnModel.saveSettingsTo(settings);
		m_columns.saveSettingsTo(settings);
		m_labelColumnModel.saveSettingsTo(settings);
		m_timeAxisModel.saveSettingsTo(settings);
		m_allowGapClosingModel.saveSettingsTo(settings);
		m_allowMergingModel.saveSettingsTo(settings);
		m_alternativeLinkingCostFactor.saveSettingsTo(settings);
		m_cutoffPercentileModel.saveSettingsTo(settings);
		m_linkingMaxDistanceModel.saveSettingsTo(settings);
		m_gapClosingMaxFrameModel.saveSettingsTo(settings);
		m_mergingMaxDistanceModel.saveSettingsTo(settings);
		m_splittingMaxDistance.saveSettingsTo(settings);
		m_trackingAlgorithmModel.saveSettingsTo(settings);
		m_allowSplittingModel.saveSettingsTo(settings);
		m_gapClosingMaxDistanceModel.saveSettingsTo(settings);
		m_sourceLabelingColumn.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_bitMaskColumnModel.validateSettings(settings);
		m_columns.validateSettings(settings);
		m_labelColumnModel.validateSettings(settings);
		m_timeAxisModel.validateSettings(settings);
		m_allowGapClosingModel.validateSettings(settings);
		m_allowMergingModel.validateSettings(settings);
		m_alternativeLinkingCostFactor.validateSettings(settings);
		m_cutoffPercentileModel.validateSettings(settings);
		m_linkingMaxDistanceModel.validateSettings(settings);
		m_gapClosingMaxFrameModel.validateSettings(settings);
		m_mergingMaxDistanceModel.validateSettings(settings);
		m_splittingMaxDistance.validateSettings(settings);
		m_trackingAlgorithmModel.validateSettings(settings);
		m_allowSplittingModel.validateSettings(settings);
		m_gapClosingMaxDistanceModel.validateSettings(settings);
		m_sourceLabelingColumn.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_bitMaskColumnModel.loadSettingsFrom(settings);
		m_columns.loadSettingsFrom(settings);
		m_labelColumnModel.loadSettingsFrom(settings);
		m_timeAxisModel.loadSettingsFrom(settings);
		m_allowGapClosingModel.loadSettingsFrom(settings);
		m_allowMergingModel.loadSettingsFrom(settings);
		m_alternativeLinkingCostFactor.loadSettingsFrom(settings);
		m_cutoffPercentileModel.loadSettingsFrom(settings);
		m_linkingMaxDistanceModel.loadSettingsFrom(settings);
		m_gapClosingMaxFrameModel.loadSettingsFrom(settings);
		m_mergingMaxDistanceModel.loadSettingsFrom(settings);
		m_splittingMaxDistance.loadSettingsFrom(settings);
		m_trackingAlgorithmModel.loadSettingsFrom(settings);
		m_allowSplittingModel.loadSettingsFrom(settings);
		m_gapClosingMaxDistanceModel.loadSettingsFrom(settings);
		m_sourceLabelingColumn.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// TODO Auto-generated method stub

	}

	/* Helper to collect all columns of DoubleType */
	private void collectAllColumns(final List<String> colNames,
			final DataTableSpec spec) {
		colNames.clear();
		for (final DataColumnSpec c : spec) {
			if (c.getType().isCompatible(DoubleValue.class)) {
				colNames.add(c.getName());
			}
		}
		if (colNames.size() == 0) {
			return;
		}
	}

	/*
	 * Retrieves the selected column indices from the given DataTableSpec and
	 * the column selection. If the selection turned out to be invalid, all
	 * columns are selected.
	 */
	protected int[] getSelectedColumnIndices(final DataTableSpec inSpec) {
		final List<String> colNames;
		if ((m_columns.getIncludeList().size() == 0)
				|| m_columns.isKeepAllSelected()) {
			colNames = new ArrayList<String>();
			collectAllColumns(colNames, inSpec);
			m_columns.setIncludeList(colNames);

		} else {
			colNames = new ArrayList<String>();
			colNames.addAll(m_columns.getIncludeList());
			if (!validateColumnSelection(colNames, inSpec)) {
				setWarningMessage("Invalid column selection. All columns are selected!");
				collectAllColumns(colNames, inSpec);
			}
		}

		// get column indices
		final List<Integer> colIndices = new ArrayList<Integer>(colNames.size());
		for (int i = 0; i < colNames.size(); i++) {
			final int colIdx = inSpec.findColumnIndex(colNames.get(i));
			if (colIdx == -1) {
				// can not occur, actually
				throw new IllegalStateException("this should really not happen");
			} else {
				colIndices.add(colIdx);
			}
		}

		final int[] colIdx = new int[colIndices.size()];
		for (int i = 0; i < colIdx.length; i++) {
			colIdx[i] = colIndices.get(i);
		}

		return colIdx;
	}

	/* Checks if a column is not present in the DataTableSpec */
	private boolean validateColumnSelection(final List<String> colNames,
			final DataTableSpec spec) {
		for (int i = 0; i < colNames.size(); i++) {
			final int colIdx = spec.findColumnIndex(colNames.get(i));
			if (colIdx == -1) {
				return false;
			}
		}
		return true;
	}

	protected int getColIndices(SettingsModelString model,
			Class<? extends DataValue> clazz, final DataTableSpec inSpec,
			Integer... excludeCols) throws InvalidSettingsException {

		int colIdx = -1;
		if (model.getStringValue() != null) {
			colIdx = NodeTools.autoColumnSelection(inSpec, model, clazz,
					this.getClass(), excludeCols);
		}
		return colIdx;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedDataTable[] getInternalTables() {
		return new BufferedDataTable[] { m_resultTable };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {
		m_resultTable = tables[0];
	}
}
