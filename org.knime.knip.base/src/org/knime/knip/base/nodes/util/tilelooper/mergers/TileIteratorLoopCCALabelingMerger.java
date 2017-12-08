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
 * Created on 7 Dec 2017 by bw
 */
package org.knime.knip.base.nodes.util.tilelooper.mergers;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.knime.knip.base.nodes.util.tilelooper.imglib2.ArrangedView;
import org.knime.knip.core.KNIPGateway;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 *
 * @author Benjamin Wilhelm
 */
public class TileIteratorLoopCCALabelingMerger<L extends Comparable<L>>
        extends TileIteratorLoopLabelingMerger<L, Integer> {

    private AtomicInteger labelGenerator = new AtomicInteger();

    private Map<Key, Integer> mapping;

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<LabelingType<Integer>> mergeTiles() {
        // Initialize the mapping
        mapping = new ConcurrentHashMap<>();

        // Get the tile size
        long[] tileSize = Intervals.dimensionsAsLongArray(tiles.get(0));
        int n = m_imgSize.length;

        // Arrange the tiles
        ArrangedView<RandomAccessibleInterval<LabelingType<L>>> arrangedView = new ArrangedView<>(tiles, m_grid);

        // Process borders TODO remove if the other approach works
        //        for (int d = 0; d < n; d++) {
        //            for (int i = 0; i + 1 < m_grid[d]; i++) {
        //                // Get cursors which loop over all tiles which have the index i and i+1 in dimension d
        //                Cursor<RandomAccessibleInterval<LabelingType<L>>> cursor1 =
        //                        Views.flatIterable(Views.hyperSlice(arrangedView, d, i)).cursor();
        //                Cursor<RandomAccessibleInterval<LabelingType<L>>> cursor2 =
        //                        Views.flatIterable(Views.hyperSlice(arrangedView, d, i + 1)).cursor();
        //
        //                // Loop over all tiles which have a common border in dimension d at index i/i+1
        //                while (cursor1.hasNext()) {
        //                    RandomAccessibleInterval<LabelingType<L>> left = cursor1.next();
        //                    RandomAccessibleInterval<LabelingType<L>> right = cursor2.next();
        //                    IterableInterval<LabelingType<L>> borderLeft =
        //                            Views.flatIterable(Views.hyperSlice(left, d, left.max(d)));
        //                    IterableInterval<LabelingType<L>> borderRight =
        //                            Views.flatIterable(Views.hyperSlice(right, d, right.min(d)));
        //                    // TODO find the index of the tile
        //                    // TODO parallelize
        //                    processBorder(cursor1, cursor2, borderLeft, borderRight);
        //                }
        //            }
        //        }

        Cursor<RandomAccessibleInterval<LabelingType<L>>> tileCursor = arrangedView.cursor();
        RandomAccess<RandomAccessibleInterval<LabelingType<L>>> tileRandomAccess = arrangedView.randomAccess();

        while (tileCursor.hasNext()) {
            RandomAccessibleInterval<LabelingType<L>> leftTile = tileCursor.next();

            // For each dimension where there is a tile right of this tile: process the border
            for (int d = 0; d < n && tileCursor.getLongPosition(d) + 1 < m_grid[d]; d++) {
                // Position the random access and get the other tile
                tileRandomAccess.setPosition(tileCursor);
                tileRandomAccess.fwd(d);
                RandomAccessibleInterval<LabelingType<L>> rightTile = tileRandomAccess.get();

                // Get the borders of both tiles
                IterableInterval<LabelingType<L>> borderLeft =
                        Views.flatIterable(Views.hyperSlice(leftTile, d, leftTile.max(d)));
                IterableInterval<LabelingType<L>> borderRight =
                        Views.flatIterable(Views.hyperSlice(rightTile, d, rightTile.min(d)));

                // Process the border
                processBorder(tileCursor, tileRandomAccess, borderLeft, borderRight);
            }
        }

        // Create new labeling
        RandomAccessibleInterval<LabelingType<Integer>> lab =
                KNIPGateway.ops().create().<Integer, IntType> imgLabeling(new FinalInterval(m_imgSize), new IntType());

        tileCursor.reset();

        while (tileCursor.hasNext()) {
            RandomAccessibleInterval<LabelingType<L>> tileLab = tileCursor.next();
            // Calculate the offset of the tile
            long[] offset =
                    IntStream.range(0, n).mapToLong((d) -> tileCursor.getLongPosition(d) * tileSize[d]).toArray();

            // Get a view on the result where the indices are the same as in the tile
            IterableInterval<LabelingType<Integer>> resTile =
                    Views.zeroMin(Views.interval(lab, Intervals.intersect(Views.translate(tileLab, offset), lab)));
            Cursor<LabelingType<Integer>> pixelCursor = resTile.cursor();
            RandomAccess<LabelingType<L>> randomAccess = tileLab.randomAccess();
            while (pixelCursor.hasNext()) {
                pixelCursor.fwd();
                randomAccess.setPosition(pixelCursor);
                LabelingType<L> from = randomAccess.get();
                if (!from.isEmpty()) {
                    LabelingType<Integer> to = pixelCursor.get();
                    to.add(getLabel(tileCursor, from.getIndex()));
                }
            }
        }

        return lab;
    }

    // TODO parallelize
    private void processBorder(final Localizable tile1Idx, final Localizable tile2Idx,
                               final IterableInterval<LabelingType<L>> border1,
                               final IterableInterval<LabelingType<L>> border2) {
        Cursor<LabelingType<L>> cursor1 = border1.cursor();
        Cursor<LabelingType<L>> cursor2 = border2.cursor();

        while (cursor1.hasNext() && cursor2.hasNext()) {
            LabelingType<L> p1 = cursor1.next();
            LabelingType<L> p2 = cursor2.next();
            processBorderPixels(tile1Idx, tile2Idx, p1, p2);
        }
    }

    // TODO maybe move it somewhere...
    // TODO synchronize
    private void processBorderPixels(final Localizable tile1Idx, final Localizable tile2Idx, final LabelingType<L> p1,
                                     final LabelingType<L> p2) {
        // If one of the labels is empty there is nothing to merge
        if (p1.isEmpty() || p2.isEmpty()) {
            return;
        }
        // They are both not empty: We have to merge them
        // First check if they are both or one of them is already mapped to something
        Key key1 = new Key(tile1Idx, p1.getIndex());
        Key key2 = new Key(tile2Idx, p2.getIndex());
        boolean contains1 = mapping.containsKey(key1);
        boolean contains2 = mapping.containsKey(key2);
        // Both are already mapped to something
        if (contains1 && contains2) {
            if (mapping.get(key1).equals(mapping.get(key2))) {
                // They are mapped to the same thing. Nothing to do anymore
                return;
            }
            // Worst possible case: Both are mapped already but not to the same labels
            // Map everything connected to the label of the first pixel
            Integer label = mapping.get(key1);
            for (Map.Entry<Key, Integer> entry : mapping.entrySet()) {
                if (entry.getValue().equals(mapping.get(key1))) {
                    mapping.put(entry.getKey(), label);
                }
                if (entry.getValue().equals(mapping.get(key2))) {
                    mapping.put(entry.getKey(), label);
                }
            }
            return;
        }

        // Only the first is already mapped: We map the second to the same
        if (contains1) {
            mapping.put(key2, mapping.get(key1));
            return;
        }
        // Only the second is already mapped: We map the first to the same
        if (contains2) {
            mapping.put(key1, mapping.get(key2));
            return;
        }

        // They are not mapped to anything. We have to add a new label for them
        // TODO: Do we need to make sure this label isn't taken yet? Or is this already the case?
        Integer label = new Integer(labelGenerator.incrementAndGet());
        mapping.put(key1, label);
        mapping.put(key2, label);
    }

    /**
     * TODO write javadoc
     *
     * @param tileCursor
     * @param labelIdx
     * @return
     */
    private Integer getLabel(final Localizable tileIdx, final IntegerType<?> labelIdx) {
        Key key = new Key(tileIdx, labelIdx);
        if (mapping.containsKey(key)) {
            return mapping.get(key);
        } else {
            return createLabel(key);
        }
    }

    /**
     * TODO javadoc
     *
     * @param key
     * @return
     */
    private synchronized Integer createLabel(final Key key) {
        // Check if it was created in the meantime
        if (mapping.containsKey(key)) {
            return mapping.get(key);
        } else {
            Integer label = labelGenerator.incrementAndGet();
            mapping.put(key, label);
            return label;
        }
    }

    private class Key {

        private long[] value;

        private Key(final Localizable tileIdx, final IntegerType<?> labelIdx) {
            value = new long[tileIdx.numDimensions() + 1];
            for (int i = 0; i < tileIdx.numDimensions(); i++) {
                value[i] = tileIdx.getLongPosition(i);
            }
            value[tileIdx.numDimensions()] = labelIdx.getIntegerLong();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof TileIteratorLoopCCALabelingMerger.Key) {
                TileIteratorLoopCCALabelingMerger.Key point = (TileIteratorLoopCCALabelingMerger.Key)obj;
                if (value.length == point.value.length) {
                    for (int i = 0; i < value.length; i++) {
                        if (point.value[i] != value[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }
}
