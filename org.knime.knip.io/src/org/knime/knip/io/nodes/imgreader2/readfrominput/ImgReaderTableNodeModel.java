/**
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
package org.knime.knip.io.nodes.imgreader2.readfrominput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.io.nodes.imgreader2.AbstractImgReaderNodeModel;
import org.knime.knip.io.nodes.imgreader2.ColumnCreationMode;
import org.knime.knip.io.nodes.imgreader2.MetadataMode;

/**
 * This Node reads images.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn"> Gabriel Einsdorf</a>
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher,
 *         University of Konstanz.</a>
 */
public class ImgReaderTableNodeModel<T extends RealType<T> & NativeType<T>> extends AbstractImgReaderNodeModel<T> {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgReaderTableNodeModel.class);

	/**
	 * @return Model to store the selected column in the optional input table
	 */
	public static SettingsModelString createFilenameColumnModel() {
		return new SettingsModelString("filename_column", "");
	}

	public static SettingsModelString createColCreationModeModel() {
		return new SettingsModelString("m_colCreationMode", ColumnCreationMode.NEW_TABLE.toString());
	}

	public static SettingsModelString createColSuffixNodeModel() {
		return new SettingsModelString("m_colSuffix", "");
	}

	private final SettingsModelString m_filenameColumn = createFilenameColumnModel();
	private final SettingsModelString m_colCreationMode = createColCreationModeModel();
	private final SettingsModelString m_colSuffix = createColSuffixNodeModel();

	public ImgReaderTableNodeModel() {
		super(1, 1);

		addSettingsModels(m_filenameColumn, m_colCreationMode, m_colSuffix);
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		final int imgIdx = getPathColIdx(inSpecs[0]);
		if (-1 == imgIdx) {
			throw new InvalidSettingsException("A string column must be selected!");
		}

		return new DataTableSpec[] { getOutspec(inSpecs[0], imgIdx) };
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		// boolean for exceptions and file format
		final AtomicBoolean encounteredExceptions = new AtomicBoolean(false);

		final int imgIdx = getPathColIdx(inData[0].getDataTableSpec());
		final ReadImgTableFunction<T> rifp = createImgTableFunction(exec, inData[0].getDataTableSpec(),
				Long.valueOf(inData[0].size()).intValue());

		final BufferedDataContainer bdc = exec.createDataContainer(getOutspec(inData[0].getDataTableSpec(), imgIdx));

		for (final DataRow x : inData[0]) {
			rifp.apply(x).forEachOrdered(dataRow -> {
				if (dataRow.getSecond().isPresent()) {
					encounteredExceptions.set(true);
					LOGGER.warn("Encountered exception while reading image: " + dataRow.getFirst().getKey()
							+ "! view log for more info.");
					LOGGER.debug("Encountered exception while reading image:", dataRow.getSecond().get());
				}
				bdc.addRowToTable(dataRow.getFirst());
			});
			exec.checkCanceled();
		}

		// close img file sources
		rifp.close();

		bdc.close();
		// data table for the table cell viewer
		m_data = bdc.getTable();

		if (encounteredExceptions.get()) {
			setWarningMessage("Encountered errors during execution!");
		}

		return new BufferedDataTable[] { bdc.getTable() };
	}

	@Override
	public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
			final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		return new StreamableOperator() {
			@Override
			public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
					throws Exception {
				final RowInput in = (RowInput) inputs[0];
				final RowOutput out = (RowOutput) outputs[0];

				// boolean for exceptions and file format
				final AtomicBoolean encounteredExceptions = new AtomicBoolean(false);

				final ReadImgTableFunction<T> readImgFunction = createImgTableFunction(exec, in.getDataTableSpec(), 1);

				DataRow row;
				while ((row = in.poll()) != null) {
					readImgFunction.apply(row).forEachOrdered(result -> {
						if (result.getSecond().isPresent()) {
							encounteredExceptions.set(true);
							LOGGER.warn("Encountered exception while reading image: " + result.getFirst().getKey()
									+ "! view log for more info.");
							LOGGER.debug("Encountered exception while reading image:", result.getSecond().get());
						}

						try {
							out.push(result.getFirst());
						} catch (final Exception exc) {
							encounteredExceptions.set(true);
							LOGGER.warn("Couldn't push row " + result.getFirst().getKey() + " into output stream.");
							LOGGER.debug("Encountered exception when trying to push result: ", exc);
						}
					});
				}

				if (encounteredExceptions.get()) {
					setWarningMessage("Encountered errors during execution!");
				}

				in.close();
				out.close();

				readImgFunction.close();
			}
		};
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	private DataTableSpec getOutspec(final DataTableSpec spec, final int imgIdx) {
		final MetadataMode metadataMode = EnumUtils.valueForName(m_metadataModeModel.getStringValue(),
				MetadataMode.values());

		final boolean readImage = (metadataMode == MetadataMode.NO_METADATA
				|| metadataMode == MetadataMode.APPEND_METADATA) ? true : false;
		final boolean readMetadata = (metadataMode == MetadataMode.APPEND_METADATA
				|| metadataMode == MetadataMode.METADATA_ONLY) ? true : false;

		DataTableSpec outSpec;
		// new table
		final ColumnCreationMode columnCreationMode = ColumnCreationMode.fromString(m_colCreationMode.getStringValue());
		if (columnCreationMode == ColumnCreationMode.NEW_TABLE) {

			final DataColumnSpec imgSpec = new DataColumnSpecCreator("Image", ImgPlusCell.TYPE).createSpec();
			final DataColumnSpec omeSpec = new DataColumnSpecCreator("OME-XML Metadata", XMLCell.TYPE).createSpec();

			if (readImage && readMetadata) {
				outSpec = new DataTableSpec(imgSpec, omeSpec);
			} else if (readImage) {
				outSpec = new DataTableSpec(imgSpec);
			} else {
				outSpec = new DataTableSpec(omeSpec);
			}

		}
		// append
		else if (columnCreationMode == ColumnCreationMode.APPEND) {

			final DataColumnSpec imgSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "Image" + m_colSuffix.getStringValue()), ImgPlusCell.TYPE)
							.createSpec();
			final DataColumnSpec omeSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "OME-XML Metadata" + m_colSuffix.getStringValue()),
					XMLCell.TYPE).createSpec();

			final List<DataColumnSpec> list = new ArrayList<>();
			for (int i = 0; i < spec.getNumColumns(); i++) {
				list.add(spec.getColumnSpec(i));
			}

			if (readImage && readMetadata) {
				list.add(imgSpec);
				list.add(omeSpec);
			} else if (readImage) {
				list.add(imgSpec);
			} else {
				list.add(omeSpec);
			}

			outSpec = new DataTableSpec(list.toArray(new DataColumnSpec[list.size()]));
		}
		// replace
		else if (columnCreationMode == ColumnCreationMode.REPLACE) {
			final DataColumnSpec imgSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "Image" + m_colSuffix.getStringValue()), ImgPlusCell.TYPE)
							.createSpec();
			final DataColumnSpec omeSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "OME-XML Metadata" + m_colSuffix.getStringValue()),
					XMLCell.TYPE).createSpec();

			final List<DataColumnSpec> list = new ArrayList<>();
			for (int i = 0; i < spec.getNumColumns(); i++) {
				list.add(spec.getColumnSpec(i));
			}

			if (readImage && readMetadata) {
				list.set(imgIdx, imgSpec);
				list.add(imgIdx + 1, omeSpec);
			} else if (readImage) {
				list.set(imgIdx, imgSpec);
			} else {
				list.set(imgIdx, omeSpec);
			}

			outSpec = new DataTableSpec(list.toArray(new DataColumnSpec[list.size()]));
		} else {
			throw new IllegalStateException("Support for the columncreation mode" + m_colCreationMode.getStringValue()
					+ " is not implemented!");
		}

		return outSpec;
	}

	private int getPathColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
		int imgColIndex = -1;
		if (null == this.m_filenameColumn.getStringValue()) {
			return imgColIndex;
		}
		imgColIndex = inSpec.findColumnIndex(this.m_filenameColumn.getStringValue());
		if (-1 == imgColIndex) {
			if ((imgColIndex = NodeUtils.autoOptionalColumnSelection(inSpec, this.m_filenameColumn,
					StringValue.class)) >= 0) {
				setWarningMessage("Auto-configure Image Column: " + this.m_filenameColumn.getStringValue());
			} else {
				throw new InvalidSettingsException("No column selected!");
			}
		}

		return imgColIndex;
	}

	private ReadImgTableFunction<T> createImgTableFunction(final ExecutionContext exec, final DataTableSpec inSpec,
			final int rowCount) throws InvalidSettingsException {

		final int imgIdx = getPathColIdx(inSpec);

		final MetadataMode metadataMode = EnumUtils.valueForName(m_metadataModeModel.getStringValue(),
				MetadataMode.values());
		final boolean readImage = (metadataMode == MetadataMode.NO_METADATA
				|| metadataMode == MetadataMode.APPEND_METADATA) ? true : false;
		final boolean readMetadata = (metadataMode == MetadataMode.APPEND_METADATA
				|| metadataMode == MetadataMode.METADATA_ONLY) ? true : false;

		// create ImgFactory
		ImgFactory<T> imgFac;
		if (m_imgFactory.getStringValue().equals(IMG_FACTORIES[1])) {
			imgFac = new PlanarImgFactory<>();
		} else if (m_imgFactory.getStringValue().equals(IMG_FACTORIES[2])) {
			imgFac = new CellImgFactory<>();
		} else {
			imgFac = new ArrayImgFactory<>();
		}

		// series selection
		int seriesSelectionFrom;
		int seriesSelectionTo;

		if (m_readAllSeries.getBooleanValue()) {
			seriesSelectionFrom = -1;
			seriesSelectionTo = -1;
		} else {
			seriesSelectionFrom = Double.valueOf(m_seriesRangeSelection.getMinRange()).intValue();
			seriesSelectionTo = Double.valueOf(m_seriesRangeSelection.getMaxRange()).intValue();
		}

		// create image function
		final ReadImgTableFunction<T> rifp = new ReadImgTableFunction<>(exec, rowCount, m_planeSelect, readImage,
				readMetadata, m_readAllMetaDataModel.getBooleanValue(), m_checkFileFormat.getBooleanValue(),
				m_isGroupFiles.getBooleanValue(), seriesSelectionFrom, seriesSelectionTo, imgFac,
				ColumnCreationMode.fromString(m_colCreationMode.getStringValue()), imgIdx,
				m_pixelType.getStringValue());

		return rifp;
	}
}
