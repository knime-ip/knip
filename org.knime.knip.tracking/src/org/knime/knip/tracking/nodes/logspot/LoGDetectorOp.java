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
package org.knime.knip.tracking.nodes.logspot;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.knime.knip.base.exceptions.KNIPException;

import fiji.plugin.trackmate.detection.DetectionUtils;

/**
 * Wrapper {@link UnaryOperation} for {@link LoGDetectorOp} which is implemented
 * in Trackmate
 * 
 * Note: This class is basically a copy of the class of Trackmate (Jean-Yves
 * Tievenez). We have to use his version as soon as available in scijava-ops
 * (TODO)
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @param <T>
 */
public class LoGDetectorOp<T extends RealType<T> & NativeType<T>> implements
		UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> {

	private final double radius;

	/**
	 * Default Constructor
	 * 
	 * @param radius
	 *            span of the resulting spots
	 * @param service
	 *            {@link ExecutorService} service
	 */
	public LoGDetectorOp(final double radius) {
		this.radius = radius;
	}

	@Override
	public ImgPlus<BitType> compute(final ImgPlus<T> input,
			final ImgPlus<BitType> output) {

		/*
		 * Create TMP Float
		 */
		Img<FloatType> floatImg = null;
		try {
			floatImg = input.factory().imgFactory(new FloatType())
					.create(input, new FloatType());
		} catch (IncompatibleTypeException e) {
			new KNIPException("Can't instantiate factory in trackmate");
		}

		final Img<FloatType> kernel = DetectionUtils.createLoGKernel(radius,
				input);

		final FFTConvolution<T, FloatType, FloatType> fftconv = FFTConvolution
				.create(input.getImg(), kernel, floatImg);
		fftconv.run();

		final ArrayList<Point> points = LocalExtrema.findLocalExtrema(floatImg,
				new LocalExtrema.MaximumCheck<FloatType>(new FloatType(
						-Float.MAX_VALUE)), Executors
						.newFixedThreadPool(Runtime.getRuntime()
								.availableProcessors()));

		// Get peaks location and values
		RandomAccess<BitType> randomAccess = output.randomAccess();

		// Prune values lower than threshold
		for (final Point point : points) {
			for (int i = 0; i < input.numDimensions(); i++) {
				randomAccess.setPosition(
						(long) (point.getLongPosition(i) * input.axis(i)
								.calibratedValue(1)), i);
			}
			randomAccess.get().set(true);
		}

		return output;

	}

	@Override
	public UnaryObjectFactory<ImgPlus<T>, ImgPlus<BitType>> bufferFactory() {
		return new UnaryObjectFactory<ImgPlus<T>, ImgPlus<BitType>>() {

			@Override
			public ImgPlus<BitType> instantiate(final ImgPlus<T> img) {
				try {
					return new ImgPlus<BitType>(img.factory()
							.imgFactory(new BitType())
							.create(img, new BitType()), img);
				} catch (IncompatibleTypeException e) {
					return new ImgPlus<BitType>(
							new ArrayImgFactory<BitType>().create(img,
									new BitType()), img);
				}

			}
		};
	}

	@Override
	public UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> copy() {
		return new LoGDetectorOp<T>(radius);
	}

}
