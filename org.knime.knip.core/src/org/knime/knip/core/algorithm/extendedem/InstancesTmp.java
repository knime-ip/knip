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
package org.knime.knip.core.algorithm.extendedem;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class InstancesTmp extends ArrayList<InstanceTmp> implements Serializable {

    private int m_classIndex;

    private String m_relationName;

    private ArrayList<InstanceTmp> m_instances;

    private ArrayList<AttributeTmp> m_attributes;

    public InstancesTmp(final InstancesTmp dataset, final int capacity) {
        initialize(dataset, capacity);
    }

    public InstancesTmp(final String name, final ArrayList<AttributeTmp> attInfo, final int capacity) {

        m_relationName = name;
        m_classIndex = -1;
        m_attributes = attInfo;
        for (int i = 0; i < numAttributes(); i++) {
            attribute(i).setIndex(i);
        }
        m_instances = new ArrayList<InstanceTmp>(capacity);
    }

    protected void initialize(final InstancesTmp dataset, int capacity) {
        if (capacity < 0) {
            capacity = 0;
        }

        // Strings only have to be "shallow" copied because
        // they can't be modified.
        m_classIndex = dataset.m_classIndex;
        m_relationName = dataset.m_relationName;
        m_attributes = dataset.m_attributes;
        m_instances = new ArrayList<InstanceTmp>(capacity);
    }

    @Override
    public InstanceTmp get(final int index) {
        return m_instances.get(index);
    }

    public InstanceTmp instance(final int index) {
        return m_instances.get(index);
    }

    public AttributeTmp attribute(final int index) {

        return m_attributes.get(index);
    }

    public int numAttributes() {

        return m_attributes.size();
    }

    @Override
    public boolean add(final InstanceTmp inst) {
        return m_instances.add(inst);
    }

    public int maxIndex(final double[] doubles) {

        double maximum = 0;
        int maxIndex = 0;

        for (int i = 0; i < doubles.length; i++) {
            if ((i == 0) || (doubles[i] > maximum)) {
                maxIndex = i;
                maximum = doubles[i];
            }
        }

        return maxIndex;
    }

    public int maxIndex(final int[] ints) {

        int maximum = 0;
        int maxIndex = 0;

        for (int i = 0; i < ints.length; i++) {
            if ((i == 0) || (ints[i] > maximum)) {
                maxIndex = i;
                maximum = ints[i];
            }
        }

        return maxIndex;
    }

    private int partition(final int[] array, final int[] index, int l, int r) {

        final long avg = (l + r) / 2;
        final double pivot = array[index[(int)avg]];
        int help;

        while (l < r) {
            while ((array[index[l]] < pivot) && (l < r)) {
                l++;
            }
            while ((array[index[r]] > pivot) && (l < r)) {
                r--;
            }
            if (l < r) {
                help = index[l];
                index[l] = index[r];
                index[r] = help;
                l++;
                r--;
            }
        }
        if ((l == r) && (array[index[r]] > pivot)) {
            r--;
        }

        return r;
    }

    private void quickSort(final int[] array, final int[] index, final int left, final int right) {

        if (left < right) {
            final int middle = partition(array, index, left, right);
            quickSort(array, index, left, middle);
            quickSort(array, index, middle + 1, right);
        }
    }

    public int[] sort(final int[] array) {

        final int[] index = new int[array.length];
        final int[] newIndex = new int[array.length];
        int[] helpIndex;
        int numEqual;

        for (int i = 0; i < index.length; i++) {
            index[i] = i;
        }
        quickSort(array, index, 0, array.length - 1);

        // Make sort stable
        int i = 0;
        while (i < index.length) {
            numEqual = 1;
            for (int j = i + 1; ((j < index.length) && (array[index[i]] == array[index[j]])); j++) {
                numEqual++;
            }
            if (numEqual > 1) {
                helpIndex = new int[numEqual];
                for (int j = 0; j < numEqual; j++) {
                    helpIndex[j] = i + j;
                }
                quickSort(index, helpIndex, 0, numEqual - 1);
                for (int j = 0; j < numEqual; j++) {
                    newIndex[i + j] = index[helpIndex[j]];
                }
                i += numEqual;
            } else {
                newIndex[i] = index[i];
                i++;
            }
        }
        return newIndex;
    }

    public int[] sort(double[] array) {

        final int[] index = new int[array.length];
        array = array.clone();
        for (int i = 0; i < index.length; i++) {
            index[i] = i;
            if (Double.isNaN(array[i])) {
                array[i] = Double.MAX_VALUE;
            }
        }
        quickSort(array, index, 0, array.length - 1);
        return index;
    }

    private int partition(final double[] array, final int[] index, int l, int r) {

        final long avg = (l + r) / 2;
        final double pivot = array[index[(int)avg]];
        int help;

        while (l < r) {
            while ((array[index[l]] < pivot) && (l < r)) {
                l++;
            }
            while ((array[index[r]] > pivot) && (l < r)) {
                r--;
            }
            if (l < r) {
                help = index[l];
                index[l] = index[r];
                index[r] = help;
                l++;
                r--;
            }
        }
        if ((l == r) && (array[index[r]] > pivot)) {
            r--;
        }

        return r;
    }

    private void quickSort(final double[] array, final int[] index, final int left, final int right) {

        if (left < right) {
            final int middle = partition(array, index, left, right);
            quickSort(array, index, left, middle);
            quickSort(array, index, middle + 1, right);
        }
    }

    public double meanOrMode(final int attIndex) {

        double result, found;
        int[] counts;

        if (attribute(attIndex).isNumeric()) {
            result = found = 0;
            for (int j = 0; j < numInstances(); j++) {
                if (!instance(j).isMissing(attIndex)) {
                    found += instance(j).weight();
                    result += instance(j).weight() * instance(j).value(attIndex);
                }
            }
            if (found <= 0) {
                return 0;
            } else {
                return result / found;
            }
        } else if (attribute(attIndex).isNominal()) {
            counts = new int[attribute(attIndex).numValues()];
            for (int j = 0; j < numInstances(); j++) {
                if (!instance(j).isMissing(attIndex)) {
                    counts[(int)instance(j).value(attIndex)] += instance(j).weight();
                }
            }
            return maxIndex(counts);
        } else {
            return 0;
        }
    }

    public/* @pure@ */double[] attributeToDoubleArray(final int index) {

        final double[] result = new double[numInstances()];
        for (int i = 0; i < result.length; i++) {
            result[i] = instance(i).value(index);
        }
        return result;
    }

    public AttributeStatsTmp attributeStats(final int index) {

        final AttributeStatsTmp result = new AttributeStatsTmp();
        if (attribute(index).isNominal()) {
            result.m_nominalCounts = new int[attribute(index).numValues()];
            result.m_nominalWeights = new double[attribute(index).numValues()];
        }
        if (attribute(index).isNumeric()) {
            result.m_numericStats = new StatsTmp();
        }
        result.m_totalCount = numInstances();

        final double[] attVals = attributeToDoubleArray(index);
        final int[] sorted = sort(attVals);
        int currentCount = 0;
        double currentWeight = 0;
        double prev = Double.NaN;
        for (int j = 0; j < numInstances(); j++) {
            final InstanceTmp current = instance(sorted[j]);
            if (current.isMissing(index)) {
                result.m_missingCount = numInstances() - j;
                break;
            }
            if (current.value(index) == prev) {
                currentCount++;
                currentWeight += current.weight();
            } else {
                result.addDistinct(prev, currentCount, currentWeight);
                currentCount = 1;
                currentWeight = current.weight();
                prev = current.value(index);
            }
        }
        result.addDistinct(prev, currentCount, currentWeight);
        result.m_distinctCount--; // So we don't count "missing" as a
        // value
        return result;
    }

    @Override
    public int size() {
        return m_instances.size();
    }

    public int numInstances() {
        return m_instances.size();
    }

}
