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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.knip.core.awt.labelingcolortable;

import java.util.Random;

import org.apache.mahout.math.map.OpenIntIntHashMap;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class RandomMissingColorHandler implements MissingColorHandler {

    // Fast HashMap implementation
    private static OpenIntIntHashMap m_colorTable = new OpenIntIntHashMap();

    private static int m_generation;

    private static int randomColor() {
        final Random rand = new Random();
        int col = rand.nextInt(255);
        col = col << 8;
        col |= rand.nextInt(255);
        col = col << 8;
        col |= rand.nextInt(255);

        if (col == 0) {
            col = randomColor();
        }

        return col;
    }

    public static <L extends Comparable<L>> void setColor(final L o, final int color) {
        m_colorTable.put(o.hashCode(), color);
        m_generation++;
    }

    public static <L extends Comparable<L>> void resetColor(final L o) {
        m_colorTable.put(o.hashCode(), randomColor());
        m_generation++;
    }

    /**
     * resets the color table such that the label colors can be assigned again. Increases the ColorMapNr to indicate the
     * change.
     */
    public static void resetColorMap() {
        m_colorTable.clear();
        m_generation++;
    }

    /**
     * @return current generation (e.g. needed for caching)
     */
    public static int getGeneration() {
        return m_generation;
    }

    public static <L extends Comparable<L>> int getLabelColor(final L label) {

        final int hashCode = label.hashCode();
        int res = m_colorTable.get(hashCode);
        if (res == 0) {
            res = LabelingColorTableUtils.getTransparentRGBA(randomColor(), 255);
            m_colorTable.put(hashCode, res);
        }

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <L extends Comparable<L>> int getColor(final L label) {
        return RandomMissingColorHandler.getLabelColor(label);
    }

}
