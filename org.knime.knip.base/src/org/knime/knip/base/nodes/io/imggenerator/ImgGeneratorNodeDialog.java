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
package org.knime.knip.base.nodes.io.imggenerator;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Dialog for the ImageReader to select the files and choose some additional options.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgGeneratorNodeDialog extends DefaultNodeSettingsPane {

    private final SettingsModelIntegerBounded m_numImgs = ImgGeneratorNodeModel.createCFGNumImg();

    private final SettingsModelString m_smFactory = ImgGeneratorNodeModel.createCFGFactory();

    private final SettingsModelBoolean m_smRandomFill = ImgGeneratorNodeModel.createCFGFillRandom();

    private final SettingsModelBoolean m_smRandomSize = ImgGeneratorNodeModel.createCFGRandomSize();

    private final SettingsModelIntegerBounded m_smSizeChannel = ImgGeneratorNodeModel.createCFGSizeChannel();

    private final SettingsModelIntegerBounded m_smSizeT = ImgGeneratorNodeModel.createCFGSizeT();

    private final SettingsModelIntegerBounded m_smSizeX = ImgGeneratorNodeModel.createCFGSizeX();

    private final SettingsModelIntegerBounded m_smSizeY = ImgGeneratorNodeModel.createCFGSizeY();

    private final SettingsModelIntegerBounded m_smSizeZ = ImgGeneratorNodeModel.createCFGSizeZ();

    private final SettingsModelString m_smType = ImgGeneratorNodeModel.createCFGType();

    private final SettingsModelDouble m_smValue = ImgGeneratorNodeModel.createCFGValue();

    private final SettingsModelIntegerBounded m_smSizeX_Min = ImgGeneratorNodeModel.createCFGSizeXMin();

    private final SettingsModelIntegerBounded m_smSizeY_Min = ImgGeneratorNodeModel.createCFGSizeYMin();

    private final SettingsModelIntegerBounded m_smSizeZ_Min = ImgGeneratorNodeModel.createCFGSizeZMin();

    private final SettingsModelIntegerBounded m_smSizeChannel_Min = ImgGeneratorNodeModel.createCFGSizeChannelMin();

    private final SettingsModelIntegerBounded m_smSizeT_Min = ImgGeneratorNodeModel.createCFGSizeTMin();

    ImgGeneratorNodeDialog() {

        createNewGroup("How many images shall be created?");
        addDialogComponent(new DialogComponentNumber(m_numImgs, "", 1));

        createNewGroup("Img size");
        addDialogComponent(new DialogComponentBoolean(m_smRandomSize, "Random size in bounds"));

        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentNumber(m_smSizeX_Min, "Size X", 1));
        addDialogComponent(new DialogComponentNumber(m_smSizeX, "", 1));
        setHorizontalPlacement(false);

        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentNumber(m_smSizeY_Min, "Size Y", 1));
        addDialogComponent(new DialogComponentNumber(m_smSizeY, "", 1));
        setHorizontalPlacement(false);

        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentNumber(m_smSizeZ_Min, "Size Z", 1));
        addDialogComponent(new DialogComponentNumber(m_smSizeZ, "", 1));
        setHorizontalPlacement(false);

        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentNumber(m_smSizeChannel_Min, "Size Channel", 1));
        addDialogComponent(new DialogComponentNumber(m_smSizeChannel, "", 1));
        setHorizontalPlacement(false);

        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentNumber(m_smSizeT_Min, "Size Time", 1));
        addDialogComponent(new DialogComponentNumber(m_smSizeT, "", 1));
        setHorizontalPlacement(false);

        createNewGroup("Pixel Type");
        addDialogComponent(new DialogComponentStringSelection(m_smType, "", ImgGeneratorNodeModel.SUPPORTED_TYPES));

        createNewGroup("Factory Type");
        addDialogComponent(new DialogComponentStringSelection(m_smFactory, "",
                ImgGeneratorNodeModel.SUPPORTED_FACTORIES));

        createNewGroup("Image content");
        addDialogComponent(new DialogComponentBoolean(m_smRandomFill, "Random filling within types bounds"));

        addDialogComponent(new DialogComponentNumber(m_smValue, "Constant fill value (will be scaled to pixel type)", 1));
    }
}
