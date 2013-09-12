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

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.img.unary.ImgRotate2D;
import net.imglib2.ops.operation.iterable.unary.Min;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class Rotation2DNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelDouble createAngleModel() {
        return new SettingsModelDouble("angle", 0);
    }

    private static SettingsModelInteger createCenterDim1Model() {
        return new SettingsModelInteger("center_dim1", -1);
    }

    private static SettingsModelInteger createCenterDim2Model() {
        return new SettingsModelInteger("center_dim2", -1);
    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelBoolean createKeepSizeModel() {
        return new SettingsModelBoolean("keep_size", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "", new DialogComponentNumber(createAngleModel(), "Angle", .01));
                addDialogComponent("Options", "", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Rotation dimensions", 2, 2));
                addDialogComponent("Options", "", new DialogComponentBoolean(createKeepSizeModel(), "Keep size"));
                addDialogComponent("Options", "", new DialogComponentNumber(createCenterDim1Model(), "Center Dim 1", 1));
                addDialogComponent("Options", "", new DialogComponentNumber(createCenterDim2Model(), "Center Dim 2", 1));

            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>() {

            private final SettingsModelDouble m_angle = createAngleModel();

            private final SettingsModelInteger m_centerDim1 = createCenterDim1Model();

            private final SettingsModelInteger m_centerDim2 = createCenterDim2Model();

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private ImgPlusCellFactory m_imgCellFactory;

            private final SettingsModelBoolean m_keepSize = createKeepSizeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_angle);
                settingsModels.add(m_dimSelection);
                settingsModels.add(m_keepSize);
                settingsModels.add(m_centerDim1);
                settingsModels.add(m_centerDim2);
            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {
                final ImgPlus<T> srcImg = cellValue.getImgPlus();

                final int[] dimIndices = m_dimSelection.getSelectedDimIndices(srcImg);
                if (dimIndices.length != 2) {
                    throw new KNIPException("Two dimensions have to be selected for the rotation!");
                }

                long[] center = null;
                if ((m_centerDim1.getIntValue() >= 0) && (m_centerDim2.getIntValue() >= 0)) {
                    center = new long[srcImg.numDimensions()];
                    center[dimIndices[0]] = m_centerDim1.getIntValue();
                    center[dimIndices[1]] = m_centerDim2.getIntValue();

                }

                final T min = new Min<T, T>().compute(srcImg.cursor(), srcImg.firstElement().createVariable());
                final ImgRotate2D<T> rot =
                        new ImgRotate2D<T>(m_angle.getDoubleValue(), dimIndices[0], dimIndices[1],
                                m_keepSize.getBooleanValue(), min, center);

                return m_imgCellFactory.createCell(Operations.compute(rot, srcImg), srcImg);
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
