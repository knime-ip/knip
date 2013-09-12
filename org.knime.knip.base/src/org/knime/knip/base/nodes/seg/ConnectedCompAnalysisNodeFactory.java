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
package org.knime.knip.base.nodes.seg;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.CCA;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgUtils;

/**
 * Factory class to produce a Connected Component Analysis Node.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ConnectedCompAnalysisNodeFactory<T extends RealType<T> & Comparable<T> & NativeType<T>> extends
        ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelInteger createBackgroundModel() {
        return new SettingsModelInteger("background", -128);
    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimensions", "X", "Y");
    }

    private static SettingsModelString createFactoryModel() {
        return new SettingsModelString("factoryselection", ImgFactoryTypes.SOURCE_FACTORY.toString());
    }

    private static SettingsModelString createTypeModel() {
        return new SettingsModelString("connection_type", ConnectedType.values()[0].toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Settings", "Factory Selection", new DialogComponentStringSelection(
                        createFactoryModel(), "Factory Type", EnumListProvider.getStringList(ImgFactoryTypes.values())));

                addDialogComponent("Options", "Settings", new DialogComponentStringSelection(createTypeModel(),
                        "Connection Type", EnumListProvider.getStringList(ConnectedType.values())));
                addDialogComponent("Options", "Settings", new DialogComponentNumber(createBackgroundModel(),
                        "Background", 1));

                addDialogComponent("Options", "Dimensions", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimensions", 2, 5));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, LabelingCell<Integer>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, LabelingCell<Integer>>() {

            private final SettingsModelInteger m_background = createBackgroundModel();

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private final SettingsModelString m_factory = createFactoryModel();

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelString m_type = createTypeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_factory);
                settingsModels.add(m_background);
                settingsModels.add(m_dimSelection);
                settingsModels.add(m_type);

            }

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings({"unchecked"})
            @Override
            protected LabelingCell<Integer> compute(final ImgPlusValue<T> cellValue) throws IOException {
                final ImgPlus<T> img = cellValue.getImgPlus();
                final T background = img.firstElement().createVariable();
                //                if (background instanceof RealType) {
                background.setReal(m_background.getIntValue());
                //                else {
                //                    setWarningMessage("Can't use the specified value as the input image isn't numeric.");
                //                }

                long[][] structuringElement;
                if (m_type.getStringValue().equals(ConnectedType.EIGHT_CONNECTED.name())) {
                    structuringElement =
                            AbstractRegionGrowing
                                    .get8ConStructuringElement(m_dimSelection.getSelectedDimIndices(img).length);
                } else {
                    structuringElement =
                            AbstractRegionGrowing
                                    .get4ConStructuringElement(m_dimSelection.getSelectedDimIndices(img).length);
                }

                final CCA<T, Img<T>, Labeling<Integer>> cca =
                        new CCA<T, Img<T>, Labeling<Integer>>(structuringElement, background);

                final Labeling<Integer> lab =
                        new NativeImgLabeling<Integer, IntType>(ImgUtils.<IntType> createEmptyCopy(ImgFactoryTypes
                                .getImgFactory(m_factory.getStringValue(), img.getImg()), img, new IntType()));

                // TODO: Logger
                try {
                    SubsetOperations.iterate(cca, m_dimSelection.getSelectedDimIndices(img), img, lab,
                                             getExecutorService());
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                } catch (final ExecutionException e) {
                    e.printStackTrace();
                }

                return m_labCellFactory.createCell(lab, new DefaultLabelingMetadata(img, img, img,
                        new DefaultLabelingColorTable()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labCellFactory = new LabelingCellFactory(exec);
            }
        };
    }
}
