package org.knime.knip.core.ops.labeling;

import java.util.List;
import java.util.PriorityQueue;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingOutOfBoundsRandomAccessFactory;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

/**
 * Watershed algorithms. The watershed algorithm segments and labels an image
 * using an analogy to a landscape. The image intensities are turned into the
 * z-height of the landscape and the landscape is "filled with water" and the
 * bodies of water label the landscape's pixels. Here is the reference for the
 * original paper:
 *
 * Lee Vincent, Pierre Soille, Watersheds in digital spaces: An efficient
 * algorithm based on immersion simulations, IEEE Trans. Pattern Anal. Machine
 * Intell., 13(6) 583-598 (1991)
 *
 * Watersheds are often performed on the gradient of an intensity image or one
 * where the edges of the object boundaries have been enhanced. The resulting
 * image has a depressed object interior and a ridge which constrains the
 * watershed boundary.
 *
 * This is a modification of the original implementation from Lee Kamentsky to be
 * able to add the watersheds as well (e.g. for region merging by means of the
 * boundaries).
 *
 * @author Lee Kamentsky, Martin Horn
 *
 *
 * @param <T> Type of input RandomAccessibleInterval
 * @param <L> Type of seed Labeling
 *
 */

public class WatershedWithSheds<T extends RealType<T>, L extends Comparable<L>>
        implements BinaryOutputOperation<RandomAccessibleInterval<T>, Labeling<L>, Labeling<String>> {

    /**
     *
     * @param <U> Pixel Type
     */
    protected static class PixelIntensity<U extends Comparable<U>> implements
            Comparable<PixelIntensity<U>> {
        /**
         * Index
         */
        protected final long m_index;

        /**
         * Age
         */
        protected final long m_age;

        /**
         * Intensity
         */
        protected final double m_intensity;

        /**
         * Labeling
         */
        protected final List<U> m_labeling;

        /**
         * Constructor of PixelIntensity
         * @param position
         * @param dimensions
         * @param intensity
         * @param age
         * @param labeling
         */
        public PixelIntensity(final long[] position, final long[] dimensions,
                final double intensity, final long age, final List<U> labeling) {
            long index = position[0];
            long multiplier = dimensions[0];
            for (int i = 1; i < dimensions.length; i++) {
                index += position[i] * multiplier;
                multiplier *= dimensions[i];
            }

            this.m_index = index;
            this.m_intensity = intensity;
            this.m_labeling = labeling;
            this.m_age = age;
        }

        @Override
        public int compareTo(final PixelIntensity<U> other) {
            int result = Double.compare(m_intensity, other.m_intensity);
            if (result == 0) {
                result = Double.compare(m_age, other.m_age);
            }
            return result;
        }

        void getPosition(final long[] position, final long[] dimensions) {
            long idx = m_index;
            for (int i = 0; i < dimensions.length; i++) {
                position[i] = (int)(idx % dimensions[i]);
                idx /= dimensions[i];
            }
        }

        List<U> getLabeling() {
            return m_labeling;
        }
    }

    /**
     * Structuring Element
     */
    protected long[][] m_structuringElement;

    /**
     * ImgFactory for the result
     */
    protected ImgFactory<IntType> m_factory;


    /**
     * Constructor of WatershedsWithSheds
     * Uses a default ArrayImgFactory<IntType>() as result factory.
     * @param structuringElement Structuring Element
     */
    public WatershedWithSheds(final long[][] structuringElement) {
        this(structuringElement, new ArrayImgFactory<IntType>());
    }

    /**
     * Constructor of WatershedsWithSheds
     * @param structuringElement Structuring Element
     * @param factory Factory for result
     */
    public WatershedWithSheds(final long[][] structuringElement, final ImgFactory<IntType> factory) {
        m_structuringElement = structuringElement;
        m_factory = factory;
    }

    /**
     * Set the structuring element that defines the connectivity
     *
     * @param structuringElement an array of offsets where each element of the
     *            array gives the offset of a connected pixel from a pixel of
     *            interest. You can use
     *            AllConnectedComponents.getStructuringElement to get an
     *            8-connected (or N-dimensional equivalent) structuring element
     *            (all adjacent pixels + diagonals).
     */
    public void setStructuringElement(final long[][] structuringElement) {
        m_structuringElement = structuringElement;
    }

    /**
     * Sets the ImgFactory used for output Img.
     * @param factory
     */
    public void setResultFactory(final ImgFactory<IntType> factory) {
        m_factory = factory;
    }

    /**
     * Check if input is valid
     * @param image
     * @param seeds
     * @param output
     */
    public void checkInput(final RandomAccessibleInterval<T> image, final Labeling<L> seeds,
            final Labeling<String> output) {
        if (seeds.numDimensions() != image.numDimensions()) {
            throw new IllegalArgumentException(
                    String.format(
                            "The dimensionality of the seed labeling (%dD) does not match that of the intensity image (%dD)",
                            seeds.numDimensions(), image.numDimensions()));
        }
        if (seeds.numDimensions() != output.numDimensions()) {
            throw new IllegalArgumentException(
                    String.format(
                            "The dimensionality of the seed labeling (%dD) does not match that of the output labeling (%dD)",
                            seeds.numDimensions(), output.numDimensions()));
        }
        for (int i = 0; i < m_structuringElement.length; i++) {
            if (m_structuringElement[i].length != seeds.numDimensions()) {
                throw new IllegalArgumentException(
                        "Some or all of the structuring element offsets do not have the same number of dimensions as the image");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Labeling<String> compute(final RandomAccessibleInterval<T> input,
            final Labeling<L> seeds, final Labeling<String> output) {
        checkInput(input, seeds, output);

        if (m_structuringElement == null) {
            m_structuringElement =
                    AllConnectedComponents.getStructuringElement(input
                            .numDimensions());
        }

        IterableInterval<T> iterableInput = Views.iterable(input);

        // if (m_structuringElement == null)
        // m_structuringElement = AbstractRegionGrowing
        // .get4ConStructuringElement(input1.numDimensions());
        /*
         * Make an OutOfBounds for the labels that returns empty labels if out
         * of bounds. Make an OutOfBounds for the intensities that returns the
         * maximum intensity if out of bounds so that in-bounds will be in a
         * deep valley.
         */
        OutOfBoundsFactory<LabelingType<String>, Labeling<String>> factory =
                new LabelingOutOfBoundsRandomAccessFactory<String, Labeling<String>>();
        OutOfBounds<LabelingType<String>> outputAccess = factory.create(output);

        T maxVal = iterableInput.firstElement().createVariable();
        maxVal.setReal(maxVal.getMaxValue());
        OutOfBoundsFactory<T, RandomAccessibleInterval<T>> oobImageFactory =
                new OutOfBoundsConstantValueFactory<T, RandomAccessibleInterval<T>>(maxVal);
        OutOfBounds<T> imageAccess = oobImageFactory.create(input);

        /*
         * Start by loading up a priority queue with the seeded pixels
         */
        PriorityQueue<PixelIntensity<String>> pq =
                new PriorityQueue<PixelIntensity<String>>();
        Cursor<LabelingType<L>> c = seeds.localizingCursor();

        long[] dimensions = new long[input.numDimensions()];
        output.dimensions(dimensions);
        long[] position = new long[input.numDimensions()];
        long[] destPosition = new long[input.numDimensions()];
        long age = 0;

        /*
         * carries over the seeding points to the new label and adds them to the
         * pixel priority queue
         */
        while (c.hasNext()) {
            LabelingType<L> tSrc = c.next();
            List<L> l = tSrc.getLabeling();
            if (l.isEmpty()) {
                continue;
            }

            c.localize(position);
            imageAccess.setPosition(position);
            if (imageAccess.isOutOfBounds()) {
                continue;
            }
            outputAccess.setPosition(position);
            if (outputAccess.isOutOfBounds()) {
                continue;
            }
            LabelingType<String> tDest = outputAccess.get();
            List<String> newl = tDest.intern(l.get(0).toString());
            tDest.setLabeling(newl);
            double intensity = imageAccess.get().getRealDouble();
            pq.add(new PixelIntensity<String>(position, dimensions, intensity,
                    age++, newl));
        }
        /*
         * Rework the structuring element into a series of consecutive offsets
         * so we can use Positionable.move to scan the image array.
         */
        long[][] strelMoves = new long[m_structuringElement.length][];
        long[] currentOffset = new long[input.numDimensions()];
        for (int i = 0; i < m_structuringElement.length; i++) {
            strelMoves[i] = new long[input.numDimensions()];
            for (int j = 0; j < input.numDimensions(); j++) {
                strelMoves[i][j] =
                        m_structuringElement[i][j] - currentOffset[j];
                if (i > 0) {
                    currentOffset[j] +=
                            m_structuringElement[i][j]
                                    - m_structuringElement[i - 1][j];
                } else {
                    currentOffset[j] += m_structuringElement[i][j];
                }
            }
        }
        /*
         * Pop the head of the priority queue, label and push all unlabeled
         * connected pixels.
         */

        // label marking the boundaries
        String boundaries = "Watershed";

        // dummy to mark nodes as visited
        String dummy = "dummy";
        while (!pq.isEmpty()) {
            PixelIntensity<String> currentPI = pq.remove();
            List<String> l = currentPI.getLabeling();
            currentPI.getPosition(position, dimensions);
            outputAccess.setPosition(position);
            imageAccess.setPosition(position);
            for (long[] offset : strelMoves) {
                outputAccess.move(offset);
                imageAccess.move(offset);
                LabelingType<String> outputLabelingType = outputAccess.get();
                if (outputAccess.isOutOfBounds()) {
                    l = outputLabelingType.intern(boundaries);
                    break;
                }

                if (outputLabelingType.getLabeling().isEmpty()) {
                    double intensity = imageAccess.get().getRealDouble();
                    outputAccess.localize(destPosition);
                    pq.add(new PixelIntensity<String>(destPosition, dimensions,
                            intensity, age++, currentPI.getLabeling()));

                    // dummy to mark positions as visited
                    outputLabelingType.setLabeling(outputLabelingType
                            .intern(dummy));

                } else if (outputLabelingType.getLabeling() != l
                        && !outputLabelingType.getLabeling().get(0)
                                .equals(dummy)
                        && !outputLabelingType.getLabeling().get(0)
                                .equals(boundaries)) {
                    l = outputLabelingType.intern(boundaries);

                    // }

                }

            }
            outputAccess.setPosition(position);
            LabelingType<String> outputLabelingType = outputAccess.get();
            outputLabelingType.setLabeling(l);
        }

        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryObjectFactory<RandomAccessibleInterval<T>, Labeling<L>, Labeling<String>> bufferFactory() {
        return new BinaryObjectFactory<RandomAccessibleInterval<T>, Labeling<L>, Labeling<String>>() {

            @Override
            public Labeling<String> instantiate(final RandomAccessibleInterval<T> inputA,
                    final Labeling<L> inputB) {
                return new NativeImgLabeling<String, IntType>(m_factory.create(inputA, new IntType()));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryOutputOperation<RandomAccessibleInterval<T>, Labeling<L>, Labeling<String>> copy() {
        return new WatershedWithSheds<T, L>(m_structuringElement.clone());
    }

}
