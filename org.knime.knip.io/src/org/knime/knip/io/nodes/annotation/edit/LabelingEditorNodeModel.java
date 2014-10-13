package org.knime.knip.io.nodes.annotation.edit;

import java.util.List;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.LabelingView;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.Unsigned12BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.knime.core.data.DataRow;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.util.ImgUtils;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorChangeTracker;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorRowKey;

/**
 * NodeModel of the InteractiveLabelingEditor node.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 * @param <L>
 */
public class LabelingEditorNodeModel<L extends Comparable<L>>
		extends
		TwoValuesToCellNodeModel<LabelingValue<L>, ImgPlusValue<?>, LabelingCell<String>> {

	static String LABEL_SETTINGS_KEY = "editedLabels";

	public static <L extends Comparable<L>> SettingsModelLabelEditor createAnnotatorSM() {
		return new SettingsModelLabelEditor(LABEL_SETTINGS_KEY);
	}

	public static SettingsModelString createImgColumnSM() {
		return new SettingsModelString(LABEL_SETTINGS_KEY + "_imgcol", "");
	}

	public static SettingsModelString createLabelColumnSM() {
		return new SettingsModelString(LABEL_SETTINGS_KEY + "_labelcol", "");
	}

	private SettingsModelLabelEditor m_annotationsSM = createAnnotatorSM();

	private LabelingCellFactory m_labelingCellFactory;

	private DataRow m_currentRow;

	@Override
	protected void addSettingsModels(final List<SettingsModel> settingsModels) {
		settingsModels.add(m_annotationsSM);
	}

	@Override
	protected void prepareExecute(final ExecutionContext exec) {
		m_labelingCellFactory = new LabelingCellFactory(exec);
	}

	@Override
	protected void computeDataRow(final DataRow row) {
		m_currentRow = row;
	}

	@Override
	protected LabelingCell<String> compute(final LabelingValue<L> cellValue1,
			final ImgPlusValue<?> cellValue2) throws Exception {

		// Get RowKey of current row

		final RowColKey k = new LabelingEditorRowKey(m_currentRow.getKey()
				.getString(), cellValue1.getDimensions());

		// Get the Map containing all changes from the settings model
		final Map<RowColKey, LabelingEditorChangeTracker> map = m_annotationsSM
				.getTrackerMap();
		// Get the tracker of the current row
		final LabelingEditorChangeTracker currentTrack = map.get(k);

		Labeling<String> src = null;

		if (currentTrack != null) {

			// Convert the input label to string, and then to the modified
			// label.
			src = new LabelingView<String>(
					Converters.convert(
							Converters
									.convert(
											(RandomAccessibleInterval<LabelingType<L>>) cellValue1
													.getLabeling(),
											new ToStringLabelingConverter<L>(),
											new LabelingType<String>()),
							currentTrack, new LabelingType<String>()),
					cellValue1.getLabeling().<String> factory());

		} else {
			src = new LabelingView<String>(Converters.convert(
					(RandomAccessibleInterval<LabelingType<L>>) cellValue1
							.getLabeling(), new ToStringLabelingConverter<L>(),
					new LabelingType<String>()), cellValue1.getLabeling()
					.<String> factory());
		}

		final Labeling<String> res = ImgUtils.createEmptyCopy(src);
		src.firstElement().getMapping().numLists();

		NativeImgLabeling<L, ? extends IntegerType<?>> lab = (NativeImgLabeling<L, ? extends IntegerType<?>>) cellValue1
				.getLabeling();

		Img<? extends IntegerType<?>> img = lab.getStorageImg();
		Img newStorageImg = null;

		int modifiedLabels = 0;
		if (currentTrack != null)
			modifiedLabels += currentTrack.getNumberOfModifiedLabels();

		IntegerType type = findMatchingType(lab.firstElement().getMapping()
				.numLists()
				+ modifiedLabels);

		try {
			newStorageImg = img.factory().imgFactory(type).create(img, type);
		} catch (Exception e) {
			throw new KNIPException("Error when creating new Labeling!");
		}

		NativeImgLabeling<String, ? extends IntegerType<?>> newLabeling = new NativeImgLabeling<>(
				newStorageImg);

		final Cursor<LabelingType<String>> resCursor = newLabeling.cursor();
		final Cursor<LabelingType<String>> srcCursor = src.cursor();

		while (resCursor.hasNext()) {
			resCursor.next().setLabeling(srcCursor.next().getLabeling());
		}

		return m_labelingCellFactory.createCell(newLabeling,
				cellValue1.getLabelingMetadata());
	}

	private IntegerType<?> findMatchingType(int i) {
		if (i < 2)
			return new BitType();
		if (i < 128)
			return new UnsignedByteType();
		if (i < Math.pow(2, 12))
			return new Unsigned12BitType();
		return new UnsignedShortType();
	}

}
