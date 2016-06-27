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

import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.core.util.MiscViews;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeAnd;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeCongruent;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeDifference;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeIntersect;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeMerge;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeXOR;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <I1>
 * @param <I2>
 * @param <O>
 */
public final class LabelingArithmeticNodeFactory<I1, I2, O extends Comparable<O>> extends
        TwoValuesToCellNodeFactory<LabelingValue<I1>, LabelingValue<I2>> {

    /**
     * Method which is performed for the labeling arithmetic operation
     *
     * @author dietzc
     */
    public enum Method {
        AND, CONGRUENT, DIFFERENCE, INTERSECT, MERGE, XOR;
    }

    private NodeLogger LOGGER = NodeLogger.getLogger(LabelingArithmeticNodeFactory.class);

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
    protected TwoValuesToCellNodeDialog<LabelingValue<I1>, LabelingValue<I2>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<LabelingValue<I1>, LabelingValue<I2>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options",
                                   "Labeling Operation",
                                   new DialogComponentStringSelection(createMethodNameModel(), "Method", EnumUtils
                                           .getStringListFromName(LabelingArithmeticNodeFactory.Method.values())));

                addDialogComponent("Options", "", new DialogComponentBoolean(createVirtuallySynchronizeModel(),
                        "Virtually Extend?"));

            }

            @Override
            protected String getFirstColumnSelectionLabel() {
                return "First Labeling";
            }

            @Override
            protected String getSecondColumnSelectionLabel() {
                return "Second Labeling";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_la";
            }
        };
    }

    /**
     *
     */
    @Override
    public TwoValuesToCellNodeModel<LabelingValue<I1>, LabelingValue<I2>, LabelingCell<O>> createNodeModel() {
        return new TwoValuesToCellNodeModel<LabelingValue<I1>, LabelingValue<I2>, LabelingCell<O>>() {

            private LabelingCellFactory m_labelingCellFactory;

            private final SettingsModelString m_methodName = createMethodNameModel();

            private final SettingsModelBoolean m_synchronize = createVirtuallySynchronizeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_methodName);
                settingsModels.add(m_synchronize);

            }

            @SuppressWarnings("unchecked")
            @Override
            protected LabelingCell<O> compute(final LabelingValue<I1> cellValue1, final LabelingValue<I2> cellValue2)
                    throws Exception {

                final RandomAccessibleInterval<LabelingType<I1>> lab1 = cellValue1.getLabeling();
                final RandomAccessibleInterval<LabelingType<I2>> lab2 = cellValue2.getLabeling();
                boolean stringBased = false;

                LabelRegions<I1> regions1 = KNIPGateway.regions().regions(lab1);
                LabelRegions<I2> regions2 = KNIPGateway.regions().regions(lab2);

                if (regions1.getExistingLabels().size() > 0 && regions2.getExistingLabels().size() > 0) {
                    if (!(regions1.getExistingLabels().iterator().next().getClass().isAssignableFrom(regions2
                            .getExistingLabels().iterator().next().getClass()))) {

                        LOGGER.debug("Labeling types are not compatible. Using Strings for comparsion. The resulting labeling will also be String based");
                        stringBased = true;
                    }
                }

                RandomAccessibleInterval<LabelingType<I2>> synchronizedLab = lab2;
                if (m_synchronize.getBooleanValue()) {
                    synchronizedLab =
                            MiscViews.synchronizeDimensionality(lab2, cellValue2.getLabelingMetadata(), lab1,
                                                                cellValue1.getLabelingMetadata());
                }

                RandomAccessibleInterval<LabelingType<O>> res = null;
                if (stringBased) {
                    res = KNIPGateway.ops().create().imgLabeling(cellValue1.getLabeling());

                    final RandomAccessibleInterval<LabelingType<String>> lab1ToProcess =
                            Converters.convert(lab1, new LToString<I1, String>(), (LabelingType<String>)Util
                                    .getTypeFromInterval(lab1).createVariable());

                    final RandomAccessibleInterval<LabelingType<String>> lab2ToProcess =
                            Converters.convert(synchronizedLab, new LToString<I2, String>(), (LabelingType<String>)Util
                                    .getTypeFromInterval(lab1).createVariable());

                    this.getOp().compute(Views.iterable(lab1ToProcess), Views.iterable(lab2ToProcess),
                                         Views.iterable(res));
                } else {
                    res = KNIPGateway.ops().create().imgLabeling(cellValue1.getLabeling());
                    this.getOp().compute(Views.iterable(lab1), Views.iterable(synchronizedLab), Views.iterable(res));
                }

                return m_labelingCellFactory.createCell(res, cellValue1.getLabelingMetadata());
            }

            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labelingCellFactory = new LabelingCellFactory(exec);
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            private BinaryOperationAssignment getOp() {
                switch (Method.valueOf(m_methodName.getStringValue())) {
                    case CONGRUENT:
                        return new BinaryOperationAssignment(new LabelingTypeCongruent());
                    case DIFFERENCE:
                        return new BinaryOperationAssignment(new LabelingTypeDifference());
                    case MERGE:
                        return new BinaryOperationAssignment(new LabelingTypeMerge());
                    case AND:
                        return new BinaryOperationAssignment(new LabelingTypeAnd());
                    case XOR:
                        return new BinaryOperationAssignment(new LabelingTypeXOR());
                    case INTERSECT:
                        return new BinaryOperationAssignment(new LabelingTypeIntersect());
                }

                throw new IllegalStateException("unknown arithmetic operation");
            }

        };
    }

    class LToString<II, OO> implements Converter<LabelingType<II>, LabelingType<OO>> {

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public void convert(final LabelingType<II> input, final LabelingType<OO> output) {
            output.clear();
            for (II label : input) {
                output.add((OO)label.toString());
            }
        }
    }
}
