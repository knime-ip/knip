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
package org.knime.knip.io.nodes.imgreader2.readfromdialog;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.io.nodes.imgreader2.AbstractImgReaderNodeModel;
import org.knime.knip.io.nodes.imgreader2.MetadataMode;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

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
public class ImgReader2NodeModel<T extends RealType<T> & NativeType<T>> extends AbstractImgReaderNodeModel<T> {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgReader2NodeModel.class);

	/**
	 * @return Model to store if the complete path should be used as row key
	 */
	public static final SettingsModelBoolean createCompletePathRowKeyModel() {
		return new SettingsModelBoolean("complete_path_rowkey", false);
	}

	/**
	 * @return Model for the settings holding the file list.
	 */
	public static SettingsModelStringArray createFileListModel() {
		return new SettingsModelStringArray("file_list", new String[] {});

	}

	private final SettingsModelStringArray m_files = createFileListModel();
	protected final SettingsModelBoolean m_completePathRowKey = createCompletePathRowKeyModel();

	/**
	 * Initializes the ImageReader
	 */
	public ImgReader2NodeModel() {
		super(0, 1);

		addSettingsModels(m_files, m_completePathRowKey);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		// check if some files are selected
		if (m_files == null || m_files.getStringArrayValue() == null || m_files.getStringArrayValue().length == 0) {
			throw new InvalidSettingsException("No files selected!");
		}

		return new DataTableSpec[] { getOutspec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		if (m_files == null || m_files.getStringArrayValue() == null || m_files.getStringArrayValue().length == 0) {
			throw new InvalidSettingsException("No files selected!");
		}

		// create image function
		ReadImg2Function<T> rifp = createImgTableFunction(exec, inData[0].getRowCount());

		// boolean for exceptions and file format
		final AtomicBoolean encounteredExceptions = new AtomicBoolean(false);

		BufferedDataContainer bdc = exec.createDataContainer(getOutspec());
		Arrays.asList(m_files.getStringArrayValue()).stream().flatMap(rifp).forEachOrdered(dataRow -> {

			if (dataRow.getSecond().isPresent()) {
				encounteredExceptions.set(true);
				LOGGER.warn("Encountered exception while reading image " + dataRow.getFirst().getKey()
						+ "! Caught Exception: " + dataRow.getSecond().get().getMessage());
				LOGGER.debug(dataRow.getSecond().get());
			}

			bdc.addRowToTable(dataRow.getFirst());
		});
		bdc.close();

		// data table for the table cell viewer
		m_data = bdc.getTable();

		return new BufferedDataTable[] { bdc.getTable() };
	}

	@Override
	public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
			final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		return new StreamableOperator() {
			@Override
			public void runFinal(PortInput[] inputs, PortOutput[] outputs, ExecutionContext exec) throws Exception {

				ReadImg2Function<T> rifp = createImgTableFunction(exec, m_files.getStringArrayValue().length);

				RowOutput out = (RowOutput) outputs[0];
				Stream.of(m_files.getStringArrayValue()).flatMap(rifp).forEachOrdered(result -> {
					if (result.getSecond().isPresent()) {
						LOGGER.warn("Encountered exception while reading image " + result.getFirst().getKey()
								+ "! Caught Exception: " + result.getSecond().get().getMessage());
						LOGGER.debug(result.getSecond().get());
					}

					try {
						out.push(result.getFirst());
					} catch (Exception exc) {
						LOGGER.warn("Couldn't push result for row " + result.getFirst().getKey());
					}
				});

				out.close();
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

	protected DataTableSpec getOutspec() {
		MetadataMode metadataMode = EnumUtils.valueForName(m_metadataModeModel.getStringValue(), MetadataMode.values());

		boolean readImage = (metadataMode == MetadataMode.NO_METADATA || metadataMode == MetadataMode.APPEND_METADATA)
				? true : false;
		boolean readMetadata = (metadataMode == MetadataMode.APPEND_METADATA
				|| metadataMode == MetadataMode.METADATA_ONLY) ? true : false;

		DataColumnSpecCreator creator;
		// size of spec from on the reader settings.
		final DataColumnSpec[] cspecs = new DataColumnSpec[(readImage ? 1 : 0) + (readMetadata ? 1 : 0)];
		if (readImage) {
			creator = new DataColumnSpecCreator("Image", ImgPlusCell.TYPE);
			cspecs[0] = creator.createSpec();
		}
		if (readMetadata) {
			creator = new DataColumnSpecCreator("OME-XML Metadata", XMLCell.TYPE);
			cspecs[cspecs.length - 1] = creator.createSpec();
		}

		return new DataTableSpec(cspecs);
	}

	private ReadImg2Function<T> createImgTableFunction(ExecutionContext exec, int rowCount) {

		MetadataMode metadataMode = EnumUtils.valueForName(m_metadataModeModel.getStringValue(), MetadataMode.values());
		boolean readImage = (metadataMode == MetadataMode.NO_METADATA || metadataMode == MetadataMode.APPEND_METADATA)
				? true : false;
		boolean readMetadata = (metadataMode == MetadataMode.APPEND_METADATA
				|| metadataMode == MetadataMode.METADATA_ONLY) ? true : false;

		// create ImgFactory
		ImgFactory<T> imgFac;
		if (m_imgFactory.getStringValue().equals(IMG_FACTORIES[1])) {
			imgFac = new PlanarImgFactory<T>();
		} else if (m_imgFactory.getStringValue().equals(IMG_FACTORIES[2])) {
			imgFac = new CellImgFactory<T>();
		} else {
			imgFac = new ArrayImgFactory<T>();
		}

		// series selection
		int seriesSelection;
		if (m_readAllSeries.getBooleanValue()) {
			seriesSelection = -1;
		} else {
			seriesSelection = m_seriesSelection.getIntValue();
		}

		// create image function
		ReadImg2Function<T> rifp = new ReadImg2Function<T>(exec, rowCount, m_planeSelect, readImage, readMetadata,
				m_readAllMetaDataModel.getBooleanValue(), m_checkFileFormat.getBooleanValue(),
				m_completePathRowKey.getBooleanValue(), m_isGroupFiles.getBooleanValue(), seriesSelection, imgFac);

		return rifp;
	}
}
