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
package org.knime.knip.base.nodes.testing;

import net.imglib2.Cursor;
import net.imglib2.img.ImgView;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataRow;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * Compares to {@link ImgPlus}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T>
 */
public class ImgComparatorNodeModel<T extends NativeType<T> & RealType<T>> extends
        ComparatorNodeModel<ImgPlusValue<T>, ImgPlusValue<T>> {

    //TODO until ImgLib is able to create cursors from RandomAccessibleIntervals in certain iteration orders (e.g. CellIterationOrder) we need to check if there exists an ImgView
    @Override
    protected void compare(final DataRow row, final ImgPlusValue<T> vin1, final ImgPlusValue<T> vin2) {

        final ImgPlus<T> img1 = vin1.getImgPlus();
        final ImgPlus<T> img2 = vin2.getImgPlus();

        if (img1.factory().getClass() != img2.factory().getClass()) {
            throw new IllegalStateException("Factory of the images are not the same! " + row.getKey().toString());
        }

        if (img1.firstElement().getClass() != img2.firstElement().getClass()) {
            throw new IllegalStateException("Types of the images are not the same! " + row.getKey().toString());
        }

        if (img1.numDimensions() != img2.numDimensions()) {
            throw new IllegalStateException("Number of dimensions of images is not the same! "
                    + row.getKey().toString());
        }

        for (int d = 0; d < img1.numDimensions(); d++) {
            if (img1.dimension(d) != img2.dimension(d)) {
                throw new IllegalStateException("Dimension " + d + " is not the same in the compared images!"
                        + row.getKey().toString());
            }
        }

        if (!img1.getName().equalsIgnoreCase(img2.getName())) {
            throw new IllegalStateException("Namesof images is not the same! " + row.getKey().toString());
        }

        if (!img1.getSource().equalsIgnoreCase(img2.getSource())) {
            throw new IllegalStateException("Sources of images is not the same! " + row.getKey().toString());
        }

        for (int d = 0; d < img1.numDimensions(); d++) {
            if (img1.axis(d).generalEquation().equalsIgnoreCase(img2.axis(d).generalEquation())) {
                throw new IllegalStateException("GeneralEquation of CalibratedAxis " + d
                        + " is not the same in the compared images!" + row.getKey().toString());
            }
        }

        for (int d = 0; d < img1.numDimensions(); d++) {
            if (img1.axis(d).type().getLabel().equalsIgnoreCase(img2.axis(d).type().getLabel())) {
                throw new IllegalStateException("Label of Axis " + d + " is not the same in the compared images!"
                        + row.getKey().toString());
            }
        }

        final Cursor<T> c1;
        final Cursor<T> c2;

        if (img1.getImg() instanceof ImgView || img2.getImg() instanceof ImgView) {
            c1 = new ImgView<T>(img1.getImg(), null).cursor();
            c2 = new ImgView<T>(img2.getImg(), null).cursor();
        } else {
            c1 = img1.cursor();
            c2 = img1.cursor();
        }

        while (c1.hasNext()) {
            c1.fwd();
            c2.fwd();
            if (Double.compare(c1.get().getRealDouble(), c2.get().getRealDouble()) != 0) {
                throw new IllegalStateException("Content of images is not the same!" + row.getKey().toString());
            }
        }
    }
}
