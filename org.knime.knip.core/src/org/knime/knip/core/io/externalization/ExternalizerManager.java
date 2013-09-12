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
package org.knime.knip.core.io.externalization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.knip.core.io.externalization.externalizers.AbstractImgExt0;
import org.knime.knip.core.io.externalization.externalizers.ArrayImgExt0;
import org.knime.knip.core.io.externalization.externalizers.CalibratedSpaceExt0;
import org.knime.knip.core.io.externalization.externalizers.CellImgExt0;
import org.knime.knip.core.io.externalization.externalizers.ClassExt0;
import org.knime.knip.core.io.externalization.externalizers.DefaultLabelingColorTableExt0;
import org.knime.knip.core.io.externalization.externalizers.ImageMetadataExt0;
import org.knime.knip.core.io.externalization.externalizers.ImageMetadataExt1;
import org.knime.knip.core.io.externalization.externalizers.ImgExt0;
import org.knime.knip.core.io.externalization.externalizers.ImgMetadataExt0;
import org.knime.knip.core.io.externalization.externalizers.ImgMetadataExt1;
import org.knime.knip.core.io.externalization.externalizers.ImgViewExt0;
import org.knime.knip.core.io.externalization.externalizers.LabelingMappingExt0;
import org.knime.knip.core.io.externalization.externalizers.LabelingMetadataExt0;
import org.knime.knip.core.io.externalization.externalizers.NamedExt0;
import org.knime.knip.core.io.externalization.externalizers.NativeImgLabelingExt0;
import org.knime.knip.core.io.externalization.externalizers.NtreeImgExt0;
import org.knime.knip.core.io.externalization.externalizers.ObjectExt0;
import org.knime.knip.core.io.externalization.externalizers.PlanarImgExt0;
import org.knime.knip.core.io.externalization.externalizers.SourcedExt0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a set of externalizers, e.g. registered via the according extension point.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class ExternalizerManager {

    private static Logger LOGGER = LoggerFactory.getLogger(ExternalizerManager.class);

    /* maps the externalizer object type to the externalizer */
    private static final Map<Class<?>, Externalizer> CLASS_EXT_MAP = new HashMap<Class<?>, Externalizer>();

    /* maps a externalizer object type to the externalizer id */
    private static final Map<Class<?>, String> CLASS_ID_MAP = new HashMap<Class<?>, String>();

    /* maps the externalizer class name to the externalizer object */
    private static final Map<String, Externalizer> ID_EXT_MAP = new HashMap<String, Externalizer>();

    /* The id of the Externalizer extension point. */
    private static final String EXT_POINT_ID = "org.knime.knip.core.Externalizer";

    /*
     * The attribute of the externalizer extension point pointing to the
     * factory class
     */
    private static final String EXT_POINT_ATTR_DF = "Externalizer";

    static {
        // register local externalizer
        registerExternalizer(new ImgExt0());
        registerExternalizer(new ImgViewExt0());
        registerExternalizer(new AbstractImgExt0());
        registerExternalizer(new ArrayImgExt0());
        registerExternalizer(new PlanarImgExt0());
        registerExternalizer(new CellImgExt0());
        registerExternalizer(new NtreeImgExt0());
        registerExternalizer(new CalibratedSpaceExt0());
        registerExternalizer(new NamedExt0());
        registerExternalizer(new SourcedExt0());
        registerExternalizer(new ImageMetadataExt0());
        registerExternalizer(new ClassExt0());
        registerExternalizer(new LabelingMappingExt0());
        registerExternalizer(new NativeImgLabelingExt0());
        registerExternalizer(new ObjectExt0());
        registerExternalizer(new ImgMetadataExt0());
        registerExternalizer(new LabelingMetadataExt0());
        registerExternalizer(new DefaultLabelingColorTableExt0());

        registerExternalizer(new ImageMetadataExt1());
        registerExternalizer(new ImgMetadataExt1());

        registerExtensionPoints();

        LOGGER.debug("Available externalizers used to write objects:");
        for (final Class<?> type : CLASS_EXT_MAP.keySet()) {
            final Externalizer ext = CLASS_EXT_MAP.get(type);
            LOGGER.debug("--- type=" + type.getSimpleName() + ";id=" + ext.getId() + ";extClass="
                    + ext.getClass().getSimpleName());
        }

        LOGGER.debug("Available externalizers used to read objects:");
        for (final String id : ID_EXT_MAP.keySet()) {
            final Externalizer ext = ID_EXT_MAP.get(id);
            LOGGER.debug("--- type=" + ext.getType().getSimpleName() + ";id=" + id + ";extClass="
                    + ext.getClass().getSimpleName());
        }

    }

    private ExternalizerManager() {
        // utility class
    }

    /**
     * Recursively retrieves the right {@link Externalizer} for the given object, beginning with the class itself. If
     * for a class no externalizer is registered, the super-classes will be checked iteratively.
     * 
     * 
     * 
     * @param <T>
     * @param out
     * @param type
     * @return
     * @throws Exception
     */
    public static synchronized <T> T read(final BufferedDataInputStream in) throws Exception {
        final String key = readString(in);
        final Externalizer<T> ext = ID_EXT_MAP.get(key);
        if (ext == null) {
            throw new IOException("No externalizer available with id " + key);
        }
        return ext.read(in);
    }

    /**
     * Writes the given object to the output stream using the most specific externalizer from the registered
     * externalizers.
     * 
     * @param <T>
     * @param out
     * @param obj
     * @throws Exception
     */
    public static synchronized <T> void write(final BufferedDataOutputStream out, final T obj) throws Exception {
        write(out, obj, (Class<T>)obj.getClass());
    }

    /**
     * Writes the object to the output stream assuming the given class.
     * 
     * @param <T>
     * @param out
     * @param obj
     * @param type
     * @throws Exception
     */
    public static synchronized <T> void write(final BufferedDataOutputStream out, final T obj, final Class<T> type)
            throws Exception {
        Externalizer<T> ext;
        if ((ext = CLASS_EXT_MAP.get(type)) == null) {
            write(out, obj, type.getSuperclass());
        } else {
            writeString(out, CLASS_ID_MAP.get(type));
            ext.write(out, obj);
        }

    }

    /**
     * Writes the object to the output stream using the given externalizer.
     * 
     * @param <T>
     * @param out
     * @param obj
     * @param ext
     * @throws Exception
     */
    public static synchronized <T> void
            write(final BufferedDataOutputStream out, final T obj, final Externalizer<T> ext) throws Exception {
        writeString(out, ext.getId());
        ext.write(out, obj);
    }

    /**
     * Registers a new externalizer at the manager. Already existing externalizers for a certain type or id will not be
     * replaced if they priority is higher.
     * 
     * @param <T>
     * @param ext the externalizer - must not be a inner class of another class
     */
    public static synchronized <T> void registerExternalizer(final Externalizer<T> ext) {
        Externalizer<T> tmpExt;
        if (((tmpExt = CLASS_EXT_MAP.get(ext.getType())) == null) || (tmpExt.getPriority() < ext.getPriority())) {
            CLASS_EXT_MAP.put(ext.getType(), ext);
            CLASS_ID_MAP.put(ext.getType(), ext.getId());
        }
        tmpExt = null;
        if (((tmpExt = ID_EXT_MAP.get(ext.getId())) == null) || (tmpExt.getPriority() < ext.getPriority())) {
            ID_EXT_MAP.put(ext.getId(), ext);
        }

    }

    private static void writeString(final BufferedDataOutputStream out, final String s) throws IOException {
        final char[] c = s.toCharArray();
        out.writeInt(c.length);
        out.write(c);
    }

    private static String readString(final BufferedDataInputStream in) throws IOException {
        final char[] s = new char[in.readInt()];
        in.read(s);
        return new String(s);
    }

    /**
     * Registers all extension point implementations.
     */
    private static void registerExtensionPoints() {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: " + " --> Invalid extension point: " + EXT_POINT_ID);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
                final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

                if ((operator == null) || operator.isEmpty()) {
                    LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                            + EXT_POINT_ATTR_DF + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }

                try {
                    final Externalizer ext = (Externalizer)elem.createExecutableExtension(EXT_POINT_ATTR_DF);
                    registerExternalizer(ext);
                } catch (final Exception e) {
                    LOGGER.error("Problems during initialization of " + "Externalizer (with id '" + operator + "'.)");
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", e.getCause());
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering " + "Externalizer extensions");
        }
    }

}
