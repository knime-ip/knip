package org.knime.knip.featurenode.view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.imagej.ops.featuresets.FeatureSet;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.dialog.DialogComponentFilterSelection;
import org.knime.knip.featurenode.FeatureNodeModel;
import org.knime.knip.featurenode.model.FeatureSetInfo;
import org.knime.knip.featurenode.model.SettingsModelFeatureSet;
import org.scijava.InstantiableException;
import org.scijava.module.ModuleException;
import org.scijava.plugin.PluginInfo;

public class FeatureNodeDialogPane<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends
			NodeDialogPane {

	/**
	 * The logger instance.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FeatureNodeModel.class);

	/**
	 * Dialog component to select the image column.
	 */
	private DialogComponentColumnNameSelection m_imgSelectionComponent;

	/**
	 * Dialog component to select the labeling column.
	 */
	private DialogComponentColumnNameSelection m_labelingSelectionComponent;

	/**
	 * Dialog component to select the column creation mode;
	 */
	private DialogComponentStringSelection m_columnCreationModeComponent;

	/**
	 * Dialog component for the dimension selection.
	 */
	private DialogComponentDimSelection m_dimSelectionModelComponent;

	private DialogComponentBoolean appendLabelSegmentsComponent;
	private DialogComponentBoolean intersectionModeComponent;
	private DialogComponentBoolean appendSegmentInformationComponent;
	private DialogComponentFilterSelection<L> includeLabelsComponent;

	/**
	 * Feature Set GUI Elements
	 */
	private ColumnSelectionPanel columnSelectionPanel;
	private DimensionSelectionPanel dimensionSelectionPanel;
	private LabelSettingsPanel labelSettingsPanel;
	private FeatureSetSelectionPanel featureSetSelectionPanel;
	private FeatureSetCollectionPanel featureSetCollectionPanel;
	private SettingsModelFeatureSet smfs;

	@SuppressWarnings({"rawtypes"})
	public FeatureNodeDialogPane() {

		// initialize dialog components
		initializeDialogComponents();

		// create the col selection panel
		this.columnSelectionPanel = new ColumnSelectionPanel(
				this.m_imgSelectionComponent, this.m_labelingSelectionComponent,
				this.m_columnCreationModeComponent);

		// create the dim selection panel
		this.dimensionSelectionPanel = new DimensionSelectionPanel(
				m_dimSelectionModelComponent);

		this.featureSetSelectionPanel = new FeatureSetSelectionPanel();
		this.featureSetCollectionPanel = new FeatureSetCollectionPanel();

		// create label settings panel
		this.labelSettingsPanel = new LabelSettingsPanel(
				appendLabelSegmentsComponent, intersectionModeComponent,
				appendSegmentInformationComponent, includeLabelsComponent);

		// add action listeners
		this.featureSetSelectionPanel.getAddButton()
				.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						final PluginInfo<FeatureSet> currentlySelectedFeatureSet = FeatureNodeDialogPane.this.featureSetSelectionPanel
								.getCurrentlySelectedFeatureSet();
						
						try {
							FeatureNodeDialogPane.this.featureSetCollectionPanel
									.addFeatureSetPanel(new FeatureSetPanel(
											currentlySelectedFeatureSet));
							getTab("Feature Set Configuration").revalidate();
						} catch (final InstantiableException e1) {
							LOGGER.error("Couldn't add feature set", e1);
						} catch (final ModuleException e1) {
							LOGGER.error("Couldn't add feature set", e1);
						}
					}
				});

		final JPanel featureSetConfigPanel = new JPanel(
				new MigLayout(new LC().wrapAfter(1), new AC().grow().fill()));
		featureSetConfigPanel.add(this.featureSetSelectionPanel);

		JScrollPane selectedFeatureSetsScrollPane = new JScrollPane();
		selectedFeatureSetsScrollPane.getVerticalScrollBar()
				.setUnitIncrement(20);
		selectedFeatureSetsScrollPane
				.setViewportView(this.featureSetCollectionPanel);

		selectedFeatureSetsScrollPane.setBorder(
				BorderFactory.createTitledBorder("Selected Feature Sets:"));

		featureSetConfigPanel.add(selectedFeatureSetsScrollPane);

		featureSetConfigPanel.setPreferredSize(new Dimension(792, 500));
		this.addTab("Feature Set Configuration", featureSetConfigPanel);

		final JPanel colDimSelectionConfigPanel = new JPanel(
				new MigLayout(new LC().wrapAfter(1), new AC().grow().fill()));
		colDimSelectionConfigPanel.add(this.columnSelectionPanel);
		colDimSelectionConfigPanel.add(this.dimensionSelectionPanel);

		this.addTab("Column & Dimension Selection", colDimSelectionConfigPanel);
		this.addTab("Label Segment Settings", this.labelSettingsPanel);
	}

	@SuppressWarnings("unchecked")
	private void initializeDialogComponents() {
		// column selection
		this.m_imgSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createImgColumnModel(), "Image", 0, false,
				true, ImgPlusValue.class);
		this.m_labelingSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createLabelingColumnModel(), "Labeling", 0,
				false, true, LabelingValue.class);

		// column creation model
		this.m_columnCreationModeComponent = new DialogComponentStringSelection(
				FeatureNodeModel.createColumnCreationModeModel(),
				"Column Creation Mode", new String[]{"Append", "New Table"});

		// dimension selection
		this.m_dimSelectionModelComponent = new DialogComponentDimSelection(
				FeatureNodeModel.createDimSelectionModel(),
				"Selected Dimensions");

		// labels segment settings
		this.appendLabelSegmentsComponent = new DialogComponentBoolean(
				FeatureNodeModel.createAppendDependenciesModel(),
				"Append labels of overlapping segments");

		this.intersectionModeComponent = new DialogComponentBoolean(
				FeatureNodeModel.createIntersectionModeModel(),
				"Overlapping segments do NOT need to completely overlap");

		this.appendSegmentInformationComponent = new DialogComponentBoolean(
				FeatureNodeModel.createAppendSegmentInfoModel(),
				"Append segment information");

		this.includeLabelsComponent = new DialogComponentFilterSelection<L>(
				FeatureNodeModel.<L> createIncludeLabelModel());

		// feature set selection
		this.smfs = FeatureNodeModel.createFeatureSetsModel();
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		if ((this.m_imgSelectionComponent.getSelected() == null)
				&& (this.m_labelingSelectionComponent.getSelected() == null)) {
			throw new InvalidSettingsException(
					"Select at least one image column or one labeling column.");
		}

		this.smfs.clearFeatureSets();

		for (final FeatureSetPanel fsp : this.featureSetCollectionPanel
				.getSelectedFeatureSets()) {
			final FeatureSetInfo p = fsp.getSerializableInfos();
			this.smfs.addFeatureSet(p);
		}

		this.m_imgSelectionComponent.saveSettingsTo(settings);
		this.m_labelingSelectionComponent.saveSettingsTo(settings);
		this.m_columnCreationModeComponent.saveSettingsTo(settings);
		this.m_dimSelectionModelComponent.saveSettingsTo(settings);

		this.appendLabelSegmentsComponent.saveSettingsTo(settings);
		this.intersectionModeComponent.saveSettingsTo(settings);
		this.includeLabelsComponent.saveSettingsTo(settings);

		this.appendSegmentInformationComponent.saveSettingsTo(settings);

		this.smfs.saveSettingsTo(settings);
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		this.m_imgSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_labelingSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_columnCreationModeComponent.loadSettingsFrom(settings, specs);
		this.m_dimSelectionModelComponent.loadSettingsFrom(settings, specs);

		this.appendLabelSegmentsComponent.loadSettingsFrom(settings, specs);
		this.intersectionModeComponent.loadSettingsFrom(settings, specs);
		this.includeLabelsComponent.loadSettingsFrom(settings, specs);
		this.appendSegmentInformationComponent.loadSettingsFrom(settings,
				specs);

		try {
			this.smfs.loadSettingsFrom(settings);
		} catch (InvalidSettingsException e1) {
			throw new NotConfigurableException("Couldn't load settings", e1);
		}

		// remove all content
		this.featureSetCollectionPanel.clear();
		for (final FeatureSetInfo p : this.smfs.getFeatureSets()) {
			try {
				this.featureSetCollectionPanel
						.addFeatureSetPanel(new FeatureSetPanel(p));
			} catch (InstantiableException | ModuleException e) {
				JOptionPane.showMessageDialog(getPanel(),
						"Could not add feature during load.",
						"Feature set error", JOptionPane.ERROR_MESSAGE);
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
}
