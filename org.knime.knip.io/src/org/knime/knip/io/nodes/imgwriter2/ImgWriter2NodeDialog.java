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
package org.knime.knip.io.nodes.imgwriter2;

import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * Dialog for the Image Writer.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class ImgWriter2NodeDialog extends DefaultNodeSettingsPane {

    /**
     * Dialog with Column Selection.
     *
     */
    @SuppressWarnings("unchecked")
    public ImgWriter2NodeDialog() {
        super();

        // image column selection
        createNewGroup("Image column to save:");
        addDialogComponent(new DialogComponentColumnNameSelection(
                ImgWriter2SettingsModels.createImgColumnModel(), "", 0,
                ImgPlusValue.class));
        closeCurrentGroup();

        // output location
        addFileOptions();

        // format and compression selection
        addFormatSelection();

        // additional writer options
        addAdditionalOptions();
    }

    private void addFileOptions() {
        createNewGroup("Output");
        // directory selection
        SettingsModelString dirChooserModel = ImgWriter2SettingsModels
                .createDirectoryModel();
        addDialogComponent(new DialogComponentFileChooser(dirChooserModel,
                ImgWriter2SettingsModels.DIRHISTORY_KEY,
                JFileChooser.OPEN_DIALOG, true));

        // filename column selection
        final SettingsModelColumnName fileCol = ImgWriter2SettingsModels
                .createFileNameColumnModel();
        fileCol.setEnabled(false);
        addDialogComponent(new DialogComponentColumnNameSelection(fileCol,
                "Filename column:", 0, false, false, StringValue.class));

        // Custom filename selection
        setHorizontalPlacement(true);
        final SettingsModelString customFileNameModel = ImgWriter2SettingsModels
                .createCustomFileNameModel();
        customFileNameModel.setEnabled(false);
        final SettingsModelBoolean useCustomFileNameModel = ImgWriter2SettingsModels
                .createUseCustomFileNameModel();

        addDialogComponent(new DialogComponentBoolean(useCustomFileNameModel,
                "Custom Filename"));
        addDialogComponent(new DialogComponentString(customFileNameModel,
                "Filename Prefix"));
        setHorizontalPlacement(false);

        final SettingsModelBoolean absolutePathsModel = ImgWriter2SettingsModels
                .createAbsolutePathsModel();
        addDialogComponent(new DialogComponentBoolean(absolutePathsModel,
                "Absolute paths in the filename column"));

        useCustomFileNameModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {

                // abort if change originated from absolute path model
                if (absolutePathsModel.getBooleanValue()) {
                    return;
                }

                customFileNameModel
                        .setEnabled(useCustomFileNameModel.getBooleanValue());
                fileCol.setEnabled(!useCustomFileNameModel.getBooleanValue());
            }
        });

        absolutePathsModel.addChangeListener(new ChangeListener() {
            @Override
            // activate or deactivate models of mutually exclusive models
            public void stateChanged(final ChangeEvent e) {
                boolean enabeled = absolutePathsModel.getBooleanValue();
                dirChooserModel.setEnabled(!enabeled);
                fileCol.setEnabled(enabeled);
                useCustomFileNameModel.setEnabled(!enabeled);
                customFileNameModel.setEnabled(!enabeled);
            }
        });

        closeCurrentGroup();
    }

    private void addAdditionalOptions() {
        createNewGroup("Writer options:");
        addDialogComponent(new DialogComponentBoolean(
                ImgWriter2SettingsModels.createOverwriteModel(),
                "Overwrite existing files?"));
        addDialogComponent(new DialogComponentBoolean(
                ImgWriter2SettingsModels.createForceDirCreationModel(),
                "Create non-existing directories"));
        closeCurrentGroup();

        createNewTab("Dimension Mapping");
        final String[] labels = KNIMEKNIPPlugin.parseDimensionLabels();
        addDialogComponent(new DialogComponentStringSelection(
                ImgWriter2SettingsModels.createZMappingModel(), "Z label",
                Arrays.asList(labels)));
        addDialogComponent(new DialogComponentStringSelection(
                ImgWriter2SettingsModels.createChannelMappingModel(),
                "Channel label", Arrays.asList(labels)));
        addDialogComponent(new DialogComponentStringSelection(
                ImgWriter2SettingsModels.createTimeMappingModel(), "Time label",
                Arrays.asList(labels)));

        createNewTab("More Writer Options");
        addDialogComponent(new DialogComponentNumber(
                ImgWriter2SettingsModels.createFrameRateModel(),
                "Frames per second (if applicable)", 1));
        addDialogComponent(new DialogComponentBoolean(
                ImgWriter2SettingsModels.createWriteSequentiallyModel(),
                "Write files sequentially"));
    }

    private void addFormatSelection() {
        ImgWriter2 writer = new ImgWriter2();

        DialogComponentStringSelection formats = new DialogComponentStringSelection(
                ImgWriter2SettingsModels.createFormatModel(), "File format:",
                Arrays.asList(writer.getWriters()));

        String[] compr = writer.getCompressionTypes(
                ((SettingsModelString) formats.getModel()).getStringValue());

        boolean hasCompression = true;
        if (compr == null || compr.length == 0) {
            compr = new String[] { "Uncompressed" };
            hasCompression = false;
        } else if (compr.length == 1
                && compr[0].equals(new String("Uncompressed"))) {
            hasCompression = false;
        }

        final DialogComponentStringSelection m_compression = new DialogComponentStringSelection(
                ImgWriter2SettingsModels.createCompressionModel(),
                "Compression type", Arrays.asList(compr));
        if (hasCompression) {
            m_compression.getModel().setEnabled(true);
        } else {
            m_compression.getModel().setEnabled(false);
        }

        formats.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final String[] compr = writer.getCompressionTypes(
                        ((SettingsModelString) formats.getModel())
                                .getStringValue());

                if (compr != null && compr.length != 0) {
                    m_compression.replaceListItems(Arrays.asList(compr),
                            compr[0]);
                    m_compression.getModel().setEnabled(true);
                } else {
                    m_compression.replaceListItems(
                            Arrays.asList(new String[] { "Uncompressed" }),
                            null);
                    m_compression.getModel().setEnabled(false);
                }
            }
        });

        createNewGroup("Format selection:");
        addDialogComponent(formats);
        addDialogComponent(m_compression);
        closeCurrentGroup();

    }

}
