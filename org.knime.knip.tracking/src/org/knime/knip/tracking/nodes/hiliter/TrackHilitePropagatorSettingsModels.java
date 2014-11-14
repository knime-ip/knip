package org.knime.knip.tracking.nodes.hiliter;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Simple helper class to store {@link SettingsModel}s used within
 * {@link TrackHilitePropagatorNodeModel}.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 *
 */
final class TrackHilitePropagatorSettingsModels {
    public static final String DEFAULT_TRACK_PREFIX = "Track:";

    private TrackHilitePropagatorSettingsModels() {
        // prevent instatiation
	}

	/**
	 * Enum describing the Hiliting Modes.
	 */
	enum TrackHilitingMode {

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
