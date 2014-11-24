package org.knime.knip.tracking.nodes.trackmate;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.tracking.data.TrackedNode;

import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.tracking.TrackerKeys;

/**
 * Simple helper class to store {@link SettingsModel}s used within
 * {@link TrackmateTrackerNodeModel}.
 *
 * @author Christian Dietz
 * @author Gabriel Einsdorf
 *
 *
 */
final class TrackmateTrackerSettingsModels {

    private TrackmateTrackerSettingsModels() {
        // prevent initialisation.
    }

    /**
     * The Features that can be calculated over the tracks.
     *
     * @author Gabriel
     *
     */
    enum TrackMateTrackFeature {

        // /**
        // * Track ID features. Only for debug
        // */
        // TRACK_INDEX("Track index"), TRACK_ID("Track ID"),

        /**
         * Location Features.
         */
        TRACK_X_LOCATION("X Location (mean)"), TRACK_Y_LOCATION(
                "Y Location (mean)"), TRACK_Z_LOCATION("Z Location (mean)"),

        /**
         * Track Branching Features.
         */
        NUMBER_SPOTS("Number of elements in track"), NUMBER_GAPS(
                "Number of gaps"), LONGEST_GAP("Longest gap"), NUMBER_SPLITS(
                "Number of split events"), NUMBER_MERGES(
                "Number of merge events"), NUMBER_COMPLEX(
                "Number of complex points"),

        /**
         * Track Speed Features.
         */
        TRACK_MEAN_SPEED("Mean velocity"), TRACK_MAX_SPEED("Maximal velocity"), TRACK_MIN_SPEED(
                "Minimal velocity"), TRACK_MEDIAN_SPEED("Median velocity"), TRACK_STD_SPEED(
                "Velocity standard deviation"),
        /**
         * Track Duration Features.
         */
        TRACK_DURATION("Duration of track"), TRACK_START("Track start"), TRACK_STOP(
                "Track stop"), TRACK_DISPLACEMENT("Track displacement");

        private TrackMateTrackFeature(String longName) {
            m_longName = longName;
        }

        @Override
        public String toString() {
            return m_longName;
        };

        private String m_longName;
    }

    /**
     * TrackMate feature calculators.
     */
    static final List<TrackAnalyzer<TrackedNode<String>>> TRACK_ANALYZERS =
            new ArrayList<TrackAnalyzer<TrackedNode<String>>>() {
                private static final long serialVersionUID =
                        7987258171225310650L;
                {
                    // add(new TrackIndexAnalyzer<TrackedNode<String>>());
                    add(new TrackLocationAnalyzer<TrackedNode<String>>());
                    add(new TrackBranchingAnalyzer<TrackedNode<String>>());
                    add(new TrackSpeedStatisticsAnalyzer<TrackedNode<String>>());
                    add(new TrackDurationAnalyzer<TrackedNode<String>>());
                }
            };

    /* KNIME TABLE SETTINGS */

    /**
     * The default prefix for the Track name.
     */
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

    /**
     * @return setting model to store whether to calculate track features
     */
    public static SettingsModelBoolean createCalculateTrackFeaturesModel() {
        return new SettingsModelBoolean("calculate_track_features", true);
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
