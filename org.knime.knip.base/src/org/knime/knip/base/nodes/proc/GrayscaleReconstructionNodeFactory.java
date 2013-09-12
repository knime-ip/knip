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

import java.util.ArrayList;
import java.util.List;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.GrayscaleReconstructionByDilation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.GrayscaleReconstructionByErosion;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author muethingc, University of Konstanz
 */
public final class GrayscaleReconstructionNodeFactory<T extends RealType<T>> extends
        TwoValuesToCellNodeFactory<ImgPlusValue<T>, ImgPlusValue<T>> {

    public enum OperationType {
        DILATE("By Dilation"), ERODE("By Erosion");

        public static final List<String> NAMES = new ArrayList<String>();

        static {
            for (final OperationType e : OperationType.values()) {
                NAMES.add(e.toString());
            }
        }

        public static OperationType value(final String name) {
            return values()[NAMES.indexOf(name)];
        }

        private final String m_name;

        private OperationType(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    private static SettingsModelString createConnectionTypeModel() {
        return new SettingsModelString("connection_type", ConnectedType.FOUR_CONNECTED.toString());
    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelString createOperationTypeModel() {
        return new SettingsModelString("operation_type", OperationType.DILATE.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<ImgPlusValue<T>, ImgPlusValue<T>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<ImgPlusValue<T>, ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Dim Selection", new DialogComponentDimSelection(
                        createDimSelectionModel(), "Dimension selection", 2, 5));

                addDialogComponent("Options", "Grayscale Reconstruction Options", new DialogComponentStringSelection(
                        createConnectionTypeModel(), "Connection Type", ConnectedType.NAMES));

                addDialogComponent("Options", "Grayscale Reconstruction Options", new DialogComponentStringSelection(
                        createOperationTypeModel(), "Operation Type", OperationType.NAMES));
            }

            @Override
            protected String getFirstColumnSelectionLabel() {
                return "Column of Image";
            }

            @Override
            protected String getSecondColumnSelectionLabel() {
                return "Column of Marker Image (BitType)";
            }
        };
    }

    @Override
    public TwoValuesToCellNodeModel<ImgPlusValue<T>, ImgPlusValue<T>, ImgPlusCell<T>> createNodeModel() {
        return new TwoValuesToCellNodeModel<ImgPlusValue<T>, ImgPlusValue<T>, ImgPlusCell<T>>() {

            private final SettingsModelString m_connection = createConnectionTypeModel();

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private ImgPlusCellFactory m_imgCellFactory;

            private final SettingsModelString m_operation = createOperationTypeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_dimSelection);
                settingsModels.add(m_connection);
                settingsModels.add(m_operation);
            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue1, final ImgPlusValue<T> cellValue2)
                    throws Exception {

                final ImgPlus<T> mask = cellValue1.getImgPlus();
                final ImgPlus<T> marker = cellValue2.getImgPlus();

                final ConnectedType type = ConnectedType.value(m_connection.getStringValue());
                UnaryOperation<Img<T>, Img<T>> op;

                if (OperationType.value(m_operation.getStringValue()) == OperationType.DILATE) {
                    op = new GrayscaleReconstructionByDilation<T, T, Img<T>, Img<T>>(type);
                } else {
                    op = new GrayscaleReconstructionByErosion<T, T, Img<T>, Img<T>>(type);
                }

                final Img<T> out =
                        SubsetOperations.iterate(op, m_dimSelection.getSelectedDimIndices(mask), mask, marker.copy(),
                                                 getExecutorService());

                return m_imgCellFactory.createCell(out, cellValue1.getMetadata());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_imgCellFactory = new ImgPlusCellFactory(exec);
            }

        };
    }

}
