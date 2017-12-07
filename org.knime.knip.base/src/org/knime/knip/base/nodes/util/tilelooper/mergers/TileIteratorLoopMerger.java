/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
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
 * Created on 6 Dec 2017 by bw
 */
package org.knime.knip.base.nodes.util.tilelooper.mergers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 *
 * @author Benjamin Wilhelm
 */
public abstract class TileIteratorLoopMerger<T> {

    protected long[] m_startGrid;

    protected long[] m_startOverlap;

    protected long[] m_startImgSize;

    protected List<RandomAccessibleInterval<T>> tiles;

    protected long[] m_grid;

    protected long[] negOverlap;

    protected long[] m_imgSize;

    /**
     * TODO javadoc
     */
    public void initialize(final long[] startGrid, final long[] startOverlap, final long[] startImgSize) {
        m_startGrid = startGrid;
        m_startOverlap = startOverlap;
        m_startImgSize = startImgSize;
        tiles = new ArrayList<>();
    }
    /**
     * TODO add javadoc
     *
     * @param tile
     */
    public void addTile(final RandomAccessibleInterval<T> tile) {
        if (tiles.isEmpty()) {
            // Create the dimensions with the first tile
            // The user is allowed to remove dimensions in the end where the grid is 1 and overlap is 0.
            // The user is allowed to add dimensions to the end.

            // Number of dimensions at the start node
            final int startN = m_startImgSize.length;
            final int newN = tile.numDimensions();

            if (newN > startN) {
                // If we have more dimensions now, add singleton dimensions to the grid
                // and zeros to the overlap
                m_grid = IntStream.range(0, newN).mapToLong(i -> i < startN ? m_startGrid[i] : 1).toArray();
                negOverlap = IntStream.range(0, newN).mapToLong(i -> i < startN ? -m_startOverlap[i] : 0).toArray();
                m_imgSize = IntStream.range(0, newN)
                        .mapToLong(i -> i < startN ? m_startImgSize[i] : tile.dimension(i)).toArray();
            } else if (newN < startN) {
                // If we have less dimensions now, check if we only removed dimensions
                // with grid = 1 and overlap = 0
                if (!Arrays.stream(m_startGrid, newN, startN).allMatch(g -> g == 1)) {
                    throw new IllegalStateException("Removed dimension where grid is not 1.");
                }
                if (!Arrays.stream(m_startOverlap, newN, startN).allMatch(o -> o == 0)) {
                    throw new IllegalStateException("Removed dimension where overlap is not 0.");
                }
                m_grid = Arrays.copyOf(m_startGrid, newN);
                negOverlap = Arrays.stream(m_startOverlap, 0, newN).map(l -> -l).toArray();
                m_imgSize = Arrays.copyOf(m_startImgSize, newN);
            } else {
                // We have the same dimensions... That's easy
                m_grid = m_startGrid.clone();
                negOverlap = Arrays.stream(m_startOverlap).map(l -> -l).toArray();
                m_imgSize = m_startImgSize.clone();
            }
        }  else {
            // Not the first tile. Make sure that this tile has the same number of dimensions
            if (tile.numDimensions() != m_grid.length) {
                throw new IllegalStateException(
                        "The table contains tiles with different number of dimensions.");
            }
        }

        // TODO remove overlap
       IntervalView<T> croppedTile = Views.zeroMin(Views.expandBorder(tile, negOverlap));

        tiles.add(croppedTile);
    }

    /**
     * TODO javadoc
     * @return
     */
    public boolean isEmpty() {
        return tiles.isEmpty();
    }

    public abstract RandomAccessibleInterval<T> mergeTiles();
}
