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
package org.knime.knip.scijava.dialog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.impl.util.Base64;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.scijava.module.Module;

/**
 * Partial configuration of a {@link Module} by the basic input parameters of an ImageJ parameter dialog. The parameters
 * get added to an {@link ExtendedInputPanel} and presented to the user with the associated
 * {@link DialogComponentModule}. This SettingsModel allows to make a configuration persistent in a {@link Config}
 * object.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SettingsModelModuleDialog extends SettingsModel {

    /* prefix if the class name of a module item has been serialized instead of the module item itself*/
    private static final String CLASS_PREFIX = "class:";

    /**
     * the data values that are set via the Dialog. Associates an item name with its data value.
     */
    private final Map<String, Object> m_itemName2Value;

    private static final String ITEM_KEYS_KEY = "SMIJD_ItemKey";

    private static final String ITEM_TYPES_KEY = "SMIJD_ItemType";

    private final String m_configName;

    /**
     * @param configName a name that is suitable for {@link SettingsModel#getConfigName}
     */
    public SettingsModelModuleDialog(final String configName) {
        m_configName = configName;
        m_itemName2Value = new HashMap<String, Object>(10);
    }

    /**
     * sets the values of the ModuleItems that are handled by this SettingsModel and changes their status to resolved.
     *
     * @param module a partially resolved Module where the basic input parameters have been configured and resolved
     */
    public void configureModule(final Module module) {

        for (final String name : m_itemName2Value.keySet()) {
            module.setInput(name, m_itemName2Value.get(name));
            module.setResolved(name, true);
        }

    }

    /**
     * @param itemNames identifies an item by name
     * @return the values of all items that are part of the model and specified in the provided item set
     */
    public Map<String, Object> getItemValues(final Set<String> itemNames) {
        final Map<String, Object> ret = new HashMap<String, Object>();
        for (final String itemName : itemNames) {
            if (m_itemName2Value.containsKey(itemName)) {
                ret.put(itemName, m_itemName2Value.get(itemName));
            }
        }
        return ret;
    }

    /**
     * sets the value of the item with the specified name in the model. E.g. could set the value of an input parameter
     * threshold that is handled in a {@link DialogComponentModule}.
     *
     * @param itemName identifies an item by name
     * @param value new value of the identified item
     */
    public void setItemValue(final String itemName, final Object value) {
        m_itemName2Value.put(itemName, value);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected <T extends SettingsModel> T createClone() {
        return (T)new SettingsModelModuleDialog(m_configName);
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_imagejdlg";
    }

    @Override
    protected String getConfigName() {
        return m_configName;
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
            // ignore keep old value for the item
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
     * not implemented
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {

        final String[] keys = settings.getStringArray(ITEM_KEYS_KEY);
        final String[] types = settings.getStringArray(ITEM_TYPES_KEY);

        for (int i = 0; i < keys.length; i++) {
            final Object item;
            if (types.equals("null")) {
                item = null;
            } else if (types[i].equals(Byte.class.getSimpleName())) {
                item = settings.getByte(keys[i]);
            } else if (types[i].equals(Character.class.getSimpleName())) {
                item = settings.getChar(keys[i]);
            } else if (types[i].equals(Short.class.getSimpleName())) {
                item = settings.getShort(keys[i]);
            } else if (types[i].equals(Integer.class.getSimpleName())) {
                item = settings.getInt(keys[i]);
            } else if (types[i].equals(Long.class.getSimpleName())) {
                item = settings.getLong(keys[i]);
            } else if (types[i].equals(Float.class.getSimpleName())) {
                item = settings.getFloat(keys[i]);
            } else if (types[i].equals(Double.class.getSimpleName())) {
                item = settings.getDouble(keys[i]);
            } else if (types[i].equals(String.class.getSimpleName())) {
                item = settings.getString(keys[i]);
            } else if (types[i].equals(Boolean.class.getSimpleName())) {
                item = settings.getBoolean(keys[i]);
            } else {
                final String itemString = settings.getString(keys[i]);
                item = stringToObject(itemString);
            }

            if (item != null) {
                m_itemName2Value.put(keys[i], item);
            } else {
                throw new InvalidSettingsException("The item with the identifier " + keys[i]
                        + " could not be restored.");
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {

        final String[] keys = m_itemName2Value.keySet().toArray(new String[]{});
        final String[] types = new String[keys.length];
        settings.addStringArray(ITEM_KEYS_KEY, keys);

        for (int i = 0; i < keys.length; i++) {
            final Object item = m_itemName2Value.get(keys[i]);
            if (item == null) {
                types[i] = "null";
            } else if (item instanceof Byte) {
                settings.addByte(keys[i], (Byte)item);
                types[i] = Byte.class.getSimpleName();
            } else if (item instanceof Character) {
                settings.addChar(keys[i], (Character)item);
                types[i] = Character.class.getSimpleName();
            } else if (item instanceof Short) {
                settings.addShort(keys[i], (Short)item);
                types[i] = Short.class.getSimpleName();
            } else if (item instanceof Integer) {
                settings.addInt(keys[i], (Integer)item);
                types[i] = Integer.class.getSimpleName();
            } else if (item instanceof Long) {
                settings.addLong(keys[i], (Long)item);
                types[i] = Long.class.getSimpleName();
            } else if (item instanceof Float) {
                settings.addFloat(keys[i], (Float)item);
                types[i] = Float.class.getSimpleName();
            } else if (item instanceof Double) {
                settings.addDouble(keys[i], (Double)item);
                types[i] = Double.class.getSimpleName();
            } else if (item instanceof String) {
                settings.addString(keys[i], (String)item);
                types[i] = String.class.getSimpleName();
            } else if (item instanceof Boolean) {
                settings.addBoolean(keys[i], (Boolean)item);
                types[i] = Boolean.class.getSimpleName();
            } else {
                final String itemString = objectToString(item);
                types[i] = "OTHER";
                if (!itemString.isEmpty()) {
                    settings.addString(keys[i], itemString);
                }
            }
        }
        settings.addStringArray(ITEM_TYPES_KEY, types);
    }

    /**
     * encodes the bytes of an object as a base 64 string.
     *
     * @param object
     * @return a string representation of an object in base 64 encoding
     */
    private String objectToString(final Object object) {
        try {
            //use java serialization, if object is serializable
            if (object instanceof Serializable) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final ObjectOutputStream out = new ObjectOutputStream(baos);
                out.writeObject(object);
                baos.flush();
                out.close();
                baos.close();

                final byte[] byteObject = baos.toByteArray();
                new Base64();
                final byte[] encodedObject = Base64.encode(byteObject);

                return new String(encodedObject);
            }

            //if there is an empty default constructor, just write the class name and re-instantiate the object later
            final Constructor<?>[] constructors = object.getClass().getConstructors();
            boolean hasPublicDefaultConstructor = false;
            for (final Constructor<?> constr : constructors) {
                if (constr.getParameterTypes().length == 0) {
                    hasPublicDefaultConstructor = true;
                    break;
                }
            }
            if (hasPublicDefaultConstructor) {
                return CLASS_PREFIX + object.getClass().getCanonicalName();
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Object of class " + object.getClass().getCanonicalName()
                + " can not be serialized.");

    }

    /**
     * @param stringRepresentation base64 encoded string representation of the bytecode of an object
     * @return the decoded object
     */
    private Object stringToObject(final String stringRepresentation) {

        if (stringRepresentation.startsWith(CLASS_PREFIX)) {
            //if only the class name has been serialized re-instantiate from the class name
            try {
                return Class.forName(stringRepresentation.substring(CLASS_PREFIX.length())).newInstance();
            } catch (final InstantiationException e) {
                throw new RuntimeException(e);
            } catch (final IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            //restore object via java serialization
            Object ret = null;
            new Base64();
            final byte[] decodedObject = Base64.decode(stringRepresentation.getBytes());

            try {
                final ByteArrayInputStream bis = new ByteArrayInputStream(decodedObject);
                ObjectInputStream ois;
                ois = new ObjectInputStream(bis);
                ret = ois.readObject();
            } catch (final IOException e) {
                e.printStackTrace();
            } catch (final ClassNotFoundException e) {
                e.printStackTrace();
            }
            return ret;
        }
    }

    @Override
    public String toString() {
        return m_configName;
    }
}
