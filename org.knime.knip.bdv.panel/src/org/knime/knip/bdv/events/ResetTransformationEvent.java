package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.viewer.ViewerPanel;

/**
 * 
 * Reset the {@link BigDataViewer} transformation.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class ResetTransformationEvent extends SciJavaEvent {

	/**
	 * Reset the {@link BigDataViewer} transformation.
	 * 
	 * Note: EventHandler will distinguish between {@link ViewerPanel} transform
	 * reset and {@link ManualTransformationEditor} reset.
	 */
	public ResetTransformationEvent() {
	}
}
