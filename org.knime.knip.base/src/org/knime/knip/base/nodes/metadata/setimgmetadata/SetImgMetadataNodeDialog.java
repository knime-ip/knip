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
package org.knime.knip.base.nodes.metadata.setimgmetadata;

import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SetImgMetadataNodeDialog extends DefaultNodeSettingsPane {

    public SetImgMetadataNodeDialog() {
        createNewGroup("Image selection");
        addDialogComponent(new DialogComponentColumnNameSelection(SetImgMetadataNodeModel.createImgColModel(), "Image",
                0, ImgPlusValue.class));
        closeCurrentGroup();

        createNewGroup("Image name and source");
        addDialogComponent(new DialogComponentColumnNameSelection(SetImgMetadataNodeModel.createNameColModel(),
                "Image Name", 0, false, true, StringValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(SetImgMetadataNodeModel.createSourceColModel(),
                "Image Source", 0, false, true, StringValue.class));

        closeCurrentGroup();

        createNewGroup("Axis labels");
        addDialogComponent(new DialogComponentLabel("If no label is specified, the axis label will not be modified."));

        final SettingsModelString[] dimModels = SetImgMetadataNodeModel.createDimLabelModels();
        int i = 0;
        for (final SettingsModelString sms : dimModels) {
            addDialogComponent(new DialogComponentString(sms, "Label of Dimension " + (i++)));
        }

        closeCurrentGroup();

        createNewTab("Calibration");

        final SettingsModelDouble[] calModels = SetImgMetadataNodeModel.createCalibrationModels();
        addDialogComponent(new DialogComponentLabel("Calibration for each dimension."));
        addDialogComponent(new DialogComponentLabel("If 0, the calibration for that dimension will not be modified."));
        i = 0;
        for (final SettingsModelDouble sms : calModels) {
            addDialogComponent(new DialogComponentNumber(sms, "Calibration for Dimension " + (i++), .1));
        }

        closeCurrentGroup();

        createNewTab("Offsets");
        final SettingsModelString[] offsetModels = SetImgMetadataNodeModel.createOffSetColModels();
        addDialogComponent(new DialogComponentLabel("Offset in each dimension."));
        addDialogComponent(new DialogComponentLabel("If <none>, the offset for that dimension will not be modified."));
        i = 0;
        for (final SettingsModelString sms : offsetModels) {
            addDialogComponent(new DialogComponentColumnNameSelection(sms, "Offset in Dimension " + (i++), 0, false,
                    true, LongValue.class));
        }

    }

}
