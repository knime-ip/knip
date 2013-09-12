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
package org.knime.knip.base.nodes.proc.resampler;

import java.util.HashMap;
import java.util.Map;

import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SettingsModelScalingValues extends SettingsModel {

    /*
     *
     */
    private static String CFG_NEWDIMENSION = "new_dimensions";

    private static String CFG_NUM_DIMS = "num_dimensions";

    private static String CFG_SELECTED_DIM_LABEL = "selected_dim_label";

    /*
     *
     */
    private final String m_configName;

    /*
     *
     */
    private final Map<String, Double> m_newDimensions;

    /**
     * Creates a new plane selection where all planes of each dimension are selected.
     * 
     * @param configName
     * @param dimLables
     */
    public SettingsModelScalingValues(final String configName) {
        m_configName = configName;
        m_newDimensions = new HashMap<String, Double>(5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final SettingsModelScalingValues createClone() {
        final SettingsModelScalingValues clone = new SettingsModelScalingValues(m_configName);
        clone.m_newDimensions.putAll(m_newDimensions);
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final String getModelTypeID() {
        return "SMID_scalingValues";
    }

    /**
     * 
     * @param interval an array of the scaling values according to the ImgPlus axes
     * @return
     */
    public final double[] getNewDimensions(final int numDims, final CalibratedSpace<CalibratedAxis> typedSpace) {
        final double[] res = new double[numDims];
        final CalibratedAxis[] axes = new CalibratedAxis[numDims];
        typedSpace.axes(axes);
        for (int i = 0; i < res.length; i++) {
            res[i] = getNewDimensions(axes[i].type().getLabel());
        }
        return res;

    }

    /**
     * 
     * @return
     */
    public final double getNewDimensions(final String dimLabel) {
        final Double res = m_newDimensions.get(dimLabel);
        if (res == null) {
            return 1;
        } else {
            return res;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        try {
            m_newDimensions.clear();
            final Config lists = settings.getConfig(m_configName);
            final int size = lists.getInt(CFG_NUM_DIMS);
            for (int i = 0; i < size; i++) {
                final String key = lists.getString(CFG_SELECTED_DIM_LABEL + i);
                m_newDimensions.put(key, lists.getDouble(CFG_NEWDIMENSION + key));
            }
            // use the current value, if no value is stored in the
            // settings

        } catch (final IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        } catch (final InvalidSettingsException e) {
            // keep the old value
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            final Config lists = settings.getConfig(m_configName);
            m_newDimensions.clear();
            final int size = lists.getInt(CFG_NUM_DIMS);
            for (int i = 0; i < size; i++) {
                final String key = lists.getString(CFG_SELECTED_DIM_LABEL + i);
                m_newDimensions.put(key, lists.getDouble(CFG_NEWDIMENSION + key));
            }

            // use the current value, if no value is stored in the
            // settings

        } catch (final IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        } catch (final InvalidSettingsException e) {
            // keep the old value
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void saveSettingsForModel(final NodeSettingsWO settings) {

        // the subconfig
        final Config lists = settings.addConfig(m_configName);
        lists.addInt(CFG_NUM_DIMS, m_newDimensions.size());
        int i = 0;
        for (final String key : m_newDimensions.keySet()) {
            lists.addString(CFG_SELECTED_DIM_LABEL + i++, key);
            lists.addDouble(CFG_NEWDIMENSION + key, m_newDimensions.get(key));
        }
    }

    /**
     * @param selectedDims
     */
    public final void setScalingValue(final String dimLabel, final double newDimSize) {
        m_newDimensions.put(dimLabel, newDimSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config lists = settings.getConfig(m_configName);
        final int numDims = lists.getInt(CFG_NUM_DIMS);
        for (int i = 0; i < numDims; i++) {
            final String key = lists.getString(CFG_SELECTED_DIM_LABEL + i);
            lists.getDouble(CFG_NEWDIMENSION + key);
        }
    }

}
