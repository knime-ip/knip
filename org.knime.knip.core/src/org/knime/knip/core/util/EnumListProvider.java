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
public class EnumListProvider {

    public static String[] getStringList(final Enum<?>... enums) {

        final String[] s = new String[enums.length];

        int i = 0;
        for (final Enum<?> e : enums) {
            s[i++] = e.name();
        }

        return s;
    }

    public static Collection<String> getStringCollection(final Enum<?>[] enums) {
        final ArrayList<String> s = new ArrayList<String>();
        for (final Enum<?> e : enums) {
            s.add(e.name());
        }
        return s;
    }
}
