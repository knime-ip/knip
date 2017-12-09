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
 * Created on 7 Dec 2017 by Benjamin Wilhelm
 */
package org.knime.knip.base.nodes.util.tilelooper.mergers;

import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
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

    private LabelGenerator labelGenerator = new LabelGenerator();

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

        Cursor<RandomAccessibleInterval<LabelingType<L>>> tileCursor = arrangedView.cursor();
        RandomAccess<RandomAccessibleInterval<LabelingType<L>>> tileRandomAccess = arrangedView.randomAccess();

        while (tileCursor.hasNext()) {
            RandomAccessibleInterval<LabelingType<L>> leftTile = tileCursor.next();

            // For each dimension where there is a tile right of this tile: process the border
            for (int d = 0; d < n; d++) {
                if (tileCursor.getLongPosition(d) + 1 < m_grid[d]) {
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

        // Both are already mapped to something
        if (mapping.get(key1) != null && mapping.get(key1).equals(mapping.get(key2))) {
            // They are mapped to the same thing. Nothing to do anymore
            return;
        }
        // We need to adjust the mapping. This needs to happen synchronized.
        matchLabels(key1, key2);
    }

    private synchronized void matchLabels(final Key key1, final Key key2) {
        Integer label1 = mapping.get(key1);
        Integer label2 = mapping.get(key2);
        if (label1 != null && label1.equals(label2)) {
            return; // They where mapped to the same thing in the meantime
        }
        if (label1 != null && label2 != null) {
            // Worst possible case: Both are mapped already but not to the same labels
            // Map everything connected to the label of the first pixel
            for (Map.Entry<Key, Integer> entry : mapping.entrySet()) {
                if (entry.getValue().equals(label1)) {
                    mapping.put(entry.getKey(), label1);
                } else if (entry.getValue().equals(label2)) {
                    mapping.put(entry.getKey(), label1);
                }
            }
            labelGenerator.freeLabel(label2);
            return;
        }

        // Only the first is already mapped: We map the second to the same
        if (label1 != null) {
            mapping.put(key2, label1);
            return;
        }
        // Only the second is already mapped: We map the first to the same
        if (label2 != null) {
            mapping.put(key1, label2);
            return;
        }

        // They are not mapped to anything. We have to add a new label for them
        Integer label = labelGenerator.nextLabel();
        mapping.put(key1, label);
        mapping.put(key2, label);
    }

    /**
     * Get the label for the given tile and label index. Creates a new one if there is none yet.
     *
     * @param tileIdx index of the tile
     * @param labelIdx index of the label
     * @return the label for this tile and label index.
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
     * Creates a new label with the key if no label for the key was generated yet. Similar to
     * {@link #getLabel(Localizable, IntegerType)} but synchronized. Use {@link #getLabel(Localizable, IntegerType)} as
     * it will call the synchronized version when necessary.
     *
     * @param key
     * @return the new label.
     */
    private synchronized Integer createLabel(final Key key) {
        // Check if it was created in the meantime
        if (mapping.containsKey(key)) {
            return mapping.get(key);
        } else {
            Integer label = labelGenerator.nextLabel();
            mapping.put(key, label);
            return label;
        }
    }

    /**
     * Small class to use as key in a mapping from labels of specific tiles to merged labels.
     */
    private class Key {

        private long[] value;

        /**
         * Create a new key with the given indices.
         *
         * @param tileIdx Index of the tile
         * @param labelIdx Index of the label
         */
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
                @SuppressWarnings("unchecked")
                Key point = (Key)obj;
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Tile: " + Arrays.toString(Arrays.copyOf(value, value.length - 1)) + ", Label index: "
                    + value[value.length - 1];
        }
    }

    /**
     * Helper class which generates integer labels. A label can also be freed if it isn't used anymore and it will be
     * returned again.
     */
    private class LabelGenerator {

        private int next = 1;

        private SortedSet<Integer> deleted = new TreeSet<>();

        /**
         * Create a label which is not used yet.
         *
         * @return A new label;
         */
        private synchronized Integer nextLabel() {
            if (!deleted.isEmpty()) {
                Integer label = deleted.first();
                deleted.remove(label);
                return label;
            } else {
                return next++;
            }
        }

        /**
         * Frees a label such that is can be created again. The caller needs to make sure that this label isn't used
         * anymore.
         *
         * @param label to free
         */
        private synchronized void freeLabel(final Integer label) {
            deleted.add(label);
        }
    }
}
