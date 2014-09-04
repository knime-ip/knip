package org.knime.knip.tracking.nodes.hiliter;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.tracking.nodes.hiliter.TrackHilitePropagatorNodeModel.TrackHilitingMode;

/**
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class TrackHilitePropagatorNodeDialog extends DefaultNodeSettingsPane {

    /*
     * Settings Models
     */
    private final SettingsModelColumnName m_trackColumnModel = TrackHilitePropagatorSettingsModels
            .createTrackColumnSelectionSettingsModel();

    private final SettingsModelBoolean m_useCustomTrackPrefixModel = TrackHilitePropagatorSettingsModels
            .createUseCustomTrackPrefixModel();

    private final SettingsModelString m_customTrackPrefixModel = TrackHilitePropagatorSettingsModels
            .createCustomTrackPrefixModel();

    private final SettingsModelString m_highlitingModeModel = TrackHilitePropagatorSettingsModels
            .createTrackHilitingModeModel();

    /**
     * Creates the Node dialog for the Track Hilite Propagator node.
     */
    protected TrackHilitePropagatorNodeDialog() {

        createNewGroup("Basic");
        addBasicOptions();
        closeCurrentGroup();
        createNewGroup("Custom Labeling");
        addLabelingOptions();
        closeCurrentGroup();
    }

    @SuppressWarnings("unchecked")
    private void addBasicOptions() {

        addDialogComponent(new DialogComponentColumnNameSelection(
                m_trackColumnModel, "Tracks Column", 0, false, false,
                StringValue.class));

        addDialogComponent(new DialogComponentStringSelection(
                m_highlitingModeModel, "Hiliting Mode",
                EnumUtils.getStringListFromToString(TrackHilitingMode.values())));
    }

    private void addLabelingOptions() {
        setHorizontalPlacement(true);

        m_useCustomTrackPrefixModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_customTrackPrefixModel.setEnabled(m_useCustomTrackPrefixModel
                        .getBooleanValue());
            }
        });

        addDialogComponent(new DialogComponentBoolean(
                m_useCustomTrackPrefixModel,
                "Tracks are labeled with a custom prefix"));

        m_customTrackPrefixModel.setEnabled(false);
        addDialogComponent(new DialogComponentString(m_customTrackPrefixModel,
                "Prefix:"));

        setHorizontalPlacement(false);
    }
}
