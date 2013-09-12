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
package org.knime.knip.core.types;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.img.sparse.NtreeImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public enum ImgFactoryTypes {

    ARRAY_IMG_FACTORY, PLANAR_IMG_FACTORY, CELL_IMG_FACTORY, NTREE_IMG_FACTORY, SOURCE_FACTORY;

    /**
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static final <T extends NativeType<T>> ImgFactory
            getImgFactory(final ImgFactoryTypes facType, final Img img) {

        if (facType == SOURCE_FACTORY) {
            return img.factory();
        } else {
            return ImgFactoryTypes.<T> getImgFactory(facType);
        }

    }

    public static final <T extends Type<T>> ImgFactoryTypes getImgFactoryType(final ImgFactory<T> factory) {
        if (factory instanceof ArrayImgFactory) {
            return ARRAY_IMG_FACTORY;
        } else if (factory instanceof PlanarImgFactory) {
            return PLANAR_IMG_FACTORY;
        } else if (factory instanceof CellImgFactory) {
            return CELL_IMG_FACTORY;
        } else if (factory instanceof NtreeImgFactory) {
            return NTREE_IMG_FACTORY;
        } else {
            throw new UnsupportedOperationException(
                    "Serializing an image with the specified storage strategy isn't supported, yet.");
        }
    }

    /**
     * @return
     */
    public static final <T extends NativeType<T>> ImgFactory<T> getImgFactory(final ImgFactoryTypes facType) {

        switch (facType) {
            case ARRAY_IMG_FACTORY:
                return new ArrayImgFactory<T>();
            case CELL_IMG_FACTORY:
                return new CellImgFactory<T>();
            case PLANAR_IMG_FACTORY:
                return new PlanarImgFactory<T>();
            case NTREE_IMG_FACTORY:
                return new NtreeImgFactory<T>();
            default:
                return null;

        }
    }

    /**
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static final ImgFactory getImgFactory(final String facTypeAsString, final Img img) {

        return getImgFactory(valueOf(facTypeAsString), img);

    }
}
