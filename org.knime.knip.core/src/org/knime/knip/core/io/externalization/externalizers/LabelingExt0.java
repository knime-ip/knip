/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * Created on Jul 2, 2014 by dietzc
 */
package org.knime.knip.core.io.externalization.externalizers;

import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.Externalizer;

/**
 * Default Externalizer for Labelings.
 *
 */
@SuppressWarnings("rawtypes")
@Deprecated
public class LabelingExt0 implements Externalizer<ImgLabeling> {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "LabelingView";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ImgLabeling> getType() {
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
    @SuppressWarnings({"unchecked"})
    @Override
    public ImgLabeling read(final BufferedDataInputStream in) throws Exception {

        int numDims = in.readInt();
        long[] dims = new long[numDims];

        for (int i = 0; i < numDims; i++) {
            dims[i] = in.readLong();
        }
        int numLabels = in.readInt();

        ImgLabeling labeling;
        if (numLabels < Byte.MAX_VALUE * 2) {
            labeling = new ImgLabeling<>(new ArrayImgFactory<UnsignedByteType>().create(dims, new UnsignedByteType()));
        } else if (numLabels < Short.MAX_VALUE * 2) {
            labeling =
                    new ImgLabeling<>(new ArrayImgFactory<UnsignedShortType>().create(dims, new UnsignedShortType()));
        } else {
            labeling = new ImgLabeling<>(new ArrayImgFactory<UnsignedIntType>().create(dims, new UnsignedIntType()));
        }

        ObjectInputStream inStream = new ObjectInputStream(in);
        Cursor<LabelingType> cursor = labeling.cursor();

        try {
            while (cursor.hasNext()) {
                cursor.fwd();

                if (!inStream.readBoolean()) {
                    continue;
                }

                // we don't need this
                inStream.readInt();
                Object[] objs = (Object[])inStream.readObject();
                Set array = new HashSet<>();

                for (int k = 0; k < objs.length; k++) {
                    array.add(objs[k]);
                }

                cursor.get().addAll(array);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return labeling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final BufferedDataOutputStream out, final ImgLabeling obj) throws Exception {
        throw new IllegalStateException("This shouldn't be called anymore");
    }

}
