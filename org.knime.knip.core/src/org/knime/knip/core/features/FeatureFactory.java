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
package org.knime.knip.core.features;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates a specific set of features (double values) for a particular {@link FeatureTarget}. The feature factory
 * itself basically takes care about which features are enabled.
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class FeatureFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureFactory.class);

    private final Map<Class<?>, List<FeatureTargetUpdater>> m_targetListeners =
            new HashMap<Class<?>, List<FeatureFactory.FeatureTargetUpdater>>();

    private final Map<Class<?>, Object> m_sharedObjects = new HashMap<Class<?>, Object>();

    private final List<FeatureSet> m_featureSetList = new ArrayList<FeatureSet>();

    private final List<Integer> m_featureSetIdOffset = new ArrayList<Integer>();

    private final List<String> m_featNames = new ArrayList<String>();

    private int[] m_featIdxMap;

    /* the enabled features */
    private BitSet m_enabled = null;

    /**
     * Creates a new feature factory
     * 
     * @param enableAll if all features of the added feature sets have to be enabled
     * @param fsets
     */
    public FeatureFactory(final boolean enableAll, final Collection<? extends FeatureSet> fsets) {
        this(enableAll, fsets.toArray(new FeatureSet[fsets.size()]));
    }

    /**
     * Creates a new feature factory
     * 
     * @param enableAll if all features of the added feature sets have to be enabled
     * @param fsets
     */
    public FeatureFactory(final boolean enableAll, final FeatureSet... fsets) {
        int currentOffset = 0;
        for (final FeatureSet fset : fsets) {
            // look for FeatureTargetListener annotations and add
            // them to
            // the listener map
            collectFeatureTargetListenersRecursively(fset.getClass(), fset);

            if (fset instanceof SharesObjects) {
                final Class<?>[] clazzes = ((SharesObjects)fset).getSharedObjectClasses();

                final Object[] instances = new Object[clazzes.length];

                for (int i = 0; i < clazzes.length; i++) {
                    Object obj;
                    if ((obj = m_sharedObjects.get(clazzes[i])) == null) {
                        try {
                            obj = clazzes[i].newInstance();
                        } catch (final Exception e) {
                            LOG.error("Can not create instance of class " + clazzes[i] + ".", e);
                        }
                        m_sharedObjects.put(clazzes[i], obj);
                    }
                    instances[i] = obj;
                }
                ((SharesObjects)fset).setSharedObjectInstances(instances);

            }

            for (int i = 0; i < fset.numFeatures(); i++) {
                m_featNames.add(fset.name(i));
                m_featureSetList.add(fset);
                m_featureSetIdOffset.add(currentOffset);
            }

            currentOffset += fset.numFeatures();
        }

        if (enableAll) {
            initFeatureFactory(null);
        }
    }

    public void initFeatureFactory(final BitSet enabledFeatures) {
        if (m_enabled != null) {
            throw new IllegalStateException("Feature factory was already initialized!");
        }

        if (enabledFeatures == null) {
            m_enabled = new BitSet();
            m_enabled.set(0, m_featNames.size());
        } else {
            m_enabled = enabledFeatures.get(0, m_featNames.size());
        }
        m_featIdxMap = new int[m_enabled.cardinality()];
        int featIdx = 0;
        for (int i = 0; i < m_featNames.size(); i++) {
            if (m_enabled.get(i)) {
                m_featIdxMap[featIdx] = i;
                featIdx++;
                m_featureSetList.get(i).enable(i - m_featureSetIdOffset.get(i));
            }

        }
    }

    /**
     * Looks for FeatureTargetListener annotations and adds them to the listener map
     */
    protected void collectFeatureTargetListenersRecursively(final Class<?> clazz, final Object listener) {
        if (clazz == null) {
            return;
        }

        final Method[] methods = clazz.getMethods();

        LOG.debug("Looking for FeatureTargetListener annotations for class " + clazz + ", methods:"
                + Arrays.toString(methods));

        for (final Method method : methods) {

            final FeatureTargetListener targetAnnotation = method.getAnnotation(FeatureTargetListener.class);
            if (targetAnnotation != null) {

                final Class<?>[] types = method.getParameterTypes();
                if (types.length != 1) {
                    LOG.error("Only methods with exactly one parameter are allowed as feature target listener. Method '"
                            + method + "' skipped.");
                } else {

                    LOG.debug("Found FeatureTargetListener: " + targetAnnotation + "  on method '" + method + "'");
                    List<FeatureTargetUpdater> listeners;
                    if ((listeners = m_targetListeners.get(types[0])) == null) {
                        listeners = new ArrayList<FeatureFactory.FeatureTargetUpdater>();
                        m_targetListeners.put(types[0], listeners);
                    }
                    listeners.add(new FeatureTargetUpdater(listener, method));
                }

            }
        }

        collectFeatureTargetListenersRecursively(clazz.getSuperclass(), listener);
        final Class<?>[] interfaces = clazz.getInterfaces();
        for (final Class<?> interfaze : interfaces) {
            collectFeatureTargetListenersRecursively(interfaze, listener);
        }
    }

    /**
     * Updates a feature target.
     * 
     * @param m_target
     * @param obj
     */
    public void updateFeatureTarget(final Object obj) {
        updateFeatureTargetRecursively(obj, obj.getClass());
    }

    private void updateFeatureTargetRecursively(final Object obj, final Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        try {
            final List<FeatureTargetUpdater> ftus = m_targetListeners.get(clazz);
            if (ftus != null) {
                for (final FeatureTargetUpdater ftu : ftus) {
                    ftu.updateFeatureTarget(obj);
                }
                return;
            } else {
                updateFeatureTargetRecursively(obj, clazz.getSuperclass());
                final Class<?>[] interfaces = clazz.getInterfaces();
                for (final Class<?> interfaze : interfaces) {
                    updateFeatureTargetRecursively(obj, interfaze);
                }

            }
        } catch (final IllegalArgumentException e) {
            LOG.debug("Error thrown: " + e.getMessage() + ". Class " + obj.getClass().getSimpleName() + "!");
        } catch (final Exception e) {
            Throwable t = e.getCause();

            while (t instanceof InvocationTargetException) {
                t = t.getCause();
            }

            if (t instanceof IllegalArgumentException) {
                LOG.debug("Error thrown: " + t.getMessage() + ". Class " + obj.getClass().getSimpleName() + "!");
            } else {
                t.printStackTrace();
            }
        }
    }

    /**
     * 
     * @param featID
     * @return the feature for the given feature ID, the feature id is assigned according to the order the feature sets
     *         were added
     */
    public double getFeatureValue(final int featID) {
        return m_featureSetList.get(m_featIdxMap[featID]).value(getFeatureSetFeatureID(featID));
    }

    /**
     * @param featID
     * @return the feature id in the feature set, where featID points to
     */
    protected int getFeatureSetFeatureID(final int featID) {
        return m_featIdxMap[featID] - m_featureSetIdOffset.get(m_featIdxMap[featID]);
    }

    /**
     * @return the feature values of all enabled features
     */
    public double[] getFeatureValues() {
        return getFeatureValues(new double[getNumFeatures()]);
    }

    /**
     * @return the feature values of all enabled features
     */
    public double[] getFeatureValues(final double[] vec) {
        int i = 0;
        for (int feat = m_enabled.nextSetBit(0); feat >= 0; feat = m_enabled.nextSetBit(feat + 1)) {
            vec[i++] = m_featureSetList.get(feat).value(feat - m_featureSetIdOffset.get(feat));
        }
        return vec;
    }

    /**
     * The total number of enabled features.
     * 
     * @return num enabled features
     */
    public int getNumFeatures() {
        isInitialized();
        return m_enabled.cardinality();
    }

    /**
     * 
     * 
     * @return the names of the enabled features.
     */
    public String[] getFeatureNames() {
        isInitialized();
        final String[] res = new String[getNumFeatures()];

        int i = 0;
        for (int feat = m_enabled.nextSetBit(0); feat >= 0; feat = m_enabled.nextSetBit(feat + 1)) {
            res[i++] = m_featNames.get(feat);
        }
        return res;
    }

    /**
     * @param obj
     * @return
     */
    public boolean isFeatureTargetRequired(final Object obj) {
        return m_targetListeners.get(obj.getClass()) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "";

    }

    /**
     * @param featIdx
     * @return the feature set used to calculate the feature of the given index
     */
    protected FeatureSet getFeatureSetForFeatureIdx(final int featIdx) {
        return m_featureSetList.get(m_featIdxMap[featIdx]);
    }

    private void isInitialized() {
        if (m_enabled == null) {
            throw new IllegalStateException("Feature factory not initialized, yet!");
        }
    }

    protected static class FeatureTargetUpdater {

        private final Method m_method;

        private final Object m_listener;

        public FeatureTargetUpdater(final Object listener, final Method method) {
            m_method = method;
            m_listener = listener;
        }

        public void updateFeatureTarget(final Object target) {
            try {
                m_method.invoke(m_listener, target);
            } catch (final Exception e) {
                throw new RuntimeException(
                        "InvocationTargetException when invoking annotated method from FeatureTarget Update. Data: "

                        + target.toString() + ", subscriber:" + m_listener, e);
            }
        }

    }

}
