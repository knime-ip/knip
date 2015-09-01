package org.knime.knip.io.nodes.imgreader2.readfromdialog;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Pair;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;
import org.knime.knip.io.nodes.imgreader2.AbstractReadImgFunction;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;

/**
 * {@link Function} to read an {@link Img}, OME-XML Metadata or both from a file
 * path.
 * 
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher, University of
 *         Konstanz.</a>
 *
 */
class ReadImg2Function<T extends RealType<T>> extends AbstractReadImgFunction<T, String> {

	public ReadImg2Function(ExecutionContext exec, int numberOfFiles, SettingsModelSubsetSelection sel,
			boolean readImage, boolean readMetadata, boolean readAllMetaData, boolean checkFileFormat,
			boolean completePathRowKey, boolean isGroupFiles, int selectedSeries, ImgFactory<T> imgFactory) {
		super(exec, numberOfFiles, sel, readImage, readMetadata, readAllMetaData, checkFileFormat, completePathRowKey,
				isGroupFiles, selectedSeries, imgFactory);
	}

	@Override
	public Stream<Pair<DataRow, Optional<Exception>>> apply(String t) {
		List<Pair<DataRow, Optional<Exception>>> results = new ArrayList<>();

		String path;
		int numSeries;
		try {
			path = FileUtil.resolveToPath(FileUtil.toURL(t)).toString();
			numSeries = m_imgSource.getSeriesCount(path);
		} catch (InvalidPathException | IOException | URISyntaxException exc) {
			m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
			return Arrays.asList(createResultFromException(t, t, exc)).stream();
		} catch (Exception exc) {
			m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
			return Arrays.asList(createResultFromException(t, t, exc)).stream();
		}

		// get start and end of the series
		int seriesStart = m_selectedSeries == -1 ? 0 : m_selectedSeries;
		int seriesEnd = m_selectedSeries == -1 ? numSeries : m_selectedSeries + 1;

		// load image and metadata for each series index
		IntStream.range(seriesStart, seriesEnd).forEachOrdered(currentSeries -> {
			String rowKey = (m_completePathRowKey) ? path : path.substring(path.lastIndexOf(File.separatorChar) + 1);
			RowKey rk;
			if (currentSeries > 1) {
				rk = new RowKey(rowKey + "_" + currentSeries);
			} else {
				rk = new RowKey(rowKey);
			}
			results.add(readImageAndMetadata(path, rk, currentSeries));
		});

		m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
		return results.stream();
	}
}