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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.meta.Named;
import net.imglib2.meta.Sourced;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.imgplus.unary.ImgPlusCopy;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.FileStoreCellMetadata;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.data.ObjectRepository;
import org.knime.knip.base.renderer.ThumbnailRenderer;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.Real2GreyColorRenderer;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.ExternalizerManager;
import org.knime.knip.core.util.ImgUtils;

/**
 * 
 * File cell keeping {@link ImgPlus}.
 * 
 * @param <T> Type of cell
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgPlusCell<T extends RealType<T>> extends FileStoreCell implements ImgPlusValue<T>, StringValue,
        IntervalValue {

    /**
     * ObjectRepository
     */
    private static final ObjectRepository m_objectRepository = ObjectRepository.getInstance();

    /**
     * Node Logger
     */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgPlusCell.class);

    /**
     * UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Convenience access method for DataType.getType(ImageCell.class).
     */
    public static final DataType TYPE = DataType.getType(ImgPlusCell.class);

    @SuppressWarnings("rawtypes")
    public static final DataCellSerializer<ImgPlusCell> getCellSerializer() {
        return new DataCellSerializer<ImgPlusCell>() {

            /** {@inheritDoc} */
            @Override
            public ImgPlusCell deserialize(final DataCellDataInput input) throws IOException {
                final ImgPlusCell res = new ImgPlusCell();
                res.load(input);
                return res;
            }

            /** {@inheritDoc} */
            @Override
            public void serialize(final ImgPlusCell cell, final DataCellDataOutput output) throws IOException {
                cell.save(output);

            }
        };
    }

    /**
     * Preferred value class of this cell implementation is ImageValue.class.
     * 
     * @return ImageValue.class
     */
    public static Class<? extends DataValue> getPreferredValueClass() {
        return ImgPlusValue.class;
    }

    private FileStoreCellMetadata m_fileMetadata;

    private Img<T> m_img;

    private ImgPlusCellMetadata m_imgMetadata;

    /**
     * @param input
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

    /**
     * Creates a new img plus cell using the given file store, i.e. writes the image data into the file provided by the
     * file store.
     * 
     * @param img
     * @param metadata
     * @param fileStore
     */
    protected ImgPlusCell(final Img<T> img, final ImgPlusMetadata metadata, final FileStore fileStore) {
        this(img, metadata, null, fileStore);

    }

    /**
     * Creates a new img plus cell using the given file store, i.e. writes the image data into the file provided by the
     * file store.
     * 
     * @param img
     * @param metadata
     * @param min
     * 
     * @param fileStore
     */
    protected ImgPlusCell(final Img<T> img, final ImgPlusMetadata metadata, final long[] min, final FileStore fileStore) {
        super(fileStore);
        if (img instanceof ImgPlus) {
            m_img = ((ImgPlus<T>)img).getImg();
        } else {
            m_img = img;
        }
        final long[] dimensions = new long[img.numDimensions()];
        img.dimensions(dimensions);

        m_fileMetadata = new FileStoreCellMetadata(-1, false, null);

        m_imgMetadata =
                new ImgPlusCellMetadata(MetadataUtil.copyImgPlusMetadata(metadata, new DefaultImgMetadata(
                        dimensions.length)), img.size(), min, dimensions, img.firstElement().getClass(), null);
    }

    /**
     * Creates a new img plus cell using the given file store, i.e. writes the image data into the file provided by the
     * file store.
     * 
     * @param imgPlus
     * 
     * @param fileStore
     */
    protected ImgPlusCell(final ImgPlus<T> imgPlus, final FileStore fileStore) {
        this(imgPlus.getImg(), imgPlus, fileStore);
    }

    /*
     * Helper to create the respective input stream (e.g. if zip file or
     * not)
     */
    private BufferedDataInputStream createInputStream(final File f, final long offset) throws IOException {
        BufferedDataInputStream stream = null;
        try {
            if (f.getName().endsWith(KNIPConstants.ZIP_SUFFIX)) {
                final FileInputStream fileInput = new FileInputStream(f);
                fileInput.skip(offset);
                final ZipInputStream zip = new ZipInputStream(fileInput);
                zip.getNextEntry();
                stream = new BufferedDataInputStream(zip);
            } else {
                stream = new BufferedDataInputStream(new FileInputStream(f));
                stream.skip(offset);
            }
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
            throw e;
        }
        return stream;
    }

    private BufferedImage createThumbnail(final double factor) {
        LOGGER.debug("Create thumbnail ...");

        // make sure that at least two dimensions exist
        RandomAccessibleInterval<T> img2d;
        if (m_img.numDimensions() > 1) {
            img2d = m_img;
        } else {
            img2d = Views.addDimension(m_img, 0, 0);
        }

        int i = 0;
        final long[] max = new long[img2d.numDimensions()];
        max[0] = img2d.max(0);
        max[1] = img2d.max(1);
        for (i = 2; i < img2d.numDimensions(); i++) {
            if ((img2d.dimension(i) == 2) || (img2d.dimension(i) == 3)) {
                max[i] = img2d.max(i);
                break;
            }
        }

        final Img<T> img = getSubImg(img2d, m_img.factory(), new FinalInterval(new long[img2d.numDimensions()], max));

        return AWTImageTools.renderScaledStandardColorImg(img, new Real2GreyColorRenderer<T>(2), factor,
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
        readImgData(m_fileMetadata.getOffset(), true);
        return m_imgMetadata.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getDimensions() {
        readImgData(m_fileMetadata.getOffset(), true);
        return m_imgMetadata.getDimensions().clone();
    }

    private ImgPlusCellMetadata getImgMetadataToWrite() {
        int height = KNIMEKNIPPlugin.getMaximumImageCellHeight();
        if (height == 0) {
            height = (int)m_imgMetadata.getDimensions()[1];
        }
        final int width = getThumbnailWidth(height);
        if (((height * width) / (double)m_imgMetadata.getSize()) < KNIMEKNIPPlugin.getThumbnailImageRatio()) {
            getThumbnail(null);
            return m_imgMetadata;
        } else {
            return new ImgPlusCellMetadata(m_imgMetadata.getMetadata(), m_imgMetadata.getSize(),
                    m_imgMetadata.getMinimum(), m_imgMetadata.getDimensions(), m_imgMetadata.getPixelType(), null);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ImgPlus<T> getImgPlus() {
        readImgData(m_fileMetadata.getOffset(), false);
        final ImgPlus<T> imgPlus = new ImgPlus<T>(m_img, m_imgMetadata.getMetadata());
        imgPlus.setSource(m_imgMetadata.getMetadata().getSource());
        return imgPlus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ImgPlus<T> getImgPlusCopy() {
        readImgData(m_fileMetadata.getOffset(), false);
        return new ImgPlusCopy<T>().compute(getImgPlus(), new ImgPlus<T>(ImgUtils.createEmptyImg(m_img)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getMaximum() {
        readImgData(m_fileMetadata.getOffset(), true);
        final long[] max = new long[m_imgMetadata.getDimensions().length];
        for (int i = 0; i < max.length; i++) {
            max[i] = m_imgMetadata.getDimensions()[i] - 1;
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ImgPlusMetadata getMetadata() {
        readImgData(m_fileMetadata.getOffset(), true);
        final ImgPlusMetadata res = new DefaultImgMetadata(m_imgMetadata.getDimensions().length);
        MetadataUtil.copyImgPlusMetadata(m_imgMetadata.getMetadata(), res);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getMinimum() {
        readImgData(m_fileMetadata.getOffset(), true);
        long[] min;
        if (m_imgMetadata.getMinimum() == null) {
            min = new long[m_imgMetadata.getDimensions().length];
        } else {
            min = m_imgMetadata.getMinimum().clone();
        }
        return min;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Named getName() {
        readImgData(m_fileMetadata.getOffset(), true);
        return m_imgMetadata.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized Class<T> getPixelType() {
        readImgData(m_fileMetadata.getOffset(), true);
        return (Class<T>)m_imgMetadata.getPixelType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Sourced getSource() {
        readImgData(m_fileMetadata.getOffset(), true);
        return m_imgMetadata.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getStringValue() {
        readImgData(m_fileMetadata.getOffset(), true);
        final StringBuffer sb = new StringBuffer();
        sb.append("Image[\nname=");
        sb.append(m_imgMetadata.getMetadata().getName());
        sb.append(";\nsource=");
        sb.append(m_imgMetadata.getMetadata().getSource());
        sb.append(";\ndimensions=");
        final int numDims = m_imgMetadata.getDimensions().length;
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(m_imgMetadata.getDimensions()[i]);
            sb.append(",");
        }
        sb.append(m_imgMetadata.getDimensions()[numDims - 1]);
        sb.append(" (");
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(m_imgMetadata.getMetadata().axis(i).type().getLabel());
            sb.append(",");
        }
        sb.append(m_imgMetadata.getMetadata().axis(numDims - 1).type().getLabel());
        sb.append(")");
        sb.append(";\nmin=");
        final long[] min = m_imgMetadata.getMinimum() == null ? new long[numDims] : m_imgMetadata.getMinimum();
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(min[i]);
            sb.append(",");
        }
        sb.append(min[numDims - 1]);
        sb.append(";\npixel type=");
        sb.append(m_imgMetadata.getPixelType().getSimpleName());
        sb.append(")]");
        return sb.toString();
    }

    private Img<T>
            getSubImg(final RandomAccessibleInterval<T> img, final ImgFactory<T> factory, final Interval interval) {
        return new ImgView<T>(SubsetOperations.subsetview(img, interval), factory);
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

            readImgData(m_fileMetadata.getOffset(), true);

            double height = 1; // default for 1d images
            double fullHeight = 1;

            if (m_imgMetadata.getDimensions().length > 1) {
                height = Math.min(m_imgMetadata.getDimensions()[1], KNIMEKNIPPlugin.getMaximumImageCellHeight());
                if (height == 0) {
                    height = m_imgMetadata.getDimensions()[1];
                }

                fullHeight = m_imgMetadata.getDimensions()[1];
            }

            if ((m_imgMetadata.getThumbnail() == null) || (m_imgMetadata.getThumbnail().getHeight() != height)) {
                readImgData(m_fileMetadata.getOffset(), false);
                m_imgMetadata =
                        new ImgPlusCellMetadata(m_imgMetadata.getMetadata(), m_imgMetadata.getSize(),
                                m_imgMetadata.getMinimum(), m_imgMetadata.getDimensions(),
                                m_imgMetadata.getPixelType(), createThumbnail(height / fullHeight));
                // update cached object
                m_objectRepository.cacheObject(this);
            }
            return m_imgMetadata.getThumbnail();
        }

    }

    private int getThumbnailWidth(final int height) {
        return (int)(((double)m_img.dimension(0) / m_img.dimension(1)) * height);
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
            m_fileMetadata = new FileStoreCellMetadata(input.readLong(), input.readBoolean(), null);
        } catch (final Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            LOGGER.error("Exception while reading the ImgPlusCellMetadata", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postConstruct() {
        // use this line of code if lazily loading the image is not
        // desired
        // readImgData(m_fileMetadata.getOffset(), false);
    }

    /* reads the image data including metadata, dimensions, thumbnail etc. */
    private synchronized void readImgData(final long offset, final boolean metadataOnly) {
        // read image if not a file store img
        if ((metadataOnly && (m_imgMetadata != null)) || (!metadataOnly && (m_img != null))) {
            return;
        }

        try {
            final Object tmp = m_objectRepository.getCachedObject(this);
            if ((tmp == null) || (!metadataOnly && (((ImgPlusCell<T>)tmp).m_img == null))) {
                if (!metadataOnly) {
                    // if (m_imgRef != null &&
                    // m_imgRef.get() !=
                    // null) {
                    // m_img = m_imgRef.get();
                    // return;
                    // }
                    final File f = getFileStore().getFile();
                    final BufferedDataInputStream stream = createInputStream(f, offset);

                    LOGGER.debug("Load image data...");
                    m_imgMetadata = ExternalizerManager.read(stream);
                    m_img = ExternalizerManager.read(stream);
                    stream.close();
                } else {
                    final File f = getFileStore().getFile();
                    final BufferedDataInputStream stream = createInputStream(f, offset);
                    LOGGER.debug("Load image metadata...");
                    m_imgMetadata = ExternalizerManager.read(stream);
                    stream.close();
                }
                m_objectRepository.cacheObject(this);

            } else {
                @SuppressWarnings("unchecked")
                final ImgPlusCell<T> cell = (ImgPlusCell<T>)tmp;
                m_imgMetadata = cell.m_imgMetadata;
                m_img = cell.m_img;
            }

        } catch (final Exception e) {
            LOGGER.error("Cannot read image.", e);
        }

    }

    /**
     * Stores the cell content. A few meta information to the data output, the heavy data to the file.
     */
    protected synchronized void save(final DataOutput output) throws IOException {
        long offset = m_fileMetadata.getOffset();
        final boolean isPersistent = m_fileMetadata.isPersistent();

        // if the image data wasn't made persitent yet ...
        if (!isPersistent) {
            final File file = getFileStore().getFile();
            LOGGER.debug("Save in file " + file.getName() + " ...");
            offset = file.length();

            // write image data
            writeImgData(file);
        }

        try {

            m_fileMetadata = new FileStoreCellMetadata(offset, true, null);
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
    protected void flushToFileStore() throws IOException {
        long offset = m_fileMetadata.getOffset();
        final boolean isPersistent = m_fileMetadata.isPersistent();

        // if the image data wasn't made persitent yet ...
        if (!isPersistent) {
            final File file = getFileStore().getFile();
            LOGGER.debug("Save in file " + file.getName() + " ...");
            offset = file.length();

            // write image data
            writeImgData(file);
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

    /* writes the image including metadata to the file */
    private void writeImgData(final File file) throws IOException {

        BufferedDataOutputStream stream;
        if (file.getName().endsWith(KNIPConstants.ZIP_SUFFIX)) {
            final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file, true));
            zip.putNextEntry(new ZipEntry("img"));
            stream = new BufferedDataOutputStream(zip);
        } else {
            stream = new BufferedDataOutputStream(new FileOutputStream(file, true));
        }

        try {
            ExternalizerManager.write(stream, getImgMetadataToWrite());
            ExternalizerManager.write(stream, m_img);
        } catch (final Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            LOGGER.error("Error in saving image data.", e);
        } finally {
            stream.flush();
            stream.close();
        }
    }

}
