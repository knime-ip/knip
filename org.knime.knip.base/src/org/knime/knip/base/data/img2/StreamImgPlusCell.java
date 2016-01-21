/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2016
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
 * ---------------------------------------------------------------------
 *
 * Created on Jan 19, 2016 by hornm
 */
package org.knime.knip.base.data.img2;

import java.awt.Image;
import java.awt.RenderingHints;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.data.img.ImgPlusCellMetadata;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip2.core.storage.SimpleStorage;
import org.knime.knip2.core.tree.Access;
import org.scijava.Named;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.Sourced;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

/**
 *
 * Temporary version of a {@link DataCell} that mainly keeps an {@link ImgPlus} and makes use of the knip-scijava
 * framework to represent and externalize objects.
 *
 * @param <T> Type of cell
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 */
public class StreamImgPlusCell<T extends RealType<T>> extends DataCell
        implements ImgPlusValue<T>, StringValue, IntervalValue, Externalizable {

    /**
     * The {@link DataType}.
     */
    public static DataType TYPE = DataType.getType(StreamImgPlusCell.class);

    /* Content most methods are delegated to */
    private ImgPlusCellContent<SimpleStorage, T> m_content;

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public StreamImgPlusCell(final Access<SimpleStorage, Img<T>> img,
                         final Access<SimpleStorage, ImgPlusCellMetadata> metadata) {
        m_content = new ImgPlusCellContent<>(img, metadata);
    }


    public StreamImgPlusCell() {
        //deserialization
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        Access<SimpleStorage,Img<T>> img = (Access<SimpleStorage, Img<T>>) in.readObject();
        Access<SimpleStorage,ImgPlusCellMetadata> metadata = (Access<SimpleStorage, ImgPlusCellMetadata>) in.readObject();
        m_content = new ImgPlusCellContent<>(img, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(m_content.getDelegatedImg());
        out.writeObject(m_content.getDelegatedMetadata());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CalibratedSpace<CalibratedAxis> getCalibratedSpace() {
        return m_content.getCalibratedSpace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getMaximum() {
        return m_content.getMaximum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Named getName() {
        return m_content.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sourced getSource() {
        return m_content.getSource();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue() {
        return m_content.getStringValue() + "DEBUG: DataCell";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getDimensions() {
        return m_content.getDimensions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlus<T> getImgPlus() {
        return m_content.getImgPlus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlus<T> getImgPlusCopy() {
        return m_content.getImgPlusCopy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusMetadata getMetadata() {
        return m_content.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getMinimum() {
        return m_content.getMinimum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getPixelType() {
        return m_content.getPixelType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getThumbnail(final RenderingHints renderingHints) {
        return m_content.getThumbnail(renderingHints);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Access getDelegatedImg() {
        return m_content.getDelegatedImg();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getStringValue().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return this.equals(dc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getStringValue();
    }


}
