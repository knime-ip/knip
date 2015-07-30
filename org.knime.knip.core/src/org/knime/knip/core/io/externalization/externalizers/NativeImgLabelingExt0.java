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

import java.io.IOException;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.Externalizer;
import org.knime.knip.core.io.externalization.ExternalizerManager;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class NativeImgLabelingExt0 implements Externalizer<ImgLabeling> {

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
    public Class<ImgLabeling> getType() {
        return ImgLabeling.class;
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
    public ImgLabeling read(final BufferedDataInputStream in) throws Exception {

        // for backwards compatibility we read in the old labeling and copy it
        final Img img = ExternalizerManager.<Img> read(in);
        final LabelingMapping mapping = ExternalizerManager.<LabelingMapping> read(in);

        final ImgLabeling<Object, ?> res = new ImgLabeling(KNIPGateway.ops().create().img(img));
        final Cursor<net.imglib2.labeling.LabelingType<?>> c1 = new ExtNativeImgLabeling(img, mapping).cursor();
        final Cursor<LabelingType<Object>> c2 = res.cursor();

        while (c1.hasNext()) {
            c2.next().addAll(c1.next().getLabeling());
        }

        return res;
    }

    private static String readString(final BufferedDataInputStream in) throws IOException {
        final char[] s = new char[in.readInt()];
        in.read(s);
        return new String(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final BufferedDataOutputStream out, final ImgLabeling obj) throws Exception {
        throw new IllegalStateException("this shouldn't ever happen again");
    }

    private class ExtNativeImgLabeling<T extends Comparable<T>, I extends IntegerType<I>> extends
            NativeImgLabeling<T, I> {
        /**
         * @param img
         */
        public ExtNativeImgLabeling(final Img<I> img, final LabelingMapping mapping) {
            super(img);
            super.mapping = mapping;
        }

    }

}
