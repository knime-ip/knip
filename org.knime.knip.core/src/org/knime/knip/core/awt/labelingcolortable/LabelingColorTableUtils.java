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
package org.knime.knip.core.awt.labelingcolortable;

import java.awt.Color;
import java.util.List;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class LabelingColorTableUtils {

    public static final Color HILITED = Color.ORANGE;

    public static final int HILITED_RGB = HILITED.getRGB();

    public static final Color HILITED_SELECTED = new Color(255, 240, 204);

    public static final Color STANDARD = new Color(240, 240, 240);

    public static final Color SELECTED = new Color(179, 168, 143);

    public static final Color NOTSELECTED = Color.LIGHT_GRAY;

    public static final int NOTSELECTED_RGB = NOTSELECTED.getRGB();

    /** color that is used for bounding boxes e.g. for label bbs. */
    private static Color m_boundingBoxColor = Color.YELLOW;

    /**
     * Globally set the Color of the BoundingBox in the Renderer
     * 
     * @param color
     */
    public static void setBoundingBoxColor(final Color color) {
        m_boundingBoxColor = color;
    }

    /**
     * 
     * @return Color for the bounding box
     */
    public static Color getBoundingBoxColor() {
        return m_boundingBoxColor;
    }

    /**
     * TODO
     * 
     * @param rgb
     * @param transparency
     * @return
     */
    public static int getTransparentRGBA(final int rgb, final int transparency) {
        return (transparency << 24) | (rgb & 0x00FFFFFF);
    }

    /**
     * TODO
     * 
     * @param r
     * @param g
     * @param b
     * @return the color
     */
    public static int getIntColorRepresentation(int r, final int g, final int b) {
        r = r << 8;
        r |= g;
        r = r << 8;
        r |= b;

        return r;
    }

    /**
     * TODO
     * 
     * @param table
     * @param labels
     * @return
     */
    public static <L extends Comparable<L>> int getAverageColor(final LabelingColorTable table, final List<L> labels) {

        double totalRes = 0;
        for (int i = 0; i < labels.size(); i++) {
            totalRes += (double)table.getColor(labels.get(i)) / labels.size();
        }

        return (int)totalRes;
    }

    /**
     * TODO: Returns an {@link ExtendedLabelingColorTable}.
     * 
     * @param table
     * @param handler
     * @return
     */
    public static <L extends Comparable<L>> ExtendedLabelingColorTable
            extendLabelingColorTable(final LabelingColorTable table, final MissingColorHandler handler) {
        return new ExtendedLabelingColorTable(table, handler);
    }

}
