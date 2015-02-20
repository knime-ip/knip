package org.knime.knip.featurenode.model;

import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

import org.knime.core.data.DataCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

public class FeatureRowResult<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> {

	private final ImgPlusValue<T> possibleImg;
	private final LabelingValue<L> possibleLabeling;
	private final L possibleLabel;
	private final List<Pair<String, T>> results;

	public FeatureRowResult(final ImgPlusValue<T> possibleImg,
			final LabelingValue<L> possibleLabeling, final L possibleLabel,
			final List<Pair<String, T>> results, final DataCell[]... dataCells) {
		this.possibleImg = possibleImg;
		this.possibleLabeling = possibleLabeling;
		this.possibleLabel = possibleLabel;
		this.results = results;
	}

	/**
	 * @return the possibleImg
	 */
	public ImgPlusValue<T> getPossibleImg() {
		return this.possibleImg;
	}

	/**
	 * @return the possibleLabeling
	 */
	public LabelingValue<L> getPossibleLabeling() {
		return this.possibleLabeling;
	}

	/**
	 * @return the possibleLabel
	 */
	public L getPossibleLabel() {
		return this.possibleLabel;
	}

	/**
	 * @return the results
	 */
	public List<Pair<String, T>> getResults() {
		return this.results;
	}

}