package org.knime.knip.io.nodes.imgreader2.readfrominput;

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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
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
 * @author Daniel Seebacher, University of Konstanz.
 *
 */
class ReadImgWithInputFunction<T extends RealType<T>> extends AbstractReadImgFunction<T, DataRow> {

	private String columnCreationMode;
	private int stringIndex;

	public ReadImgWithInputFunction(ExecutionContext exec, int numberOfFiles, SettingsModelSubsetSelection sel,
			boolean readImage, boolean readMetadata, boolean readAllMetaData, boolean checkFileFormat,
			boolean isGroupFiles, int selectedSeries, ImgFactory<T> imgFactory, String columnCreationMode,
			int stringIndex) {
		super(exec, numberOfFiles, sel, readImage, readMetadata, readAllMetaData, checkFileFormat, false, isGroupFiles,
				selectedSeries, imgFactory);

		this.columnCreationMode = columnCreationMode;
		this.stringIndex = stringIndex;
	}

	@Override
	public Stream<Pair<DataRow, Optional<Exception>>> apply(DataRow input) {
		List<Pair<DataRow, Optional<Exception>>> tempResults = new ArrayList<>();

		if (input.getCell(stringIndex).isMissing()) {
			m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
			return Arrays.asList(createResultFromException("no path specified", input.getKey().getString(),
					new IllegalArgumentException("Input was missing"))).stream();
		}

		String t = ((StringValue) input.getCell(stringIndex)).getStringValue();
		String path;
		int numSeries;
		try {
			path = FileUtil.resolveToPath(FileUtil.toURL(t)).toString();
			numSeries = m_imgSource.getSeriesCount(path);
		} catch (InvalidPathException | IOException | URISyntaxException exc) {
			m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
			return Arrays.asList(createResultFromException(t, input.getKey().getString(), exc)).stream();
		} catch (Exception exc) {
			m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
			return Arrays.asList(createResultFromException(t, input.getKey().getString(), exc)).stream();
		}

		int seriesStart = m_selectedSeries == -1 ? 0 : m_selectedSeries;
		int seriesEnd = m_selectedSeries == -1 ? numSeries : m_selectedSeries + 1;

		// load image and metadata for each series index
		IntStream.range(seriesStart, seriesEnd).forEachOrdered(currentSeries -> {
			RowKey rowKey = input.getKey();
			if (currentSeries > 1) {
				rowKey = new RowKey(rowKey.getString() + "_" + currentSeries);
			}

			tempResults.add(readImageAndMetadata(path, rowKey, currentSeries));
		});

		m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);

		return createOutput(input, tempResults, columnCreationMode, stringIndex);
	}

	/**
	 * Takes the input {@link DataRow}, the read images and metadata and creates
	 * the output result row.
	 * 
	 * @param inputRow
	 *            the input {@link DataRow}
	 * @param readFiles
	 *            the {@link List} of read {@link Img}s, with {@link Optional}
	 *            {@link Exception}s.
	 * @param columnSelectionMode
	 *            the column selection mode see
	 *            {@link ImgReaderWithInputNodeModel#COL_CREATION_MODES}
	 * @param inputColumnIndex
	 *            the column index of the path.
	 * @return a {@link Stream} with the output {@link DataRow}s.
	 */
	private Stream<Pair<DataRow, Optional<Exception>>> createOutput(DataRow inputRow,
			List<Pair<DataRow, Optional<Exception>>> readFiles, String columnSelectionMode, int inputColumnIndex) {

		List<Pair<DataRow, Optional<Exception>>> outputResults = new ArrayList<>();
		if (columnCreationMode.equalsIgnoreCase(ImgReaderWithInputNodeModel.COL_CREATION_MODES[0])) {
			return readFiles.stream();
		} else if (columnCreationMode.equalsIgnoreCase(ImgReaderWithInputNodeModel.COL_CREATION_MODES[1])) {
			for (Pair<DataRow, Optional<Exception>> result : readFiles) {

				List<DataCell> cells = new ArrayList<>();
				for (int i = 0; i < inputRow.getNumCells(); i++) {
					cells.add(inputRow.getCell(i));
				}

				for (int i = 0; i < result.getFirst().getNumCells(); i++) {
					cells.add(result.getFirst().getCell(i));
				}

				outputResults.add(new Pair<>(
						new DefaultRow(result.getFirst().getKey(), cells.toArray(new DataCell[cells.size()])),
						result.getSecond()));
			}
		} else {
			for (Pair<DataRow, Optional<Exception>> result : readFiles) {

				List<DataCell> cells = new ArrayList<>();
				for (int i = 0; i < inputRow.getNumCells(); i++) {
					cells.add(inputRow.getCell(i));
				}

				cells.set(stringIndex, result.getFirst().getCell(0));
				if (result.getFirst().getNumCells() > 1) {
					cells.add(stringIndex + 1, result.getFirst().getCell(1));
				}

				outputResults.add(new Pair<>(
						new DefaultRow(result.getFirst().getKey(), cells.toArray(new DataCell[cells.size()])),
						result.getSecond()));
			}
		}

		return outputResults.stream();
	}
}