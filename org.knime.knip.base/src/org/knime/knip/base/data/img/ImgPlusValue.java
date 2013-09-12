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
package org.knime.knip.base.data.img;

import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.Icon;

import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.knip.base.renderer.ThumbnailRenderer;

/**
 * DataValue for image objects.
 * 
 * @param <T> image type
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public interface ImgPlusValue<T extends RealType<T>> extends DataValue {

    /** Gathers meta information to this type. */
    public static final class ImgPlusUtilityFactory extends UtilityFactory {

        private static final ImgPlusValueComparator COMPARATOR = new ImgPlusValueComparator();

        private static final Icon ICON = loadIcon(ImgPlusValue.class, "../icons/imgplus.png");

        /** Limits scope of constructor, does nothing. */
        protected ImgPlusUtilityFactory() {
            //
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(ThumbnailRenderer.THUMBNAIL_RENDERER);
        }
    }

    /**
     * Static singleton for meta description.
     * 
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new ImgPlusUtilityFactory();

    /**
     * The sizes of the image dimensions.
     * 
     * @return the dimensions as an array
     */
    public long[] getDimensions();

    /**
     * Retrieves the whole image. Its just a REFERENCE. Do NOT manipulate this image and use the
     * <code>getImageCopy()</code> method for that purpose. Also avoid calling the {@link Image}.closeAllCursors()
     * functions on the returned image!
     * 
     * @return the image in this <code>DataCell</code>
     */
    public ImgPlus<T> getImgPlus();

    /**
     * Provides a copy of the image.
     * 
     * @return the image
     * 
     */
    public ImgPlus<T> getImgPlusCopy();

    /**
     * 
     * @return
     */
    public ImgPlusMetadata getMetadata();

    /**
     * @return the minimum/offset of the imageF
     */
    public long[] getMinimum();

    /**
     * @return the pixel type of the image
     */
    public Class<T> getPixelType();

    /**
     * @param renderingHints some rendering hints, most important is the height and if it should produce an image with
     *            details on it
     * @return a small {@link Image} representation of the original image
     */
    public Image getThumbnail(RenderingHints renderingHints);
}
