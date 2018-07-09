package org.knime.knip.io2.nodes.imgreader3.table;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import net.imagej.ImgPlus;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.io2.IO2Gateway;
import org.knime.knip.io2.ScifioImgSource;
import org.knime.knip.io2.nodes.imgreader3.AbstractImgReaderNodeModel;
import org.knime.knip.io2.nodes.imgreader3.ColumnCreationMode;
import org.knime.knip.io2.nodes.imgreader3.ImgReaderSettings;
import org.knime.knip.io2.nodes.imgreader3.ImgReaderSettings.ImgFactoryMode;
import org.knime.knip.io2.resolver.AuthAwareResolver;
import org.scijava.io.location.Location;
import org.scijava.io.location.LocationResolver;
import org.scijava.io.location.LocationService;

public class ImgReaderTableNodeModel<T extends RealType<T> & NativeType<T>> extends AbstractImgReaderNodeModel<T> {

	private static final int CONNECTION = 1;
	private static final int DATA = 0;
	protected static final NodeLogger LOGGER = NodeLogger.getLogger(ImgReaderTableNodeModel.class);

	/** Settings Models */
	private final SettingsModelColumnName m_filenameColumnModel = ImgReaderSettings.createFileURIColumnModel();
	private final SettingsModelString m_columnCreationModeModel = ImgReaderSettings.createColumnCreationModeModel();
	private final SettingsModelString m_columnSuffixModel = ImgReaderSettings.createColumnSuffixNodeModel();
	private final SettingsModelBoolean m_appendSeriesNumberModel = ImgReaderSettings.createAppendSeriesNumberModel();

	private final LocationService loc = IO2Gateway.locations();

	protected ImgReaderTableNodeModel() {
		super(new PortType[] { BufferedDataTable.TYPE, ConnectionInformationPortObject.TYPE_OPTIONAL },
				new PortType[] { BufferedDataTable.TYPE });

		addAdditionalSettingsModels(
				Arrays.asList(m_filenameColumnModel, m_columnCreationModeModel, m_columnSuffixModel));
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		return createOutSpec(inSpecs);
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final ImgPlusCellFactory cellFactory = new ImgPlusCellFactory(exec);
		final AtomicInteger errorCount = new AtomicInteger(0);

		final ConnectionInformation connectionInfo;
		final PortObjectSpec[] outSpec;
		if (inObjects[CONNECTION] != null) {
			connectionInfo = ((ConnectionInformationPortObject) inObjects[CONNECTION]).getConnectionInformation();
			outSpec = createOutSpec(
					new PortObjectSpec[] { inObjects[DATA].getSpec(), inObjects[CONNECTION].getSpec(), });
		} else {
			connectionInfo = null;
			outSpec = createOutSpec(new PortObjectSpec[] { null, inObjects[DATA].getSpec() });
		}

		final BufferedDataTable in = (BufferedDataTable) inObjects[DATA];
		final BufferedDataContainer container = exec.createDataContainer((DataTableSpec) outSpec[0]);
		final int uriColIdx = getUriColIdx(in.getDataTableSpec());

		final boolean checkFormat = m_checkFileFormatModel.getBooleanValue();
		final String factoryname = m_imgFactoryModel.getStringValue();
		final ImgFactory factory = ImgFactoryMode.getFactoryFromName(factoryname);

		final boolean isGroup = m_isGroupFilesModel.getBooleanValue();

		final ColumnCreationMode columnCreationMode = EnumUtils.valueForName(m_columnCreationModeModel.getStringValue(),
				ColumnCreationMode.values());

		final ScifioImgSource source = new ScifioImgSource(factory, checkFormat, isGroup);

		// use a column rearanger for replace
		if (columnCreationMode == ColumnCreationMode.REPLACE) {
			return new BufferedDataTable[] {
					exec.createColumnRearrangeTable(in, createRearanger(), exec.createSubProgress(1)) };
		}

		for (final DataRow row : in) {
			final URI uri = ((URIDataValue) row.getCell(uriColIdx)).getURIContent().getURI();
			final DataCell[] cells = readFromURI(uri, connectionInfo, source, cellFactory);

			if (columnCreationMode == ColumnCreationMode.APPEND) {
				container.addRowToTable(new AppendedColumnRow(row, cells));
			} else if (columnCreationMode == ColumnCreationMode.NEW_TABLE) {
				container.addRowToTable(new DefaultRow(row.getKey(), cells));
			}

		}

		container.close();
		setInternalTables(new BufferedDataTable[] { container.getTable() });
		return new PortObject[] { container.getTable() };
	}

	private ColumnRearranger createRearanger() {
		throw new UnsupportedOperationException("NYI");
	}

