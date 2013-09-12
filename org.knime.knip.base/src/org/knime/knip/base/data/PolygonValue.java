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
package org.knime.knip.base.data;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.StringValueComparator;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.knip.base.renderer.PolygonValueRenderer;
import org.knime.knip.core.data.algebra.ExtendedPolygon;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public interface PolygonValue extends DataValue {

    /** Gathers meta information to this type. */
    public static final class PolygonUtilityFactory extends UtilityFactory {

        private static final StringValueComparator COMPARATOR = new StringValueComparator();

        /*
         * Specialized icon for PolygonCell
         */
        private static final Icon ICON;

        /*
         * try loading this icon, if fails we use null in the probably
         * silly assumption everyone can deal with that
         */
        static {
            ImageIcon icon;
            try {
                final ClassLoader loader = PolygonValue.class.getClassLoader();
                final String path = PolygonValue.class.getPackage().getName().replace('.', '/');
                icon = new ImageIcon(loader.getResource(path + "/icon/imageicon.png"));
            } catch (final Exception e) {
                icon = null;
            }
            ICON = icon;
        }

        /** Limits scope of constructor, does nothing. */
        protected PolygonUtilityFactory() {
            //
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(PolygonValueRenderer.POLYGON_RENDERER);
        }
    }

    /**
     * Static singleton for meta description.
     * 
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new PolygonUtilityFactory();

    /**
     * To get the polygon hold by this value.
     * 
     * @deprecated Will be removed in future release
     * @return the polygon
     */
    @Deprecated
    public ExtendedPolygon getPolygon();

}
