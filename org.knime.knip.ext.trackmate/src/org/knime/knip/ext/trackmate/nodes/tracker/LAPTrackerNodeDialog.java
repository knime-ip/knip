package org.knime.knip.ext.trackmate.nodes.tracker;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.ext.trackmate.nodes.tracker.LAPTrackerNodeModel.LAPTrackerAlgorithm;

public class LAPTrackerNodeDialog extends DefaultNodeSettingsPane {

	// Trackmate

	private final SettingsModelString m_trackingAlgorithmModel = LAPTrackerSettingsModels
			.createTrackingAlgorithmModel();

	private final SettingsModelBoolean m_allowGapClosingModel = LAPTrackerSettingsModels
			.createAllowGapClosingModel();

	private final SettingsModelBoolean m_allowMergingModel = LAPTrackerSettingsModels
			.createAllowMergingModel();

	private final SettingsModelBoolean m_allowSplittingModel = LAPTrackerSettingsModels
			.createAllowSplittingModel();

	private final SettingsModelIntegerBounded m_gapClosingMaxFrameModel = LAPTrackerSettingsModels
			.createMaxFrameGapClosingModel();

	private final SettingsModelDouble m_mergingMaxDistanceModel = LAPTrackerSettingsModels
			.createMergingMaxDistance();

	private final SettingsModelDouble m_gapClosingMaxDistanceModel = LAPTrackerSettingsModels
			.createGapClosingMaxDistanceModel();

	private final SettingsModelDouble m_splittingMaxDistance = LAPTrackerSettingsModels
			.createSplittingMaxDistance();

	private final SettingsModelDouble m_alternativeLinkingCostFactor = LAPTrackerSettingsModels
			.createAlternativeLinkingCostFactor();

	private final SettingsModelDouble m_cutoffPercentileModel = LAPTrackerSettingsModels
			.createCutoffPercentileModel();

	private final SettingsModelDouble m_linkingMaxDistanceModel = LAPTrackerSettingsModels
			.createLinkingMaxDistanceModel();

	// KNIME

	private final SettingsModelString m_sourceLabelingColumn = LAPTrackerSettingsModels
			.createSourceLabelingSettingsModel();

	private final SettingsModelFilterString m_columns = LAPTrackerSettingsModels
			.createColumnSelectionModel();

	private final SettingsModelString m_timeAxisModel = LAPTrackerSettingsModels
			.createTimeAxisModel();

	private final SettingsModelString m_bitMaskColumnModel = LAPTrackerSettingsModels
			.createBitMaskModel();

	private final SettingsModelString m_labelColumnModel = LAPTrackerSettingsModels
			.createLabelModel();

	public LAPTrackerNodeDialog() {

		this.createNewGroup("Basic");
		addBasicOptions();
		this.closeCurrentGroup();

		this.createNewGroup("Splitting");
		addSplittingOptions();
		this.closeCurrentGroup();

		this.createNewGroup("Merging");
		addMergingOptions();
		this.closeCurrentGroup();

		this.createNewGroup("Gap-Closing");
		addGapClosingOptions();
		this.closeCurrentGroup();

		this.createNewTab("Advanced");
		this.createNewGroup("Advanced Tracking Settings");
		addAdvancedSettings();
		this.closeCurrentGroup();

		this.createNewTab("Column Settings");
		addKNIMEColumnSettings();
	}

	@SuppressWarnings("unchecked")
	private void addKNIMEColumnSettings() {

		this.addDialogComponent(new DialogComponentColumnFilter(m_columns, 0,
				true, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType()
								.isCompatible(DoubleValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No Double columns found! No feature added!";
					}
				}));

		this.addDialogComponent(new DialogComponentColumnNameSelection(
				m_bitMaskColumnModel, "Bitmask Column", 0, ImgPlusValue.class));

		this.addDialogComponent(new DialogComponentColumnNameSelection(
				m_labelColumnModel, "Labels", 0, StringValue.class));

		this.addDialogComponent(new DialogComponentColumnNameSelection(
				m_sourceLabelingColumn, "Source Labeling", 0,
				LabelingValue.class));
	}

	private void addAdvancedSettings() {
		this.addDialogComponent(new DialogComponentNumber(
				m_alternativeLinkingCostFactor,
				"Alternative Linking Cost Factor", 0.05));

		this.addDialogComponent(new DialogComponentNumber(
				m_cutoffPercentileModel, "Cutoff Percentile", 2.5));
	}

	private void addBasicOptions() {
		this.addDialogComponent(new DialogComponentStringSelection(
				m_trackingAlgorithmModel, "Algorithm",
				EnumUtils.getStringListFromToString(LAPTrackerAlgorithm
						.values())));
		this.addDialogComponent(new DialogComponentNumber(
				m_linkingMaxDistanceModel, "Maximum Object Distance", 2.5));

		this.addDialogComponent(new DialogComponentStringSelection(
				m_timeAxisModel, "Tracking Dimension", KNIMEKNIPPlugin
						.parseDimensionLabels()));
	}

	private void addSplittingOptions() {
		this.addDialogComponent(new DialogComponentBoolean(
				m_allowSplittingModel, "Allow Splitting"));

		this.addDialogComponent(new DialogComponentNumber(
				m_splittingMaxDistance, "Max Distance", 0.5));

		m_allowSplittingModel.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				m_splittingMaxDistance.setEnabled(m_allowSplittingModel
						.getBooleanValue());
			}
		});
	}

	private void addMergingOptions() {
		this.addDialogComponent(new DialogComponentBoolean(m_allowMergingModel,
				"Allow Merging"));

		this.addDialogComponent(new DialogComponentNumber(
				m_mergingMaxDistanceModel, "Max Distance", 0.5));

		m_allowMergingModel.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				m_mergingMaxDistanceModel.setEnabled(m_allowMergingModel
						.getBooleanValue());
			}
		});
	}

	private void addGapClosingOptions() {
		this.addDialogComponent(new DialogComponentBoolean(
				m_allowGapClosingModel, "Allow Gap Closing"));

		this.addDialogComponent(new DialogComponentNumber(
				m_gapClosingMaxDistanceModel, "Max Distance", 0.5));

		this.addDialogComponent(new DialogComponentNumber(
				m_gapClosingMaxFrameModel, "Max GAP Size (Frames)", 1));

		m_allowGapClosingModel.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				m_gapClosingMaxDistanceModel.setEnabled(m_allowGapClosingModel
						.getBooleanValue());
				m_gapClosingMaxFrameModel.setEnabled(m_allowGapClosingModel
						.getBooleanValue());
			}
		});
	}
}
