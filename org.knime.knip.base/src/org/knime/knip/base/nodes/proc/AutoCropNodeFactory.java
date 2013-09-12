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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

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
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * Automatically crops an image.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class AutoCropNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelBoolean createKeepWithinImgBordersModel() {
        return new SettingsModelBoolean("keep_within_img_borders", true);
    }

    private static SettingsModelDouble createLowerPixelValueModel() {
        return new SettingsModelDouble("lower_pixel_value", 0);
    }

    private static SettingsModelDimSelection createMarginDimSelectionModel() {
        return new SettingsModelDimSelection("margin_dim_selection", "X", "Y");
    }

    private static SettingsModelInteger createMarginModel() {
        return new SettingsModelInteger("margin", 0);
    }

    private static SettingsModelDouble createOutOfBoundsValueModel() {
        return new SettingsModelDouble("out_of_bounds_value", 0);
    }

    private static SettingsModelDouble createUpperPixelValueModel() {
        return new SettingsModelDouble("upper_pixel_value", 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Pixel values", new DialogComponentNumber(createLowerPixelValueModel(),
                        "Lower pixel value", 1.0));

                addDialogComponent("Options", "Pixel values", new DialogComponentNumber(createUpperPixelValueModel(),
                        "Upper pixel value", 1.0));
                addDialogComponent("Margin", "", new DialogComponentNumber(createMarginModel(),

                "Margin", 1));

                addDialogComponent("Margin", "", new DialogComponentDimSelection(createMarginDimSelectionModel(),
                        "Respected dimensions"));

                final SettingsModelBoolean keepImgBorders = createKeepWithinImgBordersModel();
                final SettingsModelDouble outOfBoundsVal = createOutOfBoundsValueModel();
                keepImgBorders.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        outOfBoundsVal.setEnabled(!keepImgBorders.getBooleanValue());
                    }
                });

                addDialogComponent("Margin", "", new DialogComponentBoolean(keepImgBorders,
                        "Keep result within image borders"));
                addDialogComponent("Margin", "", new DialogComponentNumber(outOfBoundsVal, "Out of bounds value", 1));

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

            private final SettingsModelBoolean m_smKeepWithinImgBorders = createKeepWithinImgBordersModel();

            private final SettingsModelDouble m_smLowerPixelValue = createLowerPixelValueModel();

            private final SettingsModelInteger m_smMargin = createMarginModel();

            private final SettingsModelDimSelection m_smMarginDimSelection = createMarginDimSelectionModel();

            private final SettingsModelDouble m_smOutOfBoundsValue = createOutOfBoundsValueModel();

            private final SettingsModelDouble m_smUpperPixelValue = createUpperPixelValueModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smMargin);
                settingsModels.add(m_smLowerPixelValue);
                settingsModels.add(m_smUpperPixelValue);
                settingsModels.add(m_smKeepWithinImgBorders);
                settingsModels.add(m_smOutOfBoundsValue);
                settingsModels.add(m_smMarginDimSelection);

            }

            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {

                final ImgPlus<T> img = cellValue.getImgPlus();
                final Cursor<T> cur = img.localizingCursor();

                final long[] min = new long[img.numDimensions()];
                img.max(min);
                final long[] max = new long[img.numDimensions()];
                img.min(max);

                while (cur.hasNext()) {
                    cur.fwd();
                    if ((cur.get().getRealDouble() >= m_smLowerPixelValue.getDoubleValue())
                            && (cur.get().getRealDouble() <= m_smUpperPixelValue.getDoubleValue())) {
                        for (int i = 0; i < img.numDimensions(); i++) {

                            min[i] = Math.min(min[i], cur.getIntPosition(i));
                            max[i] = Math.max(max[i], cur.getIntPosition(i));
                        }
                    }
                }

                // apply margins only to those dimension, which
                // are selected
                for (int i = 0; i < img.numDimensions(); i++) {
                    if (m_smMarginDimSelection.isSelectedDim(img.axis(i).type().getLabel())) {
                        min[i] -= m_smMargin.getIntValue();
                        max[i] += m_smMargin.getIntValue();
                        if (m_smKeepWithinImgBorders.getBooleanValue()) {
                            min[i] = Math.max(img.min(i), min[i]);
                            max[i] = Math.min(img.max(i), max[i]);
                        }
                    }
                }

                // crop
                final FinalInterval interval = new FinalInterval(min, max);
                if (Intervals.numElements(interval) <= 0) {
                    throw new KNIPRuntimeException("Illegal bounding box size.");
                }
                Img<T> view;
                if (m_smKeepWithinImgBorders.getBooleanValue()) {
                    view = new ImgView<T>(Views.interval(img, interval), img.factory());
                } else {
                    final T val = img.firstElement().createVariable();
                    val.setReal(m_smOutOfBoundsValue.getDoubleValue());
                    view = new ImgView<T>(Views.interval(Views.extendValue(img, val), interval), img.factory());
                }
                final Img<T> res = img.factory().create(view, img.firstElement().createVariable());
                final Cursor<T> cur1 = view.cursor();
                final Cursor<T> cur2 = res.cursor();
                while (cur1.hasNext()) {
                    cur1.fwd();
                    cur2.fwd();
                    cur2.get().set(cur1.get());
                }

                return m_imgCellFactory.createCell(res, img, min);
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
