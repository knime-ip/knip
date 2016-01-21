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
 * Created on Jan 21, 2016 by hornm
 */
package org.knime.knip.base.data.img2;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.knime.core.data.StringValue;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.data.img.ImgPlusCellMetadata;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.renderer.ThumbnailRenderer;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.Real2GreyColorRenderer;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip2.core.storage.Storage;
import org.knime.knip2.core.tree.Access;
import org.scijava.Named;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.Sourced;
import net.imagej.axis.CalibratedAxis;
import net.imagej.ops.MetadataUtil;
import net.imagej.space.CalibratedSpace;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

class ImgPlusCellContent<S extends Storage<S>, T extends RealType<T>> implements ImgPlusValue<T>, StringValue, IntervalValue {


    private Access<S, Img<T>> m_imgAccess;

    private Access<S, ImgPlusCellMetadata> m_metadataAccess;

    ImgPlusCellContent(final Access<S, Img<T>> img,
                              final Access<S, ImgPlusCellMetadata> metadata) {
        m_imgAccess = img;
        m_metadataAccess = metadata;
    }


    public CalibratedSpace<CalibratedAxis> getCalibratedSpace() {
        return m_metadataAccess.get().getMetadata();
    }

    public long[] getMaximum() {
        final ImgPlusCellMetadata imgPlusMetadta = m_metadataAccess.get();
        final long[] max = new long[imgPlusMetadta.getDimensions().length];
        if (imgPlusMetadta.getMinimum() == null) {
            for (int i = 0; i < max.length; i++) {
                max[i] = imgPlusMetadta.getDimensions()[i] - 1;
            }
        } else {
            for (int i = 0; i < max.length; i++) {
                max[i] = imgPlusMetadta.getMinimum()[i] + imgPlusMetadta.getDimensions()[i] - 1;
            }
        }
        return max;
    }

    public Named getName() {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        return tmp.getMetadata();
    }

