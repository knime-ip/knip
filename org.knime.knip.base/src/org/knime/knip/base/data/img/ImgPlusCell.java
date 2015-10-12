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
import java.awt.image.BufferedImage;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.CachedObjectAccess;
import org.knime.knip.base.data.CachedObjectAccess.StreamSkipper;
import org.knime.knip.base.data.FileStoreCellMetadata;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.renderer.ThumbnailRenderer;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.Real2GreyColorRenderer;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.ExternalizerManager;
import org.scijava.Named;
import org.scijava.cache.CacheService;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.Sourced;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.WrappedImg;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.util.MetadataUtil;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 *
 * File cell keeping {@link ImgPlus}.
 *
 * @param <T> Type of cell
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgPlusCell<T extends RealType<T>> extends FileStoreCell
        implements ImgPlusValue<T>, StringValue, IntervalValue {

    /**
     * Type
     *
     * FIXME: Replace
     */
    public static DataType TYPE = DataType.getType(ImgPlusCell.class);

    /**
     * ObjectRepository
     */
    private static final CacheService CACHE = KNIPGateway.cache();

    /**
     * Node Logger
     */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgPlusCell.class);

    /**
     * UID
     */
    private static final long serialVersionUID = 1L;

    private FileStoreCellMetadata m_fileMetadata;

    private CachedObjectAccess<Img<T>> m_imgAccess;

    private CachedObjectAccess<ImgPlusCellMetadata> m_metadataAccess;

    /**
     * @param img
     * @return
     */
    private static long[] getMinFromImg(final Img<?> img) {
        long[] min = new long[img.numDimensions()];
        img.min(min);
        return min;
    }

    /**
     * Creates a new img plus cell using the given file store, i.e. writes the image data into the file provided by the
     * file store.
     *
     * @param img
     * @param metadata
     *
     * @param fileStore
     */
    @SuppressWarnings("unchecked")
    protected ImgPlusCell(final Img<T> img, final ImgPlusMetadata metadata, final FileStore fileStore) {
        super(fileStore);

        Img<T> tmpImg = img;
        while (tmpImg instanceof WrappedImg) {
            tmpImg = ((WrappedImg<T>)tmpImg).getImg();
        }

        m_imgAccess = new CachedObjectAccess<Img<T>>(fileStore, tmpImg);

        final long[] dimensions = new long[img.numDimensions()];
        img.dimensions(dimensions);

        m_metadataAccess = new CachedObjectAccess<ImgPlusCellMetadata>(fileStore,
                new ImgPlusCellMetadata(
                        MetadataUtil.copyImgPlusMetadata(metadata, new DefaultImgMetadata(dimensions.length)),
                        img.size(), getMinFromImg(img), dimensions, img.firstElement().getClass(), null));

        m_fileMetadata = new FileStoreCellMetadata(-1, false, null);

        CACHE.put(this, this);
    }

    /**
     * @throws IOException
     */
    protected ImgPlusCell() throws IOException {
        super();

    }

    /**
     * @param fileStore
     */
    protected ImgPlusCell(final FileStore fileStore) {
        super(fileStore);
    }

    private BufferedImage createThumbnail(final double factor) {
        LOGGER.debug("Create thumbnail ...");

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
    protected boolean equalsDataCell(final DataCell dc) {
        return dc.hashCode() == hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized CalibratedSpace<CalibratedAxis> getCalibratedSpace() {
        return m_metadataAccess.get().getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getDimensions() {
        return m_metadataAccess.get().getDimensions().clone();
    }

    private ImgPlusCellMetadata getImgMetadataToWrite() {

        final ImgPlusCellMetadata tmpImgMetadata = m_metadataAccess.get();
        int height = KNIMEKNIPPlugin.getMaximumImageCellHeight();
        if (height == 0) {
            height = (int)tmpImgMetadata.getDimensions()[1];
        }
        final int width = getThumbnailWidth(height);
        if (((height * width) / (double)tmpImgMetadata.getSize()) < KNIMEKNIPPlugin.getThumbnailImageRatio()) {
            getThumbnail(null);
            return tmpImgMetadata;
        } else {
            return new ImgPlusCellMetadata(tmpImgMetadata.getMetadata(), tmpImgMetadata.getSize(),
                    tmpImgMetadata.getMinimum(), tmpImgMetadata.getDimensions(), tmpImgMetadata.getPixelType(), null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ImgPlus<T> getImgPlus() {
        Img<T> tmpImg = m_imgAccess.get();

        final long[] minimum = getMinimum();
        final long[] localMin = new long[minimum.length];
        tmpImg.min(localMin);

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
    public synchronized ImgPlus<T> getImgPlusCopy() {
        final ImgPlus<T> source = getImgPlus();
        final ImgPlus<T> dest = new ImgPlus<T>(source.copy());

        MetadataUtil.copyImgPlusMetadata(source, dest);

        return dest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getMaximum() {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ImgPlusMetadata getMetadata() {
        final ImgPlusMetadata res = new DefaultImgMetadata(m_metadataAccess.get().getDimensions().length);
        MetadataUtil.copyImgPlusMetadata(m_metadataAccess.get().getMetadata(), res);
        return res;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    @Deprecated
    @Override
    public synchronized long[] getMinimum() {

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
    public synchronized Named getName() {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        return tmp.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized Class<T> getPixelType() {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        return (Class<T>)tmp.getPixelType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Sourced getSource() {
        final ImgPlusCellMetadata tmp = m_metadataAccess.get();
        return tmp.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getStringValue() {
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
    public synchronized Image getThumbnail(final RenderingHints renderingHints) {
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

            if ((tmp.getThumbnail() == null) || (tmp.getThumbnail().getHeight() != height)) {
                m_metadataAccess = new CachedObjectAccess<>(getFileStore(),
                        tmp = new ImgPlusCellMetadata(tmp.getMetadata(), tmp.getSize(), tmp.getMinimum(),
                                tmp.getDimensions(), tmp.getPixelType(), createThumbnail(height / fullHeight)));
                // update cached object
                CACHE.put(this, this);
            }
            return tmp.getThumbnail();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (int)(getFileStore().getFile().hashCode() + (31 * m_fileMetadata.getOffset()));
    }

    /**
     * Loads the cell content. To be called after the {@link #FileImgPlusCell(DataCellDataInput)} constructor.
     *
     * @param input
     * @throws IOException
     */
    @SuppressWarnings("javadoc")
    protected synchronized void load(final DataInput input) throws IOException {
        LOGGER.debug("Load file metadata...");

        try {
            // m_fileMetadata =
            // ExternalizerManager.read(stream);
            // work-around bug#3578
            // to be backwards-compatible:
            final int length = input.readInt();
            for (int i = 0; i < length; i++) {
                input.readChar();
            }
        } catch (final Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            LOGGER.error("Exception while reading the ImgPlusCellMetadata", e);
        }
        m_fileMetadata = new FileStoreCellMetadata(input.readLong(), input.readBoolean(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postConstruct() {
        // Creates empty CachedObjectAccesses which know how to reconstruct the managed objects.
        m_metadataAccess =
                new CachedObjectAccess<ImgPlusCellMetadata>(getFileStore(), null, m_fileMetadata.getOffset(), null);
        m_imgAccess =
                new CachedObjectAccess<Img<T>>(getFileStore(), null, m_fileMetadata.getOffset(), new StreamSkipper() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void skip(final BufferedDataInputStream in) {
                        try {
                            ExternalizerManager.read(in);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        CACHE.put(this, this);
    }

    /**
     * Stores the cell content. A few meta information to the data output, the heavy data to the file.
     *
     * @param output
     * @throws IOException
     */
    protected synchronized void save(final DataOutput output) throws IOException {
        long offset = m_fileMetadata.getOffset();

        try {
            // if the image data wasn't made persitent yet ...
            flushToFileStore();

            // ExternalizerManager.write(stream,
            // m_fileMetadata);
            // work-around for bug #3578: if the output is
            // wrapped
            // with the BufferedDataOutputStream, the data
            // cell
            // cannot be wrapped into a ListCell

            output.writeInt(0); // to be
            // backwards-compatible
            output.writeLong(offset);
            output.writeBoolean(true);
        } catch (final Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            LOGGER.error("Exception while writing the ImgPlusCellMetadata", e);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void flushToFileStore() throws IOException {
        long offset = m_fileMetadata.getOffset();
        final boolean isPersistent = m_fileMetadata.isPersistent();

        // if the image data wasn't made persitent yet ...
        if (!isPersistent) {
            final File file = getFileStore().getFile();
            LOGGER.debug("Save in file " + file.getName() + " ...");
            offset = file.length();

            // write image data
            m_metadataAccess.setObject(getImgMetadataToWrite());
            m_metadataAccess.serialize();
            m_imgAccess.serialize();
        }

        m_fileMetadata = new FileStoreCellMetadata(offset, true, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getStringValue();
    }
}
