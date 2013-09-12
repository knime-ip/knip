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
package org.knime.knip.core.util;

import net.imglib2.Interval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgUtils {

    public synchronized static <T extends RealType<T>> Img<T> createEmptyImg(final Img<T> in) {
        return in.factory().create(in, in.firstElement().createVariable());
    }

    public synchronized static <T extends RealType<T>> Img<T> createEmptyCopy(final Img<T> in, final long[] newDims) {
        return in.factory().create(newDims, in.firstElement().createVariable());
    }

    public synchronized static <T extends RealType<T>> Img<T> createEmptyCopy(final ImgFactory<T> fac,
                                                                              final Interval in, final T type) {
        return fac.create(in, type);
    }

    public synchronized static <T extends RealType<T>, O extends RealType<O>> Img<O> createEmptyCopy(final Img<T> in,
                                                                                                     final O type) {
        try {
            return in.factory().imgFactory(type).create(in, type);
        } catch (final IncompatibleTypeException e) {
            throw new RuntimeException(e);
        }

    }

    public synchronized static <T extends RealType<T>, O extends NativeType<O>> Img<O>
            createEmptyCopy(final Img<T> in, final ImgFactory<O> fac, final O type) {
        return fac.create(in, type);
    }

    public synchronized static <T extends RealType<T>> Img<T> createEmptyCopy(final Img<T> in) {
        return in.factory().create(in, in.firstElement().createVariable());
    }

    public synchronized static <O extends NativeType<O>> Img<O> createEmptyCopy(final long[] dims,
                                                                                final ImgFactory<O> fac, final O type) {
        return fac.create(dims, type);
    }

    public synchronized static <L extends Comparable<L>> Labeling<L> createEmptyCopy(final Labeling<L> labeling) {
        return labeling.<L> factory().create(labeling);
    }
}
