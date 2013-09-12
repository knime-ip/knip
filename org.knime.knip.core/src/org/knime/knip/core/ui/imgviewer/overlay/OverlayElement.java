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

import java.awt.Graphics;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.roi.IterableRegionOfInterest;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class OverlayElement<L extends Comparable<L>> implements Externalizable {

    /**
     * The dimensions of the {@link OverlayElement} which have a value greater than one
     */
    private int[] m_orientation;

    /**
     * Labels of the {@link OverlayElement}
     */
    private List<String> m_labels;

    /**
     * The status of the {@link OverlayElement} corresponds to the current status of the {@link OverlayElement} in the
     * annotation process. see {@link OverlayElementStatus} for further details
     */
    private OverlayElementStatus m_status;

    /* */
    private int m_numDims;

    private List<String> m_tmpLabelList;

    /**
     *
     */
    protected long[] m_planePos;

    public abstract Interval getInterval();

    public abstract boolean contains(long[] point);

    public abstract IterableRegionOfInterest getRegionOfInterest();

    public abstract void renderOutline(Graphics g);

    public abstract void renderBoundingBox(Graphics g);

    public abstract void renderInterior(Graphics g, int[] dims);

    public abstract void add(long[] pos);

    public abstract void translate(long[] pos);

    public OverlayElement() {
        m_labels = new ArrayList<String>();
        m_status = OverlayElementStatus.IDLE;
        m_tmpLabelList = new ArrayList<String>();
    }

    public OverlayElement(final long[] planePos, final int[] orientation, final String... labels) {
        this();

        m_planePos = planePos.clone();
        m_orientation = orientation.clone();

        m_numDims = orientation.length;

        if (labels != null) {
            for (int i = 0; i < labels.length; i++) {
                m_labels.add(labels[i]);
            }
        }
    }

    /**
     * 
     * @return current {@link OverlayElementStatus} of the {@link OverlayElement}
     */
    public OverlayElementStatus getStatus() {
        return m_status;
    }

    /**
     * Return the orientation of the {@link OverlayElement}
     * 
     * @return
     */
    public int[] getOrientation() {
        return m_orientation;
    }

    /**
     * 
     * @param status the new status
     */
    public void setStatus(final OverlayElementStatus status) {
        m_status = status;
    }

    /**
     * 
     * Number of dimensions of the given OverlayElement e.g. a plane has 2-Dimensions, cube 3-dimensions etc. There is
     * no relation between the labeled {@link Img} respec. the resulting {@link Labeling} and the dimensionality of the
     * {@link OverlayElement}
     * 
     * @return
     */
    public int getNumDimensions() {
        return m_numDims;
    }

    /**
     * 
     * @return labeling of the {@link OverlayElement}
     */
    public List<String> getLabels() {
        return m_labels;
    }

    /**
     * Removes on label from the given {@link OverlayElement}
     * 
     * @param labels labels to remove
     */
    public void removeLabel(final String... labels) {
        for (final String label : labels) {
            m_labels.remove(label);
        }
    }

    /**
     * Add labels to the current Labeling of the {@link OverlayElement}
     * 
     * @param labels labels to add
     */
    public void addLabels(final String... labels) {
        for (final String label : labels) {
            m_labels.add(label);
        }
    }

    /**
     * Sets the labels of the {@link OverlayElement} and removes the previous
     * 
     * @param selectedLabels the new labels of this {@link OverlayElement}
     * @return
     */
    public boolean setLabels(final String... selectedLabels) {
        m_labels.clear();
        boolean changed = false;
        for (int i = 0; i < selectedLabels.length; i++) {
            changed = m_labels.add(selectedLabels[i]) || changed;
        }

        return changed;
    }

    /**
     * Renders the {@link OverlayElement} into a given segmentation
     * 
     * @param labeling the {@link Labeling} where the {@link OverlayElement} is rendered to
     * 
     * @param internedList the labels as InternedList representation (given by the {@link LabelingMapping})
     */
    public void renderOnSegmentationImage(final Labeling<String> labeling, final List<String> internedList) {

        if (labeling.numDimensions() != getNumDimensions()) {
            throw new IllegalArgumentException(
                    "(Sub)Labeling must have the same number of dimensions as the OverlayElement");
        }

        final Cursor<LabelingType<String>> c = getRegionOfInterest().getIterableIntervalOverROI(labeling).cursor();

        final long[] pos = new long[c.numDimensions()];
        while (c.hasNext()) {
            c.fwd();
            c.localize(pos);

            if (contains(pos)) {

                if (c.get().getLabeling().isEmpty()) {
                    c.get().setLabeling(internedList);
                } else {

                    m_tmpLabelList.addAll(internedList);

                    for (final String label : c.get().getLabeling()) {
                        if (!m_tmpLabelList.contains(label)) {
                            m_tmpLabelList.add(label);
                        }
                    }

                    c.get().setLabeling(m_tmpLabelList);
                    m_tmpLabelList.clear();
                }
            }
        }
    }

    /**
     * @return position of the upper left corner of the overlayelement in the dimensionality of the annotated img
     * 
     */
    public long[] getPlanePos() {
        return m_planePos;
    }

    /**
     * Method to indicate weather the overlay element lies in one dimensions or not
     * 
     * @param dim the dimension to check
     * 
     * @return true if overlayelement lies in one dimension
     */
    public final boolean isOrientation(final int dim) {
        for (int i = 0; i < m_orientation.length; i++) {
            if (dim == i) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(m_planePos.length);
        for (int i = 0; i < m_planePos.length; i++) {
            out.writeLong(m_planePos[i]);
        }

        out.writeInt(m_orientation.length);
        for (int i = 0; i < m_orientation.length; i++) {
            out.writeInt(m_orientation[i]);
        }

        out.writeInt(m_labels.size());
        for (int i = 0; i < m_labels.size(); i++) {
            out.writeUTF(m_labels.get(i));
        }

    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_labels.clear();

        int num = in.readInt();
        m_planePos = new long[num];
        for (int i = 0; i < num; i++) {
            m_planePos[i] = in.readLong();
        }

        num = in.readInt();
        m_orientation = new int[num];
        for (int i = 0; i < num; i++) {
            m_orientation[i] = in.readInt();
        }

        num = in.readInt();
        for (int i = 0; i < num; i++) {
            m_labels.add(in.readUTF());
        }

        m_numDims = m_orientation.length;
    }

}
