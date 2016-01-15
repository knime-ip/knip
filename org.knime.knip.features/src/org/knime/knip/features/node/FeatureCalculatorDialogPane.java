/*
 * ------------------------------------------------------------------------
 *
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
  ---------------------------------------------------------------------
 *
 */

package org.knime.knip.features.node;

import javax.swing.JOptionPane;

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
import org.knime.knip.features.node.model.FeatureSetInfo;
import org.knime.knip.features.node.model.SettingsModelFeatureSet;
import org.knime.knip.features.node.ui.ColumnSelectionPanel;
import org.knime.knip.features.node.ui.DimensionSelectionPanel;
import org.knime.knip.features.node.ui.FeatureSetCollectionPanel;
import org.knime.knip.features.node.ui.FeatureSetConfigPanel;
import org.knime.knip.features.node.ui.FeatureSetPanel;
import org.knime.knip.features.node.ui.FeatureSetSelectionPanel;
import org.knime.knip.features.node.ui.LabelSettingsPanel;
import org.scijava.InstantiableException;
import org.scijava.module.ModuleException;

public class FeatureCalculatorDialogPane<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends NodeDialogPane {

	/**
	 * The logger instance.
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(FeatureCalculatorModel.class);

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

	private DialogComponentBoolean appendLabelsOfOverlappingSegments;
	private DialogComponentBoolean intersectionModeComponent;
	private DialogComponentBoolean appendSegmentInformationComponent;
	private DialogComponentFilterSelection<L> includeLabelsComponent;

	/**
	 * Feature Set GUI Elements
	 */
	private final ColumnSelectionPanel columnSelectionPanel;
	private final DimensionSelectionPanel dimensionSelectionPanel;
	private final LabelSettingsPanel labelSettingsPanel;
	private final FeatureSetSelectionPanel featureSetSelectionPanel;
	public final FeatureSetCollectionPanel featureSetCollectionPanel;
	private final FeatureSetConfigPanel featureSetConfigPanel;
	private SettingsModelFeatureSet smfs;

	public FeatureCalculatorDialogPane() {

		// initialize dialog components
		initializeDialogComponents();

		// create the column selection panel
		this.columnSelectionPanel = new ColumnSelectionPanel(this.m_imgSelectionComponent,
				this.m_labelingSelectionComponent, this.m_columnCreationModeComponent);

		// create the dim selection panel
		this.dimensionSelectionPanel = new DimensionSelectionPanel(m_dimSelectionModelComponent);

		// create the feature set collection panel
		this.featureSetCollectionPanel = new FeatureSetCollectionPanel();

		// create the feature set selection panel
		this.featureSetSelectionPanel = new FeatureSetSelectionPanel(featureSetCollectionPanel);

		// create label settings panel
		this.labelSettingsPanel = new LabelSettingsPanel(appendLabelsOfOverlappingSegments, intersectionModeComponent,
				appendSegmentInformationComponent, includeLabelsComponent);

		// arrange all components
		this.featureSetConfigPanel = new FeatureSetConfigPanel(columnSelectionPanel, dimensionSelectionPanel,
				featureSetSelectionPanel, featureSetCollectionPanel);

		this.addTab("Feature Set Configuration", this.featureSetConfigPanel);

		this.addTab("Label Segment Settings", this.labelSettingsPanel);
	}

	@SuppressWarnings("unchecked")
	private void initializeDialogComponents() {
		// column selection
		this.m_imgSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureCalculatorModel.createImgColumnModel(), "Image", 0, false, true, ImgPlusValue.class);

		// labeling selection
		this.m_labelingSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureCalculatorModel.createLabelingColumnModel(), "Labeling", 0, false, true, LabelingValue.class);

		// column creation model
		this.m_columnCreationModeComponent = new DialogComponentStringSelection(
				FeatureCalculatorModel.createColumnCreationModeModel(), "Column Creation Mode",
				FeatureCalculatorModel.COL_CREATION_MODES);

		// dimension selection
		this.m_dimSelectionModelComponent = new DialogComponentDimSelection(
				FeatureCalculatorModel.createDimSelectionModel(), "Selected Dimensions");

		// labels segment settings
		this.appendLabelsOfOverlappingSegments = new DialogComponentBoolean(
				FeatureCalculatorModel.createAppendLabelsOfOverlappingSegments(),
				"Append labels of overlapping segments");

		// intersection segment settings
		this.intersectionModeComponent = new DialogComponentBoolean(
				FeatureCalculatorModel.createIntersectionModeModel(),
				"Overlapping segments do NOT need to completely overlap");

		// append segment settings
		this.appendSegmentInformationComponent = new DialogComponentBoolean(
				FeatureCalculatorModel.createAppendSegmentInfoModel(), "Append segment information");

		// include segment settings
		this.includeLabelsComponent = new DialogComponentFilterSelection<L>(
				FeatureCalculatorModel.<L> createIncludeLabelModel());

		// feature set selection
		this.smfs = FeatureCalculatorModel.createFeatureSetsModel();
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		if ((this.m_imgSelectionComponent.getSelected() == null)
				&& (this.m_labelingSelectionComponent.getSelected() == null)) {
			throw new InvalidSettingsException("Select at least an image column or a labeling column.");
		}

		this.smfs.clearFeatureSets();

		for (final FeatureSetPanel fsp : this.featureSetCollectionPanel.getSelectedFeatureSets()) {
			final FeatureSetInfo p = fsp.getSerializableInfos();
			this.smfs.addFeatureSet(p);
		}

		this.m_imgSelectionComponent.saveSettingsTo(settings);
		this.m_labelingSelectionComponent.saveSettingsTo(settings);
		this.m_columnCreationModeComponent.saveSettingsTo(settings);
		this.m_dimSelectionModelComponent.saveSettingsTo(settings);

		this.appendLabelsOfOverlappingSegments.saveSettingsTo(settings);
		this.intersectionModeComponent.saveSettingsTo(settings);
		this.includeLabelsComponent.saveSettingsTo(settings);

		this.appendSegmentInformationComponent.saveSettingsTo(settings);

		this.smfs.saveSettingsTo(settings);
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {
		this.m_imgSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_labelingSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_columnCreationModeComponent.loadSettingsFrom(settings, specs);
		this.m_dimSelectionModelComponent.loadSettingsFrom(settings, specs);

		this.appendLabelsOfOverlappingSegments.loadSettingsFrom(settings, specs);
		this.intersectionModeComponent.loadSettingsFrom(settings, specs);
		this.includeLabelsComponent.loadSettingsFrom(settings, specs);
		this.appendSegmentInformationComponent.loadSettingsFrom(settings, specs);

		try {
			this.smfs.loadSettingsFrom(settings);
		} catch (InvalidSettingsException e1) {
			throw new NotConfigurableException("Couldn't load settings", e1);
		}

		// remove all content
		this.featureSetCollectionPanel.clear();
		for (final FeatureSetInfo p : this.smfs.getFeatureSetInfos()) {
			try {
				this.featureSetCollectionPanel.addFeatureSetPanel(new FeatureSetPanel(p));
			} catch (InstantiableException | ModuleException e) {
				JOptionPane.showMessageDialog(getPanel(), "Could not add feature during load.", "Feature set error",
						JOptionPane.ERROR_MESSAGE);
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
}
