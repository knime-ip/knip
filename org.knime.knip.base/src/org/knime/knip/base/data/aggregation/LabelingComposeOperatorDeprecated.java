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
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.DefaultNamed;
import net.imglib2.meta.DefaultSourced;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.IntegerLabelGenerator;
import org.knime.knip.core.data.LabelGenerator;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingComposeOperatorDeprecated<T extends IntegerType<T> & NativeType<T>> extends
        LabelingAggregrationOperation {

    public static final String GLOBAL_SETTINGS_KEY_INTERVAL_COLUMN = "global_settings_key_interval_column";

    public static final String GLOBAL_SETTINGS_KEY_RES_FACTORY = "global_settings_key_res_factory";

    public static final String GLOBAL_SETTINGS_KEY_RES_TYPE = "global_settings_key_res_type";

    public static final String GLOBAL_SETTINGS_KEY_USE_IMG_NAME_AS_LABEL = "global_settings_key_use_img_name_as_label";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LabelingComposeOperatorDeprecated.class);

    // fields needed, if the result interval is not known in advance, i.e.
    // if the default settings are used
    private List<ImgPlusValue<BitType>> m_bitMaskList = null;

    private ImgFactory<T> m_factory = null;

    // the default settings, i.e. if the respective globalsettings are not
    // set
    private String m_intervalCol = null;

    private final LabelGenerator<Integer> m_labelGenerator = new IntegerLabelGenerator();

    private long[] m_maxDims = null;

    private RandomAccess<LabelingType<String>> m_resAccess = null;

    private T m_resType;

    // field for the labeling generation
    private Labeling<String> m_resultLab = null;

    private LabelingMetadata m_resultMetadata = null;

    private boolean m_useImgNameAsLabel = false;

    public LabelingComposeOperatorDeprecated() {
        super("Compose Labeling", "Compose Labeling (deprecated)", "Compose Labeling");
    }

    /**
     * @param label
     * @param globalSettings
     */
    @SuppressWarnings("unchecked")
    public LabelingComposeOperatorDeprecated(final GlobalSettings globalSettings) {
        super("Compose Labeling", "Compose Labeling", globalSettings);
        if (globalSettings.getValue(GLOBAL_SETTINGS_KEY_INTERVAL_COLUMN) != null) {
            m_intervalCol = (String)globalSettings.getValue(GLOBAL_SETTINGS_KEY_INTERVAL_COLUMN);
        } else {
            m_bitMaskList = new ArrayList<ImgPlusValue<BitType>>();
            LOGGER.warn("Creating a composed labeling with default settings may be less efficient and may not meet the requirement. Use the \"Image GroupBy\" node instead and configure it accordingly.");
        }

        if (globalSettings.getValue(GLOBAL_SETTINGS_KEY_USE_IMG_NAME_AS_LABEL) != null) {
            m_useImgNameAsLabel = (Boolean)globalSettings.getValue(GLOBAL_SETTINGS_KEY_USE_IMG_NAME_AS_LABEL);
        }

        if (globalSettings.getValue(GLOBAL_SETTINGS_KEY_RES_FACTORY) != null) {
            final String outfactory = (String)globalSettings.getValue(GLOBAL_SETTINGS_KEY_RES_FACTORY);
            m_factory = ImgFactoryTypes.getImgFactory(outfactory, null);
        } else {
            m_factory = new ArrayImgFactory<T>();
        }

        if (globalSettings.getValue(GLOBAL_SETTINGS_KEY_RES_TYPE) != null) {
            final String outtype = (String)globalSettings.getValue(GLOBAL_SETTINGS_KEY_RES_TYPE);
            m_resType = (T)NativeTypes.getTypeInstance(NativeTypes.valueOf(outtype));
        } else {
            m_resType = (T)new IntType();
        }

    }

    /*
     * adds the bitmask to the labeling and return true, if it was
     * successfull
     */
    private boolean addToLabeling(final ImgPlusValue<BitType> val) {
        final Img<BitType> img = val.getImgPlus();
        final long[] min = val.getMinimum();

        if (img.numDimensions() != m_resultLab.numDimensions()) {
            return false;
        }
        String label;
        if (m_useImgNameAsLabel) {
            final String name = val.getMetadata().getName();
            String source = val.getMetadata().getSource();
            if ((name.length() != 0) && (source.length() != 0)) {
                source += KNIPConstants.IMGID_LABEL_DELIMITER;
            }

            label = source + name;
        } else {
            label = "" + m_labelGenerator.nextLabel();
        }

        // Set segmented pixels to label
        final Cursor<BitType> cur = img.localizingCursor();
        while (cur.hasNext()) {
            cur.fwd();
            if (!cur.get().get()) {
                continue;
            }
            for (int d = 0; d < min.length; d++) {
                if (img.numDimensions() > d) {
                    m_resAccess.setPosition(Math.min(m_resultLab.dimension(d),
                                                     Math.max(0, min[d] + cur.getLongPosition(d))), d);
                } else {
                    m_resAccess.setPosition(Math.min(m_resultLab.dimension(d), Math.max(0, min[d])), d);
                }

            }

            if (m_resAccess.get().getLabeling().isEmpty()) {
                m_resAccess.get().setLabel(label);
            } else {
                final ArrayList<String> tmp = new ArrayList<String>(m_resAccess.get().getLabeling());
                tmp.add(label);
                m_resAccess.get().setLabeling(tmp);
            }

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
    @Override
    protected boolean computeInternal(final DataRow row, final DataCell cell) {

        // check correct pixel type
        if (!(((ImgPlusValue)cell).getImgPlus().firstElement() instanceof BitType)) {
            setSkipMessage("The Bitmask in row " + row.getKey() + " is not of pixel type BitType!");
            return true;
        }

        final int intervalColIdx = getGlobalSettings().findColumnIndex(m_intervalCol);

        // if no column index is there, collect all tiles and put them
        // together to an image afterwards, if not, the result image can
        // be created in advance
        if (intervalColIdx == -1) {
            final ImgPlusValue<BitType> imgVal = (ImgPlusValue<BitType>)cell;
            final long[] dims = imgVal.getDimensions();
            if (m_maxDims == null) {
                m_maxDims = new long[dims.length];
            }
            if (m_maxDims.length != dims.length) {
                LOGGER.warn("Bitmask in row " + row.getKey() + " cannot be added to the result.");
                return true;
            } else {
                final long[] min = imgVal.getMinimum();
                for (int i = 0; i < m_maxDims.length; i++) {
                    m_maxDims[i] = Math.max(m_maxDims[i], min[i] + dims[i]);
                }

                m_bitMaskList.add(imgVal);
            }

        } else {
            if (m_resultLab == null) {
                final IntervalValue iv = (IntervalValue)row.getCell(intervalColIdx);
                // create labeling and metadata
                // todo make the generation of the result
                // labeling configurable
                final Interval i = new FinalInterval(iv.getMinimum(), iv.getMaximum());
                m_resultLab = new NativeImgLabeling<String, T>(m_factory.create(i, m_resType));
                m_resAccess = Views.extendValue(m_resultLab, new LabelingType<String>("")).randomAccess();
                m_resultMetadata =
                        new DefaultLabelingMetadata(iv.getCalibratedSpace(), iv.getName(), iv.getSource(),
                                new DefaultLabelingColorTable());

            }

            final boolean success = addToLabeling((ImgPlusValue<BitType>)cell);
            if (!success) {
                setSkipMessage("Bitmask in row " + row.getKey() + " cannot be added to the result.");
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
        return new LabelingComposeOperatorDeprecated(globalSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return LabelingCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Composes labelings from binary images.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_intervalCol == null) {
            // compose result and return
            m_resultLab = new NativeImgLabeling<String, T>(m_factory.create(m_maxDims, m_resType));
            m_resultMetadata =
                    new DefaultLabelingMetadata(new DefaultCalibratedSpace(m_maxDims.length), new DefaultNamed(
                            "Unknown"), new DefaultSourced("Unknown"), new DefaultLabelingColorTable());
            m_resAccess = m_resultLab.randomAccess();
            m_labelGenerator.reset();
            for (final ImgPlusValue<BitType> imgVal : m_bitMaskList) {
                addToLabeling(imgVal);
            }
        }

        // return the already composed result
        try {
            return getLabelingCellFactory().createCell(m_resultLab, m_resultMetadata);
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
        m_resultLab = null;
        m_resultMetadata = null;
        m_resAccess = null;

        // "lazy" labeling creation
        if (m_bitMaskList != null) {
            m_bitMaskList.clear();
        }
        m_maxDims = null;

        // default settings
        m_intervalCol = null;
        m_useImgNameAsLabel = false;
        m_labelGenerator.reset();

    }
}
