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
package org.knime.knip.base.node.nodesettings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.imglib2.Interval;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.TypedAxis;
import net.imglib2.meta.TypedSpace;
import net.imglib2.ops.operation.interval.binary.IntervalsFromDimSelection;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.KNIMEKNIPPlugin;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SettingsModelDimSelection extends SettingsModel {
    private final String m_configName;

    private Set<String> m_selectedDims;

    /**
     * Creates a new plane selection where all planes of each dimension are selected.
     * 
     * @param configName
     * @param dimLables
     */
    public SettingsModelDimSelection(final String configName) {
        this(configName, KNIMEKNIPPlugin.parseDimensionLabels());
    }

    /**
     * @param configName
     * @param defaultSelectedDims
     */
    public SettingsModelDimSelection(final String configName, final AxisType... defaultSelectedDims) {
        m_configName = configName;
        m_selectedDims = new HashSet<String>(5);
        for (final AxisType s : defaultSelectedDims) {
            m_selectedDims.add(s.getLabel());
        }
    }

    /**
     * @param configName
     * @param defaultSelectedDims
     */
    public SettingsModelDimSelection(final String configName, final String... defaultSelectedDims) {
        m_configName = configName;
        m_selectedDims = new HashSet<String>(5);
        for (final String s : defaultSelectedDims) {
            m_selectedDims.add(s);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final SettingsModelDimSelection createClone() {
        final SettingsModelDimSelection model = new SettingsModelDimSelection(m_configName);
        model.m_selectedDims.clear();
        model.m_selectedDims.addAll(m_selectedDims);
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final String getConfigName() {
        return m_configName;
    }

    public final Interval[] getIntervals(final TypedSpace<? extends TypedAxis> axes, final Interval i) {
        return IntervalsFromDimSelection.compute(getSelectedDimIndices(i.numDimensions(), axes), i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final String getModelTypeID() {
        return "SMID_dimselection";
    }

    /**
     * @param space
     * @return the number of dim labels selected in the dim selection component and available in the calibrated space
     */
    public final int getNumSelectedDimLabels(final TypedSpace<? extends TypedAxis> space) {
        return getSelectedDimIndices(space).length;
    }

    /**
     * @return the number of dim labels selected in the dim selection component
     */
    public final int getNumSelectedDimLabels() {
        return m_selectedDims.size();
    }

    /**
     * Filter the selected dim indices according to axis labels of the given {@link ImgPlus} object.
     * 
     * @return
     */
    public final int[] getSelectedDimIndices(final TypedSpace<? extends TypedAxis> space) {
        return getSelectedDimIndices(space.numDimensions(), space);
    }

    /**
     * Filter the selected dim indices according to axis labels.
     * 
     * @param imgPlus
     * 
     * @return
     */
    public final int[] getSelectedDimIndices(final int numDims, final TypedAxis[] dimLabels) {
        final int[] res = new int[numDims];
        int newIdx = 0;
        for (int d = 0; d < res.length; d++) {
            if (m_selectedDims.contains(dimLabels[d].type().getLabel())) {
                res[newIdx++] = d;
            }
        }

        return Arrays.copyOfRange(res, 0, newIdx);
    }

    /**
     * Filter the selected dim indices according to axis labels of the given {@link ImgPlus} object.
     * 
     * @param imgPlus
     * 
     * @return
     */
    public final int[] getSelectedDimIndices(final int numDims, final TypedSpace<? extends TypedAxis> space) {
        final TypedAxis[] axes = new TypedAxis[numDims];

        // local dims //axes labels
        for (int d = 0; d < space.numDimensions(); d++) {
            axes[d] = space.axis(d);
        }

        return getSelectedDimIndices(numDims, axes);
    }

    /**
     * @return
     */
    public final Set<String> getSelectedDimLabels() {
        return m_selectedDims;
    }

    /**
     * @param dimLabels
     * @return true if all selected dimensions are part of the submitted parameter
     */
    public final boolean isContainedIn(final TypedAxis[] dimLabels) {
        final Set<String> labels = new HashSet<String>();
        for (final TypedAxis axis : dimLabels) {
            labels.add(axis.type().getLabel());
        }

        for (final String dimName : m_selectedDims) {
            if (!labels.contains(dimName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param space
     * @return true if all selected dimensions are part of the submitted parameter
     */
    public final boolean isContainedIn(final TypedSpace<? extends TypedAxis> space) {
        final TypedAxis[] axes = new TypedAxis[space.numDimensions()];

        // local dims //axes labels
        for (int d = 0; d < space.numDimensions(); d++) {
            axes[d] = space.axis(d);
        }

        return isContainedIn(axes);
    }

    /**
     * Determines if the given dimension is selected.
     * 
     * @param dim
     * @return
     */
    public boolean isSelectedDim(final String dimLabel) {
        return m_selectedDims.contains(dimLabel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            // use the current value, if no value is stored in the
            // settings
            final Set<String> selectedDims = new HashSet<String>(Arrays.asList(settings.getStringArray(m_configName)));
            setDimSelectionValue(selectedDims);
        } catch (final IllegalArgumentException iae) {
            // if the argument is not accepted: keep the old value.
        } catch (final InvalidSettingsException e) {
            // invalid settings
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            // no default value, throw an exception instead
            final Set<String> selectedDims = new HashSet<String>(Arrays.asList(settings.getStringArray(m_configName)));
            setDimSelectionValue(selectedDims);
        } catch (final IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
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
        settings.addStringArray(m_configName, m_selectedDims.toArray(new String[m_selectedDims.size()]));
    }

    /**
     * set the value stored to the new value.
     * 
     * @param newValue the new value to store.
     */
    public void setDimSelectionValue(final Set<String> newValue) {
        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_selectedDims == null);
        } else {
            sameValue = newValue.equals(m_selectedDims);
        }
        m_selectedDims = newValue;
        if (!sameValue) {
            notifyChangeListeners();
        }
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
        settings.getStringArray(m_configName);
    }

}
