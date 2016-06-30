package org.knime.knip.io.nodes.imgreader2;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.Pair;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.io.ScifioImgSource;

import io.scif.config.SCIFIOConfig;
import net.imagej.ImgPlus;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.TypedAxis;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * {@link Function} to read an {@link Img}, OME-XML Metadata or both from
 * somewhere.
 * 
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher,
 *         University of Konstanz.</a>
 * 
 * @param <T>
 *            They Type of the Image
 * @param <I>
 *            The input for the ReadImgFunction
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
	protected final boolean m_readImage;
	protected final boolean m_readMetadata;
	protected final ImgPlusCellFactory m_cellFactory;
	private String m_pixelType;

	public AbstractReadImgFunction(final ExecutionContext exec, final int numberOfFiles,
			final SettingsModelSubsetSelection2 sel, final boolean readImage, final boolean readMetadata,
			final boolean readAllMetaData, final boolean checkFileFormat, final boolean completePathRowKey,
			final boolean isGroupFiles, final int seriesSelectionFrom, int seriesSelectionTo,
			final ImgFactory<T> imgFactory, final String pixelType) {

		m_currentFile = new AtomicInteger();
		m_numberOfFiles = numberOfFiles;

		// initCanonicalWorkflowPath();
		m_completePathRowKey = completePathRowKey;
		// m_fileList = fileList;

		m_sel = sel;
		m_exec = exec;
		m_cellFactory = new ImgPlusCellFactory(exec);

		m_readImage = readImage;
		m_readMetadata = readMetadata;

		m_readAllMetadata = readAllMetaData;
		m_selectedSeriesFrom = seriesSelectionFrom;
		m_selectedSeriesTo = seriesSelectionTo;

		m_scifioConfig = new SCIFIOConfig().groupableSetGroupFiles(isGroupFiles)
				.parserSetSaveOriginalMetadata(m_readAllMetadata);
		m_imgSource = new ScifioImgSource(imgFactory, checkFileFormat, m_scifioConfig);

		m_pixelType = pixelType;
	}

	protected Pair<DataRow, Optional<Throwable>> createResultFromException(String path, String rowKey, Exception exc) {

		DataCell[] cells = new DataCell[((m_readImage) ? 1 : 0) + ((m_readMetadata) ? 1 : 0)];

		if (m_readImage) {
			cells[0] = new MissingCell("Exception while processing  " + path + "!\nCaught Exception" + exc.getMessage()
					+ "\n" + exc.getStackTrace());
		}

		if (m_readMetadata) {
			cells[cells.length - 1] = new MissingCell("Exception while processing  " + path + "!\nCaught Exception"
					+ exc.getMessage() + "\n" + exc.getStackTrace());
		}
		return new Pair<DataRow, Optional<Throwable>>(new DefaultRow(path, cells), Optional.of(exc));

	}

	/**
	 * Reads {@link Img} and {@link MetadataMode} from disk and returns the
	 * result with an {@link Optional} {@link Exception}.
	 * 
	 * @param pathToImage
	 *            the path to the image
	 * @param rowKey
	 *            the rowkey for the result row
	 * @param currentSeries
	 *            the number of the current series
	 * @return a pair of a datarow and an optional exception
	 */
	@SuppressWarnings({ "unchecked" })
	protected Pair<DataRow, Optional<Throwable>> readImageAndMetadata(String pathToImage, RowKey rowKey,
			int currentSeries) {

		DataCell[] cells = new DataCell[((m_readImage) ? 1 : 0) + ((m_readMetadata) ? 1 : 0)];

		try {
			if (m_readImage) {
				List<CalibratedAxis> calibAxes = m_imgSource.getAxes(pathToImage, currentSeries);
				net.imglib2.util.Pair<TypedAxis, long[]>[] axisSelectionConstraints = m_sel.createSelectionConstraints(
						m_imgSource.getDimensions(pathToImage, currentSeries),
						calibAxes.toArray(new CalibratedAxis[calibAxes.size()]));

				if (m_pixelType.equalsIgnoreCase(
						AbstractImgReaderNodeModel.PIXEL_TYPES[AbstractImgReaderNodeModel.PIXEL_TYPES.length - 1])) {
					ImgPlus<T> resImgPlus = (ImgPlus<T>) m_imgSource.getImg(pathToImage, currentSeries,
							axisSelectionConstraints);
					cells[0] = m_cellFactory.createCell(resImgPlus);
				} else {
					ImgPlus<T> resImgPlus = (ImgPlus<T>) m_imgSource.getTypedImg(pathToImage, currentSeries,
							axisSelectionConstraints,
							(T) NativeTypes.valueOf(m_pixelType.toUpperCase()).getTypeInstance());
					cells[0] = m_cellFactory.createCell(resImgPlus);
				}

			}

			if (m_readMetadata) {
				cells[cells.length - 1] = XMLCellFactory.create(m_imgSource.getOMEXMLMetadata(pathToImage));
			}
		} catch (Exception exc) {
			return createResultFromException(pathToImage, rowKey.getString(), exc);
		}

		return new Pair<DataRow, Optional<Throwable>>(new DefaultRow(rowKey, cells), Optional.empty());
	}

	/**
	 * Performs all the clean-up work, e.g. closing files etc.
	 */
	public void close() {
		m_imgSource.close();
	}
}
