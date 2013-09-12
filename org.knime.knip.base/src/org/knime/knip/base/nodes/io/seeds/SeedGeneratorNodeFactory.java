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
package org.knime.knip.base.nodes.io.seeds;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.sparse.NtreeImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.IntegerLabelGenerator;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.ops.labeling.ImgProbabilitySeeds;
import org.knime.knip.core.ops.labeling.RandomSeeds;
import org.knime.knip.core.ops.labeling.RegularGridSeeds;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgUtils;
import org.knime.knip.core.util.NeighborhoodUtils;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SeedGeneratorNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static enum SeedGenerator {
        Image_Probability, Random_Seeds, Regular_Grid;
    }

    private static SettingsModelInteger createDistanceModel() {
        return new SettingsModelInteger("distance", 5);
    }

    private static SettingsModelBoolean createMoveToMinModel() {
        return new SettingsModelBoolean("move_to_minimum", false);
    }

    private static SettingsModelString createSeedGeneratorModel() {
        return new SettingsModelString("GENERATOR", SeedGenerator.Image_Probability.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Seeds", new DialogComponentStringSelection(createSeedGeneratorModel(),
                        "Seeding method", EnumListProvider.getStringList(SeedGenerator.values())));

                addDialogComponent(new DialogComponentNumber(createDistanceModel(), "Average distance", 1));

                addDialogComponent(new DialogComponentBoolean(createMoveToMinModel(),
                        "Move seeding point to minimum in local neighborhood"));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, LabelingCell<Integer>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, LabelingCell<Integer>>() {

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelInteger m_smDistance = createDistanceModel();

            private final SettingsModelBoolean m_smMoveToMin = createMoveToMinModel();

            private final SettingsModelString m_smSeedGenerator = createSeedGeneratorModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smSeedGenerator);
                settingsModels.add(m_smDistance);
                settingsModels.add(m_smMoveToMin);
            }

            @Override
            protected LabelingCell<Integer> compute(final ImgPlusValue<T> cell) throws Exception {
                final ImgPlus<T> input = cell.getImgPlus();
                final Labeling<Integer> output =
                        new NativeImgLabeling<Integer, IntType>(
                                ImgUtils.createEmptyCopy(new NtreeImgFactory<IntType>(), input, new IntType()));

                if (m_smSeedGenerator.getStringValue().equals(SeedGenerator.Image_Probability.name())) {
                    /*
                     * if the seeding point probability is
                     * determined by the individual pixel
                     * values, but a certain average
                     * distance between labels should be
                     * garantied. TODO here: improve
                     * efficiency
                     */
                    new ImgProbabilitySeeds<T, Integer>(new IntegerLabelGenerator(), m_smDistance.getIntValue())
                            .compute(input, output);

                } else if (m_smSeedGenerator.getStringValue().equals(SeedGenerator.Random_Seeds.name())) {

                    /*
                     * random seed with a certain average
                     * distance
                     */
                    new RandomSeeds<Integer>(new IntegerLabelGenerator(), m_smDistance.getIntValue()).compute(input,
                                                                                                              output);

                } else {
                    new RegularGridSeeds<Integer>(new IntegerLabelGenerator(), m_smDistance.getIntValue())
                            .compute(input, output);
                }

                // move the seed to the minimum in its
                // 8-neigborhood
                if (m_smMoveToMin.getBooleanValue()) {
                    final long[][] strelMoves =
                            NeighborhoodUtils.reworkStructuringElement(NeighborhoodUtils
                                    .get8ConStructuringElement(output.numDimensions()));
                    final RandomAccess<T> ra = Views.extendBorder(input).randomAccess();
                    final Cursor<LabelingType<Integer>> labCur = output.localizingCursor();
                    final RandomAccess<LabelingType<Integer>> labRA = Views.extendBorder(output).randomAccess();

                    // get seed positions
                    final List<long[]> seedPos = new ArrayList<long[]>();
                    while (labCur.hasNext()) {
                        labCur.fwd();
                        if (!labCur.get().getLabeling().isEmpty()) {
                            final long[] p = new long[labCur.numDimensions()];
                            labCur.localize(p);
                            seedPos.add(p);
                        }
                    }

                    boolean posChanged = false;
                    double min;
                    final long[] minPos = new long[output.numDimensions()];
                    for (final long[] p : seedPos) {
                        ra.setPosition(p);
                        min = ra.get().getRealDouble();
                        for (final long[] offset : strelMoves) {
                            ra.move(offset);
                            if (ra.get().getRealDouble() < min) {
                                min = ra.get().getRealDouble();
                                ra.localize(minPos);
                                posChanged = true;
                            }
                        }
                        if (posChanged) {
                            labRA.setPosition(p);
                            final List<Integer> l = labRA.get().getLabeling();
                            labRA.get().setLabeling(labRA.get().getMapping().emptyList());
                            labRA.setPosition(minPos);
                            labRA.get().setLabeling(l);
                            posChanged = false;
                        }

                    }

                }

                return m_labCellFactory.createCell(output, new DefaultLabelingMetadata(input, input, input,
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
