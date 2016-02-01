package org.knime.knip.tracking.nodes.trackmate;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

/**
 * Node Dialog for the Trackmate Tracker Node.
 *
 * @author christian
 * @author gabriel
 *
 */
public class TrackmateTrackerNodeDialog extends DefaultNodeSettingsPane {

    private final SettingsModelBoolean m_allowGapClosingModel =
            TrackmateTrackerSettingsModels.createAllowGapClosingModel();

    private final SettingsModelBoolean m_allowMergingModel =
            TrackmateTrackerSettingsModels.createAllowMergingModel();

    private final SettingsModelBoolean m_allowSplittingModel =
            TrackmateTrackerSettingsModels.createAllowSplittingModel();

    private final SettingsModelIntegerBounded m_gapClosingMaxFrameModel =
            TrackmateTrackerSettingsModels.createMaxFrameGapClosingModel();

    private final SettingsModelDouble m_mergingMaxDistanceModel =
            TrackmateTrackerSettingsModels.createMergingMaxDistance();

    private final SettingsModelDouble m_gapClosingMaxDistanceModel =
            TrackmateTrackerSettingsModels.createGapClosingMaxDistanceModel();

    private final SettingsModelDouble m_splittingMaxDistance =
            TrackmateTrackerSettingsModels.createSplittingMaxDistance();

    private final SettingsModelDouble m_alternativeLinkingCostFactor =
            TrackmateTrackerSettingsModels.createAlternativeLinkingCostFactor();

    private final SettingsModelDouble m_cutoffPercentileModel =
            TrackmateTrackerSettingsModels.createCutoffPercentileModel();

    private final SettingsModelDouble m_linkingMaxDistanceModel =
            TrackmateTrackerSettingsModels.createLinkingMaxDistanceModel();

    // KNIME

    private final SettingsModelString m_sourceLabelingColumn =
            TrackmateTrackerSettingsModels.createSourceLabelingSettingsModel();

    private final SettingsModelColumnFilter2 m_columnFilterModel =
            TrackmateTrackerSettingsModels.createColumnFilterModel();

    private final SettingsModelString m_timeAxisModel =
            TrackmateTrackerSettingsModels.createTimeAxisModel();

    private final SettingsModelString m_bitMaskColumnModel =
            TrackmateTrackerSettingsModels.createBitMaskModel();

    private final SettingsModelString m_labelColumnModel =
            TrackmateTrackerSettingsModels.createLabelModel();

    private final SettingsModelBoolean m_attachSourceLabelings =
            TrackmateTrackerSettingsModels.createAttachSourceLabelingsModel();

    private final SettingsModelBoolean m_useCustomTrackPrefix =
            TrackmateTrackerSettingsModels.createUseCustomTrackPrefixModel();

    private final SettingsModelString m_customTrackPrefix =
            TrackmateTrackerSettingsModels.createCustomTrackPrefixModel();

    private final SettingsModelBoolean m_calculateTrackFeatures =
            TrackmateTrackerSettingsModels.createCalculateTrackFeaturesModel();

    /**
     * Creates the Dialog for the Trackmate Tracker node.
     */
    public TrackmateTrackerNodeDialog() {

        createNewGroup("Basic");
        addBasicOptions();
        closeCurrentGroup();

        createNewGroup("Splitting");
        addSplittingOptions();
        closeCurrentGroup();

        createNewGroup("Merging");
        addMergingOptions();
        closeCurrentGroup();

        createNewGroup("Gap-Closing");
        addGapClosingOptions();
        closeCurrentGroup();

        createNewTab("Advanced");
        createNewGroup("Advanced Tracking Settings");
        addAdvancedSettings();
        closeCurrentGroup();

        createNewGroup("Labeling Settings");
        addLabelingSettings();
        closeCurrentGroup();

        createNewTab("Column Settings");
        addKNIMEColumnSettings();
    }

    @SuppressWarnings("unchecked")
    private void addKNIMEColumnSettings() {

        createNewGroup("Feature Column Selection");
        addDialogComponent(
                new DialogComponentColumnFilter2(m_columnFilterModel, 0));
        closeCurrentGroup();

        createNewGroup("Other Column Selection");
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_bitMaskColumnModel, "Bitmask Column", 0, ImgPlusValue.class));

        addDialogComponent(new DialogComponentColumnNameSelection(
                m_labelColumnModel, "Labels", 0, StringValue.class));

        addDialogComponent(
                new DialogComponentColumnNameSelection(m_sourceLabelingColumn,
                        "Source Labeling", 0, LabelingValue.class));
    }

    private void addAdvancedSettings() {
        addDialogComponent(
                new DialogComponentNumber(m_alternativeLinkingCostFactor,
                        "Alternative Linking Cost Factor", 0.05));

        addDialogComponent(new DialogComponentNumber(m_cutoffPercentileModel,
                "Cutoff Percentile", 0.05));

        addDialogComponent(new DialogComponentBoolean(m_calculateTrackFeatures,
                "Calculate Track Features"));

    }

    private void addBasicOptions() {
        addDialogComponent(new DialogComponentNumber(m_linkingMaxDistanceModel,
                "Maximum Object Distance", 2.5));

        addDialogComponent(new DialogComponentStringSelection(m_timeAxisModel,
                "Tracking Dimension", KNIMEKNIPPlugin.parseDimensionLabels()));
    }

    private void addGapClosingOptions() {
        addDialogComponent(new DialogComponentBoolean(m_allowGapClosingModel,
                "Allow Gap Closing"));

        addDialogComponent(new DialogComponentNumber(
                m_gapClosingMaxDistanceModel, "Max Distance", 0.5));

        addDialogComponent(new DialogComponentNumber(m_gapClosingMaxFrameModel,
                "Max GAP Size (Frames)", 1));

        m_allowGapClosingModel.addChangeListener(e -> {
            m_gapClosingMaxDistanceModel
                    .setEnabled(m_allowGapClosingModel.getBooleanValue());
            m_gapClosingMaxFrameModel
                    .setEnabled(m_allowGapClosingModel.getBooleanValue());
        });
    }

    private void addLabelingSettings() {

        addDialogComponent(new DialogComponentBoolean(m_attachSourceLabelings,
                "Attach Original Labelings"));

        setHorizontalPlacement(true);

        m_customTrackPrefix.setEnabled(false);
        m_useCustomTrackPrefix.addChangeListener(e -> m_customTrackPrefix
                .setEnabled(m_useCustomTrackPrefix.getBooleanValue()));

        addDialogComponent(new DialogComponentBoolean(m_useCustomTrackPrefix,
                "Use a Custom Track Prefix"));

        addDialogComponent(new DialogComponentString(m_customTrackPrefix,
                "Custom Track Prefix:"));

        setHorizontalPlacement(false);
    }

    private void addMergingOptions() {
        addDialogComponent(new DialogComponentBoolean(m_allowMergingModel,
                "Allow Merging"));

        addDialogComponent(new DialogComponentNumber(m_mergingMaxDistanceModel,
                "Max Distance", 0.5));

        m_allowMergingModel.addChangeListener(e -> m_mergingMaxDistanceModel
                .setEnabled(m_allowMergingModel.getBooleanValue()));
    }

    private void addSplittingOptions() {
        addDialogComponent(new DialogComponentBoolean(m_allowSplittingModel,
                "Allow Splitting"));

        addDialogComponent(new DialogComponentNumber(m_splittingMaxDistance,
                "Max Distance", 0.5));

        m_allowSplittingModel.addChangeListener(e -> m_splittingMaxDistance
                .setEnabled(m_allowSplittingModel.getBooleanValue()));
    }
}
