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
package org.knime.knip.base.nodes.proc.binary;

import java.util.List;

import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.operation.real.binary.RealAnd;
import net.imglib2.ops.operation.real.binary.RealOr;
import net.imglib2.ops.operation.real.binary.RealXor;
import net.imglib2.ops.operation.subset.views.ImgPlusView;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgUtils;
import org.knime.knip.core.util.MiscViews;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class BinaryArithmeticNodeFactory extends
        TwoValuesToCellNodeFactory<ImgPlusValue<BitType>, ImgPlusValue<BitType>> {

    public enum Method {
        AND, OR, XOR;
    }

    private static NodeLogger LOGGER = NodeLogger.getLogger(BinaryArithmeticNodeFactory.class);

    private static SettingsModelString createMethodNameModel() {
        return new SettingsModelString("method", Method.values()[0].toString());
    }

    private static SettingsModelBoolean createVirtuallySynchronizeModel() {
        return new SettingsModelBoolean("synchronize", false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<ImgPlusValue<BitType>, ImgPlusValue<BitType>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<ImgPlusValue<BitType>, ImgPlusValue<BitType>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Binary operation",
                                   new DialogComponentStringSelection(createMethodNameModel(), "Method",
                                           EnumListProvider.getStringList(BinaryArithmeticNodeFactory.Method.values())));

                addDialogComponent("Options", "", new DialogComponentBoolean(createVirtuallySynchronizeModel(),
                        "Virtually Extend?"));

            }

            @Override
            protected String getFirstColumnSelectionLabel() {
                return "First BitType Image";
            }

            @Override
            protected String getSecondColumnSelectionLabel() {
                return "Second BitType Image";
            }
        };
    }

    /**
     *
     */
    @Override
    public TwoValuesToCellNodeModel<ImgPlusValue<BitType>, ImgPlusValue<BitType>, ImgPlusCell<BitType>>
            createNodeModel() {
        return new TwoValuesToCellNodeModel<ImgPlusValue<BitType>, ImgPlusValue<BitType>, ImgPlusCell<BitType>>() {

            private ImgPlusCellFactory m_imgCellFactory;

            private final SettingsModelString m_methodName = createMethodNameModel();

            private BinaryOperationAssignment<BitType, BitType, BitType> m_op;

            private final SettingsModelBoolean m_synchronize = createVirtuallySynchronizeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_methodName);
                settingsModels.add(m_synchronize);
            }

            /**
             *
             */
            @Override
            protected ImgPlusCell<BitType> compute(final ImgPlusValue<BitType> cellValue1,
                                                   final ImgPlusValue<BitType> cellValue2) throws Exception {

                if (!(cellValue1.getImgPlus().firstElement() instanceof BitType)
                        || !(cellValue2.getImgPlus().firstElement() instanceof BitType)) {
                    LOGGER.warn("Input images are not BitType. They will be processed anyway and treated as BitType images");
                }

                final ImgPlus<BitType> img1 = cellValue1.getImgPlus();
                ImgPlus<BitType> img2 = cellValue2.getImgPlus();
                final Img<BitType> out = ImgUtils.createEmptyImg(cellValue1.getImgPlus());

                if (m_synchronize.getBooleanValue()) {
                    img2 =
                            new ImgPlusView<BitType>(MiscViews.synchronizeDimensionality(img2, img2, img1, img1),
                                    img1.factory(), img1);
                }

                IterableInterval<BitType> iiOut = out;
                IterableInterval<BitType> iiIn1 = img1;
                IterableInterval<BitType> iiIn2 = img2;

                if (!Util.equalIterationOrder(img1, img2)) {
                    iiIn1 = Views.flatIterable(img1);
                    iiIn2 = Views.flatIterable(img2);
                    iiOut = Views.flatIterable(out);
                }
                m_op.compute(iiIn1, iiIn2, iiOut);

                return m_imgCellFactory.createCell(out, cellValue1.getMetadata());

            }

            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                switch (Method.valueOf(m_methodName.getStringValue())) {
                    case AND:
                        m_op =
                                new BinaryOperationAssignment<BitType, BitType, BitType>(
                                        new RealAnd<BitType, BitType, BitType>());
                        break;
                    case OR:
                        m_op =
                                new BinaryOperationAssignment<BitType, BitType, BitType>(
                                        new RealOr<BitType, BitType, BitType>());
                        break;
                    case XOR:
                        m_op =
                                new BinaryOperationAssignment<BitType, BitType, BitType>(
                                        new RealXor<BitType, BitType, BitType>());
                        break;
                }
                m_imgCellFactory = new ImgPlusCellFactory(exec);
            }

        };
    }

}
