/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.knime.knip.core.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author dietzc, University of Konstanz
 */
public class EnumUtils {

    /**
     * Retrieve {@link Enum} for name
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

        throw new IllegalArgumentException("Unknown filling mode");
    }

    /**
     * Provide nicer names and use toString methods
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
     * use from to string and provide nice names
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
     * @param enums
     * @return Collection<String> of enum based on toString
     */
    public static Collection<String> getStringCollectionFromToString(final Enum<?>[] enums) {
        final ArrayList<String> s = new ArrayList<String>();
        for (final Enum<?> e : enums) {
            s.add(e.toString());
        }
        return s;
    }
}
