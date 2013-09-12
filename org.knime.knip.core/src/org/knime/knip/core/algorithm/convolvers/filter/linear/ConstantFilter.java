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
package org.knime.knip.core.algorithm.convolvers.filter.linear;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ValuePair;

/**
 * Constant kernels of image filters.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Stephan Sellien, University of Konstanz
 */
@SuppressWarnings("unchecked")
public enum ConstantFilter {
    /**
     * Sobel filter.
     */
    Sobel("Sobel", new ValuePair<String, int[][]>("X", new int[][]{{1, 0, -1}, {2, 0, -2}, {1, 0, -1}}),
            new ValuePair<String, int[][]>("Y", new int[][]{{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}})),
    /**
     * Sharpening filter (discrete Laplacian).
     */
    Laplacian("Laplacian", new ValuePair<String, int[][]>("normal", new int[][]{{0, 1, 0}, {1, -4, 1}, {0, 1, 0}}),
            new ValuePair<String, int[][]>("diagonal", new int[][]{{1, 1, 1}, {1, -8, 1}, {1, 1, 1}})), /**
     * 
     * 
     * 
     * 
     * 
     * Prewitt
     * operator.
     */
    Prewitt("Prewitt", new ValuePair<String, int[][]>("X", new int[][]{{-1, 0, 1}, {-1, 0, 1}, {-1, 0, 1}}),
            new ValuePair<String, int[][]>("Y", new int[][]{{-1, -1, -1}, {0, 0, 0}, {1, 1, 1}})),
    /**
     * Roberts operator.
     */
    Roberts("Roberts", new ValuePair<String, int[][]>("X", new int[][]{{-1, 0}, {0, 1}}),
            new ValuePair<String, int[][]>("Y", new int[][]{{0, -1}, {1, 0}})),
    /** Kirsch-Operator (http://en.wikipedia.org/wiki/Kirsch_operator) */
    Kirsch("Kirsch", new ValuePair<String, int[][]>("1", new int[][]{{5, 5, 5}, {-3, 0, -3}, {-3, -3, -3}}),
            new ValuePair<String, int[][]>("2", new int[][]{{5, 5, -3}, {5, 0, -3}, {-3, -3, -3}}),
            new ValuePair<String, int[][]>("3", new int[][]{{5, -3, -3}, {5, 0, -3}, {5, -3, -3}}),
            new ValuePair<String, int[][]>("4", new int[][]{{-3, -3, -3}, {5, 0, -3}, {5, 5, -3}}),

            new ValuePair<String, int[][]>("5", new int[][]{{-3, -3, -3}, {-3, 0, -3}, {5, 5, 5}}),
            new ValuePair<String, int[][]>("6", new int[][]{{-3, -3, -3}, {-3, 0, 5}, {-3, 5, 5}}),

            new ValuePair<String, int[][]>("7", new int[][]{{-3, -3, 5}, {-3, 0, 5}, {-3, -3, 5}}),
            new ValuePair<String, int[][]>("8", new int[][]{{-3, 5, 5}, {-3, 0, 5}, {-3, -3, -3}})),
    /** Frei&Chen (http://www.roborealm.com/help/Frei_Chen.php) */
    FreiAndChen("Frei & Chen", new ValuePair<String, int[][]>("1", new int[][]{{2, 3, 4}, {0, 0, 0}, {-2, -3, -2}}),
            new ValuePair<String, int[][]>("2", new int[][]{{2, 0, -2}, {0, -2, 3}, {3, -2, 0}}),
            new ValuePair<String, int[][]>("3", new int[][]{{3, 0, -3}, {2, 0, -2}, {-2, 0, 2}}),
            new ValuePair<String, int[][]>("4", new int[][]{{2, 0, -2}, {-3, 2, 0}, {0, 2, -3}}));

    private ValuePair<String, int[][]>[] m_variants;

    private String m_displayName;

    private ConstantFilter(final String displayName, final ValuePair<String, int[][]>... variants) {
        m_displayName = displayName;
        m_variants = variants;
    }

    // convenience
    private ConstantFilter(final String displayName, final int[][] matrix) {
        this(displayName, new ValuePair<String, int[][]>(displayName, matrix));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_displayName;
    }

    /**
     * Creates the kernel image for the given variant.
     * 
     * @param variantName the name of the variant
     * @return the filter
     */
    public Img<DoubleType> createImage(final String variantName) {
        for (int i = 0; i < m_variants.length; i++) {
            if (m_variants[i].a.equals(variantName)) {
                return createImage(i);
            }
        }
        throw new IllegalArgumentException("Variant name must be valid.");
    }

    /**
     * Creates an {@link Img} for the selected filter containing its kernel.
     * 
     * @param index index of the variant
     * 
     * @return the kernel as {@link Img}
     */
    public Img<DoubleType> createImage(final int index) {
        if (index < 0 || index >= m_variants.length) {
            throw new IllegalArgumentException("Index must be valid.");
        }
        int[][] matrix = m_variants[index].b;
        Img<DoubleType> img =
                new ArrayImgFactory<DoubleType>().create(new long[]{matrix.length, matrix[0].length}, new DoubleType());
        RandomAccess<DoubleType> ra = img.randomAccess();
        for (int y = 0; y < matrix.length; y++) {
            for (int x = 0; x < matrix[y].length; x++) {
                ra.setPosition(new int[]{x, y});
                ra.get().set(matrix[y][x]);
            }
        }
        return img;
    }

    /**
     * Get the names of the variants.
     * 
     * @return the names
     */
    public String[] getVariantNames() {
        String[] variantNames = new String[m_variants.length];
        for (int i = 0; i < m_variants.length; i++) {
            variantNames[i] = m_variants[i].a;
        }
        return variantNames;
    }
}
