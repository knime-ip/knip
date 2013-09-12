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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Pair;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.TypedAxis;
import net.imglib2.util.ValuePair;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.exceptions.KNIPException;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SettingsModelSubsetSelection extends SettingsModel {

    private static final String CFG_INCLMODE = "includemode_";

    /*
     * keys to store selection
     */
    private static final String CFG_NUM_SELECTED_DIMS = "num_selected_dims";

    private static final String CFG_SELECTED_DIMLABEL = "selected_dimlabel_";

    private static final String CFG_SELECTION = "selection_";

    /**
     *
     */
    private final String m_configName;

    /**
     *
     */
    private Map<String, Boolean> m_isIncMode;

    /**
     * holds the selection, if selection.get(<String>).length==0 or selection.get(<String>)==null-> all coordinates in
     * this dimension i are selected
     */
    private Map<String, int[]> m_selection;

    /**
     * Creates a new plane selection where all planes of each dimension are selected.
     * 
     * @param configName
     */
    public SettingsModelSubsetSelection(final String configName) {
        m_configName = configName;
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final SettingsModelSubsetSelection createClone() {
        final SettingsModelSubsetSelection sm = new SettingsModelSubsetSelection(m_configName);
        sm.m_selection = new HashMap<String, int[]>(m_selection.size());
        sm.m_selection.putAll(m_selection);
        sm.m_isIncMode = new HashMap<String, Boolean>(m_selection.size());
        sm.m_isIncMode.putAll(m_isIncMode);
        return sm;
    }

    /**
     * Creates the selected intervals according to the given image and its metadata.
     * 
     * @param dimensions
     * 
     * @return
     * @throws KNIPException
     */
    @SuppressWarnings("unchecked")
    public final Interval[] createSelectedIntervals(final long[] dimensions, final CalibratedAxis[] axes)
            throws KNIPException {

        List<Long>[] minPosList;
        List<Long>[] maxPosList;

        final boolean[] selectionInformation = getSelectionInformation(dimensions, axes);
        /*
         * get the "connected" intervals for each dimension and
         * subsequently return each possible dimension-wise combination
         * of the intervals
         */
        minPosList = new List[dimensions.length];
        maxPosList = new List[dimensions.length];
        final long[] numIntervalsPerDim = new long[dimensions.length];
        for (int d = 0; d < dimensions.length; d++) {
            minPosList[d] = new ArrayList<Long>(dimensions.length);
            maxPosList[d] = new ArrayList<Long>(dimensions.length);

            if (selectionInformation[d]) {
                minPosList[d].add(0l);
                maxPosList[d].add(dimensions[d] - 1);
                numIntervalsPerDim[d] = 1;
            } else {
                final int[] selection = m_selection.get(axes[d].type().getLabel());
                if (getIncMode(axes[d].type().getLabel())) {
                    // include mode
                    minPosList[d].add((long)selection[0]);
                    long max = selection[0];
                    for (int i = 1; i < Math.min(selection.length, dimensions[d]); i++) {
                        if (selection[i] < dimensions[d]) {
                            if ((selection[i] - 1) != selection[i - 1]) {
                                maxPosList[d].add(max);
                                minPosList[d].add((long)selection[i]);
                                max = selection[i];
                            } else {
                                max++;
                            }
                        } else {
                            break;
                        }
                    }
                    maxPosList[d].add(max);
                    numIntervalsPerDim[d] = minPosList[d].size();
                } else {

                    int excludeCtr = 0;

                    for (int i = 0; i < dimensions[d]; i++) {
                        if ((excludeCtr == selection.length) || (selection[excludeCtr] >= dimensions[d])) {
                            minPosList[d].add((long)i);
                            maxPosList[d].add(dimensions[d] - 1);
                            break;
                        }

                        if ((selection[excludeCtr] != i) && (minPosList[d].size() == maxPosList[d].size())) {
                            minPosList[d].add((long)i);
                        }

                        if (selection[excludeCtr] == i) {
                            excludeCtr++;
                            if (minPosList[d].size() != maxPosList[d].size()) {
                                maxPosList[d].add((long)i - 1);
                            }
                        }
                    }

                    numIntervalsPerDim[d] = minPosList[d].size();
                }
            }
        }

        final IntervalIterator intervalIt = new IntervalIterator(numIntervalsPerDim);
        int numInt = 1;
        for (int i = 0; i < numIntervalsPerDim.length; i++) {
            numInt *= numIntervalsPerDim[i];
        }

        final Interval[] res = new Interval[numInt];
        final int[] pos = new int[numIntervalsPerDim.length];
        final long[] minPos = new long[numIntervalsPerDim.length];
        final long[] maxPos = new long[numIntervalsPerDim.length];
        int i = 0;
        while (intervalIt.hasNext()) {
            intervalIt.fwd();
            intervalIt.localize(pos);
            for (int d = 0; d < numIntervalsPerDim.length; d++) {
                minPos[d] = minPosList[d].get(pos[d]);
                maxPos[d] = maxPosList[d].get(pos[d]);
            }

            res[i] = new FinalInterval(minPos, maxPos);
            i++;

        }

        return res;

    }

    /**
     * @param dimensions
     * @param axes
     * @return
     * @throws InvalidSettingsException
     */
    public final Interval[] createSelectedIntervals(final long[] dimensions,
                                                    final CalibratedSpace<CalibratedAxis> labeledAxes)
            throws KNIPException {
        final CalibratedAxis[] axes = new CalibratedAxis[dimensions.length];
        labeledAxes.axes(axes);
        return createSelectedIntervals(dimensions, axes);
    }

    public Interval[] createSelectedIntervalsPlaneWise(final long[] dimensions, final CalibratedAxis[] axes)
            throws KNIPException {

        final Interval[] selected = createSelectedIntervals(dimensions, axes);

        if (selected.length == 1) {
            boolean same = true;
            for (int d = 0; d < dimensions.length; d++) {
                if (selected[0].dimension(d) != dimensions[d]) {
                    same = false;
                    break;
                }
            }

            if (same) {
                return new Interval[0];
            }
        }
        final List<Interval> resIntervals = new ArrayList<Interval>();

        final long[] min = new long[dimensions.length];
        final long[] max = new long[dimensions.length];

        for (final Interval interval : selected) {
            interval.min(min);
            interval.max(max);
            min[0] = max[0];
            min[1] = max[1];

            final IntervalIterator iterator = IntervalIterator.create(new FinalInterval(min, max));

            while (iterator.hasNext()) {
                iterator.fwd();
                iterator.localize(min);

                resIntervals.add(new FinalInterval(min, min));
            }

        }

        return resIntervals.toArray(new Interval[resIntervals.size()]);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final String getConfigName() {
        return m_configName;
    }

    /**
     * @param dimLabel
     * @return
     */
    public final boolean getIncMode(final String dimLabel) {
        final Boolean res = m_isIncMode.get(dimLabel);
        return (res == null) || res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final String getModelTypeID() {
        return "SMID_imagesubsetselecction";
    }

    /**
     * @param dimLabel
     * @return
     */
    public final int[] getSelection(final String dimLabel) {
        return m_selection.get(dimLabel);
    }

    public final long[] getSelectionDimensions(final long[] srcDim, final CalibratedAxis[] axes)
            throws InvalidSettingsException {
        final long[] dimensions = new long[srcDim.length];

        for (int d = 0; d < dimensions.length; d++) {
            final String label = axes[d].type().getLabel();
            final int[] selection = m_selection.get(label);
            if ((selection == null) || (selection.length == 0)) {
                dimensions[d] = srcDim[d];
            } else {

                int i = selection.length - 1;

                while ((i > -1) && (selection[i] >= srcDim[d])) {
                    i--;
                }

                if (((i + 1) == 0) && m_isIncMode.get(label)) {
                    throw new InvalidSettingsException("Nothing selected at dimension " + label
                            + " select only 0 and the dimension will be cut off");
                }

                if (m_isIncMode.get(label)) {
                    dimensions[d] = i + 1;
                } else {
                    dimensions[d] = srcDim[d] - (i + 1);
                }
            }
        }
        return dimensions;
    }

    public final long[] getSelectionDimensions(final long[] srcDim, final CalibratedSpace<CalibratedAxis> labeledAxes)
            throws InvalidSettingsException {
        final CalibratedAxis[] axes = new CalibratedAxis[srcDim.length];
        labeledAxes.axes(axes);

        return getSelectionDimensions(srcDim, axes);
    }

    /**
     * Check if all dimensions are completely selected
     * 
     * @param dimSize
     * @param dim
     * @return
     * @throws KNIPException
     */
    private final boolean[] getSelectionInformation(final long[] dimSizes, final CalibratedAxis[] axes)
            throws KNIPException {
        final boolean[] completelySelected = new boolean[dimSizes.length];

        for (int d = 0; d < dimSizes.length; d++) {
            completelySelected[d] = isCompletelySelected(dimSizes[d], axes[d].type().getLabel());
        }

        return completelySelected;
    }

    private void init() {

        m_selection = new HashMap<String, int[]>(5);

        m_isIncMode = new HashMap<String, Boolean>(5);

    }

    /**
     * @return true, if all selectable dimensions are completely selected, else false
     */
    public boolean isCompletelySelected() {
        return m_selection.size() == 0;
    }

    /**
     * Check if a dimension is completely selected & should be included
     * 
     * 
     * @param dimSize
     * @param dim
     * @return
     * @throws KNIPException
     */
    private final boolean isCompletelySelected(final long dimSize, final String dimLabel) throws KNIPException {

        // a null selection means, that all is selected! Anything else is
        // catched in the dialog. Also this implies selection mode (not excluding)
        final int[] selection = m_selection.get(dimLabel);

        final boolean isIncMode = getIncMode(dimLabel);

        if (isIncMode) {
            if (selection == null) {
                // inc mode and everything selected per definition
                return true;
            }

            if (dimSize <= selection[0]) {
                //selection not in range = nothing selected in inc mode
                throw new KNIPException("Selection at dim " + dimLabel + " does not cover image content.",
                        new InvalidSettingsException("image reduced to nothing."));
            }

            if (selection.length < dimSize) {
                //something should be included but not everything
                return false;
            }

            //test if some value is not included
            for (int i = 0; i < dimSize; i++) {
                if (selection[i] != i) {
                    return false;
                }
            }

            //all included
            return true;

        } else {
            //exclude mode
            if (dimSize <= selection[0]) {
                //nothing excluded
                return true;
            }

            if (selection.length < dimSize) {
                //something should be excluded but not everything
                return false;
            }

            //test if some value is included
            for (int i = 0; i < dimSize; i++) {
                if (selection[i] != i) {
                    return false;
                }
            }

            //everything excluded!
            throw new KNIPException("dimension " + dimLabel + " is completely excluded. No valid image remains",
                    new InvalidSettingsException("image reduced to nothing."));

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        try {
            m_selection.clear();
            m_isIncMode.clear();
            final Config lists = settings.getConfig(m_configName);
            final int size = lists.getInt(CFG_NUM_SELECTED_DIMS);
            for (int i = 0; i < size; i++) {
                final String key = lists.getString(CFG_SELECTED_DIMLABEL + i);
                m_selection.put(key, lists.getIntArray(CFG_SELECTION + key));
                m_isIncMode.put(key, lists.getBoolean(CFG_INCLMODE + key));
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
            m_selection.clear();
            m_isIncMode.clear();
            final int size = lists.getInt(CFG_NUM_SELECTED_DIMS);
            for (int i = 0; i < size; i++) {
                final String key = lists.getString(CFG_SELECTED_DIMLABEL + i);
                m_selection.put(key, lists.getIntArray(CFG_SELECTION + key));
                m_isIncMode.put(key, lists.getBoolean(CFG_INCLMODE + key));
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
        lists.addInt(CFG_NUM_SELECTED_DIMS, m_selection.size());
        int i = 0;
        for (final String key : m_selection.keySet()) {
            lists.addString(CFG_SELECTED_DIMLABEL + i++, key);
            lists.addIntArray(CFG_SELECTION + key, m_selection.get(key));
            if (m_isIncMode.get(key) == null) {
                lists.addBoolean(CFG_INCLMODE + key, true);
            } else {
                lists.addBoolean(CFG_INCLMODE + key, m_isIncMode.get(key));
            }
        }
    }

    /**
     * @param dimLabel
     * @param isIncMode
     */
    public final void setIncMode(final String dimLabel, final boolean isIncMode) {
        m_isIncMode.put(dimLabel, isIncMode);
        notifyChangeListeners();
    }

    /**
     * if selection.get(dimlabel) ==null -> all coordinates are selected
     * 
     * @param dimLabel
     * @param selection
     */
    public final void setSelection(final String dimLabel, final int[] selection) {
        if ((selection == null) || (selection.length == 0)) {
            m_selection.remove(dimLabel);
            m_isIncMode.remove(dimLabel);
        } else {

            m_selection.put(dimLabel, selection);
            if (m_isIncMode.get(dimLabel) == null) {
                m_isIncMode.put(dimLabel, true);
            }
        }
        notifyChangeListeners();
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
        final int numDims = lists.getInt(CFG_NUM_SELECTED_DIMS);
        for (int i = 0; i < numDims; i++) {
            final String key = lists.getString(CFG_SELECTED_DIMLABEL + i);
            lists.getIntArray(CFG_SELECTION + key);
            lists.getBoolean(CFG_INCLMODE + key);
        }

    }

    /**
     * Returns for each axis that is not completely selected a pair axes, [] {selected Indices}
     * 
     * @param dimensions
     * @param calibAxes
     * @return an array of pairs {axes}{selected indices}
     */
    @SuppressWarnings("unchecked")
    public Pair<TypedAxis, long[]>[] createSelectionConstraints(final long[] dimensions,
                                                                final CalibratedAxis[] calibAxes) throws KNIPException {

        ArrayList<Pair<TypedAxis, long[]>> ret = new ArrayList<Pair<TypedAxis, long[]>>();

        final boolean[] selectionInformation = getSelectionInformation(dimensions, calibAxes);

        for (int d = 0; d < dimensions.length; d++) {

            if (selectionInformation[d]) {
                //true if the axes should be completely included
                //skip this one no need to add a region constraint
            } else {
                final int[] selection = m_selection.get(calibAxes[d].type().getLabel());

                if (getIncMode(calibAxes[d].type().getLabel())) {
                    // include mode
                    long[] selectionL = new long[selection.length];
                    for (int i = 0; i < selection.length; i++) {
                        selectionL[i] = selection[i];
                    }

                    ret.add(new ValuePair<TypedAxis, long[]>(calibAxes[d], selectionL));
                } else {
                    //exclude mode
                    //the following code is based on the fact that the selection array is ordered
                    int j = 0;
                    long[] selectionL = new long[((int)(dimensions[d]) - selection.length)];
                    for (int i = 0; i < dimensions[d]; i++) {
                        if (j >= selection.length || selection[j] != i) {
                            selectionL[i - j] = i;
                        } else {
                            j++;
                        }
                    }

                    ret.add(new ValuePair<TypedAxis, long[]>(calibAxes[d], selectionL));
                }
            }
        }

        return ret.toArray(new ValuePair[]{});
    }

}
