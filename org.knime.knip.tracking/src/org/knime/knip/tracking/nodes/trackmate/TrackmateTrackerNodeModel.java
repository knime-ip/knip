package org.knime.knip.tracking.nodes.trackmate;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
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
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.ops.operation.iterableinterval.unary.Centroid;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.logic.BitType;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.tracking.data.TrackedNode;
import org.knime.knip.tracking.nodes.trackmate.TrackmateTrackerSettingsModels.TrackMateTrackFeature;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackmateConstants;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.tracking.DefaultTOCollection;
import fiji.plugin.trackmate.tracking.TrackableObjectCollection;
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPTracker;
import fiji.plugin.trackmate.util.LAPUtils;
import fiji.plugin.trackmate.util.TrackableObjectUtils;

/**
 * Node Model for the Trackmate Tracker Node.
 *
 * @author gabriel
 * @author christian
 *
 */
public class TrackmateTrackerNodeModel extends NodeModel implements
        BufferedDataTableHolder {

    /*
     * KNIME SETTINGS MODELS
     */
    private final SettingsModelString m_sourceLabelingColumn =
            TrackmateTrackerSettingsModels.createSourceLabelingSettingsModel();

    private final SettingsModelFilterString m_columns =
            TrackmateTrackerSettingsModels.createColumnSelectionModel();

    private final SettingsModelString m_timeAxisModel =
            TrackmateTrackerSettingsModels.createTimeAxisModel();

    private final SettingsModelString m_bitMaskColumnModel =
            TrackmateTrackerSettingsModels.createBitMaskModel();

    private final SettingsModelString m_labelColumnModel =
            TrackmateTrackerSettingsModels.createLabelModel();

    private final SettingsModelBoolean m_attachSourceLabelings =
            TrackmateTrackerSettingsModels.createAttachSourceLabelingsModel();

    private final SettingsModelBoolean m_useCustomTrackPrefix =
            TrackmateTrackerSettingsModels.createUseCustomTrackPrefixModel();

    private final SettingsModelString m_customTrackPrefix =
            TrackmateTrackerSettingsModels.createCustomTrackPrefixModel();

    private final SettingsModelBoolean m_calculateTrackFeaturesModel =
            TrackmateTrackerSettingsModels.createCalculateTrackFeaturesModel();

    /*
     * TRACKMATE SETTINGS MODELS
     */
    private final SettingsModelBoolean m_allowGapClosingModel =
            TrackmateTrackerSettingsModels.createAllowGapClosingModel();

    private final SettingsModelBoolean m_allowMergingModel =
            TrackmateTrackerSettingsModels.createAllowMergingModel();

    private final SettingsModelBoolean m_allowSplittingModel =
            TrackmateTrackerSettingsModels.createAllowSplittingModel();

    private final SettingsModelInteger m_gapClosingMaxFrameModel =
            TrackmateTrackerSettingsModels.createMaxFrameGapClosingModel();

    private final SettingsModelDouble m_alternativeLinkingCostFactor =
            TrackmateTrackerSettingsModels.createAlternativeLinkingCostFactor();

    private final SettingsModelDouble m_cutoffPercentileModel =
            TrackmateTrackerSettingsModels.createCutoffPercentileModel();

    private final SettingsModelDouble m_gapClosingMaxDistanceModel =
            TrackmateTrackerSettingsModels.createGapClosingMaxDistanceModel();

    private final SettingsModelDouble m_linkingMaxDistanceModel =
            TrackmateTrackerSettingsModels.createLinkingMaxDistanceModel();

    private final SettingsModelDouble m_mergingMaxDistanceModel =
            TrackmateTrackerSettingsModels.createMergingMaxDistance();

    private final SettingsModelDouble m_splittingMaxDistance =
            TrackmateTrackerSettingsModels.createSplittingMaxDistance();

    /*
     * GLOBAL MEMBERS
     */
    private BufferedDataTable m_labelingTable;

    private BufferedDataTable m_trackFeatureTable;

    private List<SettingsModel> m_settingsModels;

    /**********************
     * NODE SETUP METHODS *
     **********************/

    /**
     * Constructor.
     */
    protected TrackmateTrackerNodeModel() {
        // SECOND OUT PORT FOR FEATURES
        super(1, 2);

        // for state consistency:
        m_customTrackPrefix.setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        // simply to check whether the input changed
        getSelectedColumnIndices(inSpecs[0]);
        getColIndices(
                m_labelColumnModel,
                StringValue.class,
                inSpecs[0],
                getColIndices(m_bitMaskColumnModel, ImgPlusValue.class,
                        inSpecs[0]),
                getColIndices(m_sourceLabelingColumn, LabelingValue.class,
                        inSpecs[0]));

        return createOutSpec();
    }

    /**
     * @return The output DataTableSpecs for this node.
     */
    private DataTableSpec[] createOutSpec() {
        final DataTableSpec[] dataTableSpecs;

        // Create the outspec depending on the calculate track features setting.
        if (m_calculateTrackFeaturesModel.getBooleanValue()) {

            final TrackMateTrackFeature[] values =
                    TrackMateTrackFeature.values();
            final ArrayList<DataColumnSpec> colSpecs =
                    new ArrayList<>(values.length + 2);

            colSpecs.add(new DataColumnSpecCreator("TrackID", StringCell.TYPE)
                    .createSpec());
            colSpecs.add(new DataColumnSpecCreator("Bitmask", ImgPlusCell.TYPE)
                    .createSpec());

            for (final TrackMateTrackFeature feature : values) {
                colSpecs.add(new DataColumnSpecCreator(feature.toString(),
                        DoubleCell.TYPE).createSpec());
            }
            dataTableSpecs =
                    new DataTableSpec[] {
                            new DataTableSpec(new DataColumnSpecCreator(
                                    "Tracking", LabelingCell.TYPE).createSpec()),
                            new DataTableSpec(
                                    colSpecs.toArray(new DataColumnSpec[colSpecs
                                            .size()])) };
        } else {
            dataTableSpecs =
                    new DataTableSpec[] {
                            new DataTableSpec(new DataColumnSpecCreator(
                                    "Tracking", LabelingCell.TYPE).createSpec()),
                            new DataTableSpec() };
        }
        return dataTableSpecs;
    }

    /*********************
     * DATA FLOW METHODS *
     *********************/

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        // Select the name prefix for the Tracks.
        final String trackPrefix =
                m_useCustomTrackPrefix.getBooleanValue() ? m_customTrackPrefix
                        .getStringValue()
                        : TrackmateTrackerSettingsModels.DEFAULT_TRACK_PREFIX;

        // set the source labeling, only one source is allowed since now
        final int labelingIndex =
                getColIndices(m_sourceLabelingColumn, LabelingValue.class,
                        inData[0].getSpec());

        @SuppressWarnings("unchecked")
        final LabelingValue<String> srcLabelingValue =
                ((LabelingValue<String>) inData[0].iterator().next()
                        .getCell(labelingIndex));

        // create a list of all nodes
        final TrackableObjectCollection<TrackedNode<String>> trackedNodes =
                createTrackedNodes(inData, exec, srcLabelingValue
                        .getLabelingMetadata().getName());

        // Do the tracking
        final SparseLAPTracker<TrackedNode<String>> tracker =
                new SparseLAPTracker<>(trackedNodes, initTrackMateSettings());
        tracker.setNumThreads(Runtime.getRuntime().availableProcessors());
        tracker.process();

        // get the tracks from the tracker
        final ArrayList<SortedSet<TrackedNode<String>>> tracks =
                retrieveTrackSegments(tracker);

        // create the result labeling
        final Labeling<String> resultLabeling =
                createResultLabeling(srcLabelingValue.getLabeling(), tracks,
                        trackPrefix);

        // Calculate the track features (if needed)
        Map<Integer, Map<String, Double>> featureValues = null;
        if (m_calculateTrackFeaturesModel.getBooleanValue()) {
            featureValues =
                    calculateTrackFeatures(trackedNodes, tracker.getResult(),
                            exec).getAllTrackFeatureValues();
        }

        // create the output tables
        final BufferedDataTable[] resultTables =
                createResultTables(exec, srcLabelingValue, resultLabeling,
                        tracks, trackPrefix, featureValues);

        m_labelingTable = resultTables[0];
        m_trackFeatureTable = resultTables[1];

        return resultTables;
    }

    /**
     * @param inData
     * @param exec
     * @param sourceLabelingName
     * @return A {@link TrackableObjectCollection} containing all trackable
     *         objects in the input data.
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    private TrackableObjectCollection<TrackedNode<String>> createTrackedNodes(
            final BufferedDataTable[] inData, final ExecutionContext exec,
            final String sourceLabelingName) throws CanceledExecutionException,
            InvalidSettingsException {

        // get all information needed from table
        final DataTableSpec spec = inData[0].getSpec();
        final String[] columnNames = spec.getColumnNames();
        final int[] featureIndices = getSelectedColumnIndices(spec);

        // get bitmask index
        final int sourceLabelingIdx =
                getColIndices(m_sourceLabelingColumn, LabelingValue.class,
                        inData[0].getSpec());
        // time axis
        final AxisType timeAxis = Axes.get(m_timeAxisModel.getStringValue());

        // get bitmask index
        final int bitMaskColumnIdx =
                getColIndices(m_bitMaskColumnModel, ImgPlusValue.class, spec);

        // get label index
        final int labelIdx =
                getColIndices(m_labelColumnModel, StringValue.class, spec,
                        bitMaskColumnIdx);

        // create the nodes from the input data
        final TrackableObjectCollection<TrackedNode<String>> trackedNodes =
                new DefaultTOCollection<>();

        for (final DataRow row : inData[0]) {
            exec.checkCanceled();

            // get the spot
            @SuppressWarnings("unchecked")
            final ImgPlusValue<BitType> bitMaskValue =
                    ((ImgPlusValue<BitType>) row.getCell(bitMaskColumnIdx));
            final ImgPlus<BitType> bitMask = bitMaskValue.getImgPlus();
            final String label =
                    ((StringValue) row.getCell(labelIdx)).getStringValue();

            // get time dimension
            final int timeIdx = bitMask.dimensionIndex(timeAxis);

            if (timeIdx == -1) {
                throw new IllegalArgumentException(
                        "Tracking dimension doesn't exist in your BitMask. "
                                + "Please choose the correct tracking dimension!");
            }

            if (!sourceLabelingName.equalsIgnoreCase(((LabelingValue<?>) row
                    .getCell(sourceLabelingIdx)).getLabelingMetadata()
                    .getName())) {
                throw new IllegalArgumentException(
                        "Since now only labels from one Labeling are allowed. "
                                + "Use KNIME Loops!");
            }

            final Map<String, Double> featureMap =
                    new HashMap<String, Double>();
            for (final int idx : featureIndices) {
                try {
                    featureMap.put(columnNames[idx],
                            ((DoubleValue) row.getCell(idx)).getDoubleValue());
                } catch (final ClassCastException e) {
                    throw new ClassCastException("Missing values in the row: '"
                            + row.getKey() + "' in the column: '"
                            + columnNames[idx] + "'");
                }
            }

            final Centroid centroid = new Centroid();
            double[] pos =
                    centroid.compute(bitMask,
                            new double[bitMask.numDimensions()]);

            for (int d = 0; d < pos.length; d++) {
                pos[d] += bitMaskValue.getMinimum()[d];
            }

            // the TrackLocationAnalyzer calculators only works with 3D
            // images, so we extend the 2D ones.
            if (pos.length == 3) {
                pos = new double[] { pos[0], pos[1], pos[2], 0.0 };
            }

            // set correct meta data
            featureMap.put(TrackmateConstants.FRAME, pos[timeIdx]);
            featureMap.put(TrackmateConstants.POSITION_T, pos[timeIdx]);

            // add the node
            final TrackedNode<String> trackedNode =
                    new TrackedNode<String>(bitMask, pos,
                            bitMaskValue.getMinimum(), label, timeIdx,
                            featureMap);

            trackedNodes.add(trackedNode, trackedNode.frame());
        }
        return trackedNodes;
    }

    /**
     * Retrieves the calculated track segments in a harmonised form from the
     * Tracker.
     *
     * @param tracker
     *            the tracker to retrieve the tracks from.
     * @return the retrieved track segments
     */
    private ArrayList<SortedSet<TrackedNode<String>>> retrieveTrackSegments(
            final SparseLAPTracker<TrackedNode<String>> tracker) {
        // get the tracks from the tracker and
        final ConnectivityInspector<TrackedNode<String>, DefaultWeightedEdge> inspector =
                new ConnectivityInspector<TrackedNode<String>, DefaultWeightedEdge>(
                        tracker.getResult());
        final List<Set<TrackedNode<String>>> unsortedSegments =
                inspector.connectedSets();
        final ArrayList<SortedSet<TrackedNode<String>>> trackSegments =
                new ArrayList<SortedSet<TrackedNode<String>>>(
                        unsortedSegments.size());

        // sort the track nodes by adding all segments to a sorted TreeSet
        for (final Set<TrackedNode<String>> segmentSet : unsortedSegments) {
            final SortedSet<TrackedNode<String>> sortedSet =
                    new TreeSet<TrackedNode<String>>(
                            TrackableObjectUtils.frameComparator());
            sortedSet.addAll(segmentSet);
            trackSegments.add(sortedSet);
        }
        return trackSegments;
    }

    /**
     * Creates a Labeling which contains all tracks.
     *
     * @param sourceLabeling
     *            the source labeling.
     * @param tracks
     *            the calculated tracks.
     * @param trackPrefix
     *            the string prefix for the tracks.
     * @return
     */
    private Labeling<String> createResultLabeling(
            final Labeling<?> sourceLabeling,
            final List<SortedSet<TrackedNode<String>>> tracks,
            final String trackPrefix) {

        final RandomAccess<?> srcAccess = sourceLabeling.randomAccess();
        final Labeling<String> resultLabeling =
                sourceLabeling.<String> factory().create(sourceLabeling);
        final RandomAccess<LabelingType<String>> resAccess =
                resultLabeling.randomAccess();

        // loop invariants
        final boolean attachSourceLabelings =
                m_attachSourceLabelings.getBooleanValue();
        final int numDims = resAccess.numDimensions();

        int trackCtr = 0;
        for (final SortedSet<TrackedNode<String>> track : tracks) {
            for (final TrackedNode<String> node : track) {
                final ImgPlus<BitType> bitMask = node.bitMask();
                final Cursor<BitType> bitMaskCursor = bitMask.cursor();
                while (bitMaskCursor.hasNext()) {
                    if (!bitMaskCursor.next().get()) {
                        continue;
                    }

                    for (int d = 0; d < numDims; d++) {
                        resAccess.setPosition(bitMaskCursor.getLongPosition(d)
                                + node.offset(d), d);
                    }
                    // set all the important information
                    final List<String> labeling =
                            new ArrayList<String>(resAccess.get().getLabeling());

                    labeling.add(trackPrefix + trackCtr);

                    // add original labelings if selected by the user
                    if (attachSourceLabelings) {
                        srcAccess.setPosition(resAccess);
                        final List<?> localLabelings =
                                ((LabelingType<?>) srcAccess.get())
                                        .getLabeling();
                        for (final Object o : localLabelings) {
                            labeling.add(o.toString());
                        }
                    }
                    resAccess.get().setLabeling(labeling);
                }
            }
            trackCtr++;
        }
        return resultLabeling;
    }

    /**
     * Calculates the Track Features.
     *
     * @param spots
     *            A TrackableObjectCollection containing the tracked spots
     *
     * @param trackingResult
     *            the result of the TrackMate tracker
     * @param exec
     * @throws CanceledExecutionException
     */
    private FeatureModel<TrackedNode<String>> calculateTrackFeatures(
            final TrackableObjectCollection<TrackedNode<String>> spots,
            final SimpleWeightedGraph<TrackedNode<String>, DefaultWeightedEdge> trackingResult,
            final ExecutionContext exec) throws CanceledExecutionException {

        final Model<TrackedNode<String>> model = new Model<>();

        model.setTracks(trackingResult, false);
        model.setSpots(spots, false);

        final Set<Integer> trackIDs = model.getTrackModel().trackIDs(false);

        for (final TrackAnalyzer<TrackedNode<String>> analyzer : TrackmateTrackerSettingsModels.TRACK_ANALYZERS) {
            exec.checkCanceled();
            if (analyzer.isLocal()) {
                analyzer.process(trackIDs, model);
            } else {
                analyzer.process(model.getTrackModel().trackIDs(false), model);
            }
        }
        return model.getFeatureModel();
    }

    /**
     * Creates the tables holding the results of the node.
     *
     * @param exec
     *            the execution context.
     * @param srcLabelingValue
     *            the {@link LabelingValue} of the source Labeling.
     * @param resultLabeling
     *            the labeling to return as the result of the tracking.
     * @param tracks
     *            the Tracks to write out.
     * @param trackPrefix
     *            the prefix for the tracks.
     * @param featureValues
     *            A map containing the calculated feature values.
     * @return BufferedDataTable[] the Tables containing the results
     * @throws IOException
     * @throws CanceledExecutionException
     */
    private BufferedDataTable[] createResultTables(final ExecutionContext exec,
            final LabelingValue<String> srcLabelingValue,
            final Labeling<String> resultLabeling,
            final ArrayList<SortedSet<TrackedNode<String>>> tracks,
            final String trackPrefix,
            final Map<Integer, Map<String, Double>> featureValues)

    throws IOException, CanceledExecutionException {

        final DataTableSpec[] outSpec = createOutSpec();

        // create the labeling result table
        final LabelingCellFactory labelingCellFactory =
                new LabelingCellFactory(exec);
        final BufferedDataContainer labelingContainer =
                exec.createDataContainer(outSpec[0]);
        final LabelingMetadata sourceLabelingMetadata =
                srcLabelingValue.getLabelingMetadata();
        final String sourceLabelingName = sourceLabelingMetadata.getName();
        labelingContainer.addRowToTable(new DefaultRow(sourceLabelingName,
                labelingCellFactory.createCell(resultLabeling,
                        sourceLabelingMetadata)));
        labelingContainer.close();

        // (optionally) create the track features result table
        final BufferedDataContainer featureContainer =
                exec.createDataContainer(outSpec[1]);
        if (m_calculateTrackFeaturesModel.getBooleanValue()) {
            // create the metadata for the bitmasks
            final DefaultImgMetadata mdata =
                    new DefaultImgMetadata(srcLabelingValue.getLabeling()
                            .numDimensions());
            MetadataUtil.copyTypedSpace(sourceLabelingMetadata, mdata);
            MetadataUtil.copyName(sourceLabelingMetadata, mdata);
            MetadataUtil.copySource(srcLabelingValue.getLabelingMetadata(),
                    mdata);
            mdata.setSource(sourceLabelingMetadata.getName());

            // create a row for each track with containing its features
            for (int i = 0, n = tracks.size(); i < n; i++) {
                final String trackName = trackPrefix + i;
                featureContainer.addRowToTable(createTrackFeatureRow(
                        sourceLabelingName, featureValues.get(i), trackName,
                        mdata, resultLabeling, exec));
            }
        }
        featureContainer.close();

        return new BufferedDataTable[] { labelingContainer.getTable(),
                featureContainer.getTable() };
    }

    /****************************
     * DATA FLOW HELPER METHODS *
     ****************************/

    /**
     * Creates the a row for a given track and its features for the
     * trackfeatures table.
     *
     * @param sourceLabelingName
     * @param featureMap
     * @param dimensions
     * @param exec
     * @param id
     * @param exec
     * @return a DataRow containing all the feature values for the specified
     *         track.
     * @throws IOException
     */
    private DataRow createTrackFeatureRow(final String sourceLabelingName,
            final Map<String, Double> featureMap, final String trackName,
            final ImgPlusMetadata mdata, final Labeling<String> resultLabeling,
            final ExecutionContext exec) throws IOException {

        final List<DataCell> cells =
                new ArrayList<>(TrackMateTrackFeature.values().length);

        // TrackID Column
        cells.add(new StringCell(trackName));

        // Bitmask Column
        final IterableRegionOfInterest labelRoi =
                resultLabeling.getIterableRegionOfInterest(trackName);

        final long[] dimensions = new long[resultLabeling.numDimensions()];
        resultLabeling.dimensions(dimensions);

        final Img<BitType> bitMask =
                createBinaryMask(
                        labelRoi.getIterableIntervalOverROI(new ConstantRandomAccessible<BitType>(
                                new BitType(), resultLabeling.numDimensions())),
                        dimensions);
        cells.add(new ImgPlusCellFactory(exec).createCell(bitMask, mdata));

        // Feature Co
        for (final TrackMateTrackFeature feature : TrackMateTrackFeature
                .values()) {
            cells.add(new DoubleCell(featureMap.get(feature.name())));
        }

        return new DefaultRow(
                sourceLabelingName + '#' + trackName,
                cells.toArray(new DataCell[TrackMateTrackFeature.values().length]));
    }

    /**
     * Helper to create a binary mask from a region of interest. The mask has
     * the size given in the dimensions.
     *
     * @param roi
     *            the region of interest.
     * @param dims
     *            the dimensions of the original labeling.
     * @returns
     */
    private Img<BitType> createBinaryMask(final IterableInterval<BitType> roi,
            final long... dims) {
        final Img<BitType> mask = ArrayImgs.bits(dims);
        final RandomAccess<BitType> maskRA = mask.randomAccess();
        final Cursor<BitType> cur = roi.localizingCursor();

        while (cur.hasNext()) {
            cur.fwd();
            for (int d = 0; d < cur.numDimensions(); d++) {
                maskRA.setPosition(cur.getLongPosition(d) - roi.min(d), d);
            }
            maskRA.get().set(true);
        }

        return mask;

    }

    /**
     * @return A Map containing the settings for the TrackMateTracker, obtained
     *         from the node settings models.
     */
    private Map<String, Object> initTrackMateSettings() {
        // Set the tracking settings
        final Map<String, Object> settings =
                LAPUtils.getDefaultLAPSettingsMap();
        settings.put(KEY_LINKING_MAX_DISTANCE,
                m_linkingMaxDistanceModel.getDoubleValue());
        settings.put(KEY_ALLOW_GAP_CLOSING,
                m_allowGapClosingModel.getBooleanValue());
        settings.put(KEY_ALLOW_TRACK_MERGING,
                m_allowMergingModel.getBooleanValue());
        settings.put(KEY_ALLOW_TRACK_SPLITTING,
                m_allowSplittingModel.getBooleanValue());
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

    /*******************************************
     * COLUMN SELECTION AND VALIDATION METHODS *
     *******************************************/

    /**
     * Retrieves the selected column indices from the given DataTableSpec and
     * the column selection. If the selection turned out to be invalid, all
     * columns are selected.
     *
     * @param inSpec
     *            the data table spec of the input table
     * @return the column indices of selected columns
     */
    private int[] getSelectedColumnIndices(final DataTableSpec inSpec) {
        final List<String> colNames;
        if ((m_columns.getIncludeList().size() == 0)
                || m_columns.isKeepAllSelected()) {
            colNames = new ArrayList<String>();
            collectAllDoubleTypeColumns(colNames, inSpec);
            m_columns.setIncludeList(colNames);

        } else {
            colNames = new ArrayList<String>();
            colNames.addAll(m_columns.getIncludeList());
            if (!validateColumnSelection(colNames, inSpec)) {
                setWarningMessage("Invalid column selection. "
                        + "All columns are selected!");
                collectAllDoubleTypeColumns(colNames, inSpec);
            }
        }

        // get column indices
        final List<Integer> colIndices =
                new ArrayList<Integer>(colNames.size());
        for (int i = 0; i < colNames.size(); i++) {
            final int colIdx = inSpec.findColumnIndex(colNames.get(i));
            if (colIdx == -1) {
                // can not occur, actually
                throw new IllegalStateException("this should not happen");
            } else {
                colIndices.add(colIdx);
            }
        }

        // Can't use List.toArray() because of int.
        final int[] colIdx = new int[colIndices.size()];
        for (int i = 0; i < colIdx.length; i++) {
            colIdx[i] = colIndices.get(i);
        }

        return colIdx;
    }

    /**
     * Helper to collect all columns of DoubleType.
     */
    private void collectAllDoubleTypeColumns(final List<String> colNames,
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

    /**
     * Checks if a column is not present in the DataTableSpec.
     */
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

    /**
     * Gets the column index associated with a given settings model and class.
     */
    private int getColIndices(final SettingsModelString model,
            final Class<? extends DataValue> clazz, final DataTableSpec inSpec,
            final Integer... excludeCols) throws InvalidSettingsException {

        int colIdx = -1;
        if (model.getStringValue() != null) {
            colIdx =
                    NodeUtils.autoColumnSelection(inSpec, model, clazz,
                            this.getClass(), excludeCols);
        }
        return colIdx;
    }

    /******************************
     * DEFAULT KNIME NODE METHODS *
     ******************************/

    /*
     * Helper to collect all settings models and add them to one list (if not
     * already done)
     */
    private void collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<SettingsModel>();

            m_settingsModels.add(m_bitMaskColumnModel);
            m_settingsModels.add(m_columns);
            m_settingsModels.add(m_labelColumnModel);
            m_settingsModels.add(m_timeAxisModel);
            m_settingsModels.add(m_allowGapClosingModel);
            m_settingsModels.add(m_allowMergingModel);
            m_settingsModels.add(m_alternativeLinkingCostFactor);
            m_settingsModels.add(m_cutoffPercentileModel);
            m_settingsModels.add(m_linkingMaxDistanceModel);
            m_settingsModels.add(m_gapClosingMaxFrameModel);
            m_settingsModels.add(m_mergingMaxDistanceModel);
            m_settingsModels.add(m_splittingMaxDistance);
            m_settingsModels.add(m_allowSplittingModel);
            m_settingsModels.add(m_gapClosingMaxDistanceModel);
            m_settingsModels.add(m_sourceLabelingColumn);
            m_settingsModels.add(m_useCustomTrackPrefix);
            m_settingsModels.add(m_customTrackPrefix);
            m_settingsModels.add(m_attachSourceLabelings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        collectSettingsModels();
        for (final SettingsModel s : m_settingsModels) {
            s.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        collectSettingsModels();
        for (final SettingsModel s : m_settingsModels) {
            s.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        collectSettingsModels();
        for (final SettingsModel s : m_settingsModels) {
            s.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_labelingTable = null;
        m_trackFeatureTable = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[] { m_labelingTable, m_trackFeatureTable };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_labelingTable = tables[0];
        m_trackFeatureTable = tables[1];
    }
}
