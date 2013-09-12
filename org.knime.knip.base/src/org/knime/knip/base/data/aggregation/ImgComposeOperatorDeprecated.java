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
package org.knime.knip.base.data.aggregation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.DefaultNamed;
import net.imglib2.meta.DefaultSourced;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.data.img.DefaultImageMetadata;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.types.NativeTypes;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgComposeOperatorDeprecated<T1 extends RealType<T1>, T2 extends RealType<T2> & NativeType<T2>> extends
        ImgAggregrationOperation {

    public static final String GLOBAL_SETTINGS_KEY_INTERVAL_COLUMN = "global_settings_key_interval_column";

    public static final String GLOBAL_SETTINGS_KEY_OUTPUT_TYPE = "global_settings_key_output_type";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgComposeOperatorDeprecated.class);

    // the default settings, i.e. if the respective globalsettings are not
    // set
    private String m_intervalCol = null;

    private long[] m_maxDims = null;

    // fields needed, if the result interval is not known in advance, i.e.
    // if the default settings are used
    private List<ImgPlusValue<T1>> m_patchList = null;

    private RandomAccess<T2> m_resAccess = null;

    // field for the labeling generation
    private Img<T2> m_resultImg = null;

    private ImgPlusMetadata m_resultMetadata = null;

    private T2 m_resultType = null;

    public ImgComposeOperatorDeprecated() {
        super("Compose Image", "Compose Image (deprecated)", "Compose Image");
    }

    /**
     * @param label
     * @param globalSettings
     */
    @SuppressWarnings("unchecked")
    public ImgComposeOperatorDeprecated(final GlobalSettings globalSettings) {
        super("Compose Image", "Compose Image (deprecated)", globalSettings);
        if (globalSettings.getValue(GLOBAL_SETTINGS_KEY_INTERVAL_COLUMN) != null) {
            m_intervalCol = (String)globalSettings.getValue(GLOBAL_SETTINGS_KEY_INTERVAL_COLUMN);
        } else {
            m_patchList = new ArrayList<ImgPlusValue<T1>>();
            LOGGER.warn("Creating a composed images with default settings may be less efficient and may not meet the requirement. Use the \"Image GroupBy\" node instead and configure it accordingly.");
        }

        if (globalSettings.getValue(GLOBAL_SETTINGS_KEY_OUTPUT_TYPE) != null) {
            final String outputType = (String)globalSettings.getValue(GLOBAL_SETTINGS_KEY_OUTPUT_TYPE);
            m_resultType = (T2)NativeTypes.getTypeInstance(NativeTypes.valueOf(outputType));
        }
        if (m_resultType == null) {
            m_resultType = (T2)new FloatType();
            LOGGER.warn("By default the result image is of type float. If you want to choose a different type use the \"Image GroupBy\" node instead.");

        }

    }

    /*
     * adds the bitmask to the labeling and return true, if it was
     * successfull
     */
    private boolean addToImage(final ImgPlusValue<T1> val) {
        final Img<T1> patch = val.getImgPlus();
        final long[] min = val.getMinimum();

        if (patch.numDimensions() != m_resultImg.numDimensions()) {
            return false;
        }

        // Set segmented pixels to label
        final Cursor<T1> patchCursor = patch.localizingCursor();
        final T1 minVal = patch.firstElement().createVariable();
        minVal.setReal(minVal.getMinValue());
        while (patchCursor.hasNext()) {
            patchCursor.fwd();
            if (patchCursor.get().compareTo(minVal) == 0) {
                continue;
            }
            final double curr = patchCursor.get().getRealDouble();

            for (int d = 0; d < Math.min(patchCursor.numDimensions(), m_resAccess.numDimensions()); d++) {
                m_resAccess.setPosition(min[d] + patchCursor.getLongPosition(d), d);
            }

            m_resAccess.get().setReal(curr);
        }
        return true;

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
    @SuppressWarnings("unchecked")
    @Override
    protected boolean computeInternal(final DataRow row, final DataCell cell) {

        final int intervalColIdx = getGlobalSettings().findColumnIndex(m_intervalCol);

        // if no column index is there, collect all tiles and put them
        // together to an image afterwards, if not, the result image can
        // be created in advance
        if (intervalColIdx == -1) {
            final ImgPlusValue<T1> imgVal = (ImgPlusValue<T1>)cell;
            final long[] dims = imgVal.getDimensions();
            if (m_maxDims == null) {
                m_maxDims = new long[dims.length];
            }
            if (m_maxDims.length != dims.length) {
                LOGGER.warn("Patch in row " + row.getKey() + " cannot be added to the result.");
                return true;
            } else {
                final long[] min = imgVal.getMinimum();
                for (int i = 0; i < m_maxDims.length; i++) {
                    m_maxDims[i] = Math.max(m_maxDims[i], min[i] + dims[i]);
                }

                m_patchList.add(imgVal);
            }

        } else {
            if (m_resultImg == null) {
                final IntervalValue iv = (IntervalValue)row.getCell(intervalColIdx);
                // create labeling and metadata
                // todo make the generation of the result
                // labeling configurable
                final Interval i = new FinalInterval(iv.getMinimum(), iv.getMaximum());
                m_resultImg = new ArrayImgFactory<T2>().create(i, m_resultType);
                m_resAccess = Views.extendValue(m_resultImg, m_resultType).randomAccess();
                m_resultMetadata =
                        new DefaultImgMetadata(iv.getCalibratedSpace(), iv.getName(), iv.getSource(),
                                new DefaultImageMetadata());

            }

            if (!addToImage((ImgPlusValue<T1>)cell)) {
                setSkipMessage("Patch in row " + row.getKey() + " cannot be added to the result.");
                return true;
            }

        }
        return false;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
                                              final OperatorColumnSettings opColSettings) {
        return new ImgComposeOperatorDeprecated<T1, T2>(globalSettings);
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
        return "Composes images from image patches.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_intervalCol == null) {
            // compose result and return
            m_resultImg = new ArrayImgFactory<T2>().create(m_maxDims, m_resultType);
            m_resultMetadata =
                    new DefaultImgMetadata(new DefaultCalibratedSpace(m_maxDims.length), new DefaultNamed("Unknown"),
                            new DefaultSourced("Unknown"), new DefaultImageMetadata());
            m_resAccess = m_resultImg.randomAccess();
            for (final ImgPlusValue<T1> imgVal : m_patchList) {
                addToImage(imgVal);
            }
        }

        // return the already composed result
        try {
            return getImgPlusCellFactory().createCell(m_resultImg, m_resultMetadata);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        // labeling creation fields
        m_resultImg = null;
        m_resultMetadata = null;
        m_resAccess = null;
        m_resultType = null;

        // "lazy" labeling creation
        if (m_patchList != null) {
            m_patchList.clear();
        }
        m_maxDims = null;

        // default settings
        m_intervalCol = null;

    }
}
