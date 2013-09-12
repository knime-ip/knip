/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2012
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 18, 2012 (hornm): created
 */

package org.knime.knip.base.data.aggregation;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.meta.Axes;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.data.img.DefaultImgMetadata;

/**
 * Aggregation operator which merges images.
 * 
 * @author Martin Horn, University of Konstanz
 * 
 */
public class ImgMergeOperator<T extends RealType<T> & NativeType<T>, A, ADA extends ArrayDataAccess<ADA>> extends
        ImgAggregrationOperation {

    private class ByteTypeHandler implements RealTypeHandler<ByteType, byte[], ByteArray> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int copyData(final byte[] srcArray, final byte[] resArray, final int fromIndex) {
            System.arraycopy(srcArray, 0, resArray, fromIndex, srcArray.length);
            return fromIndex + srcArray.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] createArray(final int size) {
            return new byte[size];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteType createLinkedType(final PlanarImg img) {
            return new ByteType(img);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setType(final byte[] dataArray, final double val, final int index) {
            dataArray[index] = (byte)val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteArray wrap(final byte[] dataArray) {
            return new ByteArray(dataArray);
        }

    }

    private class CustomPlanarImg extends PlanarImg<T, ADA> {

        /**
         * @param dim
         * @param entitiesPerPixel
         */
        public CustomPlanarImg(final ArrayList<ADA> mirror, final long[] dim, final int entitiesPerPixel) {
            super(dim, entitiesPerPixel);
            for (int i = 0; i < super.mirror.size(); i++) {
                super.mirror.set(i, mirror.get(i));
            }
        }

    }

    private class DoubleTypeHandler implements RealTypeHandler<DoubleType, double[], DoubleArray> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int copyData(final double[] srcArray, final double[] resArray, final int fromIndex) {
            System.arraycopy(srcArray, 0, resArray, fromIndex, srcArray.length);
            return fromIndex + srcArray.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double[] createArray(final int size) {
            return new double[size];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DoubleType createLinkedType(final PlanarImg img) {
            return new DoubleType(img);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setType(final double[] dataArray, final double val, final int index) {
            dataArray[index] = val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DoubleArray wrap(final double[] dataArray) {
            return new DoubleArray(dataArray);
        }
    }

    private class FloatTypeHandler implements RealTypeHandler<FloatType, float[], FloatArray> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int copyData(final float[] srcArray, final float[] resArray, final int fromIndex) {
            System.arraycopy(srcArray, 0, resArray, fromIndex, srcArray.length);
            return fromIndex + srcArray.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public float[] createArray(final int size) {
            return new float[size];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FloatType createLinkedType(final PlanarImg img) {
            return new FloatType(img);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setType(final float[] dataArray, final double val, final int index) {
            dataArray[index] = (float)val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FloatArray wrap(final float[] dataArray) {
            return new FloatArray(dataArray);
        }
    }

    private class IntTypeHandler implements RealTypeHandler<IntType, int[], IntArray> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int copyData(final int[] srcArray, final int[] resArray, final int fromIndex) {
            System.arraycopy(srcArray, 0, resArray, fromIndex, srcArray.length);
            return fromIndex + srcArray.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int[] createArray(final int size) {
            return new int[size];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IntType createLinkedType(final PlanarImg img) {
            return new IntType(img);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setType(final int[] dataArray, final double val, final int index) {
            dataArray[index] = (int)val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IntArray wrap(final int[] dataArray) {
            return new IntArray(dataArray);
        }
    }

    private class LongTypeHandler implements RealTypeHandler<LongType, long[], LongArray> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int copyData(final long[] srcArray, final long[] resArray, final int fromIndex) {
            System.arraycopy(srcArray, 0, resArray, fromIndex, srcArray.length);
            return fromIndex + srcArray.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long[] createArray(final int size) {
            return new long[size];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LongType createLinkedType(final PlanarImg img) {
            return new LongType(img);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setType(final long[] dataArray, final double val, final int index) {
            dataArray[index] = (long)val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LongArray wrap(final long[] dataArray) {
            return new LongArray(dataArray);
        }
    }

    private interface RealTypeHandler<T, A, ADA> {
        /**
         * @param srcArray
         * @param resArray
         * @param fromIndex
         * @return the new index
         */
        int copyData(A srcArray, A resArray, int fromIndex);

        A createArray(int size);

        T createLinkedType(PlanarImg img);

        void setType(A dataArray, double val, int index);

        ADA wrap(A dataArray);

    }

    private class ShortTypeHandler implements RealTypeHandler<ShortType, short[], ShortArray> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int copyData(final short[] srcArray, final short[] resArray, final int fromIndex) {
            System.arraycopy(srcArray, 0, resArray, fromIndex, srcArray.length);
            return fromIndex + srcArray.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public short[] createArray(final int size) {
            return new short[size];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ShortType createLinkedType(final PlanarImg img) {
            return new ShortType(img);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setType(final short[] dataArray, final double val, final int index) {
            dataArray[index] = (short)val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ShortArray wrap(final short[] dataArray) {
            return new ShortArray(dataArray);
        }
    }

    private class UnsignedByteTypeHandler implements RealTypeHandler<UnsignedByteType, byte[], ByteArray> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int copyData(final byte[] srcArray, final byte[] resArray, final int fromIndex) {
            System.arraycopy(srcArray, 0, resArray, fromIndex, srcArray.length);
            return fromIndex + srcArray.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] createArray(final int size) {
            return new byte[size];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public UnsignedByteType createLinkedType(final PlanarImg img) {
            return new UnsignedByteType(img);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setType(final byte[] dataArray, final double val, final int index) {
            dataArray[index] = (byte)val;
        }

        @Override
        public ByteArray wrap(final byte[] dataArray) {
            return new ByteArray(dataArray);
        };

    }

    private class UnsignedShortTypeHandler implements RealTypeHandler<UnsignedShortType, short[], ShortArray> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int copyData(final short[] srcArray, final short[] resArray, final int fromIndex) {
            System.arraycopy(srcArray, 0, resArray, fromIndex, srcArray.length);
            return fromIndex + srcArray.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public short[] createArray(final int size) {
            return new short[size];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public UnsignedShortType createLinkedType(final PlanarImg img) {
            return new UnsignedShortType(img);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setType(final short[] dataArray, final double val, final int index) {
            dataArray[index] = (short)val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ShortArray wrap(final short[] dataArray) {
            return new ShortArray(dataArray);
        }
    }

    private static SettingsModelString createAxisLabelModel() {
        return new SettingsModelString("axis_label", "UNKNOWN");
    }

    private String m_axisLabel;

    /* aggregated pixel data */
    private ArrayList<A> m_data = null;

    // dialog components
    private DialogComponentString m_dcAxisLabel;

    private long[] m_dims;

    private ImgPlusMetadata m_metadata;

    // settings models
    private final SettingsModelString m_smAxisLabel = createAxisLabelModel();

    private T m_type;

    private RealTypeHandler<T, A, ADA> m_typeHandler;

    public ImgMergeOperator() {
        super("Merge Image", "Merge Image", "Merge Image");
    }

    public ImgMergeOperator(final GlobalSettings globalSettings) {
        this(globalSettings, null);
    }

    public ImgMergeOperator(final GlobalSettings globalSettings, final String axisLabel) {
        super("Merge Image", "Merge Image", globalSettings);
        if (axisLabel != null) {
            m_smAxisLabel.setStringValue(axisLabel);
        }
        m_axisLabel = m_smAxisLabel.getStringValue();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataRow row, final DataCell cell) {
        final ImgPlus<T> img = ((ImgPlusValue<T>)cell).getImgPlus();

        if ((m_type != null) && !img.firstElement().getClass().isAssignableFrom(m_type.getClass())) {
            throw new IllegalArgumentException("Image " + img.getName()
                    + " not compatible with first-row image. Different type!");
        }

        if (m_dims != null) {
            for (int i = 0; i < 2; i++) {
                if (img.dimension(i) != m_dims[i]) {
                    throw new IllegalArgumentException("Image " + img.getName()
                            + " not compatible with first-row image. Different dimension!");
                }
            }

        }

        if (m_data == null) {
            m_type = img.firstElement().createVariable();
            if (m_type instanceof ByteType) {
                m_typeHandler = (RealTypeHandler<T, A, ADA>)new ByteTypeHandler();
            } else if (m_type instanceof UnsignedByteType) {
                m_typeHandler = (RealTypeHandler<T, A, ADA>)new UnsignedByteTypeHandler();
            } else if (m_type instanceof UnsignedShortType) {
                m_typeHandler = (RealTypeHandler<T, A, ADA>)new UnsignedShortTypeHandler();
            } else if (m_type instanceof ShortType) {
                m_typeHandler = (RealTypeHandler<T, A, ADA>)new ShortTypeHandler();
            } else if (m_type instanceof IntType) {
                m_typeHandler = (RealTypeHandler<T, A, ADA>)new IntTypeHandler();
            } else if (m_type instanceof LongType) {
                m_typeHandler = (RealTypeHandler<T, A, ADA>)new LongTypeHandler();
            } else if (m_type instanceof FloatType) {
                m_typeHandler = (RealTypeHandler<T, A, ADA>)new FloatTypeHandler();
            } else if (m_type instanceof DoubleType) {
                m_typeHandler = (RealTypeHandler<T, A, ADA>)new DoubleTypeHandler();
            } else {
                throw new IllegalArgumentException("Pixel type " + m_type.getClass().getSimpleName()
                        + " not supported for merging.");
            }
            m_data = new ArrayList<A>();

            final CalibratedAxis[] axes = new CalibratedAxis[img.numDimensions()];
            img.axes(axes);
            final CalibratedAxis[] newAxes = new CalibratedAxis[3];
            for (int i = 0; i < 2; i++) {
                newAxes[i] = axes[i];
            }
            newAxes[2] = new DefaultCalibratedAxis(Axes.get(m_axisLabel));
            m_metadata = new DefaultImgMetadata(new DefaultCalibratedSpace(newAxes), img, img, img);
            m_dims = new long[3];
            m_dims[0] = img.dimension(0);
            m_dims[1] = img.dimension(1);

        }

        final int planeSize = (int)(img.dimension(0) * img.dimension(1));
        final int numPlanes = (int)(img.size() / planeSize);
        m_dims[2] += numPlanes;

        // copy data
        if (img.getImg() instanceof ArrayImg) {
            for (int i = 0; i < numPlanes; i++) {
                final A plane = m_typeHandler.createArray(planeSize);
                m_typeHandler.copyData((A)((ArrayDataAccess<A>)((ArrayImg)img.getImg()).update(null))
                        .getCurrentStorageArray(), plane, 0);
                m_data.add(plane);
            }

        } else if (img.getImg() instanceof PlanarImg) {

            for (int i = 0; i < ((PlanarImg)img.getImg()).numSlices(); i++) {
                final A plane = m_typeHandler.createArray(planeSize);
                m_typeHandler.copyData((A)((ArrayDataAccess<A>)((PlanarImg)img.getImg()).getPlane(i))
                        .getCurrentStorageArray(), plane, 0);
                m_data.add(plane);
            }

        } else {
            if (img.numDimensions() != (m_dims.length - 1)) {
                throw new IllegalArgumentException("Image type not supported, yet.");
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        if ((m_smAxisLabel.getStringValue().length() > 1) && (spec.findColumnIndex(m_smAxisLabel.getStringValue()) < 0)) {
            throw new InvalidSettingsException("Cannot find interval column.");
        }
    }

    private void createDCs() {
        if (m_dcAxisLabel == null) {
            m_dcAxisLabel = new DialogComponentString(createAxisLabelModel(), "New axis label");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
                                              final OperatorColumnSettings opColSettings) {
        return new ImgMergeOperator(globalSettings, m_smAxisLabel.getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return ImgPlusCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Merges the n-dimension to one (n+1) dimensional image object. The images to be merged must have the same X and Y dimensions and same pixel type as the first image in the group. If not, they will be skipped.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        final ArrayList<ADA> mirror = new ArrayList<ADA>(m_data.size());
        for (int i = 0; i < m_data.size(); i++) {
            mirror.add(m_typeHandler.wrap(m_data.get(i)));
        }
        final CustomPlanarImg img = new CustomPlanarImg(mirror, m_dims, 1);
        img.setLinkedType(m_typeHandler.createLinkedType(img));
        try {
            return getImgPlusCellFactory().createCell(img, m_metadata);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Component getSettingsPanel() {
        createDCs();
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(m_dcAxisLabel.getComponentPanel());
        return panel;
    }

    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        createDCs();
        m_dcAxisLabel.loadSettingsFrom(settings, new DataTableSpec[]{spec});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smAxisLabel.loadSettingsFrom(settings);
        if (m_smAxisLabel.getStringValue() == null) {
            m_smAxisLabel.setStringValue("UNKNOWN");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_data = null;

    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_dcAxisLabel != null) {
            try {
                m_dcAxisLabel.saveSettingsTo(settings);
            } catch (final InvalidSettingsException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            m_smAxisLabel.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smAxisLabel.validateSettings(settings);
    }

}