    public Sourced getSource() {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        return tmp.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue() {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        final StringBuffer sb = new StringBuffer();
        sb.append("Image[\nname=");
        sb.append(tmp.getMetadata().getName());
        sb.append(";\nsource=");
        sb.append(tmp.getMetadata().getSource());
        sb.append(";\ndimensions=");
        final int numDims = tmp.getDimensions().length;
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(tmp.getDimensions()[i]);
            sb.append(",");
        }
        sb.append(tmp.getDimensions()[numDims - 1]);
        sb.append(" (");
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(tmp.getMetadata().axis(i).type().getLabel());
            sb.append(",");
        }
        sb.append(tmp.getMetadata().axis(numDims - 1).type().getLabel());
        sb.append(")");
        sb.append(";\nmin=");
        final long[] min = tmp.getMinimum() == null ? new long[numDims] : tmp.getMinimum();
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(min[i]);
            sb.append(",");
        }
        sb.append(min[numDims - 1]);
        sb.append(";\npixel type=");
        sb.append(tmp.getPixelType().getSimpleName());
        sb.append(")]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getDimensions() {
        return m_metadataAccess.get().getDimensions().clone();
    }

    /**
     * {@inheritDoc}
     */
    public ImgPlus<T> getImgPlus() {
        Img<T> tmpImg = m_imgAccess.get();

        final long[] minimum = getMinimum();
        final long[] localMin = new long[minimum.length];
        tmpImg.min(localMin);

        //TODO: this can be solved by introducing a appropriate AccessProvider?
        if (!Arrays.equals(minimum, localMin)) {
            for (int d = 0; d < minimum.length; d++) {
                if (minimum[d] != 0) {
                    tmpImg = ImgView.wrap(Views.translate(tmpImg, minimum), tmpImg.factory());
                    break;
                }
            }
        }
        final ImgPlus<T> imgPlus = new ImgPlus<T>(tmpImg, m_metadataAccess.get().getMetadata());
        imgPlus.setSource(m_metadataAccess.get().getMetadata().getSource());

        return imgPlus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlus<T> getImgPlusCopy() {
        final ImgPlus<T> source = getImgPlus();
        final ImgPlus<T> dest = new ImgPlus<T>(source.copy());

        MetadataUtil.copyImgPlusMetadata(source, dest);

        return dest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusMetadata getMetadata() {
        final ImgPlusMetadata res = new DefaultImgMetadata(m_metadataAccess.get().getDimensions().length);
        MetadataUtil.copyImgPlusMetadata(m_metadataAccess.get().getMetadata(), res);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getMinimum() {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        long[] min;
        if (tmp.getMinimum() == null) {
            min = new long[tmp.getDimensions().length];
        } else {
            min = tmp.getMinimum().clone();
        }
        return min;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getPixelType() {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        return (Class<T>)tmp.getPixelType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getThumbnail(final RenderingHints renderingHints) {
        boolean renderMetadata;
        if ((renderingHints != null)
                && (renderMetadata = renderingHints.get(ThumbnailRenderer.RENDERING_HINT_KEY_METADATA) != null)
                && renderMetadata) {
            final int height = 200;
            return AWTImageTools.makeTextBufferedImage(getStringValue(), height * 3, height, "\n");

        } else {

            double height = 1; // default for 1d images
            double fullHeight = 1;

            ImgPlusCellMetadata tmp = m_metadataAccess.get();
            if (tmp.getDimensions().length > 1) {
                height = Math.min(tmp.getDimensions()[1], KNIMEKNIPPlugin.getMaximumImageCellHeight());
                if (height == 0) {
                    height = tmp.getDimensions()[1];
                }

                fullHeight = tmp.getDimensions()[1];
            }

            //TODO: use caching for the thumbnail
            return createThumbnail(height / fullHeight);
//
//            if ((tmp.getThumbnail() == null) || (tmp.getThumbnail().getHeight() != height)) {
//
//                m_metadataAccess = new CachedObjectAccess<>(getFileStore(),
//                        tmp = new ImgPlusCellMetadata(tmp.getMetadata(), tmp.getSize(), tmp.getMinimum(),
//                                tmp.getDimensions(), tmp.getPixelType(), createThumbnail(height / fullHeight)));
//                // update cached object
//                CACHE.put(this, this);
//            }
//            return tmp.getThumbnail();
        }
    }

    private int getThumbnailWidth(final int height) {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        if (tmp.getDimensions().length == 1) {
            return (int)tmp.getDimensions()[0];
        } else {
            return (int)(((double)tmp.getDimensions()[0] / tmp.getDimensions()[1]) * height);
        }
    }

    private BufferedImage createThumbnail(final double factor) {
        final Img<T> tmpImg = m_imgAccess.get();

        // make sure that at least two dimensions exist
        RandomAccessibleInterval<T> img2d;
        if (tmpImg.numDimensions() > 1) {
            img2d = tmpImg;
        } else {
            img2d = Views.addDimension(tmpImg, 0, 0);
        }

        int i = 0;
        final long[] max = new long[img2d.numDimensions()];
        final long[] min = new long[img2d.numDimensions()];
        max[0] = img2d.max(0);
        max[1] = img2d.max(1);
        min[0] = img2d.min(0);
        min[1] = img2d.min(1);
        for (i = 2; i < img2d.numDimensions(); i++) {
            if ((img2d.dimension(i) == 2) || (img2d.dimension(i) == 3)) {
                max[i] = img2d.max(i);
                min[i] = img2d.min(i);
                break;
            } else {
                min[i] = img2d.min(i);
                max[i] = img2d.min(i);
            }
        }

        final RandomAccessibleInterval<T> toRender;
        if (img2d == tmpImg) {
            toRender = SubsetOperations.subsetview(tmpImg, new FinalInterval(min, max));
        } else {
            toRender = img2d;
        }

        return AWTImageTools.renderScaledStandardColorImg(toRender, new Real2GreyColorRenderer<T>(2), factor,
                                                          new long[max.length]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Access getDelegatedImg() {
        return m_imgAccess;
    }

    Access getDelegatedMetadata() {
        return m_metadataAccess;
    }

}
