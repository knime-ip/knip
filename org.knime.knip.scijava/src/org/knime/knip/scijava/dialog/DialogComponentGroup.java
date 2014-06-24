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

import org.knime.core.node.defaultnodesettings.DialogComponent;


/**
 * container for a group of dialog components that are added to a config dialog. If possible the components in the
 * DialogComponentGroup should be added in the order of their array indices under consideration of the placement and
 * border hints.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DialogComponentGroup {

    /**
     * if a class supplies multiple components that should be added to a config dialog it may provide a placement_hint
     * that influences the placement of the components.
     *
     * WRAP_2 means place 2 components horizontal than wrap to the next line ... WRAP_3 same with 3 components
     *
     * @author zinsmaie
     */
    public static enum PLACEMENT_HINT {
        /**
         * Vertical {@link PLACEMENT_HINT}
         */
        VERTICAL,
        /**
         * Horizontal {@link PLACEMENT_HINT}
         */
        HORIZONTAL,
        /**
         * Horizontal wrap 2 {@link PLACEMENT_HINT}
         */
        HORIZ_WRAP_2,
        /**
         * Horizontal wrap 3 {@link PLACEMENT_HINT}
         */
        HORIZ_WRAP_3
    }

    private final DialogComponent[] m_dialogComponents;

    private final PLACEMENT_HINT m_placement;

    private String m_borderText = null;

    /**
     * wrap the dialog component such that it is simply added to the dialog.
     *
     * @param dialogComponent
     */
    public DialogComponentGroup(final DialogComponent dialogComponent) {
        m_dialogComponents = new DialogComponent[]{dialogComponent};
        m_placement = PLACEMENT_HINT.VERTICAL;
    }

    /**
     * wrap the dialog component such that it is added to the dialog with a group border and potentially a border text.
     *
     * @param dialogComponent
     * @param borderText the text of the group border or the empty string for a border without text
     */
    public DialogComponentGroup(final DialogComponent dialogComponent, final String borderText) {
        m_dialogComponents = new DialogComponent[]{dialogComponent};
        m_placement = PLACEMENT_HINT.VERTICAL;
        m_borderText = borderText;
    }

    /**
     * wrap the dialog components such that they are simply added to the. dialog
     *
     * @param dialogComponents
     */
    public DialogComponentGroup(final DialogComponent[] dialogComponents) {
        m_dialogComponents = dialogComponents;
        m_placement = PLACEMENT_HINT.VERTICAL;
    }

    /**
     * wrap the dialog components such that they are added to the dialog with a group border and potentially a border
     * text.
     *
     * @param dialogComponents
     * @param borderText the text of the group border or the empty string for a border without text
     */
    public DialogComponentGroup(final DialogComponent[] dialogComponents, final String borderText) {
        m_dialogComponents = dialogComponents;
        m_placement = PLACEMENT_HINT.VERTICAL;
        m_borderText = borderText;
    }

    /**
     * wrap the dialog components such that they are added to the dialog with a placement hint that specifies their
     * layout.
     *
     * @param dialogComponents
     * @param placement {@link PLACEMENT_HINT}
     */
    public DialogComponentGroup(final DialogComponent[] dialogComponents, final PLACEMENT_HINT placement) {
        m_dialogComponents = dialogComponents;
        m_placement = placement;
    }

    /**
     * wrap the dialog components such that they are added to the dialog with a placement hint that specifies their
     * layout. Additionally specifies a group border and potentially a border text.
     *
     * @param dialogComponents
     * @param placement {@link PLACEMENT_HINT}
     * @param borderText the text of the group border or the empty string for a border without text
     */
    public DialogComponentGroup(final DialogComponent[] dialogComponents, final PLACEMENT_HINT placement,
                                final String borderText) {
        m_dialogComponents = dialogComponents;
        m_placement = placement;
        m_borderText = borderText;
    }

    /**
     * @return true if a box/border should be added that contains the components of this group
     */
    public boolean hasGroupBorder() {
        return (m_borderText != null);
    }

    /**
     * @return a text for the surrounding border if any. The empty string can be interpreted as with border but without
     *         text and null means no border at all.
     */
    public String getGroupBorderText() {
        return m_borderText;
    }

    /**
     * @return the dialog components of the group the array indices represent the order in which the components should
     *         be added
     */
    public DialogComponent[] getDialogComponents() {
        return m_dialogComponents;
    }

    /**
     * @return a {link PLACEMENT_HINT} that specifies how to add the components to a dialog
     */
    public PLACEMENT_HINT getPlacement() {
        return m_placement;
    }
}
