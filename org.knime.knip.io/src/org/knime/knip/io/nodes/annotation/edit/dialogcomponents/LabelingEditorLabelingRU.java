package org.knime.knip.io.nodes.annotation.edit.dialogcomponents;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorLabelsColResetEvent;
import org.knime.knip.core.ui.imgviewer.panels.providers.LabelingRU;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorChangeTracker;

/**
 * Special rendering unit used by the LabelingEditor. This rendering unit is
 * aware of the change-trackers and is able to detect changes to their mapping.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 */
public class LabelingEditorLabelingRU extends LabelingRU<String> {

	private LabelingEditorChangeTracker m_tracker;
	
	// Hacky. Required to ensure color resets actually take effect.
	private int m_colorReset = 0;

	/**
	 * Sets the tracker of this render unit.
	 * 
	 * @param t
	 *            The tracker
	 */
	public void setTracker(final LabelingEditorChangeTracker t) {
		m_tracker = t;
	}

	/**
	 * Listens for color resets triggered by the user.
	 * 
	 * @param e
	 *            The color reset event
	 */
	@EventListener
	public void onLabelsColorReset(final AnnotatorLabelsColResetEvent e) {
		++m_colorReset;

	}

	@Override
	public int generateHashCode() {
		int trackerHash;
		if (m_tracker == null)
			trackerHash = 1;
		else
			trackerHash = m_tracker.hashCode();
		return 31 * (super.generateHashCode() * 31 + trackerHash)
				+ m_colorReset;
	}
}