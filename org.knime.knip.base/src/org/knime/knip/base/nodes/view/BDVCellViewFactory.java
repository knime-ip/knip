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
package org.knime.knip.base.nodes.view;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPanel;

import org.knime.core.data.DataValue;
import org.knime.core.data.MissingValue;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.data.ui.ViewerFactory;
import org.knime.knip.bdv.BigDataViewerUI;
import org.knime.knip.cellviewer.interfaces.CellView;
import org.knime.knip.cellviewer.interfaces.CellViewFactory;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.util.waitingindicator.WaitingIndicatorUtils;

import gnu.trove.map.hash.TIntIntHashMap;
import net.imagej.ImgPlusMetadata;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.DefaultCalibratedSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 * @param <I>
 * @param <T>
 * @param <L>
 */
public class BDVCellViewFactory<I extends IntegerType<I>, T extends RealType<T>, L> implements CellViewFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public CellView createCellView() {
        return new CellView() {

            private BigDataViewerUI<I, T, L> m_view = null;

            @Override
            public JPanel getViewComponent() {
                if (m_view == null) {
                    m_view = ViewerFactory.createBDV();
                }

                return m_view.getPanel();
            }

            @Override
            public void onClose() {
                m_view.removeAll();
            }

            @Override
            public void onReset() {
                // Nothing to do here
            }

            @SuppressWarnings("unchecked")
            @Override
            public void updateComponent(final List<DataValue> valueToView) {
                m_view.removeAll();
                WaitingIndicatorUtils.setWaiting(m_view.getPanel(), true);
                for (DataValue v : valueToView) {
                    if (v instanceof ImgPlusValue) {
                        final ImgPlusValue imgPlusValue = (ImgPlusValue)v;

                        if (imgPlusValue.getMetadata().numDimensions() <= 1) {
                            Img<T> img2d = imgPlusValue.getImgPlus().getImg();
                            final ImgPlusMetadata meta = imgPlusValue.getMetadata();

                            img2d = ImgView.wrap(Views.addDimension(img2d, 0, 0), img2d.factory());
                            final Set<String> groupNames = new HashSet<>();
                            groupNames.add("Group-" + meta.getName());

                            final T type = Util.getTypeFromInterval(img2d);
                            final CalibratedAxis[] axes = new CalibratedAxis[meta.numDimensions()];
                            meta.axes(axes);

                            m_view.addImage(img2d, type.getClass().getSimpleName(), meta.getName(), true, groupNames,
                                            Color.white, createAxisCalibration(axes), type.getMinValue(),
                                            type.getMaxValue());

                        } else {
                            final Img<T> img = imgPlusValue.getImgPlus().getImg();
                            final ImgPlusMetadata meta = imgPlusValue.getMetadata();

                            final CalibratedAxis[] axes = new CalibratedAxis[meta.numDimensions()];
                            meta.axes(axes);

                            prepareForBDV(img, axes, meta.getName(), img.firstElement().getClass().getSimpleName(),
                                          null);
                        }

                    } else if (v instanceof LabelingValue) {
                        final LabelingValue<L> labValue = (LabelingValue<L>)v;

                        RandomAccessibleInterval<LabelingType<L>> img = labValue.getLabeling();

                        LabelingMetadata meta = labValue.getLabelingMetadata();

                        final String type = Util.getTypeFromInterval(img).getClass().getSimpleName();

                        if (labValue.getLabelingMetadata().numDimensions() <= 1) {
                            img = Views.addDimension(img, 0, 0);
                            meta = new DefaultLabelingMetadata(new DefaultCalibratedSpace(2), meta, meta,
                                    meta.getLabelingColorTable());

                            final Set<String> groupNames = new HashSet<>();
                            groupNames.add("Group-" + meta.getName());

                            final CalibratedAxis[] axes = new CalibratedAxis[meta.numDimensions()];
                            meta.axes(axes);
                            if (meta.getLabelingColorTable() instanceof DefaultLabelingColorTable) {
                                final TIntIntHashMap lut =
                                        ((DefaultLabelingColorTable)meta.getLabelingColorTable()).getColorTable();
                                m_view.addLabeling(img, type, meta.getName(), true, groupNames,
                                                   createAxisCalibration(axes), lut);
                            } else {
                                m_view.addLabeling(img, type, meta.getName(), true, groupNames,
                                                   createAxisCalibration(axes), null);
                            }
                        } else {
                            final CalibratedAxis[] axes = new CalibratedAxis[meta.numDimensions()];
                            meta.axes(axes);

                            if (meta.getLabelingColorTable() instanceof DefaultLabelingColorTable) {
                                final TIntIntHashMap lut =
                                        ((DefaultLabelingColorTable)meta.getLabelingColorTable()).getColorTable();
                                prepareForBDV(img, axes, meta.getName(), type, lut);
                            } else {
                                prepareForBDV(img, axes, meta.getName(), type, null);
                            }

                        }
                    }
                }

                WaitingIndicatorUtils.setWaiting(m_view.getPanel(), false);

            }

            private void prepareForBDV(final RandomAccessibleInterval<?> img, final CalibratedAxis[] axes,
                                       final String name, final String type, final TIntIntHashMap lut) {
                final Map<String, Integer> dims = new HashMap<>();
                for (int i = 0; i < axes.length; i++) {
                    dims.put(axes[i].type().getLabel(), i);
                }

                if (dims.containsKey("X") && dims.containsKey("Y") && dims.containsKey("Z")
                        && dims.containsKey("Time")) {
                    // 3D
                    m_view.switch2D(false);
                    dims.remove("X");
                    dims.remove("Y");
                    dims.remove("Z");
                    dims.remove("Time");
                } else if (dims.containsKey("X") && dims.containsKey("Y") && dims.containsKey("Z")
                        && dims.containsKey("Feature Dimension")) {
                    // 3D
                    m_view.switch2D(false);
                    dims.remove("X");
                    dims.remove("Y");
                    dims.remove("Z");
                    dims.remove("Feature Dimension");
                } else if (dims.containsKey("X") && dims.containsKey("Y") && dims.containsKey("Z")) {
                    // 3D
                    m_view.switch2D(false);
                    dims.remove("X");
                    dims.remove("Y");
                    dims.remove("Z");
                } else if (dims.containsKey("X") && dims.containsKey("Y") && dims.containsKey("Time")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("X");
                    dims.remove("Y");
                    dims.remove("Time");
                } else if (dims.containsKey("X") && dims.containsKey("Z") && dims.containsKey("Time")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("X");
                    dims.remove("Z");
                    dims.remove("Time");
                } else if (dims.containsKey("Y") && dims.containsKey("Z") && dims.containsKey("Time")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("Y");
                    dims.remove("Z");
                    dims.remove("Time");
                } else if (dims.containsKey("X") && dims.containsKey("Y") && dims.containsKey("Feature Dimension")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("X");
                    dims.remove("Y");
                    dims.remove("Feature Dimension");
                } else if (dims.containsKey("X") && dims.containsKey("Z") && dims.containsKey("Feature Dimension")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("X");
                    dims.remove("Z");
                    dims.remove("Feature Dimension");
                } else if (dims.containsKey("Y") && dims.containsKey("Z") && dims.containsKey("Feature Dimension")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("Y");
                    dims.remove("Z");
                    dims.remove("Feature Dimension");
                } else if (dims.containsKey("X") && dims.containsKey("Y")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("X");
                    dims.remove("Y");
                } else if (dims.containsKey("X") && dims.containsKey("Z")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("X");
                    dims.remove("Z");
                } else if (dims.containsKey("Y") && dims.containsKey("Z")) {
                    // 2D
                    m_view.switch2D(true);
                    dims.remove("Y");
                    dims.remove("Z");
                } else if (dims.size() > 2) {
                    // 3D
                    m_view.switch2D(false);
                } else {
                    // 2D
                    m_view.switch2D(true);
                }

                if (lut != null) {
                    addLabsToBDV((RandomAccessibleInterval<LabelingType<L>>)img, axes, name, type, dims, lut);
                } else {
                    addImgsToBDV((RandomAccessibleInterval<T>)img, axes, name, dims);
                }
            }

            private void addLabsToBDV(final RandomAccessibleInterval<LabelingType<L>> img, final CalibratedAxis[] axes,
                                      final String name, final String type, final Map<String, Integer> dims,
                                      final TIntIntHashMap lut) {
                final Set<String> groups = new HashSet<>();
                groups.add("Group-" + name);
                final AffineTransform3D t = createAxisCalibration(axes);
                if (dims.isEmpty()) {
                    m_view.addLabeling(img, type, name, true, groups, t, lut);
                } else {
                    List<Pair<String, RandomAccessibleInterval<?>>> imgs = new ArrayList<>();
                    imgs.add(new ValuePair<String, RandomAccessibleInterval<?>>("", img));

                    imgs = sliceUp(imgs, dims);

                    for (int i = 0; i < imgs.size(); i++) {
                        final Pair<String, RandomAccessibleInterval<?>> pair = imgs.get(i);
                        final RandomAccessibleInterval<?> slicedImg = pair.getB();

                        m_view.addLabeling((RandomAccessibleInterval<LabelingType<L>>)slicedImg, type,
                                           name + pair.getA(), true, groups, t, lut);
                    }
                }
            }

            private void addImgsToBDV(final RandomAccessibleInterval<T> img, final CalibratedAxis[] axes,
                                      final String name, final Map<String, Integer> dims) {
                final Set<String> groups = new HashSet<>();
                groups.add("Group-" + name);
                final AffineTransform3D t = createAxisCalibration(axes);
                final T type = Util.getTypeFromInterval(img);
                if (dims.isEmpty()) {
                    m_view.addImage(img, type.getClass().getSimpleName(), name, true, groups, Color.white, t,
                                    type.getMinValue(), type.getMaxValue());
                } else {
                    List<Pair<String, RandomAccessibleInterval<?>>> imgs = new ArrayList<>();
                    imgs.add(new ValuePair<String, RandomAccessibleInterval<?>>("", img));

                    imgs = sliceUp(imgs, dims);

                    for (int i = 0; i < imgs.size(); i++) {
                        final Pair<String, RandomAccessibleInterval<?>> pair = imgs.get(i);
                        final RandomAccessibleInterval<?> slicedImg = pair.getB();

                        Util.getTypeFromInterval(img).createVariable();
                        m_view.addImage((RandomAccessibleInterval<T>)slicedImg, type.getClass().getSimpleName(),
                                        name + pair.getA(), true, groups, Color.white, t, type.getMinValue(),
                                        type.getMaxValue());
                    }
                }
            }

            private List<Pair<String, RandomAccessibleInterval<?>>>
                    sliceUp(final List<Pair<String, RandomAccessibleInterval<?>>> imgs,
                            final Map<String, Integer> dims) {
                final List<Pair<String, RandomAccessibleInterval<?>>> slicedImgs = new ArrayList<>();
                int min = Integer.MAX_VALUE;
                String minKey = null;
                final Iterator<Entry<String, Integer>> it = dims.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, Integer> i = it.next();
                    if (i.getValue() < min) {
                        min = i.getValue();
                        minKey = i.getKey();
                    }
                }

                if (minKey != null) {
                    dims.remove(minKey);

                    for (int i = 0; i < imgs.size(); i++) {
                        final Pair<String, RandomAccessibleInterval<?>> pair = imgs.get(i);
                        final String name = pair.getA();
                        final RandomAccessibleInterval<?> img = pair.getB();
                        for (int j = 0; j < img.dimension(min); j++) {
                            slicedImgs.add(new ValuePair<String, RandomAccessibleInterval<?>>(
                                    name + "_" + minKey + "#" + j, Views.hyperSlice(img, min, j)));
                        }
                    }

                    return sliceUp(slicedImgs, dims);
                } else {
                    return imgs;
                }
            }

            private AffineTransform3D createAxisCalibration(final CalibratedAxis[] axes) {
                final AffineTransform3D t = new AffineTransform3D();
                for (int i = 0; i < axes.length; i++) {
                    final CalibratedAxis axis = axes[i];
                    final String label = axis.type().getLabel();
                    final double scale = axis.averageScale(0, 1);
                    if (label.equals("X")) {
                        t.set(scale, 0, 0);
                    }
                    if (label.equals("Y")) {
                        t.set(scale, 1, 1);
                    }
                    if (label.equals("Z")) {
                        t.set(scale, 2, 2);
                    }
                }
                return t;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompatible(final List<Class<? extends DataValue>> values) {

        boolean canHandle = true;

        for (Class<? extends DataValue> v : values) {
            if (v != ImgPlusValue.class) {
                if (v != LabelingValue.class) {
                    if (v != MissingValue.class) {
                        canHandle = false;
                    }
                }
            }
        }
        return canHandle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCellViewName() {
        return "BigDataViewer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCellViewDescription() {
        return "A viewer shown when the user selects an interval of rows and columns in the viewer. "
                + "This viewer combines all images and labelings in the selected interval to one image by rendering them next to each other. "
                + "Alternatively, the images and labelings can be layed over each other.";
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}
