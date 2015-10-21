package org.knime.knip.io.nodes.annotation.edit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.data.filter.row.FilterRowGenerator;
import org.knime.base.data.filter.row.FilterRowTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DataAwareDefaultNodeSettingsPane;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorChangeTracker;
import org.knime.knip.io.nodes.annotation.edit.control.LabelingEditorRowKey;
import org.knime.knip.io.nodes.annotation.edit.dialogcomponents.DialogComponentLabelingEditorView;
import org.knime.knip.io.nodes.annotation.edit.dialogcomponents.LabelingEditorView;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Dialog Panel of the InteractiveLabelingEditor Node.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 * @param <T>
 * @param <L>
 */
public class LabelingEditorNodeDialog<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends DataAwareDefaultNodeSettingsPane {

	private DialogComponentLabelingEditorView m_dialogComponentAnnotator;

	private SettingsModelString m_smColCreationMode = LabelingEditorNodeModel.createColCreationModeModel();
	private SettingsModelString m_smColumnSuffix = LabelingEditorNodeModel.createColSuffixModel();

	private SettingsModelString m_smImgColumn = LabelingEditorNodeModel.createSecondColModel();

	private SettingsModelString m_smLabelColumn = LabelingEditorNodeModel.createFirstColModel();

	private SettingsModelLabelEditor m_annotatorSM = LabelingEditorNodeModel.createAnnotatorSM();

	private BufferedDataTable m_inputTable;

