/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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
 * Created on Oct 24, 2015 by dietzc
 */
package org.knime.knip.core;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.ops.Namespace;
import net.imagej.ops.create.CreateNamespace;
import net.imglib2.Dimensions;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.IntegerType;

/**
 * @author Christian Dietz, University of Konstanz
 */
@Plugin(type = Namespace.class, priority = org.scijava.Priority.HIGH_PRIORITY)
public class KNIPCreateNamespace extends CreateNamespace {

    @Parameter
    private KNIPGuavaCacheService cache;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object img(final Object... args) {
        cache.cleanUp();
        return super.img(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object img(final Integer... dims) {
        cache.cleanUp();
        return super.img(dims);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object img(final Long... dims) {
        cache.cleanUp();
        return super.img(dims);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends NativeType<T>> Img<T> img(final Img<T> input) {
        cache.cleanUp();
        return super.img(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Img<T> img(final Dimensions dims) {
        cache.cleanUp();
        return super.img(dims);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Img<T> img(final Dimensions dims, final T outType) {
        cache.cleanUp();
        return super.img(dims, outType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Img<T> img(final Dimensions dims, final T outType, final ImgFactory<T> fac) {
        cache.cleanUp();
        return super.img(dims, outType, fac);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Type<T>> Img<T> img(final Interval interval) {
        cache.cleanUp();
        return super.img(interval);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Type<T>> Img<T> img(final Interval interval, final T outType) {
        cache.cleanUp();
        return super.img(interval, outType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Type<T>> Img<T> img(final Interval interval, final T outType, final ImgFactory<T> fac) {
        cache.cleanUp();
        return super.img(interval, outType, fac);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends NativeType<T>> ImgFactory<T> imgFactory() {
        cache.cleanUp();
        return super.imgFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object imgLabeling(final Object... args) {
        cache.cleanUp();
        return super.imgLabeling(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L, T extends IntegerType<T>> ImgLabeling<L, T> imgLabeling(final Dimensions dims) {
        cache.cleanUp();
        return super.imgLabeling(dims);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L, T extends IntegerType<T>> ImgLabeling<L, T> imgLabeling(final Dimensions dims, final T outType) {
        cache.cleanUp();
        return super.imgLabeling(dims, outType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L, T extends IntegerType<T>> ImgLabeling<L, T> imgLabeling(final Dimensions dims, final T outType,
                                                                       final ImgFactory<T> fac) {
        cache.cleanUp();
        return super.imgLabeling(dims, outType, fac);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L, T extends IntegerType<T>> ImgLabeling<L, T>
           imgLabeling(final Dimensions dims, final T outType, final ImgFactory<T> fac, final int maxNumLabelSets) {
        cache.cleanUp();
        return super.imgLabeling(dims, outType, fac, maxNumLabelSets);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L, T extends IntegerType<T>> ImgLabeling<L, T> imgLabeling(final Interval interval) {
        cache.cleanUp();
        return super.imgLabeling(interval);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L, T extends IntegerType<T>> ImgLabeling<L, T> imgLabeling(final Interval interval, final T outType) {
        cache.cleanUp();
        return super.imgLabeling(interval, outType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L, T extends IntegerType<T>> ImgLabeling<L, T> imgLabeling(final Interval interval, final T outType,
                                                                       final ImgFactory<T> fac) {
        cache.cleanUp();
        return super.imgLabeling(interval, outType, fac);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L, T extends IntegerType<T>> ImgLabeling<L, T>
           imgLabeling(final Interval interval, final T outType, final ImgFactory<T> fac, final int maxNumLabelSets) {
        cache.cleanUp();
        return super.imgLabeling(interval, outType, fac, maxNumLabelSets);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object imgPlus(final Object... args) {
        cache.cleanUp();
        return super.imgPlus(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ImgPlus<T> imgPlus(final Img<T> img) {
        cache.cleanUp();
        return super.imgPlus(img);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ImgPlus<T> imgPlus(final Img<T> img, final ImgPlusMetadata metadata) {
        cache.cleanUp();
        return super.imgPlus(img, metadata);
    }

}
