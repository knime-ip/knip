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
package org.knime.knip.core.awt.specializedrendering;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ScreenImage;
import net.imglib2.display.projectors.Abstract2DProjector;
import net.imglib2.display.projectors.screenimages.ByteScreenImage;
import net.imglib2.display.projectors.screenimages.ShortScreenImage;
import net.imglib2.display.projectors.specializedprojectors.ArrayImgXYByteProjector;
import net.imglib2.display.projectors.specializedprojectors.ArrayImgXYShortProjector;
import net.imglib2.display.projectors.specializedprojectors.PlanarImgXYByteProjector;
import net.imglib2.display.projectors.specializedprojectors.PlanarImgXYShortProjector;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;

import org.knime.knip.core.types.NativeTypes;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class FastNormalizingGreyRendering {

    public static <R extends RealType<R>> ScreenImage tryRendering(RandomAccessibleInterval<R> source, final int dimX,
                                                                   final int dimY, final long[] planePos,
                                                                   final double normalizationFactor, final double min) {

        // unwrap img plus if necessary
        while (source instanceof ImgPlus) {
            source = ((ImgPlus)source).getImg();
        }

        RenderTripel match = new RenderTripel();

        if (!match.isSuccessfull()) {
            // try ArrayImage
            match = tryArrayImage(source, dimX, dimY, planePos, normalizationFactor, min);
        }

        if (!match.isSuccessfull()) {
            // try PlanarImage
            match = tryPlanarImage(source, dimX, dimY, planePos, normalizationFactor, min);
        }

        if (match.isSuccessfull()) {
            // speed up possible use tuned implementation
            match.getProjector().setPosition(planePos);
            match.getProjector().map();
            return match.getImage();
        } else {
            return null;
        }
    }

    private static <R extends RealType<R>> RenderTripel
            tryArrayImage(final RandomAccessibleInterval<R> source, final int dimX, final int dimY,
                          final long[] planePos, final double normalizationFactor, final double min) {

        if ((dimX == 0) && (dimY == 1) && (source instanceof ArrayImg)) {
            Abstract2DProjector<?, ?> projector;
            ScreenImage target;
            final NativeTypes type = NativeTypes.getPixelType(source.randomAccess().get());

            final long w = source.dimension(dimX);
            final long h = source.dimension(dimY);

            if ((type == NativeTypes.BYTETYPE) || (type == NativeTypes.UNSIGNEDBYTETYPE)) {
                target = new ByteScreenImage(new ByteArray(new byte[(int)(w * h)]), new long[]{w, h});

                projector =
                        new ArrayImgXYByteProjector<ByteType>((ArrayImg<ByteType, ByteArray>)source,
                                ((ByteScreenImage)target), normalizationFactor, min);
                return new RenderTripel(projector, target);
            } else if ((type == NativeTypes.SHORTTYPE) || (type == NativeTypes.UNSIGNEDSHORTTYPE)) {
                target = new ShortScreenImage(new ShortArray(new short[(int)(w * h)]), new long[]{w, h});

                projector =
                        new ArrayImgXYShortProjector<ShortType>((ArrayImg<ShortType, ShortArray>)source,
                                ((ShortScreenImage)target), normalizationFactor, min);
                return new RenderTripel(projector, target);
            }
        }

        return new RenderTripel();
    }

    private static <R extends RealType<R>> RenderTripel tryPlanarImage(final RandomAccessibleInterval<R> source,
                                                                       final int dimX, final int dimY,
                                                                       final long[] planePos,
                                                                       final double normalizationFactor,
                                                                       final double min) {

        if ((dimX == 0) && (dimY == 1) && (source instanceof PlanarImg)) {
            Abstract2DProjector<?, ?> projector;
            ScreenImage target;
            final NativeTypes type = NativeTypes.getPixelType(source.randomAccess().get());

            final long w = source.dimension(dimX);
            final long h = source.dimension(dimY);

            if ((type == NativeTypes.BYTETYPE) || (type == NativeTypes.UNSIGNEDBYTETYPE)) {
                target = new ByteScreenImage(new ByteArray(new byte[(int)(w * h)]), new long[]{w, h});

                projector =
                        new PlanarImgXYByteProjector<ByteType>((PlanarImg<ByteType, ByteArray>)source,
                                ((ByteScreenImage)target), normalizationFactor, min);
                return new RenderTripel(projector, target);
            } else if ((type == NativeTypes.SHORTTYPE) || (type == NativeTypes.UNSIGNEDSHORTTYPE)) {
                target = new ShortScreenImage(new ShortArray(new short[(int)(w * h)]), new long[]{w, h});

                projector =
                        new PlanarImgXYShortProjector<ShortType>((PlanarImg<ShortType, ShortArray>)source,
                                ((ShortScreenImage)target), normalizationFactor, min);
                return new RenderTripel(projector, target);
            }
        }

        return new RenderTripel();
    }
}
