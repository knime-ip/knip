package org.knime.knip.core.ops.labeling;

import java.util.List;
import java.util.PriorityQueue;

import net.imglib2.Cursor;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
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
 * This is a modification of the orginal implementation from Lee Kamentsky to be
 * able to add the watersheds as well (e.g. for region merging by means of the
 * boundaries).
 * 
 * @author Lee Kamentsky, Martin Horn
 * 
 * 
 * 
 */

public class WatershedWithSheds<T extends RealType<T>, L extends Comparable<L>>
        implements BinaryOutputOperation<Img<T>, Labeling<L>, Labeling<String>> {

    protected static class PixelIntensity<U extends Comparable<U>> implements
            Comparable<PixelIntensity<U>> {
        protected final long index;

        protected final long age;

        protected final double intensity;

        protected final List<U> labeling;

        public PixelIntensity(long[] position, long[] dimensions,
                double intensity, long age, List<U> labeling) {
            long index = position[0];
            long multiplier = dimensions[0];
            for (int i = 1; i < dimensions.length; i++) {
                index += position[i] * multiplier;
                multiplier *= dimensions[i];
            }

            this.index = index;
            this.intensity = intensity;
            this.labeling = labeling;
            this.age = age;
        }

        @Override
        public int compareTo(PixelIntensity<U> other) {
            int result = Double.compare(intensity, other.intensity);
            if (result == 0)
                result = Double.compare(age, other.age);
            return result;
        }

        void getPosition(long[] position, long[] dimensions) {
            long idx = index;
            for (int i = 0; i < dimensions.length; i++) {
                position[i] = (int)(idx % dimensions[i]);
                idx /= dimensions[i];
            }
        }

        List<U> getLabeling() {
            return labeling;
        }
    }

    long[][] m_structuringElement;

    public WatershedWithSheds(long[][] structuringElement) {
        m_structuringElement = structuringElement;

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
    public void setStructuringElement(long[][] structuringElement) {
        m_structuringElement = structuringElement;
    }

    public void checkInput(Img<T> image, Labeling<L> seeds,
            Labeling<String> output) {
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
    public Labeling<String> compute(final Img<T> input1,
            final Labeling<L> seeds, final Labeling<String> output) {
        checkInput(input1, seeds, output);

        if (m_structuringElement == null)
            m_structuringElement =
                    AllConnectedComponents.getStructuringElement(input1
                            .numDimensions());

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

        T maxVal = input1.firstElement().createVariable();
        maxVal.setReal(maxVal.getMaxValue());
        OutOfBoundsFactory<T, Img<T>> oobImageFactory =
                new OutOfBoundsConstantValueFactory<T, Img<T>>(maxVal);
        OutOfBounds<T> imageAccess = oobImageFactory.create(input1);

        /*
         * Start by loading up a priority queue with the seeded pixels
         */
        PriorityQueue<PixelIntensity<String>> pq =
                new PriorityQueue<PixelIntensity<String>>();
        Cursor<LabelingType<L>> c = seeds.localizingCursor();

        long[] dimensions = new long[input1.numDimensions()];
        output.dimensions(dimensions);
        long[] position = new long[input1.numDimensions()];
        long[] destPosition = new long[input1.numDimensions()];
        long age = 0;

        /*
         * carries over the seeding points to the new label and adds them to the
         * pixel priority queue
         */
        while (c.hasNext()) {
            LabelingType<L> tSrc = c.next();
            List<L> l = tSrc.getLabeling();
            if (l.isEmpty())
                continue;

            c.localize(position);
            imageAccess.setPosition(position);
            if (imageAccess.isOutOfBounds())
                continue;
            outputAccess.setPosition(position);
            if (outputAccess.isOutOfBounds())
                continue;
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
        long[] currentOffset = new long[input1.numDimensions()];
        for (int i = 0; i < m_structuringElement.length; i++) {
            strelMoves[i] = new long[input1.numDimensions()];
            for (int j = 0; j < input1.numDimensions(); j++) {
                strelMoves[i][j] =
                        m_structuringElement[i][j] - currentOffset[j];
                if (i > 0)
                    currentOffset[j] +=
                            m_structuringElement[i][j]
                                    - m_structuringElement[i - 1][j];
                else
                    currentOffset[j] += m_structuringElement[i][j];
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
    public BinaryObjectFactory<Img<T>, Labeling<L>, Labeling<String>> bufferFactory() {
        return new BinaryObjectFactory<Img<T>, Labeling<L>, Labeling<String>>() {

            public Labeling<String> instantiate(Img<T> inputA,
                    Labeling<L> inputB) {
                try {
                    return new NativeImgLabeling<String, IntType>(inputA
                            .factory().imgFactory(new IntType())
                            .create(inputA, new IntType()));
                } catch (IncompatibleTypeException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public BinaryOutputOperation<Img<T>, Labeling<L>, Labeling<String>> copy() {
        return new WatershedWithSheds<T, L>(m_structuringElement.clone());
    }

}
