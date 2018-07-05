
package org.knime.knip.io2.nodes.imgreader3;

import io.scif.config.SCIFIOConfig;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import net.imagej.ImgPlus;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.TypedAxis;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.Pair;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;
import org.knime.knip.io2.ScifioImgSource;
import org.scijava.io.location.Location;

/**
 * {@link Function} to read an {@link Img},
 *
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher,
 *         University of Konstanz.</a>
 * 
 * @param <T> They Type of the Image
 * @param <I> The input for the ReadImgFunction
 */
public abstract class AbstractReadImgFunction<T extends RealType<T> & NativeType<T>, I>
		implements Function<I, Stream<Pair<DataRow, Optional<Throwable>>>> {

	protected final AtomicInteger m_currentFile;
	protected final double m_numberOfFiles;

	protected final boolean m_completePathRowKey;
	protected final SettingsModelSubsetSelection2 m_sel;
	protected final ExecutionContext m_exec;
	protected final boolean m_readAllMetadata;
	protected final int m_selectedSeriesFrom;
	protected final int m_selectedSeriesTo;

	protected final SCIFIOConfig m_scifioConfig;
	protected final ScifioImgSource m_imgSource;
	protected final ImgPlusCellFactory m_cellFactory;

	public AbstractReadImgFunction(final ExecutionContext exec, final int numberOfFiles,
			final SettingsModelSubsetSelection2 sel, final boolean readAllMetaData, final boolean checkFileFormat,
			final boolean completePathRowKey, final boolean isGroupFiles, final int seriesSelectionFrom,
			final int seriesSelectionTo, final ImgFactory<T> imgFactory) {

		m_currentFile = new AtomicInteger();
		m_numberOfFiles = numberOfFiles;

		m_completePathRowKey = completePathRowKey;

		m_sel = sel;
		m_exec = exec;
		m_cellFactory = new ImgPlusCellFactory(exec);

		m_readAllMetadata = readAllMetaData;
		m_selectedSeriesFrom = seriesSelectionFrom;
		m_selectedSeriesTo = seriesSelectionTo;

		m_scifioConfig = new SCIFIOConfig().groupableSetGroupFiles(isGroupFiles)
				.parserSetSaveOriginalMetadata(m_readAllMetadata);
		m_imgSource = new ScifioImgSource(imgFactory, checkFileFormat, m_scifioConfig);
	}

	protected Pair<DataRow, Optional<Throwable>> createResultFromException(final Location path, final String rowKey,
			final Exception exc) {

		final DataCell cell = new MissingCell(
				"Exception while processing  " + (path != null ? path : "no location given") + "!\nCaught Exception"
						+ exc.getMessage() + "\n" + exc.getStackTrace());

		return new Pair<>(new DefaultRow(rowKey, cell), Optional.of(exc));
	}

	/**
	 * Reads {@link Img} from a Location and returns the result with an
	 * {@link Optional} {@link Exception}.
	 *
	 * @param imageLocation the path to the image
	 * @param rowKey        the rowkey for the result row
	 * @param currentSeries the number of the current series
	 * @return a pair of a datarow and an optional exception
	 */
	@SuppressWarnings({ "unchecked" })
	protected Pair<DataRow, Optional<Throwable>> readImage(final Location imageLocation, final RowKey rowKey,
			final int currentSeries) {

		DataCell cell = null;

		try {
			final List<CalibratedAxis> calibAxes = m_imgSource.getAxes(imageLocation, currentSeries);
			final net.imglib2.util.Pair<TypedAxis, long[]>[] axisSelectionConstraints = m_sel
					.createSelectionConstraints(m_imgSource.getDimensions(imageLocation, currentSeries),
							calibAxes.toArray(new CalibratedAxis[calibAxes.size()]));

			final ImgPlus<T> resImgPlus = (ImgPlus<T>) m_imgSource.getImg(imageLocation, currentSeries,
					axisSelectionConstraints);
			cell = m_cellFactory.createCell(resImgPlus);
		} catch (final Exception exc) {
			return createResultFromException(imageLocation, rowKey.getString(), exc);
		}
		return new Pair<>(new DefaultRow(rowKey, cell), Optional.empty());
	}

	/**
	 * Performs all the clean-up work, e.g. closing files etc.
	 */
	public void close() {
		m_imgSource.close();
	}
}
