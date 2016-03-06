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
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentSubsetSelection;
import org.knime.knip.base.node.dialog.DialogComponentSubsetSelection2;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;
import org.knime.knip.core.data.img.DefaultImgMetadata;

import net.imagej.ImgPlus;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.DefaultCalibratedSpace;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.iterableinterval.unary.MergeIterableIntervals;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * ImgCropperNodeFactory2. Uses {@link DialogComponentSubsetSelection2} instead of
 * {@link DialogComponentSubsetSelection};
 *
 * @param <T>
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgCropperNodeFactory2<T extends RealType<T> & NativeType<T>>
        extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelBoolean createAdjustDimModel() {
        return new SettingsModelBoolean("cfg_adjust_dimensionality", true);
    }

    private static SettingsModelSubsetSelection2 createSubsetSelectionModel() {
        return new SettingsModelSubsetSelection2("cfg_subset_selection");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Subset Selection",
                                   new DialogComponentSubsetSelection2(createSubsetSelectionModel(), true, true));

                addDialogComponent("Options", "Options",
                                   new DialogComponentBoolean(createAdjustDimModel(), "Adjust Dimensionality?"));

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_cropped";
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

            private final SettingsModelBoolean m_smAdjustDimensionality = createAdjustDimModel();

            private final SettingsModelSubsetSelection2 m_smSubsetSel = createSubsetSelectionModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smSubsetSel);
                settingsModels.add(m_smAdjustDimensionality);
            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {
                if (m_smSubsetSel.isCompletelySelected()) {
                    return m_imgCellFactory.createCell(cellValue.getImgPlusCopy());
                } else {

                    ImgPlus<T> img = cellValue.getImgPlus();

                    //if an offset is set, translate the img to the origin for now
                    long[] minimum = new long[img.numDimensions()];
                    img.min(minimum);
                    long minSum = Arrays.stream(minimum).sum();
                    if (minSum > 0) {
                        //if a minimum has been set (i.e. different from 0),
                        //translate the image to the origin
                        img = new ImgPlus<T>(ImgView.wrap(Views.translate(img, Arrays.stream(minimum).map(l -> {
                            return -l;
                        }).toArray()), cellValue.getImgPlus().factory()), img);
                    }

                    final long[] dimensions = new long[img.numDimensions()];
                    img.dimensions(dimensions);

                    final Interval[] intervals = m_smSubsetSel.createSelectedIntervals(dimensions, img);

                    @SuppressWarnings("unchecked")
                    final IterableInterval<T>[] iis = new IterableInterval[intervals.length];

                    for (int i = 0; i < intervals.length; i++) {
                        iis[i] = Views.iterable(Views.interval(img, intervals[i]));
                    }

                    final MergeIterableIntervals<T> mergeOp =
                            new MergeIterableIntervals<T>(img.factory(), m_smAdjustDimensionality.getBooleanValue());

                    if ((iis.length == 1) && m_smAdjustDimensionality.getBooleanValue()) {
                        // special case handling if dim
                        // adjusting is active and no
                        // dims with
                        // size > 1 remain everything
                        // will be deleted else
                        boolean valid = false;

                        for (int i = 0; i < iis[0].numDimensions(); i++) {
                            if (iis[0].min(i) != iis[0].max(i)) {
                                valid = true;
                            }
                        }

                        if (!valid) {
                            throw new KNIPException(
                                    "dimension adjusting reduced the image to nothing(no dimension with size > 1 exists)",
                                    new IllegalArgumentException("no dimensions left"));
                        }
                    }

                    Img<T> res = Operations.compute(mergeOp, iis);

                    final List<CalibratedAxis> validAxes = new ArrayList<CalibratedAxis>();
                    for (int d = 0; d < img.numDimensions(); d++) {
                        if (!mergeOp.getInvalidDims().contains(d)) {
                            validAxes.add(cellValue.getMetadata().axis(d).copy());
                        }
                    }

                    final DefaultImgMetadata metadata =
                            new DefaultImgMetadata(new DefaultCalibratedSpace(validAxes), img, img, img);

                    metadata.setSource(img.getSource());
                    metadata.setName(img.getName());

                    //translate the result image to the given offset, if present
                    if (minSum > 0) {
                        if (m_smAdjustDimensionality.getBooleanValue()) {
                            //remove the invalid dimensions (dim == 1) from the minimum as well
                            for (Integer i : mergeOp.getInvalidDims()) {
                                minimum[i] = -1;
                            }
                            minimum = Arrays.stream(minimum).filter(l -> {
                                return l >= 0;
                            }).toArray();
                        }
                        res = ImgView.wrap(Views.translate(res, minimum), res.factory());
                    }
                    return m_imgCellFactory.createCell(new ImgPlus<T>(res, metadata));
                }
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
