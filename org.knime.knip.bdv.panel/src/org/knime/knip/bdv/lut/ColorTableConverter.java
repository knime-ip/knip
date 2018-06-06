package org.knime.knip.bdv.lut;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.knip.bdv.projector.ColorUtils;

import net.imglib2.converter.Converter;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;

/**
 * This {@link Converter} aggregates multiple color lookup tables into one by
 * adding the aRGB values component wise.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 * @param <L>
 * @param <I>
 */
public class ColorTableConverter<L> implements Converter<LabelingType<L>, ARGBType> {

	/**
	 * The {@link ImgLabeling} mapping the indices to label-sets.
	 */
	private final LabelingMapping<L> labelingMapping;

	/**
	 * All luts.
	 */
	private final ArrayList<ColorTable> colorTables;

	/**
	 * The combined lut.
	 */
	private int[] lut;

	/**
	 * Map from the lut-value to index in the imgLabeling.
	 */
	private Map<Integer, Set<L>> reverseLut;

	/**
	 * This color table converter aggregates all given luts and converts
	 * imgLabeling-pixels to the corresponding aggregated ARGB-LUT.
	 * 
	 * @param labelingMapping
	 *            index image of the labeling
	 */
	public ColorTableConverter(final LabelingMapping<L> mapping) {
		this.labelingMapping = mapping;
		colorTables = new ArrayList<>();

	}

	/**
	 * Update the aggregated LUT.
	 */
	public synchronized void update() {
		final int[] newlut = new int[labelingMapping.numSets()];
		reverseLut = new HashMap<>();

		for (final ColorTable colorTable : colorTables) {
			final int[] ct = colorTable.getLut();
			if (ct == null)
				continue;

			for (int i = 0; i < ct.length; i++) {
				final int acc = newlut[i];
				final int col = ct[i];
				newlut[i] = ColorUtils.combineAlphaColors(acc, col);
				reverseLut.put(newlut[i], labelingMapping.labelsAtIndex(i));

			}
		}

		lut = newlut;
	}

	@Override
	public void convert(LabelingType<L> input, ARGBType output) {
		output.set(lut[input.getIndex().getInteger()]);
	}

	/**
	 * Add another LUT.
	 * 
	 * @param colorTable
	 *            LUT
	 * @return success
	 */
	public synchronized boolean addColorTable(final ColorTable colorTable) {
		if (!colorTables.contains(colorTable)) {
			colorTables.add(colorTable);
			return true;
		}
		return false;
	}

	/**
	 * Remove LUT.
	 * 
	 * @param colortable
	 * @return success
	 */
	public synchronized boolean removeColorTable(final ColorTable colortable) {
		return colorTables.remove(colortable);
	}

	/**
	 * Get {@link ImgLabeling} index for a given aRGB value.
	 * 
	 * @param argbValue
	 * @return corresponding index-value in {@link ImgLabeling}
	 */
	public Set<L> getLabelIndex(final int argbValue) {
		return reverseLut.get(argbValue);
	}
}
