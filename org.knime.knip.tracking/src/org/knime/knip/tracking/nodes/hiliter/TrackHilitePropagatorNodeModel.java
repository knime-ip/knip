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
import java.util.Set;

import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.NativeLabeling;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
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

/**
 * Node that connects the Track resulting from the LAP Tracker with the
 * corresponding rows, so that they can be hilited correctly.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class TrackHilitePropagatorNodeModel extends NodeModel implements
        HiLiteListener {

    private static final String SERIALISATION_KEY = "TrackHilitePropagatorState";

    /*
     * SETTING MODELS
     */

    private final SettingsModelColumnName m_trackColumnModel = TrackHilitePropagatorSettingsModels
            .createTrackColumnSelectionSettingsModel();

    private final SettingsModelString m_customTrackPrefixModel = TrackHilitePropagatorSettingsModels
            .createCustomTrackPrefixModel();

    private final SettingsModelBoolean m_useCustomTrackPrefixModel = TrackHilitePropagatorSettingsModels
            .createUseCustomTrackPrefixModel();

    private final SettingsModelString m_trackHilitingModeModel = TrackHilitePropagatorSettingsModels
            .createTrackHilitingModeModel();

    /**
     * Enum describing the Hiliting Modes.
     */
    public enum TrackHilitingMode {

        /**
         * Hiliting a track row also hilites all rows which are in that track.
         */
        TRACK_TO_POINTS("Track to Points"),

        /**
         * Hiliting a row also hilites all other rows that are on the same
         * track.
         */
        POINTS_TO_POINTS("Points to Points"),

        /**
         * No influence on the hiliting.
         */
        OFF("Disabled");

        private String m_name;

        private TrackHilitingMode(final String describingName) {
            m_name = describingName;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    /*
     * MEMBER
     */

    // Stores the labels for each rack.
    private HashMap<String, List<String>> m_trackToLabels;
    // Store track for each label.
    private HashMap<String, String> m_labelToTrack;
    // the HiliteHandler
    private HiLiteHandler m_hiliteHandler;

    /*
     * Node Begins
     */

    /**
     * Constructor.
     */
    protected TrackHilitePropagatorNodeModel() {
        super(1, 0);

        // for state consistency
        m_customTrackPrefixModel.setEnabled(false);

    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        final int colIndex = inSpecs[0].findColumnIndex(m_trackColumnModel
                .getStringValue());
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

        // Get the column from the input table.
        final int trackingColIndex = inData[0].getDataTableSpec()
                .findColumnIndex(m_trackColumnModel.getStringValue());

        // Get the cell
        final CloseableRowIterator it = inData[0].iterator();
        final LabelingCell<?> cell = (LabelingCell<?>) it.next().getCell(
                trackingColIndex);

        // Get the Labeling, and the Labels in it
        final NativeLabeling<?> labeling = (NativeLabeling<?>) cell
                .getLabeling();
        final LabelingMapping<?> mapping = labeling.getMapping();

        final String trackPrefix;
        if (m_useCustomTrackPrefixModel.getBooleanValue()) {
            // ignoring leading and trailing whitespace for matching
            trackPrefix = m_customTrackPrefixModel.getStringValue().trim();
        } else {
            trackPrefix = TrackHilitePropagatorSettingsModels.DEFAULT_TRACK_PREFIX;
        }

        m_labelToTrack = new HashMap<String, String>();
        m_trackToLabels = new HashMap<String, List<String>>();

        // Fill the maps with the associated labelings.
        for (int i = 0; i < mapping.numLists(); i++) {
            @SuppressWarnings("unchecked")
            final List<String> localLabels = (List<String>) mapping
                    .listAtIndex(i);

            // skip mappings that contain less than two labels
            if (localLabels.size() <= 1) {
                continue;
            }

            // Identify the labels and the track
            String track = "";
            final ArrayList<String> otherLabels = new ArrayList<String>();
            for (final String label : localLabels) {
                if (label.startsWith(trackPrefix)) {
                    track = label;
                } else {
                    otherLabels.add(label);
                }
            }

            // Store the identified labels in the maps
            final List<String> labelsInTrack = m_trackToLabels.get(track);
            if (labelsInTrack == null) {
                m_trackToLabels.put(track, otherLabels);
            } else {
                labelsInTrack.addAll(otherLabels);
                m_trackToLabels.put(track, labelsInTrack);
            }

            for (final String label : otherLabels) {
                m_labelToTrack.put(label, track);
            }
            exec.checkCanceled();
        }
        // there is not output so this is safe.
        return null;
    }

    /*
     * Standard Node methods
     */

    @SuppressWarnings("unchecked")
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        m_hiliteHandler.addHiLiteListener(this);

        final ObjectInputStream in = new ObjectInputStream(new FileInputStream(
                new File(nodeInternDir, SERIALISATION_KEY)));
        try {
            m_trackToLabels = (HashMap<String, List<String>>) in.readObject();
            exec.checkCanceled();
            exec.setProgress(0.5);
            m_labelToTrack = (HashMap<String, String>) in.readObject();
        } catch (final ClassNotFoundException e) {
            in.close();
            throw new IOException("Could not restore the state!", e);
        }
        in.close();
    }

    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        final ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(new File(nodeInternDir, SERIALISATION_KEY)));
        out.writeObject(m_trackToLabels);
        out.writeObject(m_labelToTrack);
        out.close();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_trackColumnModel.saveSettingsTo(settings);
        m_customTrackPrefixModel.saveSettingsTo(settings);
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
        m_labelToTrack = null;
        m_trackToLabels = null;
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
    };

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
        final Set<String> tracksToHilite = new HashSet<String>();
        final String trackPrefix = m_useCustomTrackPrefixModel
                .getBooleanValue() ? m_customTrackPrefixModel.getStringValue()
                : TrackHilitePropagatorSettingsModels.DEFAULT_TRACK_PREFIX;

        for (final RowKey k : event.keys()) {

            // split the row key into prefix and unique part
            final String rowKey = k.getString();
            final String lookUpkey = rowKey
                    .substring(rowKey.lastIndexOf('#') + 1);

            // only select rows that are tracks
            if (lookUpkey.startsWith(trackPrefix)) {
                tracksToHilite.add(lookUpkey);
            } else {
                continue;
            }
        }
        return getRowKeysFromTracks(tracksToHilite, event);

    }

    /**
     * Creates a List of row keys that are on the same Track as the row keys in
     * the Event. If the event contains
     *
     * @return @ param event the hiliting event
     *
     * @return a set of rowkeys that are on the same tracks as the keys in the
     *         hiliting event.
     */
    private RowKey[] pointsToPoints(final KeyEvent event) {

        // Get all the Labels that are on the same track

        final Set<String> tracksToHilite = new HashSet<String>();

        final String trackPrefix = m_useCustomTrackPrefixModel
                .getBooleanValue() ? m_customTrackPrefixModel.getStringValue()
                : TrackHilitePropagatorSettingsModels.DEFAULT_TRACK_PREFIX;

        for (final RowKey k : event.keys()) {

            // split the row key into prefix and unique part
            final String rowKey = k.getString();
            final String lookUpkey = rowKey
                    .substring(rowKey.lastIndexOf('#') + 1);

            if (lookUpkey.startsWith(trackPrefix)) {
                tracksToHilite.add(lookUpkey);
            } else {
                final String value = m_labelToTrack.get(lookUpkey);
                if (value != null) {
                    tracksToHilite.add(value);
                }
            }
        }
        return getRowKeysFromTracks(tracksToHilite, event);
    }

    /**
     * Takes a set of tracks and returns their {@link RowKey}s and of all the
     * items that are on these tracks.
     *
     * @param tracksToHilite
     *            the tracks which members
     * @param event
     *            the hilite event, used to extract the key information
     *
     * @return The RowKeys of the tracks and the items that are on them.
     */
    private RowKey[] getRowKeysFromTracks(final Set<String> tracksToHilite,
            final KeyEvent event) {

        // early escape
        if (tracksToHilite == null || tracksToHilite.size() == 0) {
            return new RowKey[0];
        }

        // get the key prefix from a random row
        final String rowKey = event.keys().iterator().next().getString();
        final String keyPrefix = rowKey.substring(0,
                rowKey.lastIndexOf('#') + 1);

        // mark all rows on all given tracks and the tracks them self for
        // hiliting.
        final Set<String> toHilite = new HashSet<String>(event.keys().size());
        for (final String track : tracksToHilite) {
            final List<String> list = m_trackToLabels.get(track);
            if (list == null) {
                continue;
            }
            for (final String label : list) {
                toHilite.add(keyPrefix + label);
            }
            toHilite.add(keyPrefix + track);
        }
        return RowKey.toRowKeys(toHilite.toArray(new String[0]));
    }
}
