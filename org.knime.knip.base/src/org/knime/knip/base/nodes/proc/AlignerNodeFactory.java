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

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.dialog.DialogComponentSubsetSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;
import org.knime.knip.core.ops.img.algorithms.Aligner;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class AlignerNodeFactory<T extends RealType<T>, V extends RealType<V>> extends
        TwoValuesToCellNodeFactory<ImgPlusValue<T>, ImgPlusValue<V>> {

    private static final String[] ALIGNMODES = new String[]{"First", "Last", "Pairwise", "Stepwise"};

    private static final String[] SIZEMODES = new String[]{"Crop", "Nothing", "Extend"};

    private static SettingsModelString createAlignModeModel() {
        return new SettingsModelString("align_mode", ALIGNMODES[0]);
    }

    private static SettingsModelDimSelection createDimSelection1Model() {
        return new SettingsModelDimSelection("dim_selection1", "X", "Y");
    }

    private static SettingsModelDimSelection createDimSelection2Model() {
        return new SettingsModelDimSelection("dim_selection2", "Z");
    }

    private static SettingsModelInteger createMinPixOverlapModel() {
        return new SettingsModelInteger("min_pix_overlap", 1);
    }

    private static SettingsModelString createSizeModeSelModel() {
        return new SettingsModelString("size_mode", SIZEMODES[0]);
    }

    private static SettingsModelInteger createStepSizeModel() {
        return new SettingsModelInteger("step_size", 1);
    }

    private static SettingsModelSubsetSelection createSubsetSelectionModel() {
        return new SettingsModelSubsetSelection("subset_selection");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<ImgPlusValue<T>, ImgPlusValue<V>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<ImgPlusValue<T>, ImgPlusValue<V>>() {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "Alignment dimension", new DialogComponentDimSelection(
                        createDimSelection1Model(), "Dimension selection", 2, 2));
                addDialogComponent("Options", "Alignment dimension", new DialogComponentDimSelection(
                        createDimSelection2Model(), "Shift dimension selection", 1, 1));
                addDialogComponent("Options", "Start coordinates", new DialogComponentSubsetSelection(
                        createSubsetSelectionModel(), false, false, new int[]{}));
                addDialogComponent("Options", "Options", new DialogComponentStringSelection(createSizeModeSelModel(),
                        "Crop mode", SIZEMODES));
                addDialogComponent("Options", "Options", new DialogComponentStringSelection(createAlignModeModel(),
                        "Align relative to", ALIGNMODES));

                addDialogComponent("Options", "Options", new DialogComponentNumber(createStepSizeModel(),
                        "Step size for stepwise comparison", 1));
                addDialogComponent("Options", "Options", new DialogComponentNumber(createMinPixOverlapModel(),
                        "Minimum Pixel Overlap", 1));

            }

            @Override
            protected String getFirstColumnSelectionLabel() {
                return "Image to be aligned";
            }

            @Override
            protected String getSecondColumnSelectionLabel() {
                return "Filtered image";
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TwoValuesToCellNodeModel<ImgPlusValue<T>, ImgPlusValue<V>, ImgPlusCell<T>> createNodeModel() {
        return new TwoValuesToCellNodeModel<ImgPlusValue<T>, ImgPlusValue<V>, ImgPlusCell<T>>() {

            private final SettingsModelString m_alignmodeSelection = createAlignModeModel();

            private final SettingsModelDimSelection m_dimSelection1 = createDimSelection1Model();

            private final SettingsModelDimSelection m_dimSelection2 = createDimSelection2Model();

            private ImgPlusCellFactory m_imgCellFactory;

            private final SettingsModelInteger m_minPixOverlap = createMinPixOverlapModel();

            private final SettingsModelString m_sizemodeSelection = createSizeModeSelModel();

            private final SettingsModelInteger m_stepSize = createStepSizeModel();

            private final SettingsModelSubsetSelection m_subsetSelect = createSubsetSelectionModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_dimSelection1);
                settingsModels.add(m_dimSelection2);
                settingsModels.add(m_sizemodeSelection);
                settingsModels.add(m_alignmodeSelection);
                settingsModels.add(m_subsetSelect);
                settingsModels.add(m_stepSize);
                settingsModels.add(m_minPixOverlap);
            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValueA, final ImgPlusValue<V> cellValueB)
                    throws Exception {
                final ImgPlus<T> imgPlus = cellValueA.getImgPlus();
                final int[] selectedDims1 = m_dimSelection1.getSelectedDimIndices(imgPlus);
                final int[] selectedDims2 = m_dimSelection2.getSelectedDimIndices(imgPlus);

                if (selectedDims1.length != 2) {
                    throw new IllegalStateException("Wrong number of valid image dimensions: '" + selectedDims1.length
                            + "' or dimensions do not exist in Image! Skipping Image "
                            + cellValueA.getMetadata().getName());
                }

                if (selectedDims2.length != 1) {
                    throw new IllegalStateException("Wrong number of valid shift dimensions '" + selectedDims1.length
                            + "' or dimensions do not exist in Image! Skipping Image "
                            + cellValueA.getMetadata().getName());
                }

                final int selectedDim2 = selectedDims2[0];

                Aligner.SIZEMODES sizemode = Aligner.SIZEMODES.CROP;
                if (m_sizemodeSelection.getStringValue().equals(SIZEMODES[1])) {
                    sizemode = Aligner.SIZEMODES.NOTHING;
                } else if (m_sizemodeSelection.getStringValue().equals(SIZEMODES[2])) {
                    sizemode = Aligner.SIZEMODES.EXTEND;
                }

                Aligner.ALIGNMODES alignmode = Aligner.ALIGNMODES.FIRST;
                if (m_alignmodeSelection.getStringValue().equals(ALIGNMODES[1])) {
                    alignmode = Aligner.ALIGNMODES.LAST;
                } else if (m_alignmodeSelection.getStringValue().equals(ALIGNMODES[2])) {
                    alignmode = Aligner.ALIGNMODES.PAIRWISE;
                } else if (m_alignmodeSelection.getStringValue().equals(ALIGNMODES[3])) {
                    alignmode = Aligner.ALIGNMODES.STEPWISE;
                }

                final long[] dims = new long[imgPlus.numDimensions()];
                imgPlus.dimensions(dims);
                Interval ivs[] = m_subsetSelect.createSelectedIntervals(dims, imgPlus);
                if ((ivs == null) || (ivs.length == 0)) {
                    ivs = new Interval[1];
                    final long mins[] = new long[imgPlus.numDimensions()];
                    final long maxs[] = new long[imgPlus.numDimensions()];
                    imgPlus.min(mins);
                    imgPlus.max(maxs);
                    ivs[0] = new FinalInterval(mins, maxs);
                }

                return m_imgCellFactory.createCell(Operations.compute(new Aligner<T, V>(selectedDims1, selectedDim2,
                                                                              ivs[0], sizemode, alignmode, m_stepSize
                                                                                      .getIntValue(), m_minPixOverlap
                                                                                      .getIntValue()), imgPlus,
                                                                      cellValueB.getImgPlus()), cellValueA
                        .getMetadata());
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
