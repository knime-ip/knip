package org.knime.knip.featurenode.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.knip.base.data.img.ImgPlusCell;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

/**
 * The output of the {@link FeatureComputationTask}. Contains the input and the
 * results.
 *
 * @author Daniel Seebacher, University of Konstanz.
 *
 */
@SuppressWarnings("deprecation")
public class FeatureTaskOutput<T extends Type<T>, L extends Comparable<L>> {

	private final FeatureTaskInput<T, L> m_input;

	private final DataRow outputRow;
	private final DataTableSpec outputSpec;

	public FeatureTaskOutput(final FeatureTaskInput<T, L> input, final List<Pair<String, T>> results, final Img<T> img,
			final LabelRegion<L> label, final int index) {
		this.m_input = input;

		this.outputRow = createOutputRow(input, results, img, label, index);
		this.outputSpec = createOutputSpec(input, results);
	}

	public FeatureTaskInput<T, L> getInput() {
		return this.m_input;
	}

	public DataRow getDataRow() {
		return this.outputRow;
	}

	/**
	 *
	 * @return Output {@link DataTableSpec} for this {@link FeatureTaskOutput}
	 */
	public DataTableSpec getOutputSpec() {
		return this.outputSpec;
	}

	/**
	 * Creates a {@link DataTableSpec} from the given {@link DataTableSpec} and
	 * this {@link FeatureTaskOutput}
	 *
	 * @param input
	 *            the input for this {@link FeatureTaskOutput}
	 * @param results
	 *            the computed results
	 * @return a new {@link DataTableSpec} existing of all columns from the
	 *         {@link DataTableSpec} and a column for each given
	 *         {@link FeatureTaskOutput}
	 */
	private DataTableSpec createOutputSpec(final FeatureTaskInput<T, L> input, final List<Pair<String, T>> results) {

		final List<DataColumnSpec> outcells = new ArrayList<DataColumnSpec>();

		if (input.isAppend()) {
			for (int i = 0; i < input.getInputSpec().getNumColumns(); i++) {
				outcells.add(input.getInputSpec().getColumnSpec(i));
			}
		}

		if (input.getLabelingColumnIndex() != -1) {
			outcells.add(new DataColumnSpecCreator("Label", StringCell.TYPE).createSpec());

			if (input.getLabelSettings().isAppendSegmentInformation()) {
				outcells.add(new DataColumnSpecCreator("Bitmask", ImgPlusCell.TYPE).createSpec());
			}

			if (input.getLabelSettings().isAppendLabelSegments()) {
				outcells.add(new DataColumnSpecCreator("Overlapping Labels", StringCell.TYPE).createSpec());
			}
		}

		// add a new column for each given feature result
		for (int i = 0; i < results.size(); i++) {
			outcells.add(new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(input.getInputSpec(), results.get(i).getA() + " #" + i),
					DoubleCell.TYPE).createSpec());
		}

		return new DataTableSpec(outcells.toArray(new DataColumnSpec[outcells.size()]));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private <K extends RealType<K> & NativeType<K>> DataRow createOutputRow(final FeatureTaskInput<T, L> input,
			final List<Pair<String, T>> results, final Img<T> img, final LabelRegion<L> label, final int index) {

		// save results of this iterableinterval in a row
		final List<DataCell> cells = new ArrayList<DataCell>();

		if (input.isAppend()) {
			final DataRow dataRow = input.getDataRow();
			for (int i = 0; i < dataRow.getNumCells(); i++) {
				cells.add(dataRow.getCell(i));
			}
		}

		if (input.getLabelingColumnIndex() != -1) {
			cells.add(new StringCell(label.getLabel().toString()));

			if (input.getLabelSettings().isAppendSegmentInformation()) {
				final RandomAccessibleInterval<BitType> convert = Converters.convert(
						(RandomAccessibleInterval<BoolType>) Regions.iterable(label),
						new Converter<BoolType, BitType>() {

							@Override
							public void convert(final BoolType arg0, final BitType arg1) {
								arg1.set(arg0.get());
							}
						}, new BitType());
				Views.translate(convert, Intervals.minAsLongArray(label));

				try {
					cells.add(input.getImgCellFactory().createCell(
							new ImgPlus<BitType>(new ImgView<BitType>(convert, new ArrayImgFactory<BitType>()))));
				} catch (final IOException exc) {
					throw new IllegalArgumentException("Can't create output cell for bitmask", exc);
				}
			}

			if (input.getLabelSettings().isAppendLabelSegments()) {
				final StringBuilder sb = new StringBuilder();

				final List<L> overlappingLabels = input.getOverlappingLabels().get(label);
				if (overlappingLabels != null) {
					for (int i = 0; i < overlappingLabels.size(); i++) {
						if (i > 0) {
							sb.append(";");
						}

						sb.append(overlappingLabels.get(i));
					}
				}

				cells.add(new StringCell(sb.toString()));
			}
		}

		// store new results
		for (final Pair<String, T> pair : results) {
			cells.add(new DoubleCell(((K) pair.getB()).getRealDouble()));
		}

		final RowKey newRowKey = (index == 0) ? input.getDataRow().getKey()
				: new RowKey(input.getDataRow().getKey().getString() + "_#" + index);

		return new DefaultRow(newRowKey, cells.toArray(new DataCell[cells.size()]));
	}
}