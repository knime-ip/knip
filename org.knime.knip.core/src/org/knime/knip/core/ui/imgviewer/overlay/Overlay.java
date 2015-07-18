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
package org.knime.knip.core.ui.imgviewer.overlay;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.event.EventServiceClient;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.OverlayChgEvent;

/**
 * Overlay
 *
 * @TODO: Replace by ImageJ2 implementations or actually use ImageJ2?
 *
 * @param
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class Overlay implements EventServiceClient, Externalizable {

    // we need this ID for backwards compability
    private static final long serialVersionUID = -6681043404923479564L;

    private long[] m_dims;

    private final List<OverlayElement2D> m_elements;

    private final LabelingColorTable m_defaultLabelingColorMapping;

    private EventService m_eventService;

    private Color m_activeColor;

    /**
     * No-arguments constructor need for externalization of overlays. Don't use this.
     *
     * @throws SecurityException
     * @throws IllegalArgumentException
     *
     */
    public Overlay() {
        m_elements = new ArrayList<OverlayElement2D>();
        m_defaultLabelingColorMapping =
                LabelingColorTableUtils.extendLabelingColorTable(new DefaultLabelingColorTable(),
                                                                 new RandomMissingColorHandler());
        m_activeColor = Color.YELLOW;
    }

    /**
     * @param dimension
     */
    public Overlay(final long[] dimension) {
        this();
        m_dims = dimension.clone();

    }

    /**
     * @param interval
     */
    public Overlay(final Interval interval) {
        this();
        m_dims = new long[interval.numDimensions()];
        interval.dimensions(m_dims);
    }

    /**
     * @param elmnts
     * @return
     */
    public boolean addElement(final OverlayElement2D... elmnts) {
        boolean changed = false;
        for (final OverlayElement2D e : elmnts) {
            changed = m_elements.add(e) || changed;
        }

        return changed;
    }

    public boolean removeAll(final List<OverlayElement2D> m_removeList) {
        return m_elements.removeAll(m_removeList);
    }

    public boolean removeElement(final OverlayElement2D e) {
        return m_elements.remove(e);
    }

    @SuppressWarnings("unchecked")
    public OverlayElement2D[] getElements() {
        final OverlayElement2D[] ret = new OverlayElement2D[m_elements.size()];
        m_elements.toArray(ret);
        return ret;
    }

    public final List<OverlayElement2D> getElementsByPosition(final long[] pos, final int[] dimIndices) {

        final int tolerance = 10;
        final ArrayList<OverlayElement2D> ret = new ArrayList<OverlayElement2D>();
        for (final OverlayElement2D e : m_elements) {

            final Interval interval = e.getInterval();
            if (isVisible(e, pos, dimIndices)) {
                for (int i = 0; i < dimIndices.length; i++) {
                    if (((pos[dimIndices[i]] + tolerance) < interval.min(i))
                            || ((pos[dimIndices[i]] - tolerance) > interval.max(i))) {
                        break;
                    }

                    if (i == (dimIndices.length - 1)) {
                        ret.add(e);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * @param pos
     * @return
     */
    public final List<OverlayElement2D> getElementsByPosition(final long[] pos) {
        final ArrayList<OverlayElement2D> res = new ArrayList<OverlayElement2D>();
        for (final OverlayElement2D e : m_elements) {
            if (e.contains(pos)) {
                res.add(e);
            }
        }
        return res;
    }

    public void renderBufferedImage(final Graphics g, final int[] dimIndices, final long[] pos, final int alpha) {

        for (final OverlayElement2D e : m_elements) {

            if (isVisible(e, pos, dimIndices)) {
                renderOverlayElement(g, e, alpha);
            }
        }

    }

    private boolean isVisible(final OverlayElement2D e, final long[] pos, final int[] dimIndices) {

        for (int d = 0; d < dimIndices.length; d++) {
            if (!e.isOrientation(dimIndices[d])) {
                return false;
            }
        }

        for (int i = 0; i < pos.length; i++) {
            if (!e.isOrientation(i)) {
                if (pos[i] != e.getPlanePos()[i]) {
                    return false;
                }
            }
        }
        return true;
    }

    private void renderOverlayElement(final Graphics g, final OverlayElement2D e, final int alpha) {
        switch (e.getStatus()) {
            case ACTIVE:
                g.setColor(m_activeColor);
                e.renderBoundingBox(g);
                e.renderOutline(g);
                g.setColor(new Color(LabelingColorTableUtils.getTransparentRGBA(m_activeColor.getRGB(), alpha), true));
                e.renderInterior((Graphics2D)g);
                break;
            case DRAWING:
                g.setColor(new Color(LabelingColorTableUtils.getAverageColor(m_defaultLabelingColorMapping,
                                                                             e.getLabels())).darker());
                e.renderOutline(g);
                g.setColor(new Color(LabelingColorTableUtils.getTransparentRGBA(LabelingColorTableUtils
                        .getAverageColor(m_defaultLabelingColorMapping, e.getLabels()), alpha), true));
                e.renderInterior((Graphics2D)g);
                break;
            case IDLE:
                g.setColor(new Color(LabelingColorTableUtils.getAverageColor(m_defaultLabelingColorMapping,
                                                                             e.getLabels())));
                e.renderOutline(g);
                g.setColor(new Color(LabelingColorTableUtils.getTransparentRGBA(LabelingColorTableUtils
                        .getAverageColor(m_defaultLabelingColorMapping, e.getLabels()), alpha), true));
                e.renderInterior((Graphics2D)g);
                break;
            default:
                break;
        }
    }

    /**
     * @return
     */
    public RandomAccessibleInterval<LabelingType<String>> renderSegmentationImage(final NativeTypes type) {
        return renderSegmentationImage(true, type);
    }

    /**
     *
     * @param addSegmentID if true, an additional label with a unique id for each segment is added
     * @return
     */
    @SuppressWarnings("unchecked")
    public ImgLabeling<String, ?> renderSegmentationImage(final boolean addSegmentID, final NativeTypes type) {

        ImgLabeling<String, ?> res = null;
        switch (type) {
            case BITTYPE:
                res =
                        (ImgLabeling<String, ?>)KNIPGateway.ops().createImgLabeling(new FinalInterval(m_dims),
                                                                                    new BitType());
                break;
            case BYTETYPE:
                res =
                        (ImgLabeling<String, ?>)KNIPGateway.ops().createImgLabeling(new FinalInterval(m_dims),
                                                                                    new ByteType());
                break;
            case SHORTTYPE:
                res =
                        (ImgLabeling<String, ?>)KNIPGateway.ops().createImgLabeling(new FinalInterval(m_dims),
                                                                                    new ShortType());
                break;
            case LONGTYPE:
                res =
                        (ImgLabeling<String, ?>)KNIPGateway.ops().createImgLabeling(new FinalInterval(m_dims),
                                                                                    new LongType());
                break;
            case UNSIGNEDSHORTTYPE:
                res =
                        (ImgLabeling<String, ?>)KNIPGateway.ops().createImgLabeling(new FinalInterval(m_dims),
                                                                                    new UnsignedShortType());
                break;
            case UNSIGNEDBYTETYPE:
                res =
                        (ImgLabeling<String, ?>)KNIPGateway.ops().createImgLabeling(new FinalInterval(m_dims),
                                                                                    new UnsignedShortType());
                break;
            default:
                res =
                        (ImgLabeling<String, ?>)KNIPGateway.ops().createImgLabeling(new FinalInterval(m_dims),
                                                                                    new ShortType());
        }

        final long[] minExtend = new long[res.numDimensions()];
        final long[] maxExtend = new long[res.numDimensions()];

        int segId = 0;
        for (final OverlayElement2D e : m_elements) {
            List<String> listToSet = new ArrayList<String>(e.getLabels());
            if (addSegmentID) {
                listToSet.add("Segment: " + segId++);
            }

            for (int d = 0; d < res.numDimensions(); d++) {
                if (e.isOrientation(d)) {
                    minExtend[d] = 0;
                    maxExtend[d] = res.max(d);
                } else {
                    minExtend[d] = e.getPlanePos()[d];
                    maxExtend[d] = minExtend[d];
                }
            }

            e.renderOnSegmentationImage(SubsetOperations.subsetview(res, new FinalInterval(minExtend, maxExtend)),
                                        listToSet);
        }
        return res;
    }

    public void fireOverlayChanged() {
        m_eventService.publish(new OverlayChgEvent(this));
        m_eventService.publish(new ImgRedrawEvent());
    }

    @Override
    public int hashCode() {

        int hashCode = 31;
        for (final OverlayElement2D element : m_elements) {
            hashCode *= 31;
            hashCode += element.hashCode();
        }

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);

    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_elements.clear();

        m_dims = new long[in.readInt()];
        for (int i = 0; i < m_dims.length; i++) {
            m_dims[i] = in.readLong();
        }

        final int num = in.readInt();
        for (int d = 0; d < num; d++) {
            m_elements.add((OverlayElement2D)in.readObject());
        }
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(m_dims.length);
        for (final long i : m_dims) {
            out.writeLong(i);
        }

        out.writeInt(m_elements.size());
        for (final OverlayElement2D element : m_elements) {
            out.writeObject(element);
        }
    }
}
