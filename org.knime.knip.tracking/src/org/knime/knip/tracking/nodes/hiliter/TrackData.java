package org.knime.knip.tracking.nodes.hiliter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Helper class, in extra file so it can be made Serializable
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */

class TrackData implements Serializable {

    private static final long serialVersionUID = -6481605948023318801L;

    // Stores the labels for each Track.
    final Map<String, List<String>> m_trackToLabels;
    // Store track for each label.
    final Map<String, String> m_labelToTrack;

    /**
     * Maps must be Serializable!
     *
     * @param trackToLabels
     * @param labelToTrack
     */
    protected TrackData(Map<String, List<String>> trackToLabels,
            Map<String, String> labelToTrack) {
        m_trackToLabels = trackToLabels;
        m_labelToTrack = labelToTrack;
    }
}