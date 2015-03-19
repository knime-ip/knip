package org.knime.knip.tracking.nodes.laptracker;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.tracking.nodes.laptracker.LAPTrackerNodeModel.LAPTrackerAlgorithm;

import fiji.plugin.trackmate.tracking.TrackerKeys;

/**
 * Simple helper class to store {@link SettingsModel}s used within
 * {@link LAPTrackerNodeModel}
 *
 * @author Christian Dietz
 *
 */
final class LAPTrackerSettingsModels {

    private LAPTrackerSettingsModels() {
        // Utility class
    }

    /* KNIME TABLE SETTINGS */

    static final String DEFAULT_TRACK_PREFIX = "Track: ";

    /**
     * @return settings model to store the source labeling
     */
    public static SettingsModelString createSourceLabelingSettingsModel() {
        return new SettingsModelString("source_labeling_settings_model", "");
    }

    /**
     * @return settings model for BitMask selection
     */
    public static SettingsModelString createBitMaskModel() {
        return new SettingsModelString("bitmask_column", "");
    }

    /**
     * @return settings model to store the time axis selection
     */
    public static SettingsModelString createTimeAxisModel() {
        return new SettingsModelString("feature_columns", "Time");
    }

    /**
     * @return settings model to store Label column
     */
    public static SettingsModelString createLabelModel() {
        return new SettingsModelString("label_column_selection", "");
    }

    /**
     * @return settings model for the column selection
     */
    public static SettingsModelFilterString createColumnSelectionModel() {
        return new SettingsModelFilterString("column_selection");
    }

    /**
     * @return settings model to store the labeling attachment selection
     */
    public static SettingsModelBoolean createAttachSourceLabelingsModel() {
        return new SettingsModelBoolean("attach_original_labeling", false);
    }

    /**
     * @return settings model to store the custom track label selection
     */
    public static SettingsModelBoolean createUseCustomTrackPrefixModel() {
        return new SettingsModelBoolean("use_custom_track_label", false);
    }

    /**
     * @return settings model to store the custom label for the tracks
     */
    public static SettingsModelString createCustomTrackPrefixModel() {
        return new SettingsModelString("custom_track_label",
                DEFAULT_TRACK_PREFIX);
    }

    /* TRACKMATE SETTING MODELS */

    /**
     * @return settings model to store default gap closing max frame distance
     */
    public static SettingsModelDouble createAlternativeLinkingCostFactor() {
        return new SettingsModelDouble("alternative_linking_cost_factor",
                TrackerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR);
    }

    /**
     * @return settings model to store whether splitting should be allowed
     */
    public static SettingsModelBoolean createAllowSplittingModel() {
        return new SettingsModelBoolean("allow_track_splitting",
                TrackerKeys.DEFAULT_ALLOW_TRACK_SPLITTING);
    }

    /**
     * @return settings model to store whether merging should be allowed
     */
    public static SettingsModelBoolean createAllowMergingModel() {
        return new SettingsModelBoolean("allow_track_merging",
                TrackerKeys.DEFAULT_ALLOW_TRACK_MERGING);
    }

    /**
     * @return settings model to store the used tracking algorithm
     */
    public static SettingsModelString createTrackingAlgorithmModel() {
        return new SettingsModelString("tracking_algorithm",
                LAPTrackerAlgorithm.MUNKRESKUHN.toString());
    }

    /**
     * @return settings model to store splitting max distance
     */
    public static SettingsModelDouble createSplittingMaxDistance() {
        return new SettingsModelDouble("splitting_max_distance",
                TrackerKeys.DEFAULT_SPLITTING_MAX_DISTANCE);
    }

    /**
     * @return settings model to store merging max distance
     */
    public static SettingsModelDouble createMergingMaxDistance() {
        return new SettingsModelDouble("merging_max_distance",
                TrackerKeys.DEFAULT_MERGING_MAX_DISTANCE);
    }

    /**
     * @return settings model to store the max distance of cell
     */
    public static SettingsModelDouble createLinkingMaxDistanceModel() {
        return new SettingsModelDouble("linking_max_distance",
                TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE);
    }

    /**
     * @return settings model to store the cutoff percentile
     */
    public static SettingsModelDouble createCutoffPercentileModel() {
        return new SettingsModelDouble("cutoff_percentile",
                TrackerKeys.DEFAULT_CUTOFF_PERCENTILE);
    }

    /**
     * @return settings model to store whether gap closing should be allowed
     */
    public static SettingsModelBoolean createAllowGapClosingModel() {
        return new SettingsModelBoolean("allow_gap_closing",
                TrackerKeys.DEFAULT_ALLOW_GAP_CLOSING);
    }

    /**
     * @return settings model to store default gap closing max frame distance
     */
    public static SettingsModelIntegerBounded createMaxFrameGapClosingModel() {
        return new SettingsModelIntegerBounded("max_frame_gap_closing",
                TrackerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP, 0,
                Integer.MAX_VALUE);
    }

    /**
     * @return settings model to store default gap closing max distance
     */
    public static SettingsModelDouble createGapClosingMaxDistanceModel() {
        return new SettingsModelDouble("gap_closing_max_distance",
                TrackerKeys.DEFAULT_GAP_CLOSING_MAX_DISTANCE);
    }
}
