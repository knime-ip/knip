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
package org.knime.knip.base.nodes.proc.dogdetector;

import java.util.concurrent.ExecutorService;

import net.imagej.ImgPlus;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.view.Views;

/**
 * Wrapper for the {@link DogDetection} implementation of Tobias Pietzsch
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 *
 * @param <T>
 */
public class DoGDetectorOp<T extends RealType<T> & NativeType<T>>
        implements UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> {

    private final ExtremaType m_type;

    private final double m_sigma2;

    private final double m_sigma1;

    private final OutOfBoundsFactory<T, RandomAccessibleInterval<T>> m_oob;

    private final double m_threshold;

    private final ExecutorService m_service;

    private final boolean m_normalize;

    /**
     * Constructor
     *
     * @param sigma1 sigma of first gaussian
     * @param sigma2 sigma of second gaussian
     * @param type are you looking for minima or maxima?
     * @param oob {@link OutOfBoundsBorderFactory}
     * @param threshold maximum/minum value of pixel such that it is considered as a maxima/minima
     * @param normalize true, if threshold should be normalized
     * @param service executionservice to be used
     */
    public DoGDetectorOp(final double sigma1, final double sigma2, final ExtremaType type,
                         final OutOfBoundsFactory<T, RandomAccessibleInterval<T>> oob, final double threshold,
                         final boolean normalize, final ExecutorService service) {
        this.m_sigma1 = sigma1;
        this.m_sigma2 = sigma2;
        this.m_type = type;
        this.m_oob = oob;
        this.m_service = service;
        this.m_threshold = threshold;
        this.m_normalize = normalize;

    }

    @Override
    public ImgPlus<BitType> compute(final ImgPlus<T> input, final ImgPlus<BitType> output) {

        double[] calibration = new double[input.numDimensions()];
        for (int d = 0; d < calibration.length; d++) {
            calibration[d] = input.axis(d).calibratedValue(1);
        }

        if (input.firstElement() instanceof ShortType) {
            throw new IllegalArgumentException(
                    "Due to upstream changes shorttype images produce different results now, please update this node to continue using it with short type images.");
        }

        DogDetection<T> dogDetection = new DogDetection<T>(Views.extend(input, m_oob), input, calibration, m_sigma1,
                m_sigma2, m_type, m_threshold, m_normalize, input.firstElement().createVariable());

        dogDetection.setExecutorService(m_service);

        RandomAccess<BitType> randomAccess = output.randomAccess();
        for (Point peak : dogDetection.getPeaks()) {
            randomAccess.setPosition(peak);
            randomAccess.get().set(true);
        }

        return output;

    }

    @Override
    public UnaryObjectFactory<ImgPlus<T>, ImgPlus<BitType>> bufferFactory() {
        return img -> {
            try {
                return new ImgPlus<>(img.factory().imgFactory(new BitType()).create(img), img);
            } catch (IncompatibleTypeException e) {
                return new ImgPlus<>(new ArrayImgFactory<>(new BitType()).create(img), img);
            }

        };
    }

    @Override
    public UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> copy() {
        return new DoGDetectorOp<T>(m_sigma1, m_sigma2, m_type, m_oob, m_threshold, m_normalize, m_service);
    }

}
