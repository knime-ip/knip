package org.knime.knip.tracking.nodes.hiliter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Helper class, in extra file so it can be made Serializable
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */

class TrackData implements Serializable {

    private static final long serialVersionUID = -6481605948023318801L;

    // Stores the labels for each Track.
    final HashMap<String, List<String>> m_trackToLabels;
    // Store track for each label.
    final HashMap<String, String> m_labelToTrack;

    protected TrackData(HashMap<String, List<String>> trackToLabels,
            HashMap<String, String> labelToTrack) {
        m_trackToLabels = trackToLabels;
        m_labelToTrack = labelToTrack;
    }
}