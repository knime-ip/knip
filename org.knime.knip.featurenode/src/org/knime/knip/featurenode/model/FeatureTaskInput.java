package org.knime.knip.featurenode.model;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.knime.core.data.DataRow;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

/**
 * The input for a {@link FeatureComputationTask}
 * 
 * @author Daniel Seebacher
 *
 * @param <T extends RealType<T> & NativeType<T>
 * @param <L extends Comparable<L>
 */
public class FeatureTaskInput<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> {

	private final int imgColumnIndex;
	private final int labelingColumnIndex;
	private final DataRow dataRow;

	/**
	 * Default constructor
	 * 
	 * @param imgColumnIndex
	 *            The column index of the image, if present, otherwise -1
	 * @param labelingColumnIndex
	 *            The column index of the image, if present, otherwise -1
	 * @param otherCells
	 */
	public FeatureTaskInput(final int imgColumnIndex,
			final int labelingColumnIndex, final DataRow row) {
		if ((-1 == imgColumnIndex) && (-1 == labelingColumnIndex)) {
			throw new IllegalArgumentException(
					"At least and image or an labeling must be present");
		}

		this.imgColumnIndex = imgColumnIndex;
		this.labelingColumnIndex = labelingColumnIndex;
		this.dataRow = row;
	}

	/**
	 * @return the imgColumnIndex
	 */
	public int getImgColumnIndex() {
		return imgColumnIndex;
	}

	/**
	 * @return the labelingColumnIndex
	 */
	public int getLabelingColumnIndex() {
		return labelingColumnIndex;
	}

	/**
	 * @return the dataRow
	 */
	public DataRow getDataRow() {
		return dataRow;
	}

	/**
	 * Returns a List of {@link Pair}s of {@link IterableInterval}s and Labels.
	 * If only an image is present this list would have a size of 1 and the
	 * {@link Pair} would contain the image and no Label (null). Otherwise if a
	 * {@link Labeling} is present, then for each Label in the {@link Labeling}
	 * a {@link Pair} of the {@link IterableInterval} over the Label and the
	 * Label would be returned.
	 * 
	 * @return A List of {@link Pair}s of {@link IterableInterval}s and Labels.
	 */
	@SuppressWarnings("unchecked")
	public List<Pair<IterableInterval<T>, L>> getIterableIntervals() {

		// get img and labeling, if present
		ImgPlusValue<T> imgValue = (ImgPlusValue<T>) ((-1 != imgColumnIndex) ? this.dataRow
				.getCell(imgColumnIndex) : null);
		LabelingValue<L> labelingValue = (LabelingValue<L>) ((-1 != labelingColumnIndex) ? this.dataRow
				.getCell(labelingColumnIndex) : null);

		final List<Pair<IterableInterval<T>, L>> resultList = new ArrayList<Pair<IterableInterval<T>, L>>();
		// if both are present
		if ((imgValue != null) && (labelingValue != null)) {
			for (final L label : labelingValue.getLabeling().getLabels()) {
				final IterableRegionOfInterest iiROI = labelingValue
						.getLabeling().getIterableRegionOfInterest(label);

				final IterableInterval<T> ii = iiROI
						.getIterableIntervalOverROI(imgValue.getImgPlus());

				resultList
						.add(new ValuePair<IterableInterval<T>, L>((IterableInterval<T>) iterableIntervalToArrayImg(ii), label));
			}
		}
		// if only the labeling is present
		else if (imgValue == null) {

			Labeling<L> next = labelingValue.getLabeling(); // .getLabels().iterator().next();
			long[] dimensions = new long[next.numDimensions()];
			next.dimensions(dimensions);

			for (final L label : labelingValue.getLabeling().getLabels()) {
				final IterableInterval<LabelingType<L>> ii = labelingValue
						.getLabeling()
						.getIterableRegionOfInterest(label)
						.getIterableIntervalOverROI(labelingValue.getLabeling());

				final IterableInterval<BitType> convert = Converters.convert(
						ii, new Converter<LabelingType<L>, BitType>() {

							@Override
							public void convert(final LabelingType<L> arg0,
									final BitType arg1) {
								arg1.set(!arg0.getLabeling().isEmpty());
							}
						}, new BitType());

				resultList.add(new ValuePair<IterableInterval<T>, L>(
						(IterableInterval<T>) iterableIntervalToArrayImg(ii), label));
			}

		}
		// if only the image is present
		else {
			resultList.add(new ValuePair<IterableInterval<T>, L>(imgValue
					.getImgPlus(), null));
		}

		return resultList;
	}

	private ArrayImg<BitType, LongArray> iterableIntervalToArrayImg(
			IterableInterval<?> ii) {

		ArrayImg<BitType, LongArray> bits = ArrayImgs.bits(ii.dimension(0),
				ii.dimension(1));
		ArrayRandomAccess<BitType> randomAccess = bits.randomAccess();
		Cursor<?> localizingCursor = ii.localizingCursor();
		while (localizingCursor.hasNext()) {
			localizingCursor.next();
			randomAccess.setPosition(localizingCursor);
//			randomAccess.get().set(true);
		}

		return bits;
	}
}