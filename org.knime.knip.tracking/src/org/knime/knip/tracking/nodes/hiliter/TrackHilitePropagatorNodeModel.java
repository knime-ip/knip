package org.knime.knip.tracking.nodes.hiliter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.tracking.nodes.hiliter.TrackHilitePropagatorSettingsModels.TrackHilitingMode;

import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;

/**
 * Node that connects the Track resulting from the LAP Tracker with the
 * corresponding rows, so that they can be hilited correctly.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class TrackHilitePropagatorNodeModel extends NodeModel
        implements HiLiteListener {

    private static final String SERIALISATION_KEY =
            "TrackHilitePropagatorState";

    /*
     * SETTING MODELS
     */

    private final SettingsModelColumnName m_trackColumnModel =
            TrackHilitePropagatorSettingsModels
                    .createTrackColumnSelectionSettingsModel();

    private final SettingsModelString m_customTrackPrefixModel =
            TrackHilitePropagatorSettingsModels.createCustomTrackPrefixModel();

    private final SettingsModelBoolean m_useCustomTrackPrefixModel =
            TrackHilitePropagatorSettingsModels
                    .createUseCustomTrackPrefixModel();

    private final SettingsModelString m_trackHilitingModeModel =
            TrackHilitePropagatorSettingsModels.createTrackHilitingModeModel();

    /*
     * MEMBER
     */

    // the HiliteHandler
    private HiLiteHandler m_hiliteHandler;

    // stores the trackdata for each row
    Map<String, TrackData> m_rowIdToTrackData;

    /*
     * Node Begins
     */

    /**
     * Constructor.
     */
    protected TrackHilitePropagatorNodeModel() {
        super(1, 1);

        // for state consistency
        m_customTrackPrefixModel.setEnabled(false);

    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        final int colIndex =
                inSpecs[0].findColumnIndex(m_trackColumnModel.getStringValue());
        if (colIndex == -1) {
            if ((NodeUtils.autoOptionalColumnSelection(inSpecs[0],
                    m_trackColumnModel, ImgPlusValue.class)) >= 0) {
                setWarningMessage("Auto-configure Image Column: "
                        + m_trackColumnModel.getStringValue());
            } else {
                throw new InvalidSettingsException("No column selected!");
            }
        }
        return null;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException {
        getInHiLiteHandler(0).addHiLiteListener(this);
        m_rowIdToTrackData = new HashMap<>((int) inData[0].size());

        // Get the column from the input table.
        final int trackingColIndex = inData[0].getDataTableSpec()
                .findColumnIndex(m_trackColumnModel.getStringValue());

        for (final DataRow row : inData[0]) {
            exec.checkCanceled();
            final LabelingCell<?> labelingCell =
                    (LabelingCell<?>) row.getCell(trackingColIndex);

            // Get the Labeling, and the Labels in it
            @SuppressWarnings("rawtypes")
            final ImgLabeling labeling =
                    (ImgLabeling) labelingCell.getLabeling();
            @SuppressWarnings("rawtypes")
            final LabelingMapping mapping = labeling.getMapping();

            final String trackPrefix;
            if (m_useCustomTrackPrefixModel.getBooleanValue()) {
                // ignoring leading and trailing whitespace for matching
                trackPrefix = m_customTrackPrefixModel.getStringValue().trim();
            } else {
                trackPrefix =
                        TrackHilitePropagatorSettingsModels.DEFAULT_TRACK_PREFIX;
            }

            final Map<String, String> labelToTrack =
                    new HashMap<String, String>();
            final Map<String, List<String>> trackToLabels =
                    new HashMap<String, List<String>>();

            // Fill the maps with the associated labelings.
            for (int i = 0; i < mapping.numSets(); i++) {
                @SuppressWarnings("unchecked")
                final Set<String> localLabels = mapping.labelsAtIndex(i);

                // skip mappings that contain less than two labels
                if (localLabels.size() <= 1) {
                    continue;
                }

                // Identify the labels and the track
                String track = "";
                final List<String> otherLabels = new ArrayList<String>();
                for (final String label : localLabels) {
                    if (label.startsWith(trackPrefix)) {
                        track = label;
                    } else {
                        otherLabels.add(label);
                    }
                }

                // Store the identified labels in the maps
                final List<String> labelsInTrack = trackToLabels.get(track);
                if (labelsInTrack == null) {
                    trackToLabels.put(track, otherLabels);
                } else {
                    labelsInTrack.addAll(otherLabels);
                    trackToLabels.put(track, labelsInTrack);
                }

                for (final String label : otherLabels) {
                    labelToTrack.put(label, track);
                }
                exec.checkCanceled();
            }
            m_rowIdToTrackData.put(row.getKey().getString(),
                    new TrackData(trackToLabels, labelToTrack));
        }
        return inData;
    }

    /*
     * Standard Node methods
     */

    @SuppressWarnings("unchecked")
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        m_hiliteHandler.addHiLiteListener(this);

        final ObjectInputStream in = new ObjectInputStream(new FileInputStream(
                new File(nodeInternDir, SERIALISATION_KEY)));
        try {
            m_rowIdToTrackData = (HashMap<String, TrackData>) in.readObject();
        } catch (final ClassNotFoundException e) {
            in.close();
            throw new IOException("Could not restore the state!", e);
        }
        in.close();
    }

    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {

        final ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(
                        new File(nodeInternDir, SERIALISATION_KEY)));
        out.writeObject(m_rowIdToTrackData);
        out.close();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_trackColumnModel.saveSettingsTo(settings);
        m_customTrackPrefixModel.saveSettingsTo(settings);
        m_useCustomTrackPrefixModel.saveSettingsTo(settings);
        m_trackHilitingModeModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_trackColumnModel.validateSettings(settings);
        m_customTrackPrefixModel.validateSettings(settings);
        m_useCustomTrackPrefixModel.validateSettings(settings);
        m_trackHilitingModeModel.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_trackColumnModel.loadSettingsFrom(settings);
        m_customTrackPrefixModel.loadSettingsFrom(settings);
        m_useCustomTrackPrefixModel.loadSettingsFrom(settings);
        m_trackHilitingModeModel.loadSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        m_rowIdToTrackData = null;
        if (m_hiliteHandler != null) {
            m_hiliteHandler.removeHiLiteListener(this);
        }
    }

    /*
     * HILITING
     */

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler handler) {
        m_hiliteHandler = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hiliteHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        getOutHiLiteHandler(0).fireHiLiteEvent(getKeysForEvent(event));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        getOutHiLiteHandler(0).fireUnHiLiteEvent(getKeysForEvent(event));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteAll(final KeyEvent event) {
        // nothing to do here
    }

    /**
     * Returns the rowkeys that are to be hilited.
     *
     * @param event
     * @return
     */
    private RowKey[] getKeysForEvent(final KeyEvent event) {

        final TrackHilitingMode mode = EnumUtils.valueForName(
                m_trackHilitingModeModel.getStringValue(),
                TrackHilitingMode.values());

        final RowKey[] keys;
        switch (mode) {
        case OFF:
            keys = new RowKey[0];
            break;
        case POINTS_TO_POINTS:
            keys = pointsToPoints(event);
            break;
        case TRACK_TO_POINTS:
            keys = trackToPoints(event);
            break;
        default:
            throw new AssertionError("Unimplemented hiliting mode!");
        }
        return keys;
    }

    private RowKey[] trackToPoints(final KeyEvent event) {
        final Set<RowKey> tracksToHilite = new HashSet<>();
        final String trackPrefix = m_useCustomTrackPrefixModel.getBooleanValue()
                ? m_customTrackPrefixModel.getStringValue()
                : TrackHilitePropagatorSettingsModels.DEFAULT_TRACK_PREFIX;

        for (final RowKey k : event.keys()) {

            // split the row key into prefix and unique part
            final String rowKey = k.getString();

            final String lookUpkey;
            try {
                lookUpkey = rowKey.substring(rowKey.lastIndexOf('#') + 1);
            } catch (final StringIndexOutOfBoundsException e) {
                // the key can't be matched
                continue;
            }

            // only select rows that are tracks
            if (lookUpkey.startsWith(trackPrefix)) {
                tracksToHilite.add(k);
            } else {
                continue;
            }
        }
        return getRowKeysFromTracks(tracksToHilite);

    }

    /**
     * Creates a List of row keys that are on the same Track as the row keys in
     * the Event. If the event contains
     *
     * @return a set of rowkeys that are on the same tracks as the keys in the
     *         hiliting event.
     */
    private RowKey[] pointsToPoints(final KeyEvent event) {

        // Get all the Labels that are on the same track

        final Set<RowKey> tracksToHilite = new HashSet<>();

        final String trackPrefix = m_useCustomTrackPrefixModel.getBooleanValue()
                ? m_customTrackPrefixModel.getStringValue()
                : TrackHilitePropagatorSettingsModels.DEFAULT_TRACK_PREFIX;

        for (final RowKey k : event.keys()) {

            // split the row key into prefix and unique part
            final String rowKeyString = k.getString();
            final TrackData data;
            try {
                data = m_rowIdToTrackData.get(rowKeyString.substring(0,
                        rowKeyString.lastIndexOf('#')));
            } catch (final StringIndexOutOfBoundsException e) {
                // the key can't be matched
                continue;
            }
            final String lookupKey =
                    rowKeyString.substring(rowKeyString.lastIndexOf('#') + 1);
            if (lookupKey.startsWith(trackPrefix)) {
                tracksToHilite.add(k);
            } else {
                final String trackName = data.m_labelToTrack.get(lookupKey);
                if (trackName != null) {
                    final String keyprefix = rowKeyString.substring(0,
                            rowKeyString.lastIndexOf('#'));
                    tracksToHilite.add(new RowKey(keyprefix + '#' + trackName));
                }
            }
        }
        return getRowKeysFromTracks(tracksToHilite);
    }

    /**
     * Takes a set of track {@link RowKey}s and returns their {@link RowKey}s
     * and of all the items that are on these tracks.
     *
     * @param tracksToHilite
     *            the tracks which members
     * @param event
     *            the hilite event, used to extract the key information
     *
     * @return The RowKeys of the tracks and the items that are on them.
     */
    private RowKey[] getRowKeysFromTracks(final Set<RowKey> tracksToHilite) {

        // early escape
        if (tracksToHilite == null || tracksToHilite.isEmpty()) {
            return new RowKey[0];
        }

        // mark all rows on all given tracks and the tracks them self for
        // hiliting.
        final Set<RowKey> toHilite = new HashSet<>();
        for (final RowKey trackRowKey : tracksToHilite) {
            final String keyPrefix = trackRowKey.getString().substring(0,
                    trackRowKey.getString().lastIndexOf('#'));
            final String trackKey = trackRowKey.getString()
                    .substring(trackRowKey.getString().lastIndexOf('#') + 1);

            final TrackData trackData = m_rowIdToTrackData.get(keyPrefix);
            final List<String> list = trackData.m_trackToLabels.get(trackKey);
            if (list == null) {
                continue;
            }
            for (final String label : list) {
                toHilite.add(new RowKey(keyPrefix + '#' + label));
            }
            toHilite.add(trackRowKey);
        }

        return toHilite.toArray(new RowKey[toHilite.size()]);
    }
}
