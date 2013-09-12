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
package org.knime.knip.base.data.img;

import java.awt.image.BufferedImage;

import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.Type;

import org.knime.core.node.NodeLogger;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.Externalizer;
import org.knime.knip.core.io.externalization.ExternalizerManager;

/**
 * TODO documentation
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgPlusCellMetadataExt0 implements Externalizer<ImgPlusCellMetadata> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgPlusCellMetadataExt0.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ImgPlusCellMetadata> getType() {
        return ImgPlusCellMetadata.class;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ImgPlusCellMetadata read(final BufferedDataInputStream in) throws Exception {

        // read metadata
        final ImgPlusMetadata metadata = ExternalizerManager.read(in);

        // read number of pixels
        final long size = in.readLong();

        final boolean hasMinimum = in.readBoolean();
        long[] min = null;
        if (hasMinimum) {
            // read minimum
            min = new long[in.readInt()];
            for (int i = 0; i < min.length; i++) {
                min[i] = in.readLong();
            }

        }

        // read dimensions
        final long[] dimensions = new long[in.readInt()];
        for (int i = 0; i < dimensions.length; i++) {
            dimensions[i] = in.readLong();
        }

        // read pixel type class
        final Class<? extends Type> pixType = ExternalizerManager.<Class> read(in);

        // read thumbnail if image exceeds a certain size
        // TODO
        final boolean hasThumbnail = in.readBoolean();
        BufferedImage thumbnail = null;
        if (hasThumbnail) {
            // read thumbnail
            LOGGER.debug("Read thumbnail ...");
            thumbnail = ExternalizerManager.read(in);

        }

        return new ImgPlusCellMetadata(metadata, size, min, dimensions, pixType, thumbnail);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final BufferedDataOutputStream out, final ImgPlusCellMetadata obj) throws Exception {

        // write metadata
        ExternalizerManager.write(out, obj.getMetadata(), ImgPlusMetadata.class);

        // write number of pixels
        out.writeLong(obj.getSize());

        // write minima, if not null
        out.writeBoolean(obj.getMinimum() != null);
        if (obj.getMinimum() != null) {
            out.writeInt(obj.getMinimum().length);
            for (int i = 0; i < obj.getMinimum().length; i++) {
                out.writeLong(obj.getMinimum()[i]);
            }
        }
        // write dimensions
        out.writeInt(obj.getDimensions().length);
        for (int i = 0; i < obj.getDimensions().length; i++) {
            out.writeLong(obj.getDimensions()[i]);
        }

        // write pixel type class
        ExternalizerManager.write(out, obj.getPixelType());

        // write thumbnail if !=null
        if (obj.getThumbnail() != null) {
            LOGGER.debug("Write thumbnail ...");
            out.writeBoolean(true);
            ExternalizerManager.write(out, obj.getThumbnail());
        } else {
            out.writeBoolean(false);
        }

    }

}
