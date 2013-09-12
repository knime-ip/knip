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

import java.util.Arrays;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryRelationAssigment;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.relation.real.unary.RealGreaterThanConstant;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.algorithm.types.ThresholdingType;
import org.knime.knip.core.ops.interval.AutoThreshold;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgUtils;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;

/**
 * New global thresholder
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ThresholderNodeFactory2<T extends RealType<T>> extends ValueToCellNodeFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ThresholderNodeFactory2.class);

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("thresholder_dimselection", "X", "Y");
    }

    private static SettingsModelDouble createManualThresholdModel() {
        return new SettingsModelDouble("manual_threshold", 0);
    }

    private static SettingsModelStringArray createThresholderSelectionModel() {
        return new SettingsModelStringArray("thresholder", new String[]{ThresholdingType.MANUAL.name()});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescriptionContent(final KnimeNode node) {
        DialogComponentDimSelection.createNodeDescription(node.getFullDescription().getTabList().get(0).addNewOption());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {
            @Override
            public void addDialogComponents() {
                final SettingsModelDouble settModManual = createManualThresholdModel();
                addDialogComponent("Options", "Manual Threshold", new DialogComponentNumber(settModManual,
                        "Threshold Value", .01));
                final SettingsModelStringArray method = createThresholderSelectionModel();
                addDialogComponent("Options", "Thresholding Method", new DialogComponentStringListSelection(method,
                        "Method", EnumListProvider.getStringList(ThresholdingType.values())));

                addDialogComponent("Options", "Dimension Selection", new DialogComponentDimSelection(
                        createDimSelectionModel(), "", 2, 5));

                method.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        final String[] selectedMethods = method.getStringArrayValue();
                        for (final String s : selectedMethods) {
                            if (ThresholdingType.valueOf(s) == ThresholdingType.MANUAL) {
                                settModManual.setEnabled(true);
                                return;
                            }
                        }
                        settModManual.setEnabled(false);
                    }
                });
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel createNodeModel() {
        return new ValueToCellNodeModel() {

            /*
             * The dimension selection
             */
            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            /*
             * ImgPlusCellFactory
             */
            private ImgPlusCellFactory m_imgCellFactory;

            /*
             * The value of the manual threshold
             */
            private final SettingsModelDouble m_manualThreshold = createManualThresholdModel();

            /*
             * The selected thresholder
             */
            private final SettingsModelStringArray m_thresholderSettings = createThresholderSelectionModel();

            /**
             * {@inheritDoc}
             */
            @Override
            protected void addSettingsModels(final List settingsModels) {
                m_manualThreshold.setEnabled(true);
                settingsModels.add(m_dimSelection);
                settingsModels.add(m_manualThreshold);
                settingsModels.add(m_thresholderSettings);
            }

            @Override
            protected DataCell compute(final DataValue cellValue) throws Exception {

                final ImgPlusValue<T> imgPlusValue = (ImgPlusValue<T>)cellValue;
                final ImgPlus<T> imgPlus = imgPlusValue.getImgPlus();

                if (imgPlus.firstElement() instanceof BitType) {
                    LOGGER.warn("Threshold of BitType image cannot be calculated. Image is returned as is.");
                    return m_imgCellFactory.createCell(imgPlus, imgPlus, ((ImgPlusValue)cellValue).getMinimum());
                }

                final String[] methods = m_thresholderSettings.getStringArrayValue();
                final ThresholdingType[] thresholders = new ThresholdingType[methods.length];
                for (int i = 0; i < thresholders.length; i++) {
                    thresholders[i] = Enum.valueOf(ThresholdingType.class, methods[i]);
                }
                final ImgPlusCell[] resCells = new ImgPlusCell[thresholders.length];
                for (int i = 0; i < resCells.length; i++) {
                    final ImgPlus<BitType> res =
                            new ImgPlus<BitType>(ImgUtils.createEmptyCopy(imgPlus,
                                                                          imgPlus.factory().imgFactory(new BitType()),
                                                                          new BitType()), imgPlus);
                    res.setName(res.getName());
                    if (thresholders[i] == ThresholdingType.MANUAL) {

                        final T type = imgPlus.firstElement().createVariable();
                        type.setReal(m_manualThreshold.getDoubleValue());

                        new UnaryRelationAssigment<T>(new RealGreaterThanConstant<T>(type)).compute(imgPlus, res);

                        resCells[i] = m_imgCellFactory.createCell(res, res, ((ImgPlusValue)cellValue).getMinimum());
                    } else if (thresholders[i] == ThresholdingType.INTERMODES
                            || thresholders[i] == ThresholdingType.MINIMUM
                            || thresholders[i] == ThresholdingType.ISODATA) {
                        LOGGER.warn("MINIMUM, INTERMODES and ISODATA are currently not supported because their implementation can result in incorrect results. A missing cell has been created.");
                        return m_imgCellFactory.createCell(imgPlus, imgPlus, ((ImgPlusValue)cellValue).getMinimum());
                    } else {
                        final AutoThreshold<T, Img<T>, Img<BitType>> op =
                                new AutoThreshold<T, Img<T>, Img<BitType>>(thresholders[i]);

                        final Img<BitType> out;

                        try {
                            //the different thresholding methods can fail and throw a runtime exception in these cases
                            out =
                                    SubsetOperations.iterate(op, m_dimSelection.getSelectedDimIndices(imgPlus),
                                                             imgPlus, res, getExecutorService());
                        } catch (Exception e) {
                            throw new KNIPRuntimeException(e.getMessage(), e);
                        }

                        resCells[i] = m_imgCellFactory.createCell(out, res, ((ImgPlusValue)cellValue).getMinimum());
                    }

                }

                if (resCells.length == 1) {
                    return resCells[0];
                } else {

                    return CollectionCellFactory.createListCell(Arrays.asList(resCells));
                }

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
                m_inValueClass = ImgPlusValue.class;
                if (m_thresholderSettings.getStringArrayValue().length == 1) {
                    m_outCellClass = ImgPlusCell.class;
                } else {
                    m_outCellClass = ListCell.class;
                }
                return super.configure(inSpecs);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected DataType getOutDataCellListCellType() {
                if (m_thresholderSettings.getStringArrayValue().length == 1) {
                    return null;
                } else {
                    return ImgPlusCell.TYPE;
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void getTypeArgumentClasses() {
                // do nothing here
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
