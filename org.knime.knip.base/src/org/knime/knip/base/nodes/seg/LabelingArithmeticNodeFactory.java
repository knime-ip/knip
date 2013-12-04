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

import java.util.ArrayList;
import java.util.List;

import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.LabelingView;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeAnd;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeCongruent;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeDifference;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeIntersect;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeMerge;
import net.imglib2.ops.operation.labeling.binary.LabelingTypeXOR;

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
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.core.util.ImgUtils;
import org.knime.knip.core.util.MiscViews;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <IL>
 * @param <OL>
 */
public final class LabelingArithmeticNodeFactory<IL extends Comparable<IL>, OL extends Comparable<OL>> extends
        TwoValuesToCellNodeFactory<LabelingValue<IL>, LabelingValue<IL>> {

    /**
     * TODO: javadoc Operations
     */
    @SuppressWarnings("javadoc")
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
    protected TwoValuesToCellNodeDialog<LabelingValue<IL>, LabelingValue<IL>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<LabelingValue<IL>, LabelingValue<IL>>() {

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
        };
    }

    /**
     *
     */
    @Override
    public TwoValuesToCellNodeModel<LabelingValue<IL>, LabelingValue<IL>, LabelingCell<OL>> createNodeModel() {
        return new TwoValuesToCellNodeModel<LabelingValue<IL>, LabelingValue<IL>, LabelingCell<OL>>() {

            private LabelingCellFactory m_labelingCellFactory;

            private final SettingsModelString m_methodName = createMethodNameModel();

            private BinaryOperationAssignment<LabelingType<OL>, LabelingType<OL>, LabelingType<OL>> m_op;

            private final SettingsModelBoolean m_synchronize = createVirtuallySynchronizeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_methodName);
                settingsModels.add(m_synchronize);

            }

            @SuppressWarnings("unchecked")
            @Override
            protected LabelingCell<OL> compute(final LabelingValue<IL> cellValue1, final LabelingValue<IL> cellValue2)
                    throws Exception {

                final Labeling<IL> lab1 =
                        new LabelingView<IL>(cellValue1.getLabeling(), cellValue1.getLabeling().<IL> factory());
                final Labeling<IL> lab2 = new LabelingView<IL>(cellValue2.getLabeling(), lab1.<IL> factory());
                boolean stringBased = false;
                if (lab1.firstElement().getMapping().getLabels().size() > 0
                        && lab2.firstElement().getMapping().getLabels().size() > 0) {
                    if (!(lab1.firstElement().getMapping().getLabels().get(0).getClass().isAssignableFrom(lab2
                            .firstElement().getMapping().getLabels().get(0).getClass()))) {

                        LOGGER.warn("Labeling types are not compatible. Using Strings for comparsion. The resulting labeling will also be String based");
                        stringBased = true;
                    }
                }

                Labeling<IL> synchronizedLab = lab2;
                if (m_synchronize.getBooleanValue()) {
                    synchronizedLab =
                            new LabelingView<IL>(MiscViews.synchronizeDimensionality(lab2,
                                                                                     cellValue2.getLabelingMetadata(),
                                                                                     lab1,
                                                                                     cellValue1.getLabelingMetadata()),
                                    lab1.<IL> factory());
                }
                Labeling<OL> nativeRes = (Labeling<OL>)ImgUtils.createEmptyCopy(cellValue1.getLabeling());
                Labeling<OL> res = new LabelingView<OL>(nativeRes, lab1.<OL> factory());

                if (stringBased) {
                    LabelingView<OL> lab1ToProcess =
                            new LabelingView<OL>(
                                    new ConvertedRandomAccessibleInterval<LabelingType<IL>, LabelingType<OL>>(lab1,
                                            new LToString(), new LabelingType<OL>()), lab1.<OL> factory());

                    LabelingView<OL> lab2ToProcess =
                            new LabelingView<OL>(
                                    new ConvertedRandomAccessibleInterval<LabelingType<IL>, LabelingType<OL>>(
                                            synchronizedLab, new LToString(), new LabelingType<OL>()),
                                    lab2.<OL> factory());

                    m_op.compute(lab1ToProcess, lab2ToProcess, res);
                } else {
                    m_op.compute((Labeling<OL>)lab1, (Labeling<OL>)synchronizedLab, res);
                }


                return m_labelingCellFactory.createCell(nativeRes, cellValue1.getLabelingMetadata());
            }

            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                switch (Method.valueOf(m_methodName.getStringValue())) {
                    case CONGRUENT:
                        m_op =
                                new BinaryOperationAssignment<LabelingType<OL>, LabelingType<OL>, LabelingType<OL>>(
                                        new LabelingTypeCongruent<OL>());
                        break;
                    case DIFFERENCE:
                        m_op =
                                new BinaryOperationAssignment<LabelingType<OL>, LabelingType<OL>, LabelingType<OL>>(
                                        new LabelingTypeDifference<OL>());
                        break;
                    case MERGE:
                        m_op =
                                new BinaryOperationAssignment<LabelingType<OL>, LabelingType<OL>, LabelingType<OL>>(
                                        new LabelingTypeMerge<OL>());
                        break;
                    case AND:
                        m_op =
                                new BinaryOperationAssignment<LabelingType<OL>, LabelingType<OL>, LabelingType<OL>>(
                                        new LabelingTypeAnd<OL>());
                        break;
                    case XOR:
                        m_op =
                                new BinaryOperationAssignment<LabelingType<OL>, LabelingType<OL>, LabelingType<OL>>(
                                        new LabelingTypeXOR<OL>());
                        break;
                    case INTERSECT:
                        m_op =
                                new BinaryOperationAssignment<LabelingType<OL>, LabelingType<OL>, LabelingType<OL>>(
                                        new LabelingTypeIntersect<OL>());
                        break;
                }
                m_labelingCellFactory = new LabelingCellFactory(exec);
            }

        };
    }

    class LToString implements Converter<LabelingType<IL>, LabelingType<OL>> {

        private ArrayList<OL> list;
        {
            list = new ArrayList<OL>();
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public void convert(final LabelingType<IL> input, final LabelingType<OL> output) {
            list.clear();
            for (IL label : input.getLabeling()) {
                list.add((OL)label.toString());
            }
            output.setLabeling(list);
        }
    }
}
