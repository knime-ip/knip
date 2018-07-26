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
package org.knime.knip.core.io.externalization.externalizers;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.knip.core.data.img.DefaultImageMetadata;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.Externalizer;

import net.imagej.ImageMetadata;
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable16;
import net.imglib2.display.ColorTable8;

/**
 * {@link Externalizer} for {@link ImageMetadata}. De-duplicates color tables before saving, which can save a lot of
 * memory on images with many planes.
 *
 * @author Gabriel
 */
public class ImageMetadataExt3 implements Externalizer<ImageMetadata> {

    private enum ColorTables {
        ColorTable8, ColorTable16
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Class<ImageMetadata> getType() {
        return ImageMetadata.class;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public ImageMetadata read(final BufferedDataInputStream in) throws Exception {
        final DefaultImageMetadata meta = new DefaultImageMetadata();

        // Valid bits are deserialized
        meta.setValidBits(in.readInt());

        // Channel Min/Max are deserialized
        final int numChannels = in.readInt();

        for (int c = 0; c < numChannels; c++) {
            meta.setChannelMinimum(c, in.readDouble());
            meta.setChannelMaximum(c, in.readDouble());
        }

        // Colortables are deserialized
        final int numColorTables = in.readInt();
        final int numDedupedColorTables = in.readInt();

        meta.initializeColorTables(numColorTables);

        for (int t = 0; t < numDedupedColorTables; t++) {

            // read the number and idx of duplicate color tables
            final int dupcount = in.readInt();
            final int[] dupIdxes = new int[dupcount];
            in.read(dupIdxes);

            ColorTable ct = null;

            final boolean nonNullColorTable = in.readBoolean();
            if (nonNullColorTable) {

                // read the color table
                final int componentCount = in.readInt();
                final int length = in.readInt();

                final ColorTables colorTables = ColorTables.values()[in.readInt()];
                if (colorTables == ColorTables.ColorTable8) {
                    final byte[][] ct8 = new byte[componentCount][length];
                    for (int c = 0; c < componentCount; c++) {
                        in.read(ct8[c]);
                    }
                    ct = new ColorTable8(ct8);
                } else if (colorTables == ColorTables.ColorTable16) {
                    final short[][] ct16 = new short[componentCount][length];
                    for (int c = 0; c < componentCount; c++) {
                        in.read(ct16[c]);
                    }
                    ct = new ColorTable16(ct16);
                } else {
                    throw new IllegalArgumentException(
                            "Fatal error! Unknown ColorTable in ImageMetadataExt3.java! Please contact Administrators!");
                }
            }

            // set the color table
            for (final int idx : dupIdxes) {
                meta.setColorTable(ct, idx);
            }
        }

        // read the properties
        final int numObjects = in.readInt();
        final ObjectInputStream ois = new ObjectInputStream(in);
        for (int n = 0; n < numObjects; n++) {
            meta.getProperties().put((String)ois.readObject(), ois.readObject());
        }

        return meta;
    }

    @Override
    public void write(final BufferedDataOutputStream out, final ImageMetadata meta) throws Exception {

        // Valid bits
        out.writeInt(meta.getValidBits());

        // Channels are serialized
        final int numChannels = meta.getCompositeChannelCount();
        out.writeInt(numChannels);

        for (int c = 0; c < numChannels; c++) {
            out.writeDouble(meta.getChannelMinimum(c));
            out.writeDouble(meta.getChannelMaximum(c));
        }

        // Color Tables are serialized
        final int numTables = meta.getColorTableCount();
        out.writeInt(numTables);

        // occurences list
        final Map<ColorTable, List<Integer>> tables = deduplicateColorTables(meta);

        // number of deduped tables
        out.writeInt(tables.size());

        for (final Entry<ColorTable, List<Integer>> entry : tables.entrySet()) {
            final ColorTable table = entry.getKey();
            final int[] occurences = toPrimitiveIntArray(entry.getValue());

            out.writeInt(occurences.length); // number of duplicates
            out.write(occurences); // the indexes of the duplicates

            out.writeBoolean(table != null);

            if (table != null) {
                // write the color table
                out.writeInt(table.getComponentCount());
                out.writeInt(table.getLength());
                if (table instanceof ColorTable8) {
                    out.writeInt(ColorTables.ColorTable8.ordinal());

                    final byte[][] values = ((ColorTable8)table).getValues();

                    for (int s = 0; s < values.length; s++) {
                        out.write(values[s]);
                    }
                } else if (table instanceof ColorTable16) {
                    out.writeInt(ColorTables.ColorTable16.ordinal());

                    final short[][] values = ((ColorTable16)table).getValues();

                    for (int s = 0; s < values.length; s++) {
                        out.write(values[s]);
                    }
                }
            }
        }

        // write metadata properties
        out.writeInt(meta.getProperties().size());

        final ObjectOutputStream oos = new ObjectOutputStream(out);
        for (final Entry<String, Object> entry : meta.getProperties().entrySet()) {
            oos.writeObject(entry.getKey());
            oos.writeObject(entry.getValue());
        }
    }

    /**
     * Deduplicates the color-tables contained in the given {@link ImageMetadata};
     *
     * @param obj the {@link ImageMetadata} which may contain duplicate color tables
     * @return a Map which maps a {@link ColorTable} to all it's duplicate occurrences.
     */
    private Map<ColorTable, List<Integer>> deduplicateColorTables(final ImageMetadata obj) {
        final Map<ColorTable, List<Integer>> tables = new HashMap<>();

        final int numTables = obj.getColorTableCount();
        for (int t = 0; t < numTables; t++) {
            final ColorTable ct = obj.getColorTable(t);
            if (tables.containsKey(ct)) {
                tables.get(ct).add(t);
            } else {
                final List<Integer> occurences = new ArrayList<>();
                occurences.add(t);
                tables.put(ct, occurences);
            }
        }
        return tables;
    }

    /**
     * Transforms a {@code List<Integer>} to a {@code{int[]}.
     *
     * @param list the list
     * @return the array
     */
    private int[] toPrimitiveIntArray(final List<Integer> list) {
        final int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }
}
