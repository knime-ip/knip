package org.knime.knip.tracking.nodes.hiliter;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.tracking.nodes.hiliter.TrackHilitePropagatorNodeModel.TrackHilitingMode;

/**
 * Simple helper class to store {@link SettingsModel}s used within
 * {@link TrackHilitePropagatorNodeModel}.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 *
 */
final class TrackHilitePropagatorSettingsModels {
    public static final String DEFAULT_TRACK_PREFIX = "Track: ";

    private TrackHilitePropagatorSettingsModels() {
        // prevent instatiation
    }

    /**
     * @return settings model to store the selected column containing the
     *         tracks.
     */
    public static SettingsModelColumnName createTrackColumnSelectionSettingsModel() {
        return new SettingsModelColumnName("tracks_column_selection_model", "");
    }

    /**
     * @return settings model to store the source labeling
     */
    public static SettingsModelString createSourceLabelingSettingsModel() {
        return new SettingsModelString("source_labeling_settings_model", "");
    }

    /**
     * @return settings model to store the custom track label selection
     */
    public static SettingsModelBoolean createUseCustomTrackPrefixModel() {
        return new SettingsModelBoolean("use_custom_track_prefix", false);
    }

    /**
     * @return settings model to store the custom label for the tracks
     */
    public static SettingsModelString createCustomTrackPrefixModel() {
        return new SettingsModelString("custom_track_label_prefix",
                DEFAULT_TRACK_PREFIX);
    }

    /**
     * @return settings model to store the track hiliting mode
     */
    public static SettingsModelString createTrackHilitingModeModel() {
        return new SettingsModelString("track_hiliting_mode",
                TrackHilitingMode.OFF.toString());
    }
}
