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

import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable16;
import net.imglib2.display.ColorTable8;
import net.imglib2.meta.ImageMetadata;

import org.knime.knip.core.data.img.DefaultImageMetadata;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.Externalizer;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImageMetadataExt1 implements Externalizer<ImageMetadata> {

    private enum ColorTables {
        ColorTable8, ColorTable16
    };

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
        return 1;
    }

    @Override
    public ImageMetadata read(final BufferedDataInputStream in) throws Exception {
        final DefaultImageMetadata obj = new DefaultImageMetadata();

        // Valid bits are deserialized
        obj.setValidBits(in.readInt());

        // Channel Min/Max are deserialized
        final int numChannels = in.readInt();

        for (int c = 0; c < numChannels; c++) {
            obj.setChannelMinimum(c, in.readDouble());
            obj.setChannelMaximum(c, in.readDouble());
        }

        // Colortables are deserialized
        final int numColorTables = in.readInt();
        obj.initializeColorTables(numColorTables);

        for (int t = 0; t < numColorTables; t++) {

            if (in.readBoolean()) {
                final int componentCount = in.readInt();
                final int length = in.readInt();

                switch (ColorTables.values()[in.readInt()]) {
                    case ColorTable8:
                        final byte[][] ct8 = new byte[componentCount][length];

                        for (int c = 0; c < componentCount; c++) {
                            for (int k = 0; k < length; k++) {
                                ct8[c][k] = in.readByte();
                            }
                        }

                        obj.setColorTable(new ColorTable8(ct8), t);
                        break;
                    case ColorTable16:
                        final short[][] ct16 = new short[componentCount][length];

                        for (int c = 0; c < componentCount; c++) {
                            for (int k = 0; k < length; k++) {
                                ct16[c][k] = in.readShort();
                            }
                        }

                        obj.setColorTable(new ColorTable16(ct16), t);
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Fatal error! Unknown ColorTable in ImageMetadataExt1.java! Please contact Administrators!");
                }

            }
        }

        return obj;
    }

    @Override
    public void write(final BufferedDataOutputStream out, final ImageMetadata obj) throws Exception {

        // Valid bits
        out.writeInt(obj.getValidBits());

        // Channels are serialized
        final int numChannels = obj.getCompositeChannelCount();
        out.writeInt(numChannels);

        for (int c = 0; c < numChannels; c++) {
            out.writeDouble(obj.getChannelMinimum(c));
            out.writeDouble(obj.getChannelMaximum(c));
        }

        // Color Tables are serialized
        final int numTables = obj.getColorTableCount();

        out.writeInt(numTables);

        for (int t = 0; t < numTables; t++) {
            final ColorTable table = obj.getColorTable(t);
            out.writeBoolean(table != null);

            if (table != null) {

                out.writeInt(table.getComponentCount());
                out.writeInt(table.getLength());
                if (table instanceof ColorTable8) {
                    out.writeInt(ColorTables.ColorTable8.ordinal());
                    for (int c = 0; c < table.getComponentCount(); c++) {
                        for (int k = 0; k < table.getLength(); k++) {
                            out.writeByte(((ColorTable8)table).getNative(c, k));
                        }
                    }
                } else if (table instanceof ColorTable16) {
                    out.writeInt(ColorTables.ColorTable16.ordinal());
                    for (int c = 0; c < table.getComponentCount(); c++) {
                        for (int k = 0; k < table.getLength(); k++) {
                            out.writeShort(((ColorTable16)table).getNative(c, k));
                        }
                    }
                }
            }
        }

    }
}
