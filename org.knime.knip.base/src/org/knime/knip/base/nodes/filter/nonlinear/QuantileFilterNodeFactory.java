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
package org.knime.knip.base.nodes.filter.nonlinear;

import java.util.List;

import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.iterableinterval.unary.QuantileFilter;
import net.imglib2.ops.operation.real.unary.Convert;
import net.imglib2.ops.operation.real.unary.Convert.TypeConversionTypes;
import net.imglib2.ops.operation.subset.views.ImgPlusView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.img.ImgPlusToImgPlusWrapperOp;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class QuantileFilterNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelIntegerBounded createlRadiusModel() {
        return new SettingsModelIntegerBounded("radius", 3, 1, 100);
    }

    private static SettingsModelIntegerBounded createQuantileModel() {
        return new SettingsModelIntegerBounded("quantile", 50, 1, 100);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "", new DialogComponentNumber(createQuantileModel(), "Quantile", 1));

                addDialogComponent("Options", "", new DialogComponentNumber(createlRadiusModel(), "Radius", 1));

                addDialogComponent(new DialogComponentDimSelection(createDimSelectionModel(), "Dimension selection", 2,
                        2));

            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>() {

            private ImgPlusCellFactory m_imgCellFactory;

            /*
             * Dim selection
             */
            private final SettingsModelDimSelection m_smDimSel = createDimSelectionModel();

            /*
             * Int settings model for quantile
             */
            private final SettingsModelIntegerBounded m_smQuantile = createQuantileModel();

            /*
             * Int settings model for radius
             */
            private final SettingsModelIntegerBounded m_smRadius = createlRadiusModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smDimSel);
                settingsModels.add(m_smQuantile);
                settingsModels.add(m_smRadius);
            }

            /*
             * (non-Javadoc)
             *
             * @see
             * org.knime.knip.base.node.ValueToCellNodeModel#compute
             * (org.knime.core.data.DataValue)
             */
            @SuppressWarnings("unchecked")
            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {

                final ImgPlus<T> inImg = cellValue.getImgPlus();
                final T type = cellValue.getImgPlus().firstElement();

                ImgPlus<UnsignedByteType> unsignedByteTypeImg = null;
                if (!(type instanceof UnsignedByteType)) {
                    final Convert<T, UnsignedByteType> convertOp =
                            new Convert<T, UnsignedByteType>(type, new UnsignedByteType(), TypeConversionTypes.SCALE);

                    final ConvertedRandomAccessibleInterval<T, UnsignedByteType> randomAccessibleInterval =
                            new ConvertedRandomAccessibleInterval<T, UnsignedByteType>(inImg, convertOp,
                                    new UnsignedByteType());

                    unsignedByteTypeImg =
                            new ImgPlusView<UnsignedByteType>(randomAccessibleInterval, inImg.factory()
                                    .imgFactory(new UnsignedByteType()), inImg);
                } else {
                    unsignedByteTypeImg = (ImgPlus<UnsignedByteType>)inImg;
                }

                ImgPlus<UnsignedByteType> wrappedImgPlus = new ImgPlus<UnsignedByteType>(unsignedByteTypeImg, inImg);

                ImgPlusToImgPlusWrapperOp<UnsignedByteType, UnsignedByteType> wrappedOp =
                        new ImgPlusToImgPlusWrapperOp<UnsignedByteType, UnsignedByteType>(
                                new QuantileFilter<UnsignedByteType>(m_smRadius.getIntValue(),
                                        m_smQuantile.getIntValue()), new UnsignedByteType());

                final Img<UnsignedByteType> unsignedByteTypeResImg =
                        SubsetOperations.iterate(wrappedOp, m_smDimSel.getSelectedDimIndices(inImg), wrappedImgPlus,
                                                 wrappedOp.bufferFactory().instantiate(wrappedImgPlus),
                                                 getExecutorService()).getImg();

                Img<T> resImg = null;
                if (!(type instanceof UnsignedByteType)) {
                    final Convert<UnsignedByteType, T> convertOp =
                            new Convert<UnsignedByteType, T>(new UnsignedByteType(), type, TypeConversionTypes.SCALE);

                    final ConvertedRandomAccessibleInterval<UnsignedByteType, T> randomAccessibleInterval =
                            new ConvertedRandomAccessibleInterval<UnsignedByteType, T>(unsignedByteTypeResImg,
                                    convertOp, type);

                    resImg = new ImgPlusView<T>(randomAccessibleInterval, inImg.factory(), inImg);
                } else {
                    resImg = (Img<T>)unsignedByteTypeResImg;
                }

                return m_imgCellFactory.createCell(resImg, cellValue.getMetadata());

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
