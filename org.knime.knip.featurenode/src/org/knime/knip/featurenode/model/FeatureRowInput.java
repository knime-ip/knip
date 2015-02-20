package org.knime.knip.featurenode.model;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.IterableInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.labeling.LabelingType;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.knime.core.data.DataCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

public class FeatureRowInput<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> {

	private final ImgPlusValue<T> imgValue;
	private final LabelingValue<L> labelingValue;
	private final DataCell[] otherCells;

	public FeatureRowInput(final ImgPlusValue<T> img,
			final LabelingValue<L> labeling, final DataCell... otherCells) {
		if ((img == null) && (labeling == null)) {
			throw new IllegalArgumentException(
					"At least and image or an labeling must be present");
		}

		this.imgValue = img;
		this.labelingValue = labeling;
		this.otherCells = otherCells;
	}

	/**
	 * @return the imgValue
	 */
	public ImgPlusValue<T> getImgValue() {
		return this.imgValue;
	}

	/**
	 * @return the labelingValue
	 */
	public LabelingValue<L> getLabelingValue() {
		return this.labelingValue;
	}

	/**
	 * @return the otherCells
	 */
	public DataCell[] getOtherCells() {
		return this.otherCells;
	}

	@SuppressWarnings("unchecked")
	public List<Pair<IterableInterval<T>, L>> getIterableIntervals() {

		final List<Pair<IterableInterval<T>, L>> iterableIntervals = new ArrayList<Pair<IterableInterval<T>, L>>();

		// if both are present
		if ((this.imgValue != null) && (this.labelingValue != null)) {
			for (final L label : this.labelingValue.getLabeling().getLabels()) {
				final IterableRegionOfInterest iiROI = this.labelingValue
						.getLabeling().getIterableRegionOfInterest(label);

				final IterableInterval<T> ii = iiROI
						.getIterableIntervalOverROI(this.imgValue.getImgPlus());
				iterableIntervals.add(new ValuePair<IterableInterval<T>, L>(ii,
						label));
			}
		}
		// if only the labeling is present
		else if (this.imgValue == null) {

			for (final L label : this.labelingValue.getLabeling().getLabels()) {
				final IterableInterval<LabelingType<L>> ii = this.labelingValue
						.getLabeling()
						.getIterableRegionOfInterest(label)
						.getIterableIntervalOverROI(
								this.labelingValue.getLabeling());

				final IterableInterval<BitType> convert = Converters.convert(
						ii, new Converter<LabelingType<L>, BitType>() {

							@Override
							public void convert(final LabelingType<L> arg0,
									final BitType arg1) {
								arg1.set(!arg0.getLabeling().isEmpty());
							}
						}, new BitType());

				iterableIntervals.add(new ValuePair<IterableInterval<T>, L>(
						(IterableInterval<T>) convert, label));

				// iterableIntervals.add((IterableInterval<T>) convert);
			}

		}
		// if only the image is present
		else {
			iterableIntervals.add(new ValuePair<IterableInterval<T>, L>(
					this.imgValue.getImgPlus(), null));
		}

		return iterableIntervals;
	}

}