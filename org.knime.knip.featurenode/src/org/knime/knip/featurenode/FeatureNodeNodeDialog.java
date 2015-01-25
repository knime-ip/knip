package org.knime.knip.featurenode;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.imagej.ops.features.FeatureSet;
import net.imglib2.util.Pair;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.featurenode.ui.FeatureSetInputPanel;
import org.knime.knip.featurenode.ui.PluginInfoComboboxItem;
import org.scijava.InstantiableException;
import org.scijava.module.ModuleException;
import org.scijava.plugin.PluginInfo;

/**
 * <code>NodeDialog</code> for the "FeatureNode" Node.
 * 
 * @author Daniel Seebacher
 * @author Tim-Oliver Buchholz
 */
public class FeatureNodeNodeDialog extends NodeDialogPane {

    private static final String FEATURE_MODEL_SETTINGS = "feature_model_settings";

    /**
     * The logger instance.
     */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FeatureNodeNodeModel.class);

    /**
     * Dialog component to select the image column.
     */
    private DialogComponentColumnNameSelection m_imgPlusSelectionCB;

    /**
     * Dialog component to select the labeling column.
     */
    private DialogComponentColumnNameSelection m_labelingSelectionCB;

    /**
     * Panel which holds all selected features with parameters.
     */
    private FeatureSetPanel m_featureSetPanel;

    public FeatureNodeNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        p.add(createColumnSelectionPanel(), c);

        c.gridy++;
        p.add(createFeatureSetSelectionPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.weighty = 1;
        c.gridy++;
        p.add(createSelectedFeaturePanel(), c);

        addTab("Configure", p);
    }

    /**
     * @return panel which holds all selected features
     */
    private JPanel createSelectedFeaturePanel() {
        m_featureSetPanel = new FeatureSetPanel();
        JScrollPane sp = new JScrollPane(m_featureSetPanel);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.setPreferredSize(new Dimension(200, 200));

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Selected Features"));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        p.add(sp, c);

        return p;
    }

    /**
     * @return panel to select a feature which should be added to
     *         {@link FeatureNodeNodeDialog#m_featureSetPanel}.
     */
    private JPanel createFeatureSetSelectionPanel() {
        List<PluginInfo<FeatureSet>> fs = OpsGateway.getPluginService()
                .getPluginsOfType(FeatureSet.class);

        PluginInfoComboboxItem[] plugins = new PluginInfoComboboxItem[fs.size()];
        for (int i = 0; i < fs.size(); i++) {
            plugins[i] = new PluginInfoComboboxItem(fs.get(i));
        }

        final JComboBox<PluginInfoComboboxItem> jComboBox = new JComboBox<PluginInfoComboboxItem>(
                plugins);

        final JButton add = new JButton("Add");
        add.setToolTipText("Add selected feature set.");
        add.setPreferredSize(jComboBox.getPreferredSize());

        add.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                FeatureSetInputPanel f;
                try {
                    f = new FeatureSetInputPanel(jComboBox.getItemAt(
                            jComboBox.getSelectedIndex()).getPluginInfo());
                    m_featureSetPanel.addFeature(f);
                    m_featureSetPanel.revalidate();
                } catch (InstantiableException | ModuleException e1) {
                    JOptionPane.showMessageDialog(getPanel(),
                            "Could not add feature.", "Feature set error",
                            JOptionPane.ERROR_MESSAGE);
                    LOGGER.error(e1.getMessage(), e1);
                }

            }
        });

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Feature selection: "));

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 4, 0, 4);
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        p.add(jComboBox, c);

        c.gridx++;
        p.add(add, c);

        return p;
    }

    /**
     * @return panel to select one or two input columns.
     */
    private JPanel createColumnSelectionPanel() {
        m_imgPlusSelectionCB = new DialogComponentColumnNameSelection(
                FeatureNodeNodeModel.createImgSelectionModel(),
                "Select image column: ", 0, false, true,
                FeatureNodeNodeModel.imgPlusFilter());
        m_imgPlusSelectionCB
                .setToolTipText("Select the image column to compute the features on.");

        m_labelingSelectionCB = new DialogComponentColumnNameSelection(
                FeatureNodeNodeModel.createLabelingSelectionModel(),
                "Select labeling column: ", 0, false, true,
                FeatureNodeNodeModel.labelingFilter());
        m_labelingSelectionCB
                .setToolTipText("Select the labeling column to compute the features on.");

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Column selection: "));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;

        p.add(m_imgPlusSelectionCB.getComponentPanel(), c);
        c.gridx++;
        p.add(m_labelingSelectionCB.getComponentPanel(), c);

        return p;
    }

    @Override
    protected void saveSettingsTo(NodeSettingsWO settings)
            throws InvalidSettingsException {
        if (m_imgPlusSelectionCB.getSelected() == null
                && m_labelingSelectionCB.getSelected() == null) {
            throw new InvalidSettingsException(
                    "Select at least one image column or one labeling column.");
        }

        m_imgPlusSelectionCB.saveSettingsTo(settings);
        m_labelingSelectionCB.saveSettingsTo(settings);
        List<FeatureSetInputPanel> features = m_featureSetPanel
                .getFeatureList();

        SettingsModelFeatureSet featureSetSettings = new SettingsModelFeatureSet(
                FEATURE_MODEL_SETTINGS);

        for (int i = 0; i < features.size(); i++) {
            Pair<Class<?>, Map<String, Object>> p = features.get(i)
                    .getSerializableInfos();
            featureSetSettings.addFeatureSet(p.getA(), p.getB());
        }

        featureSetSettings.saveSettingsForModel(settings);
    }

    @Override
    protected void loadSettingsFrom(NodeSettingsRO settings,
            DataTableSpec[] specs) throws NotConfigurableException {
        m_imgPlusSelectionCB.loadSettingsFrom(settings, specs);
        m_labelingSelectionCB.loadSettingsFrom(settings, specs);
        SettingsModelFeatureSet s = new SettingsModelFeatureSet(
                FEATURE_MODEL_SETTINGS);

        // remove all content
        m_featureSetPanel.clear();

        s.loadSettingsForDialog(settings, specs);
        List<Pair<Class<?>, Map<String, Object>>> features = s.getFeatureSets();
        for (Pair<Class<?>, Map<String, Object>> p : features) {
            try {
                m_featureSetPanel.addFeature(new FeatureSetInputPanel(
                        (Class<? extends FeatureSet<?>>) p.getA(), p.getB()));
            } catch (InstantiableException | ModuleException e) {
                JOptionPane.showMessageDialog(getPanel(),
                        "Could not add feature during load.",
                        "Feature set error", JOptionPane.ERROR_MESSAGE);
                LOGGER.error(e.getMessage(), e);
            }
        }

    }
}
