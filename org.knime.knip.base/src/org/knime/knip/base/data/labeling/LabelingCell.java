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
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.CachedObjectAccess;
import org.knime.knip.base.data.CachedObjectAccess.StreamSkipper;
import org.knime.knip.base.data.FileStoreCellMetadata;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.renderer.ThumbnailRenderer;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.ColorLabelingRenderer;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.data.LabelingView;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.ExternalizerManager;
import org.scijava.Named;
import org.scijava.cache.CacheService;

import net.imagej.Sourced;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 *
 * Cell holding a labeling and some metadata.
 *
 * @param <L> image type
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingCell<L> extends FileStoreCell implements LabelingValue<L>, StringValue, IntervalValue {

    /**
     * ObjectRepository
     */
    private static final CacheService CACHE = KNIPGateway.cache();

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

    /* information needed to store the data into a file */
    private FileStoreCellMetadata m_fileMetadata;

    /* the labeling object */
    private CachedObjectAccess<LabelingView<L>> m_labelingAccess;

    /* metadata of the labeling */
    private CachedObjectAccess<LabelingCellMetadata> m_metadataAccess;

    /**
     * Constructor for Serialization
     *
     * @throws IOException
     */
    protected LabelingCell() throws IOException {
        super();
    }

    /**
     * Default Constructor
     *
     * @param labeling the {@link RandomAccessibleInterval} stored in this cell
     * @param metadata {@link LabelingMetadata} of the {@link RandomAccessibleInterval} stored in this cell
     * @param fileStore {@link FileStore} used to serialize/deserialize this cell
     */
    protected LabelingCell(final RandomAccessibleInterval<LabelingType<L>> labeling, final LabelingMetadata metadata,
                           final FileStore fileStore) {
        super(fileStore);
        final long[] dimensions = new long[labeling.numDimensions()];
        labeling.dimensions(dimensions);
        m_metadataAccess = new CachedObjectAccess<LabelingCellMetadata>(fileStore,
                new LabelingCellMetadata(metadata, Views.iterable(labeling).size(), dimensions, null));
        m_labelingAccess = new CachedObjectAccess<>(fileStore, new LabelingView<L>(labeling));
        m_fileMetadata = new FileStoreCellMetadata(-1, false, null);

        CACHE.put(this, this);

    }

    private BufferedImage createThumbnail(final double factor) {
        // make sure that at least two dimensions exist
        RandomAccessibleInterval<LabelingType<L>> lab2d;
        if (getLabeling().numDimensions() > 1) {
            lab2d = getLabeling();
        } else {
            lab2d = Views.addDimension(getLabeling(), 0, 0);
        }

        // set the labeling mapping
        final ColorLabelingRenderer<L> rend = new ColorLabelingRenderer<L>();
        rend.setLabelMapping(lab2d.randomAccess().get().getMapping());
        int i = 0;
        final long[] max = new long[lab2d.numDimensions()];
        max[0] = lab2d.max(0);
        max[1] = lab2d.max(1);
        for (i = 2; i < lab2d.numDimensions(); i++) {
            if ((lab2d.dimension(i) == 2) || (lab2d.dimension(i) == 3)) {
                max[i] = lab2d.max(i);
                break;
            }
        }
        final RandomAccessibleInterval<LabelingType<L>> toRender;
        if (getLabeling() == lab2d) {
            toRender = getSubInterval(new FinalInterval(Intervals.minAsLongArray(lab2d), max));
        } else {
            toRender = lab2d;
        }
        rend.setLabelingColorTable(LabelingColorTableUtils
                .extendLabelingColorTable(m_metadataAccess.get().getLabelingMetadata().getLabelingColorTable(),
                                          new RandomMissingColorHandler()));
        return AWTImageTools.renderScaledStandardColorImg(toRender, rend, factor, new long[max.length]);
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
        return m_metadataAccess.get().getLabelingMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getDimensions() {
        return m_metadataAccess.get().getDimensions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized RandomAccessibleInterval<LabelingType<L>> getLabeling() {
        final Object o = m_labelingAccess.get();
        if (o instanceof LabelingView) {
            return ((LabelingView)o).getSrc();
        } else {
            return (RandomAccessibleInterval<LabelingType<L>>)o;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized LabelingMetadata getLabelingMetadata() {
        return m_metadataAccess.get().getLabelingMetadata();
    }

    private LabelingCellMetadata getLabelingMetadataToWrite() {
        final LabelingCellMetadata tmp = m_metadataAccess.get();
        int height = KNIMEKNIPPlugin.getMaximumImageCellHeight();
        if (height == 0) {
            height = (int)tmp.getDimensions()[1];
        }
        final int width = getThumbnailWidth(height);
        if (((height * width) / (double)tmp.getSize()) < KNIMEKNIPPlugin.getThumbnailImageRatio()) {
            getThumbnail(null);
            return tmp;
        } else {
            return new LabelingCellMetadata(tmp.getLabelingMetadata(), tmp.getSize(), tmp.getDimensions(), null);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getMaximum() {
        final LabelingCellMetadata labelingCellMetadata = m_metadataAccess.get();
        final long[] max = new long[labelingCellMetadata.getDimensions().length];
        for (int i = 0; i < max.length; i++) {
            max[i] = labelingCellMetadata.getDimensions()[i] - 1;
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getMinimum() {
        final LabelingCellMetadata labelingCellMetadata = m_metadataAccess.get();
        final long[] min = new long[labelingCellMetadata.getDimensions().length];
        return min;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Named getName() {
        return m_metadataAccess.get().getLabelingMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Sourced getSource() {
        return m_metadataAccess.get().getLabelingMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getStringValue() {
        final LabelingCellMetadata tmp = m_metadataAccess.get();
        final StringBuffer sb = new StringBuffer();
        sb.append("Labeling[\nname=");
        sb.append(tmp.getLabelingMetadata().getName());
        sb.append(";\nsource=");
        sb.append(tmp.getLabelingMetadata().getSource());
        sb.append(";\ndimensions=");
        final int numDims = tmp.getDimensions().length;
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(tmp.getDimensions()[i]);
            sb.append(",");
        }
        sb.append(tmp.getDimensions()[numDims - 1]);
        sb.append(" (");
        for (int i = 0; i < (numDims - 1); i++) {
            sb.append(tmp.getLabelingMetadata().axis(i).type().getLabel());
            sb.append(",");
        }
        sb.append(tmp.getLabelingMetadata().axis(numDims - 1).type().getLabel());
        sb.append(")]");
        return sb.toString();
    }

    private RandomAccessibleInterval<LabelingType<L>> getSubInterval(final Interval interval) {
        return SubsetOperations.subsetview(getLabeling(), interval);
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

            LabelingCellMetadata tmp = m_metadataAccess.get();
            if (tmp.getDimensions().length > 1) {
                height = Math.min(tmp.getDimensions()[1], KNIMEKNIPPlugin.getMaximumImageCellHeight());
                if (height == 0) {
                    height = tmp.getDimensions()[1];
                }

                fullHeight = tmp.getDimensions()[1];
            }
            if ((tmp.getThumbnail() == null) || (tmp.getThumbnail().getHeight() != height)) {
                m_metadataAccess = new CachedObjectAccess<LabelingCellMetadata>(getFileStore(),
                        tmp = new LabelingCellMetadata(tmp.getLabelingMetadata(), tmp.getSize(), tmp.getDimensions(),
                                createThumbnail(height / fullHeight)));
                // update cached object
                CACHE.put(this, this);
            }
            return tmp.getThumbnail();
        }

    }

    private int getThumbnailWidth(final int height) {
        final LabelingCellMetadata tmp = m_metadataAccess.get();
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
     * Loads the cell content. To be called after the #FileImgPlusCell(DataCellDataInput) constructor.
     *
     * @param input
     * @throws IOException
     */
    protected synchronized void load(final DataInput input) throws IOException {

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
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postConstruct() {

        @SuppressWarnings("unchecked")
        final LabelingCell<L> tmp = (LabelingCell<L>)CACHE.get(this);

        if (tmp == null) {
            // Creates empty CachedObjectAccesses which know how to reconstruct the managed objects.
            m_metadataAccess = new CachedObjectAccess<>(getFileStore(), null, m_fileMetadata.getOffset(), null);
            m_labelingAccess =
                    new CachedObjectAccess<>(getFileStore(), null, m_fileMetadata.getOffset(), new StreamSkipper() {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void skip(final BufferedDataInputStream in) {
                            try {
                                m_metadataAccess.setObject(ExternalizerManager.read(in));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

            CACHE.put(this, this);
        } else {
            m_labelingAccess = tmp.m_labelingAccess;
            m_metadataAccess = tmp.m_metadataAccess;
        }

    }

    /**
     * Stores the cell content. A few meta information to the data output, the heavy data to the file.
     *
     * @param output {@link DataOutput} to persist cell
     * @throws IOException can be thrown during serialization
     */
    protected synchronized void save(final DataOutput output) throws IOException {
        long offset = m_fileMetadata.getOffset();

        try {
            flushToFileStore();

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
            offset = file.length();

            m_metadataAccess.setObject(getLabelingMetadataToWrite());
            m_metadataAccess.serialize();
            m_labelingAccess.serialize();
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
