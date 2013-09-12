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
package org.knime.knip.base.nodes.proc;

import java.util.List;

import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.interval.MaximumFinder;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;

/**
 * 
 * @author Tino Klingebiel, University of Konstanz
 */
public class MaximumFinderNodeFactory<T extends RealType<T> & NativeType<T>> extends
        ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelDouble createNoiseModel() {
        return new SettingsModelDouble("Noise Tolerance", 0);
    }

    private static SettingsModelDouble createSuppressionModel() {
        return new SettingsModelDouble("Suppression", 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<BitType>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<BitType>>() {

            private SettingsModelDouble m_noise = createNoiseModel();

            private SettingsModelDouble m_suppression = createSuppressionModel();

            private SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private ImgPlusCellFactory m_cellFactory;

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_cellFactory = new ImgPlusCellFactory(exec);
            }

            @Override
            protected ImgPlusCell<BitType> compute(final ImgPlusValue<T> cellValue) throws Exception {

                // Get the img
                ImgPlus<T> img = cellValue.getImgPlus();

                // ImgPlus (Img+Metadata -> We need to set the metadata from the
                // incoming img).
                // Output may be ImgPlus as ImgPlus extends Img and Img is
                // nothing else than RandomAccessibleInterval and
                // IterableInterval
                ImgPlus<BitType> output =
                        new ImgPlus<BitType>(new ArrayImgFactory<BitType>().create(cellValue.getDimensions(),
                                                                                   new BitType()), img);

                // We iterate (according to the dim selection) over each plane,
                // cube, hypercube or whatever and run our operation. Results is
                // written into the according position of the plane, cube,
                // hypercube, etc of the output
                SubsetOperations
                        .iterate(new MaximumFinder<T>(m_noise.getDoubleValue(), m_suppression.getDoubleValue()),
                                 m_dimSelection.getSelectedDimIndices(img), img, output, getExecutorService());

                // Simply return the output
                return m_cellFactory.createCell(output);

                /*
                 * return
                 * (ImgPlusCell<BitType>)m_cellFactory.createCell((ImgPlus
                 * <BitType>)(new MaximumOperation<T>( m_noise.getDoubleValue(),
                 * m_suppression
                 * .getDoubleValue(),cellValue.getDimensions().length
                 * ).compute(input, output)));
                 */
            }

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_noise);
                settingsModels.add(m_suppression);
                settingsModels.add(m_dimSelection);
            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Options", new DialogComponentNumber(createNoiseModel(),
                        "Noise Tolerance", 0.5));
                addDialogComponent("Options", "Options", new DialogComponentNumber(createSuppressionModel(),
                        "Supression", 0.5));
                addDialogComponent(new DialogComponentDimSelection(createDimSelectionModel(), "Dimension Selection"));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescriptionContent(final KnimeNode node) {
        DialogComponentDimSelection.createNodeDescription(node.getFullDescription().getTabList().get(0).addNewOption());
    }
}