	private DataCell[] readFromURI(final URI uri, final ConnectionInformation connectionInfo,
			final ScifioImgSource source, final ImgPlusCellFactory cellFactory) throws Exception {

		Location resolved;
		final LocationResolver resolver = loc.getResolver(uri);
		if (resolver == null) {
			throw new IllegalArgumentException("No resulver found for location: " + loc.toString());
		}
		// handle authentication
		if (resolver instanceof AuthAwareResolver) {
			if (connectionInfo == null) {
				throw new IOException("Connection information required but not provided!");
			}
			resolved = ((AuthAwareResolver) resolver).resolveWithAuth(uri, connectionInfo);
		} else {
			resolved = resolver.resolve(uri);
		}

		if (resolved == null) {
			throw new IllegalArgumentException("Could not resolve url: " + uri.toString());
		}
		final ImgPlus<RealType> img = source.getImg(resolved, 0);
		final ImgPlusCell cell = cellFactory.createCell(img);
		return new DataCell[] { cell };

	}

	private PortObjectSpec[] createOutSpec(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

		// ensure there is a valid column
		final int uriColIdx = getUriColIdx(inSpecs[DATA]);

		// initialze the settings

		final DataTableSpec spec = (DataTableSpec) inSpecs[DATA];

//		final DataTableSpec outSpec = new DataTableSpecCreator(spec).addColumns(imgSpec).createSpec();
//		return new PortObjectSpec[] { outSpec };

//		final MetadataMode metaDataMode = EnumUtils.valueForName(m_metadataModeModel.getStringValue(),
//				MetadataMode.values());

//		final boolean readImage = metaDataMode == MetadataMode.NO_METADATA
//				|| metaDataMode == MetadataMode.APPEND_METADATA;
//		final boolean readMetadata = metaDataMode == MetadataMode.APPEND_METADATA
//				|| metaDataMode == MetadataMode.METADATA_ONLY;

		final ColumnCreationMode columnCreationMode = EnumUtils.valueForName(m_columnCreationModeModel.getStringValue(),
				ColumnCreationMode.values());

//		 Create the outspec

		final DataTableSpec outSpec;
		if (columnCreationMode == ColumnCreationMode.NEW_TABLE) {

			final DataTableSpecCreator specBuilder = new DataTableSpecCreator();
			specBuilder.addColumns(new DataColumnSpecCreator("Image", ImgPlusCell.TYPE).createSpec());
			// TODO add series reading
//			if (m_readAllSeriesModel.getBooleanValue() || m_appendSeriesNumberModel.getBooleanValue()) {
//				specBuilder.addColumns(new DataColumnSpecCreator("Series Number", StringCell.TYPE).createSpec());
//			}

			outSpec = specBuilder.createSpec();

		} else { // Append and replace
			final DataColumnSpec imgSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "Image" + m_columnSuffixModel.getStringValue()),
					ImgPlusCell.TYPE).createSpec();
//			final DataColumnSpec metaDataSpec = new DataColumnSpecCreator(
//					DataTableSpec.getUniqueColumnName(spec, "OME-XML Metadata" + m_columnSuffixModel.getStringValue()),
//					XMLCell.TYPE).createSpec();
			final DataColumnSpec seriesNumberSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "Series Number"), StringCell.TYPE).createSpec();

			final DataTableSpecCreator outSpecBuilder = new DataTableSpecCreator(spec);

			if (columnCreationMode == ColumnCreationMode.APPEND) {
				outSpecBuilder.addColumns(imgSpec);
				if (m_appendSeriesNumberModel.getBooleanValue()) {
					outSpecBuilder.addColumns(seriesNumberSpec);
				}

				outSpec = outSpecBuilder.createSpec();

			} else if (columnCreationMode == ColumnCreationMode.REPLACE) {

				// As we can only replace the URI column, we append all
				// additional columns.
				boolean replaced = false;

				// replaced is always false in this case
				outSpecBuilder.replaceColumn(uriColIdx, imgSpec);
				replaced = true;
				if (m_appendSeriesNumberModel.getBooleanValue()) {
					if (!replaced) {
						outSpecBuilder.replaceColumn(uriColIdx, seriesNumberSpec);
					} else {
						outSpecBuilder.addColumns(seriesNumberSpec);
					}
				}

				outSpec = outSpecBuilder.createSpec();
			} else {
				// should really not happen
				throw new IllegalStateException("Support for the columncreation mode"
						+ m_columnCreationModeModel.getStringValue() + " is not implemented!");
			}
		}
		return new PortObjectSpec[] { outSpec };
	}

	private int getUriColIdx(final PortObjectSpec inSpec) throws InvalidSettingsException {
		return NodeUtils.autoColumnSelection((DataTableSpec) inSpec, m_filenameColumnModel, URIDataValue.class,
				ImgReaderTableNodeModel.class);
	}

	@Override
	public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
			final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

		return new StreamableOperator() {
			@Override
			public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
					throws Exception {

				final RowInput in = (RowInput) inputs[DATA];
				final RowOutput out = (RowOutput) outputs[0];

				final AtomicInteger encounteredExceptionsCount = new AtomicInteger(0);

				// FIXME implement

				in.close();
				out.close();
//				reader.close();
			}
		};
	}

	@Override
	protected void doLoadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
		// nothing to do
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	private void handleReadErrors(final AtomicInteger encounteredExceptionsCount, final RowKey rowKey,
			final Throwable throwable) {
		encounteredExceptionsCount.incrementAndGet();

		LOGGER.warn("Encountered exception while reading from source: " + rowKey + " ; view log for more info.");

		LOGGER.debug(throwable);
	}

}
