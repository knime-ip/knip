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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Helpers to sort arrays or lists. The sorting result leaves the lists unchanged and just gives the permutation of the
 * element positions.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PermutationSort {

    private PermutationSort() {
        // utility class
    }

    /**
     * Sorts an arbitrary array by given the permutation of the array elements.
     * 
     * @return the permutation of the positions
     */
    public static <T> int[] sort(final T[] a, final Comparator<? super T> c) {

        final Integer[] perm = new Integer[a.length];
        for (int i = 0; i < perm.length; i++) {
            perm[i] = i;
        }
        Arrays.sort(perm, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return c.compare(a[o1], a[o2]);
            }
        });
        final int[] res = new int[perm.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = perm[i].intValue();
        }
        return res;
    }

    /**
     * Sorts an arbitrary list by given the permutation of the list elements.
     * 
     * @return the permutation of the positions
     */
    public static <T> int[] sort(final List<T> l, final Comparator<? super T> c) {

        final Integer[] perm = new Integer[l.size()];
        for (int i = 0; i < perm.length; i++) {
            perm[i] = i;
        }
        Arrays.sort(perm, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return c.compare(l.get(o1), l.get(o2));
            }
        });
        final int[] res = new int[perm.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = perm[i].intValue();
        }
        return res;
    }

    /**
     * Sorts an double array by given the permutation of the array elements.
     * 
     * @return the permutation of the positions
     */
    public static int[] sort(final double[] a) {

        final Integer[] perm = new Integer[a.length];
        sortPermInPlace(a, perm);
        final int[] res = new int[perm.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = perm[i].intValue();
        }
        return res;
    }

    public static void sortPermInPlace(final double[] a, final Integer[] permutation) {
        for (int i = 0; i < permutation.length; i++) {
            permutation[i] = i;
        }
        Arrays.sort(permutation, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return Double.compare(a[o1], a[o2]);
            }
        });
    }

    /**
     * Sorts an double array by given the permutation of the array elements.
     * 
     * @return the permutation of the positions
     */
    public static int[] sort(final double[] a, final int from, final int to) {

        final Integer[] perm = new Integer[to - from];
        for (int i = 0; i < perm.length; i++) {
            perm[i] = i;
        }
        Arrays.sort(perm, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return Double.compare(a[from + o1], a[from + o2]);
            }
        });
        final int[] res = new int[perm.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = perm[i].intValue();
        }
        return res;
    }

    /**
     * Sorts <code>perm</code> according to the given double array. without touching 'a'.
     * 
     * @return the permutation of the positions
     */
    public static void sort(final List<Integer> perm, final double[] a) {
        Collections.sort(perm, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return Double.compare(a[o1], a[o2]);
            }
        });
    }

    public static <T> T[] reorder(final T[] a, final int[] permutation) {
        final T[] res = a.clone();
        for (int i = 0; i < res.length; i++) {
            res[i] = a[permutation[i]];
        }
        return res;
    }

}
