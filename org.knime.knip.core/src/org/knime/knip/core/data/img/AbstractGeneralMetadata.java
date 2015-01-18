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
package org.knime.knip.core.data.img;

import java.util.ArrayList;
import java.util.List;

import net.imagej.Sourced;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.space.AbstractCalibratedSpace;
import net.imagej.space.CalibratedSpace;
import net.imglib2.ops.util.MetadataUtil;

import org.knime.knip.core.data.DefaultNamed;
import org.knime.knip.core.data.DefaultSourced;
import org.scijava.Named;

/**
 * Shared Metadata of Labeling and Img
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
abstract class AbstractGeneralMetadata extends AbstractCalibratedSpace<CalibratedAxis> implements Sourced, Named {

    private static final AxisType[] DEFAULT_AXES = {Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL, Axes.TIME};

    private final Named m_named;

    private final Sourced m_sourced;

    public AbstractGeneralMetadata(final int numDimensions) {
        super(defaultAxes(numDimensions));
        this.m_named = new DefaultNamed();
        this.m_sourced = new DefaultSourced();
    }

    /**
     * @param numDimensions
     * @return
     */
    private static List<CalibratedAxis> defaultAxes(final int numDimensions) {
        final List<CalibratedAxis> axes = new ArrayList<CalibratedAxis>();

        for (int d = 0; d < numDimensions; d++) {

            if (d < DEFAULT_AXES.length) {
                axes.add(new DefaultLinearAxis(DEFAULT_AXES[d]));
            } else {
                axes.add(null);
            }
        }

        return axes;
    }

    public AbstractGeneralMetadata(final CalibratedSpace<CalibratedAxis> space, final Named named, final Sourced sourced) {
        this(space.numDimensions());

        MetadataUtil.copyName(named, m_named);
        MetadataUtil.copySource(sourced, m_sourced);
        MetadataUtil.copyTypedSpace(space, this);
    }

    @Override
    public String getName() {
        return m_named.getName();
    }

    @Override
    public void setName(final String name) {
        m_named.setName(name);
    }

    @Override
    public String getSource() {
        return m_sourced.getSource();
    }

    @Override
    public void setSource(final String source) {
        this.m_sourced.setSource(source);
    }

}
