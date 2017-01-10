/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.base.nodes.seg.cropper;

import org.knime.core.node.NodeDialog;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.dialog.DialogComponentFilterSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.base.nodes.seg.cropper.SegmentCropperNodeSettings.BACKGROUND_OPTION;
import org.knime.knip.core.util.EnumUtils;

/**
 * {@link NodeDialog} for {@link SegmentCropperNodeModel}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <L>
 *
 */
public class SegmentCropperNodeDialog<L extends Comparable<L>> extends DefaultNodeSettingsPane {

    /**
     * Default Constructor
     */
    @SuppressWarnings("unchecked")
    public SegmentCropperNodeDialog() {
        super();
        final SettingsModelString imgSelectionModel = SegmentCropperNodeSettings.createImgColumnSelectionModel();
        final SettingsModelString backgroundModel = SegmentCropperNodeSettings.createBackgroundSelectionModel();

        createNewGroup("Column Selection");
        addDialogComponent(new DialogComponentColumnNameSelection(
                SegmentCropperNodeSettings.createSMLabelingColumnSelection(), "Labeling Column", 0, true,
                LabelingValue.class));

        imgSelectionModel.addChangeListener(e -> {
            if (imgSelectionModel.getStringValue() != null) {
                backgroundModel.setEnabled(!"".equals(imgSelectionModel.getStringValue()));
            } else if (backgroundModel != null) {
                backgroundModel.setEnabled(false);
            }
        });

        // add some change listeners
        backgroundModel.setEnabled(!"".equals(imgSelectionModel.getStringValue()));

        addDialogComponent(new DialogComponentColumnNameSelection(imgSelectionModel, "Image Column (optional)", 0,
                false, true, ImgPlusValue.class));

        closeCurrentGroup();

        createNewGroup("Background");

        addDialogComponent(new DialogComponentStringSelection(backgroundModel, "Background",
                EnumUtils.getStringCollectionFromToString(BACKGROUND_OPTION.values())));

        closeCurrentGroup();

        createNewTab("Segment Label Filter");

        createNewGroup("Filter on segment labels");
        addDialogComponent(new DialogComponentFilterSelection<L>(SegmentCropperNodeSettings.createLabelFilterModel()));
        closeCurrentGroup();

        createNewGroup("Overlapping segment labels");
        final SettingsModelBoolean addDependencies = SegmentCropperNodeSettings.createAddOverlappingLabels();
        final SettingsModelFilterSelection<L> nonRoiFilterModel =
                SegmentCropperNodeSettings.createOverlappingLabelFilterModel(false);
        final SettingsModelBoolean noCompleteOverlap =
                SegmentCropperNodeSettings.createNotEnforceCompleteOverlapModel(false);

        addDependencies.addChangeListener(e -> {
            nonRoiFilterModel.setEnabled(addDependencies.getBooleanValue());
            noCompleteOverlap.setEnabled(addDependencies.getBooleanValue());
        });

        addDialogComponent(new DialogComponentBoolean(addDependencies, "Append labels of overlapping segments"));
        addDialogComponent(new DialogComponentBoolean(noCompleteOverlap,
                "Overlapping segments do NOT need to completely overlap"));
        addDialogComponent(new DialogComponentFilterSelection<L>(nonRoiFilterModel));
        closeCurrentGroup();

    }
}
