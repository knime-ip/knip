package org.knime.knip.featurenode.model;

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.misc.LabelingDependency;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.ops.operation.Operations;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;

/**
 * The input for a {@link FeatureComputationTask}
 *
 * @author Daniel Seebacher
 *
 *
 *         TODO: Remove SuppressWarning when new Labeling Version is available
 */
@SuppressWarnings("deprecation")
public class FeatureTaskInput<T extends Type<T>, L extends Comparable<L>> {

	private final int m_imgColumnIndex;
	private final int m_labelingColumnIndex;
	private final boolean m_append;
	private final LabelSettings<L> m_labelSettings;
	private final SettingsModelDimSelection m_dimSelection;
	private final ImgPlusCellFactory m_imgCellFactory;
	private final DataRow m_dataRow;
	private final DataTableSpec m_inputSpec;
	private final Map<L, List<L>> m_overlappingLabels;

	/**
	 * Default constructor
	 *
	 * @param imgColumnIndex
	 *            The column index of the image, if present, otherwise -1
	 * @param labelingColumnIndex
	 *            The column index of the image, if present, otherwise -1
	 * @param m_dimselectionModel
	 */
	public FeatureTaskInput(final int imgColumnIndex, final int labelingColumnIndex, final boolean append,
			final SettingsModelDimSelection m_dimselectionModel, final LabelSettings<L> labelSettings,
			final ImgPlusCellFactory imgCellFactory, final DataTableSpec inputSpec, final DataRow row) {
		if ((-1 == imgColumnIndex) && (-1 == labelingColumnIndex)) {
			throw new IllegalArgumentException("At least and image or an labeling must be present");
		}

		this.m_imgColumnIndex = imgColumnIndex;
		this.m_labelingColumnIndex = labelingColumnIndex;
		this.m_append = append;
		this.m_dimSelection = m_dimselectionModel;
		this.m_labelSettings = labelSettings;
		this.m_imgCellFactory = imgCellFactory;
		this.m_inputSpec = inputSpec;
		this.m_dataRow = row;

		this.m_overlappingLabels = Operations.compute(new LabelingDependency<L>(labelSettings.getRuleBasedLabelFilter(),
				new RulebasedLabelFilter<L>(), labelSettings.isIntersectionMode()), getLabelRegions());

	}

	/**
	 * @return the imgColumnIndex
	 */
	public int getImgColumnIndex() {
		return this.m_imgColumnIndex;
	}

	/**
	 * @return the labelingColumnIndex
	 */
	public int getLabelingColumnIndex() {
		return this.m_labelingColumnIndex;
	}

	/**
	 * @return the dataRow
	 */
	public DataRow getDataRow() {
		return this.m_dataRow;
	}

	/**
	 * @return If the FeatureTaskInput has Labelings
	 */
	public boolean hasLabeling() {
		return this.m_labelingColumnIndex != -1;
	}

	/**
	 * @return If the FeatureTaskInput has an Img.
	 */
	public boolean hasImg() {
		return this.m_imgColumnIndex != -1;
	}

	public SettingsModelDimSelection getDimSelection() {
		return this.m_dimSelection;
	}

	public LabelSettings<L> getLabelSettings() {
		return this.m_labelSettings;
	}

	@SuppressWarnings({ "unchecked" })
	public <K extends RealType<K>> Img<T> getImage() {
		return (Img<T>) ((ImgPlusValue<K>) this.m_dataRow.getCell(this.m_imgColumnIndex)).getImgPlus().getImg();
	}

	@SuppressWarnings({ "unchecked" })
	public RandomAccessibleInterval<LabelingType<L>> getLabelRegions() {
		return ((LabelingValue<L>) this.m_dataRow.getCell(this.m_labelingColumnIndex)).getLabeling();
	}

	@SuppressWarnings("unchecked")
	public <K extends RealType<K>> int[] getSelectedDimensions() {

		if (hasImg()) {
			return this.m_dimSelection.getSelectedDimIndices(
					((ImgPlusValue<K>) this.m_dataRow.getCell(this.m_imgColumnIndex)).getImgPlus());
		}

		return this.m_dimSelection.getSelectedDimIndices(
				((LabelingValue<L>) this.m_dataRow.getCell(this.m_labelingColumnIndex)).getLabelingMetadata());
	}

	public ImgPlusCellFactory getImgCellFactory() {
		return this.m_imgCellFactory;
	}

	public DataTableSpec getInputSpec() {
		return this.m_inputSpec;
	}

	public boolean isAppend() {
		return this.m_append;
	}

	public Map<L, List<L>> getOverlappingLabels() {
		return this.m_overlappingLabels;
	}
}