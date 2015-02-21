package org.knime.knip.featurenode.model;

import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

import org.knime.core.data.DataRow;

/**
 * The output of the {@link FeatureComputationTask}. Contains the input and the results.
 * 
 * @author Daniel Seebacher, University of Konstanz.
 *
 * @param <T extends RealType<T> & NativeType<T>
 * @param <L extends Comparable<L>>
 */
public class FeatureTaskOutput<T extends RealType<T> & NativeType<T>, L extends Comparable<L>> {

	private final int imgColumnIndex;
	private final int labelingColumnIndex;
	private final DataRow dataRow;
	private final Pair<List<Pair<String, T>>, L> resultsWithPossibleLabel;

	public FeatureTaskOutput(FeatureTaskInput<T, L> input,
			Pair<List<Pair<String, T>>, L> resultsWithPossibleLabel) {
		this.imgColumnIndex = input.getImgColumnIndex();
		this.labelingColumnIndex = input.getLabelingColumnIndex();
		this.dataRow = input.getDataRow();
		this.resultsWithPossibleLabel = resultsWithPossibleLabel;
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
	 * @return the resultsWithPossibleLabel
	 */
	public Pair<List<Pair<String, T>>, L> getResultsWithPossibleLabel() {
		return resultsWithPossibleLabel;
	}

}