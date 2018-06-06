package org.knime.knip.bdv.control;

import java.util.List;

import bdv.cache.CacheControl;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;

/**
 * Removing the "Press F1 for help" overlay.
 * 
 * @author Tim-Oliver Buchholz, MPI-CBG/CSBD, Dresden
 *
 */
public class KnipBdvViewerPanel extends ViewerPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9026399420884831208L;

	public KnipBdvViewerPanel(List<SourceAndConverter<?>> sources, int numTimePoints, CacheControl cacheControl,
			ViewerOptions viewerOptions) {
		super(sources, numTimePoints, cacheControl, viewerOptions);
		overlayAnimators.remove(1);
	}
}
