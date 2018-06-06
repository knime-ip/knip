package org.knime.knip.bdv.projector;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

/**
 * Accumulate projector which performs alpha blending with labelings and simple
 * addition with images.
 * 
 * If images and labelings are present, the labelings are added with alpha
 * blending on top of the images.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class AccumulateProjectorAlphaBlendingARGB extends AccumulateProjector<ARGBType, ARGBType> {

	/**
	 * Lookup indicating if the current access is from a labeling.
	 */
	private boolean[] labelingLookup;

	/**
	 * Start index of the images.
	 */
	private int startImgs;

	/**
	 * Start index of the labelings.
	 */
	private int startLabs;

	/**
	 * {@inheritDoc}
	 * 
	 * @param labelingLookup
	 *            indicates if an access is from a labeling
	 * @param startImgs
	 *            index of the first image
	 * @param startLabs
	 *            index of the first labeling
	 */
	public AccumulateProjectorAlphaBlendingARGB(final ArrayList<VolatileProjector> sourceProjectors,
			final ArrayList<? extends RandomAccessible<? extends ARGBType>> sources,
			final RandomAccessibleInterval<ARGBType> target, final int numThreads,
			final ExecutorService executorService, final boolean[] labelingLookup, final int startImgs,
			final int startLabs) {
		super(sourceProjectors, sources, target, numThreads, executorService);
		this.labelingLookup = labelingLookup;
		this.startImgs = startImgs;
		this.startLabs = startLabs;
	}

	@Override
	protected void accumulate(final Cursor<? extends ARGBType>[] accesses, final ARGBType target) {

		if (startImgs > -1 && startLabs > -1) {
			// Images and Labelings
			int imgC = accesses[startImgs].get().get();
			int labC = accesses[startLabs].get().get();

			for (int i = 0; i < accesses.length; i++) {
				if (startImgs < i && !labelingLookup[i]) {
					imgC = ColorUtils.combineColors(imgC, accesses[i].get().get());
				} else if (startLabs < i && labelingLookup[i]) {
					labC = ColorUtils.combineAlphaColors(accesses[i].get().get(), labC);
				}
			}
			target.set(ColorUtils.blendAlphaColors(imgC, labC));

		} else if (startImgs > -1) {
			// Only images
			int imgC = accesses[startImgs].get().get();
			for (int i = startImgs + 1; i < accesses.length; i++) {
				imgC = ColorUtils.combineColors(imgC, accesses[i].get().get());
			}
			target.set(imgC);
		} else if (startLabs > -1) {
			// Only labelings
			int labC = accesses[startLabs].get().get();
			for (int i = startLabs + 1; i < accesses.length; i++) {
				labC = ColorUtils.combineAlphaColors(accesses[i].get().get(), labC);
			}
			target.set(labC);
		} else {
			// No sources
		}
	}

}