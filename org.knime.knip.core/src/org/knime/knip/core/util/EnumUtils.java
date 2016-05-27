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
package org.knime.knip.core.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Helper class to create String[] Lists of Labelings.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 */
public final class EnumUtils {

    // prevent instantiation
    private EnumUtils() {
    }

    /**
     * Retrieve {@link Enum} for name.
     *
     * @param name
     * @param values
     *
     * @return {@link Enum} with the given name
     */
    public static <E extends Enum<?>> E valueForName(final String name, final E[] values) {
        for (E mode : values) {
            if (mode.toString().equalsIgnoreCase(name)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unknown enum value");
    }

    /**
     * @deprecated
     * Please provide readable names and use the toString methods instead:
     * {@link #getStringListFromToString(Enum...)}
     * {@link #getStringCollectionFromToString(Enum...)}
     *
     * @param enums
     * @return enum as String[] array based on names()
     */
    @Deprecated
    public static String[] getStringListFromName(final Enum<?>... enums) {

        final String[] s = new String[enums.length];

        int i = 0;
        for (final Enum<?> e : enums) {
            s[i++] = e.name();
        }

        return s;
    }

    /**
     * @deprecated
     * Please provide readable names and use the toString based methods instead:
     * {@link #getStringListFromToString(Enum...)} or
     * {@link #getStringCollectionFromToString(Enum...)}
     *
     * @param enums
     * @return Collection<String> based on name()
     */
    @Deprecated
    public static Collection<String> getStringCollectionFromName(final Enum<?>[] enums) {
        final ArrayList<String> s = new ArrayList<String>();
        for (final Enum<?> e : enums) {
            s.add(e.name());
        }
        return s;
    }

    /**
     * Creates a {@link String}[] using toString of the given {@link Enum}s.
     *
     * @param enums
     *
     * @return enum as String[] based on toString
     */
    public static String[] getStringListFromToString(final Enum<?>... enums) {

        final String[] s = new String[enums.length];

        int i = 0;
        for (final Enum<?> e : enums) {
            s[i++] = e.toString();
        }

        return s;
    }

    /**
     * Creates a {@link Collection} of {@link String}s using toString of the given {@link Enum}.
     *
     * @param enums
     * @return Collection<String> of enum based on toString
     */
    public static Collection<String> getStringCollectionFromToString(final Enum<?>... enums) {
        final ArrayList<String> s = new ArrayList<String>();
        for (final Enum<?> e : enums) {
            s.add(e.toString());
        }
        return s;
    }
}