	@SuppressWarnings("unchecked")
	public LabelingEditorNodeDialog() {
		super();

		removeTab("Options");
		createNewTab("Selection");
		createNewGroup("Image Annotation");

		// final SettingsModelLabelEditor annotatorSM = LabelingEditorNodeModel
		// .createAnnotatorSM();
		m_dialogComponentAnnotator = new DialogComponentLabelingEditorView(new LabelingEditorView<T, L>(m_annotatorSM),
				m_annotatorSM);
		addDialogComponent(m_dialogComponentAnnotator);
		closeCurrentGroup();

		// column selection dialog component
		createNewTab("Column Selection");
		createNewGroup("Change Columns");
		addDialogComponent(new DialogComponentColumnNameSelection(m_smImgColumn, "(Optional) Image Column:", 0, false,
				true, ImgPlusValue.class));
		addDialogComponent(
				new DialogComponentColumnNameSelection(m_smLabelColumn, "Labeling Column:", 0, LabelingValue.class));
		closeCurrentGroup();
		createNewGroup("Creation Mode");
		addDialogComponent(new DialogComponentStringSelection(m_smColCreationMode, "Column Creation Mode",
				ValueToCellNodeModel.COL_CREATION_MODES));
		closeCurrentGroup();

		createNewGroup("Column suffix");
		addDialogComponent(new DialogComponentString(m_smColumnSuffix, "Column suffix"));
		closeCurrentGroup();

		m_smImgColumn.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent arg0) {
				DataTable filteredTable;
				// Input not yet loaded
				if (m_inputTable == null)
					return;
				final DataTableSpec inspec = m_inputTable.getDataTableSpec();
				try {
					final int imgColIndex = NodeUtils.getColumnIndex(m_smImgColumn, inspec, ImgPlusValue.class,
							LabelingEditorNodeModel.class);
					final int labelColIndex = NodeUtils.getColumnIndex(m_smLabelColumn, inspec, LabelingValue.class,
							LabelingEditorNodeModel.class);
					if (imgColIndex != -1 && labelColIndex != -1) {
						filteredTable = new FilterColumnTable(m_inputTable, imgColIndex, labelColIndex);
					} else if (labelColIndex != -1) {

						filteredTable = new FilterColumnTable(m_inputTable, labelColIndex);
					} else {
						filteredTable = new FilterColumnTable(m_inputTable, new int[] {});
					}
					m_dialogComponentAnnotator
							.updateDataTable(new FilterRowTable(filteredTable, new FilterMissingRows()));

				} catch (final InvalidSettingsException e) {
					// TODO What to do?
					e.printStackTrace();
				}
			}

		});

		m_smLabelColumn.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent arg0) {
				DataTable filteredTable;
				// Input not yet loaded
				if (m_inputTable == null)
					return;
				final DataTableSpec inspec = m_inputTable.getDataTableSpec();
				try {
					final int imgColIndex = NodeUtils.getColumnIndex(m_smImgColumn, inspec, ImgPlusValue.class,
							LabelingEditorNodeModel.class);
					final int labelColIndex = NodeUtils.getColumnIndex(m_smLabelColumn, inspec, LabelingValue.class,
							LabelingEditorNodeModel.class);
					if (imgColIndex != -1 && labelColIndex != -1) {
						filteredTable = new FilterColumnTable(m_inputTable, imgColIndex, labelColIndex);
					} else if (labelColIndex != -1) {

						filteredTable = new FilterColumnTable(m_inputTable, labelColIndex);
					} else {
						filteredTable = new FilterColumnTable(m_inputTable, new int[] {});
					}
					m_dialogComponentAnnotator
							.updateDataTable(new FilterRowTable(filteredTable, new FilterMissingRows()));

				} catch (final InvalidSettingsException e) {
					// TODO What to do?
					e.printStackTrace();
				}
			}

		});

	}

	@Override
	public void onClose() {
		m_inputTable = null;
		m_dialogComponentAnnotator.reset();
	}

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
			throws NotConfigurableException {

		// update input data dependent
		m_inputTable = (BufferedDataTable) input[0];
		final DataTableSpec inSpec = m_inputTable.getDataTableSpec();

		// get index of first image column (if available) and first label column
		// issue a warning
		// if there are more

		final int firstImage = inSpec.findColumnIndex(m_smImgColumn.getStringValue());
		final int firstLabel = inSpec.findColumnIndex(m_smLabelColumn.getStringValue());

		// Whenever we load the dialog, we check for outdated rows. Otherwise,
		// the underlying SettingsModel increases in size whenever the input is
		// changed.

		final Map<RowColKey, LabelingEditorChangeTracker> map = m_annotatorSM.getTrackerMap();

		Set<LabelingEditorRowKey> inputKeySet = new HashSet<LabelingEditorRowKey>();

		RowIterator it = m_inputTable.iterator();
		while (it.hasNext()) {
			DataRow r = it.next();
			if (r.getCell(firstLabel).isMissing())
				continue;
			// Fetch the necessary data for the key
			String rowName = r.getKey().getString();

			// There is always at least a labeling accessible
			@SuppressWarnings("unchecked")
			long[] labelingDims = ((LabelingValue<L>) r.getCell(firstLabel)).getDimensions();

			LabelingEditorRowKey k = new LabelingEditorRowKey(rowName, labelingDims);

			inputKeySet.add(k);

		}

		// Now that we have all RowKeys of the input table, we check the
		// currently existing keys and remove all keys that are not present in
		// the current table.

		Set<RowColKey> keysToRemove = new HashSet<RowColKey>();
		for (RowColKey k : map.keySet()) {
			// InteractiveLabelingEditor uses only LabelingEditorRowKeys
			LabelingEditorRowKey key = (LabelingEditorRowKey) k;
			if (!inputKeySet.contains(key))
				keysToRemove.add(k);
		}
		// Avoid ConcurrentModificationException
		for (RowColKey k : keysToRemove) {
			map.remove(k);
		}

		DataTable filteredTable;
		if (firstImage != -1 && firstLabel != -1) {
			filteredTable = new FilterColumnTable(m_inputTable, firstImage, firstLabel);
		} else if (firstLabel != -1) {
			filteredTable = new FilterColumnTable(m_inputTable, firstLabel);
		} else {
			filteredTable = new FilterColumnTable(m_inputTable, new int[] {});
		}

		m_dialogComponentAnnotator.updateDataTable(new FilterRowTable(filteredTable, new FilterMissingRows()));
	}

	/**
	 * If column creation mode is 'append', a suffix needs to be chosen!
	 */
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		if (m_smColCreationMode.getStringValue().equals(ValueToCellNodeModel.COL_CREATION_MODES[1])
				&& m_smColumnSuffix.getStringValue().trim().isEmpty()) {
			throw new InvalidSettingsException(
					"If the selected column creation mode is 'append', a column suffix for the resulting column name must to be chosen!");
		}
		super.saveAdditionalSettingsTo(settings);
	}

	/**
	 * Filter all rows with missing cells from a data table
	 * 
	 * @author Michael Zinsmaier
	 */
	private class FilterMissingRows implements FilterRowGenerator {

		@Override
		public boolean isIn(final DataRow row) {
			boolean somethingMissing = false;

			for (final DataCell cell : row) {
				if (cell.isMissing()) {
					somethingMissing = true;
				}
			}

			return !somethingMissing;
		}
	}
}
