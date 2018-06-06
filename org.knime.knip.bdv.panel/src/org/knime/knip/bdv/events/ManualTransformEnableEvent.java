package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.tools.transformation.ManualTransformationEditor;

/**
 * 
 * Enable single source/group transformation with
 * {@link ManualTransformationEditor}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class ManualTransformEnableEvent extends SciJavaEvent {

	/**
	 * Enable {@link ManualTransformationEditor}.
	 */
	private final boolean enable;

	/**
	 * Enable the {@link ManualTransformationEditor}.
	 * 
	 * @param enable
	 */
	public ManualTransformEnableEvent(final boolean enable) {
		this.enable = enable;
	}

	/**
	 * 
	 * @return enable state of {@link ManualTransformationEditor}
	 */
	public boolean isEnabled() {
		return enable;
	}
}
