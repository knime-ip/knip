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
package org.knime.knip.base.data.labeling;

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
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.Named;
import net.imglib2.meta.Sourced;
import net.imglib2.ops.operation.SubsetOperations;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.BlobDataCell;
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
import org.knime.knip.core.awt.ColorLabelingRenderer;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.ExternalizerManager;

/**
 * 
 * Cell holding a labeling and some metadata.
 * 
 * @param <L> image type
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingCell<L extends Comparable<L>> extends FileStoreCell implements LabelingValue<L>, StringValue,
        IntervalValue {

    /**
     * ObjectRepository
     */
    private static final ObjectRepository m_objectRepository = ObjectRepository.getInstance();

    /**
     * NodeLogger
     */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(LabelingCell.class);

    /**
     * UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Convenience access method for DataType.getType(ImageCell.class).
     */
    public static final DataType TYPE = DataType.getType(LabelingCell.class);

    /**
     * Image cells need not to be compressed.
     * 
     * @see BlobDataCell#USE_COMPRESSION
     */
    public static final boolean USE_COMPRESSION = false;

    /**
     * Returns the factory to read/write DataCells of this class from/to a DataInput/DataOutput. This method is called
     * via reflection.
     * 
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    @SuppressWarnings("rawtypes")
    public static <L extends Number & Comparable<L>> DataCellSerializer<LabelingCell> getCellSerializer() {
        return new DataCellSerializer<LabelingCell>() {

            @Override
            public LabelingCell<L> deserialize(final DataCellDataInput input) throws IOException {
                final LabelingCell<L> res = new LabelingCell<L>();
                res.load(input);
                return res;

            }

            @Override
            public void serialize(final LabelingCell cell, final DataCellDataOutput output) throws IOException {
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
        return LabelingValue.class;
    }

    /* information needed to store the data into a file */
    private FileStoreCellMetadata m_fileMetadata;

    /* the labeling object */
    private Labeling<L> m_lab;

    /* metadata of the labeling */
    private LabelingCellMetadata m_labelingMetadata;

    protected LabelingCell() throws IOException {
        super();
    }

    protected LabelingCell(final Labeling<L> labeling, final LabelingMetadata metadata, final FileStore fileStore) {
        super(fileStore);
        final long[] dimensions = new long[labeling.numDimensions()];
        labeling.dimensions(dimensions);
        m_lab = labeling;
        m_labelingMetadata = new LabelingCellMetadata(metadata, labeling.size(), dimensions, null);
        m_fileMetadata = new FileStoreCellMetadata(-1, false, null);

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

        // set the labeling mapping
        final ColorLabelingRenderer<L> rend = new ColorLabelingRenderer<L>();
        rend.setLabelMapping(m_lab.randomAccess().get().getMapping());

        int i = 0;
        final long[] max = new long[m_lab.numDimensions()];
        max[0] = m_lab.max(0);
        max[1] = m_lab.max(1);
        for (i = 2; i < m_lab.numDimensions(); i++) {
            if ((m_lab.dimension(i) == 2) || (m_lab.dimension(i) == 3)) {
                max[i] = m_lab.max(i);
                break;
            }
        }

        final RandomAccessibleInterval<LabelingType<L>> subInterval =
                getSubInterval(new FinalInterval(new long[m_lab.numDimensions()], max));

        rend.setLabelingColorTable(LabelingColorTableUtils.extendLabelingColorTable(m_labelingMetadata
                .getLabelingMetadata().getLabelingColorTable(), new RandomMissingColorHandler()));
        return AWTImageTools.renderScaledStandardColorImg(subInterval, rend, factor, new long[max.length]);
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
        readLabelingData(m_fileMetadata.getOffset(), true);
        return m_labelingMetadata.getLabelingMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getDimensions() {
        readLabelingData(m_fileMetadata.getOffset(), true);
        return m_labelingMetadata.getDimensions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Labeling<L> getLabeling() {
        // lazily load the labeling data only on demand
        readLabelingData(m_fileMetadata.getOffset(), false);
        return m_lab;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Labeling<L> getLabelingCopy() {
        // lazily load the labeling data only on demand
        readLabelingData(m_fileMetadata.getOffset(), false);
        return m_lab.copy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized LabelingMetadata getLabelingMetadata() {
        readLabelingData(m_fileMetadata.getOffset(), true);
        return m_labelingMetadata.getLabelingMetadata();
    }

    private LabelingCellMetadata getLabelingMetadataToWrite() {
        int height = KNIMEKNIPPlugin.getMaximumImageCellHeight();
        if (height == 0) {
            height = (int)m_labelingMetadata.getDimensions()[1];
        }
        final int width = getThumbnailWidth(height);
        if (((height * width) / (double)m_labelingMetadata.getSize()) < KNIMEKNIPPlugin.getThumbnailImageRatio()) {
            getThumbnail(null);
            return m_labelingMetadata;
        } else {
            return new LabelingCellMetadata(m_labelingMetadata.getLabelingMetadata(), m_labelingMetadata.getSize(),
                    m_labelingMetadata.getDimensions(), null);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getMaximum() {
        readLabelingData(m_fileMetadata.getOffset(), true);
        final long[] max = new long[m_labelingMetadata.getDimensions().length];
        for (int i = 0; i < max.length; i++) {
            max[i] = m_labelingMetadata.getDimensions()[i] - 1;
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getMinimum() {
        readLabelingData(m_fileMetadata.getOffset(), true);
        final long[] min = new long[m_labelingMetadata.getDimensions().length];
        return min;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Named getName() {
        readLabelingData(m_fileMetadata.getOffset(), true);
        return m_labelingMetadata.getLabelingMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Sourced getSource() {
        readLabelingData(m_fileMetadata.getOffset(), true);
        return m_labelingMetadata.getLabelingMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getStringValue() {
        readLabelingData(m_fileMetadata.getOffset(), true);
        final StringBuffer sb = new StringBuffer();
        sb.append("Labeling[\nname=");
        sb.append(m_labelingMetadata.getLabelingMetadata().getName());
        sb.append(";\nsource=");
        sb.append(m_labelingMetadata.getLabelingMetadata().getSource());
        sb.append(";\ndimensions=");
        final int numDims = m_labelingMetadata.getDimensions().length;
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(m_labelingMetadata.getDimensions()[i]);
            sb.append(",");
        }
        sb.append(m_labelingMetadata.getDimensions()[numDims - 1]);
        sb.append(" (");
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(m_labelingMetadata.getLabelingMetadata().axis(i).type().getLabel());
            sb.append(",");
        }
        sb.append(m_labelingMetadata.getLabelingMetadata().axis(numDims - 1).type().getLabel());
        sb.append(")]");
        return sb.toString();
    }

    private RandomAccessibleInterval<LabelingType<L>> getSubInterval(final Interval interval) {
        return SubsetOperations.subsetview(m_lab, interval);
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
            readLabelingData(m_fileMetadata.getOffset(), true);
            double height =
                    Math.min(m_labelingMetadata.getDimensions()[1], KNIMEKNIPPlugin.getMaximumImageCellHeight());
            if (height == 0) {
                height = (int)m_labelingMetadata.getDimensions()[1];
            }
            if ((m_labelingMetadata.getThumbnail() == null)
                    || (m_labelingMetadata.getThumbnail().getHeight() != height)) {
                readLabelingData(m_fileMetadata.getOffset(), false);
                m_labelingMetadata =
                        new LabelingCellMetadata(m_labelingMetadata.getLabelingMetadata(),
                                m_labelingMetadata.getSize(), m_labelingMetadata.getDimensions(),
                                createThumbnail(height / m_labelingMetadata.getDimensions()[1]));
                // update cached object
                m_objectRepository.cacheObject(this);
            }
            return m_labelingMetadata.getThumbnail();
        }

    }

    private int getThumbnailWidth(final int height) {
        return (int)(((double)m_lab.dimension(0) / m_lab.dimension(1)) * height);
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

        // BufferedDataInputStream stream = new
        // BufferedDataInputStream(
        // (InputStream) input);

        try {
            // work-around for bug#3578
            // m_fileMetadata =
            // ExternalizerManager.read(stream);
            final int length = input.readInt();
            for (int i = 0; i < length; i++) {
                input.readChar();
            }
            m_fileMetadata = new FileStoreCellMetadata(input.readLong(), input.readBoolean(), null);
        } catch (final Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            LOGGER.error("Exception while reading the labeling cell  metadata", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postConstruct() {
        // use this, if lazy loading is not wanted
        // readLabelingData(m_fileMetadata.getOffset());
    }

    /*
     * reads the labeling data including metadata, dimensions, thumbnail
     * etc.
     */
    private synchronized void readLabelingData(final long offset, final boolean metadataOnly) {

        if ((metadataOnly && (m_labelingMetadata != null)) || (!metadataOnly && (m_lab != null))) {
            return;
        }

        try {
            final Object tmp = m_objectRepository.getCachedObject(this);
            if ((tmp == null) || (!metadataOnly && (((LabelingCell<L>)tmp).m_lab == null))) {
                if (!metadataOnly) {
                    final File f = getFileStore().getFile();
                    final BufferedDataInputStream stream = createInputStream(f, offset);

                    // read labeling if not a file store img
                    LOGGER.debug("Load labeling data...");
                    m_labelingMetadata = ExternalizerManager.read(stream);
                    m_lab = ExternalizerManager.read(stream);
                    stream.close();
                } else {
                    final File f = getFileStore().getFile();
                    final BufferedDataInputStream stream = createInputStream(f, offset);

                    LOGGER.debug("Load labeling metadata...");
                    m_labelingMetadata = ExternalizerManager.read(stream);
                    stream.close();
                }
                m_objectRepository.cacheObject(this);

            } else {

                @SuppressWarnings("unchecked")
                final LabelingCell<L> cell = (LabelingCell<L>)tmp;
                m_labelingMetadata = cell.m_labelingMetadata;
                m_lab = cell.m_lab;
            }

        } catch (final Exception e) {
            LOGGER.error("Cannot read labeling.", e);
        }

    }

    /**
     * Stores the cell content. A few meta information to the data output, the heavy data to the file.
     */
    protected synchronized void save(final DataOutput output) throws IOException {
        long offset = m_fileMetadata.getOffset();
        final boolean isPersistent = m_fileMetadata.isPersistent();

        // if the labeling data wasn't made persitent yet ...
        if (!isPersistent) {
            final File file = getFileStore().getFile();
            LOGGER.debug("Save in file " + file.getName() + " ...");
            offset = file.length();
            // write labeling data
            writeLabelingData(file);

        }

        // write file metadata
        // BufferedDataOutputStream stream = new
        // BufferedDataOutputStream(
        // (OutputStream) output);
        try {
            m_fileMetadata = new FileStoreCellMetadata(offset, true, null);
            // work-around for bug #3578:
            output.writeInt(0); // to be
            // backwards-compatible
            output.writeLong(offset);
            output.writeBoolean(true);
            // ExternalizerManager.write(stream,
            // m_fileMetadata);
        } catch (final Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            LOGGER.error("Exception while writing the LabelingCell metadata", e);
            // } finally {
            // stream.flush();
            // stream.close();

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
            writeLabelingData(file);
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

    /* writes the labeling data including the metadata */
    private void writeLabelingData(final File file) throws IOException {
        // write image data if not a FileStoreImg
        BufferedDataOutputStream stream;
        if (file.getName().endsWith(KNIPConstants.ZIP_SUFFIX)) {
            final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file, true));
            zip.putNextEntry(new ZipEntry("labeling"));
            stream = new BufferedDataOutputStream(zip);
        } else {
            stream = new BufferedDataOutputStream(new FileOutputStream(file, true));
        }
        try {
            ExternalizerManager.write(stream, getLabelingMetadataToWrite());
            ExternalizerManager.write(stream, m_lab);
        } catch (final Exception e) {
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            LOGGER.error("Error in saving labeling data.", e);
        } finally {
            stream.flush();
            stream.close();
        }

    }

}
