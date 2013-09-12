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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.xmlbeans.impl.util.Base64;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.Externalizer;
import org.knime.knip.core.io.externalization.ExternalizerManager;

/**
 * Saves serializable objects as base64 encoded string. Base64 is used to increase the performance of the serialization
 * because the NodeSettingsWO addByteArray method writes the byte array as single bytes.
 * 
 * 
 * @param <S>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SettingsModelSerializableObjects<S extends Serializable> extends SettingsModel {

    private final NodeLogger logger = NodeLogger.getLogger(SettingsModelSerializableObjects.class);

    private final String m_configName;

    private Externalizer<Object> m_objectExternalizerToUse = null;

    private final List<S> m_objects;

    /*
     * Copy constructor.
     */
    protected SettingsModelSerializableObjects(final String configName, final Collection<S> objects,
                                               final Externalizer<Object> objExternalizer) {
        m_configName = configName;
        m_objects = new ArrayList<S>();
        setObjects(objects);
        m_objectExternalizerToUse = objExternalizer;
    }

    public SettingsModelSerializableObjects(final String configName, final Externalizer<Object> objExternalizer) {
        m_configName = configName;
        m_objects = new ArrayList<S>();
        m_objectExternalizerToUse = objExternalizer;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelSerializableObjects<S> createClone() {
        final SettingsModelSerializableObjects<S> model =
                new SettingsModelSerializableObjects<S>(getConfigName(), getObjects(), m_objectExternalizerToUse);
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_kernel_selection";
    }

    public List<S> getObjects() {
        return m_objects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            loadSettingsForModel(settings);
        } catch (final InvalidSettingsException e) {
            //
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.getString(m_configName) != null) {
            byte[] bytes;
            try {
                new Base64();
                // use base64 to increase the performance
                // compared to the settings.addByteArray method
                bytes = Base64.decode(settings.getString(m_configName).getBytes());
                final BufferedDataInputStream in = new BufferedDataInputStream(new ByteArrayInputStream(bytes));
                final int num = in.readInt();
                m_objects.clear();
                for (int i = 0; i < num; i++) {
                    m_objects.add(ExternalizerManager.<S> read(in));
                }
                notifyChangeListeners();
            } catch (final Exception e) {
                e.printStackTrace();
                logger.error("Exception while loading object", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        String res = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final BufferedDataOutputStream out = new BufferedDataOutputStream(baos);
            out.writeInt(m_objects.size());
            for (final S kc : m_objects) {
                if (m_objectExternalizerToUse == null) {
                    ExternalizerManager.write(out, kc);
                } else {
                    ExternalizerManager.write(out, kc, m_objectExternalizerToUse);
                }
            }
            out.close();
            new Base64();
            // use base64 to increase the performance compared to
            // the settings.addByteArray method
            res = new String(Base64.encode(baos.toByteArray()));
        } catch (final Exception e) {
            logger.error("Exception while saving object", e);
        }
        settings.addString(m_configName, res);
    }

    public void setObjects(final Collection<S> objects) {
        m_objects.clear();
        m_objects.addAll(objects);
        notifyChangeListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        //
    }
}
