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
package org.knime.knip.base.nodes.proc.binary.morphops;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Dilate;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.DilateGray;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Erode;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.ErodeGray;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.StructuringElementCursor;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;
import org.knime.knip.core.util.ImgUtils;

/**
 * NodeModel for Morphological Image Operations
 *
 * @param <T>
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 */
public class MorphImgOpsNodeModel<T extends RealType<T>> extends ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>> {

    private static NodeLogger LOGGER = NodeLogger.getLogger(MorphImgOpsNodeModel.class);

    enum ConnectedType {
        EIGHT_CONNECTED("Eight-Connected"), FOUR_CONNECTED("Four-Connected"),
        STRUCTURING_ELEMENT("Structuring Element");
        static final List<String> NAMES = new ArrayList<String>();

        static {
            for (final ConnectedType e : ConnectedType.values()) {
                NAMES.add(e.toString());
            }
        }

        static ConnectedType value(final String name) {
            return values()[NAMES.indexOf(name)];
        }

        private final String m_name;

        private ConnectedType(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    enum MorphOp {
        CLOSE("Close"), DILATE("Dilate"), ERODE("Erode"), OPEN("Open");
        static final List<String> NAMES = new ArrayList<String>();

        static {
            for (final MorphOp e : MorphOp.values()) {
                NAMES.add(e.toString());
            }
        }

        static MorphOp value(final String name) {
            return values()[NAMES.indexOf(name)];
        }

        private final String m_name;

        private MorphOp(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    static SettingsModelString createColStructureModel() {
        return new SettingsModelString("column_structure", "<none>");
    }

    static SettingsModelString createConnectionTypeModel() {
        return new SettingsModelString("connection_type", ConnectedType.FOUR_CONNECTED.toString());
    }

    static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimension_selection", "X", "Y");
    }

    static SettingsModelIntegerBounded createIterationsModel() {
        return new SettingsModelIntegerBounded("iterations", 1, 1, Integer.MAX_VALUE);
    }

    @Deprecated
    static SettingsModelIntegerBounded createNeighborhoodCountModel() {
        return new SettingsModelIntegerBounded("neighborhood_count", 1, 1, Integer.MAX_VALUE);
    }

    static SettingsModelString createOperationModel() {
        return new SettingsModelString("OPERATION", MorphOp.ERODE.toString());
    }

    static SettingsModelString createOutOfBoundsModel() {
        return new SettingsModelString("outofboundsstrategy", OutOfBoundsStrategyEnum.BORDER.toString());
    }

    private UnaryObjectFactory<RandomAccessibleInterval<BitType>, RandomAccessibleInterval<BitType>> m_bitFac;

    private UnaryObjectFactory<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> m_fac;

    private ImgPlusCellFactory m_imgCellFactory;

    private final SettingsModelString m_smConnectionType = createConnectionTypeModel();

    private final SettingsModelDimSelection m_smDimensions = createDimSelectionModel();

    private final SettingsModelIntegerBounded m_smIterations = createIterationsModel();

    // TODO: Remove with 2.0.0
    @Deprecated
    private final SettingsModelIntegerBounded m_smNeighborhood = createNeighborhoodCountModel();

    private final SettingsModelString m_smOperation = createOperationModel();

    private final SettingsModelString m_smStructurColumn = createColStructureModel();

    private long[][] m_structElement = null;

    private int m_structurColumnIndex;

    private final SettingsModelString m_smOutOfBoundsStrategy = createOutOfBoundsModel();

    /**
     * Constructor
     */
    protected MorphImgOpsNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE_OPTIONAL});

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_smConnectionType);
        settingsModels.add(m_smStructurColumn);
        settingsModels.add(m_smDimensions);
        settingsModels.add(m_smOperation);

        // TODO: Remove with 2.0.0
        // settingsModels.add(m_smNeighborhood);

        settingsModels.add(m_smIterations);
        settingsModels.add(m_smOutOfBoundsStrategy);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws KNIPException, IOException {
        final ImgPlus<T> in = cellValue.getImgPlus();

        if ((m_structElement != null) && ( (m_smDimensions.getNumSelectedDimLabels() != m_structElement[0].length) || (in.numDimensions() != m_structElement[0].length) )) {
            throw new KNIPException("Structuring element must have the same dimensionality as the chosen dims");
        }

        if (in.firstElement() instanceof BitType) {

            m_bitFac = new UnaryObjectFactory<RandomAccessibleInterval<BitType>, RandomAccessibleInterval<BitType>>() {

                @Override
                public Img<BitType> instantiate(final RandomAccessibleInterval<BitType> a) {

                    try {
                        return in.factory().imgFactory(new BitType())
                                .create(a, Views.iterable(a).firstElement().createVariable());
                    } catch (IncompatibleTypeException e) {
                        //TODO better error handling here
                        throw new RuntimeException(e);
                    }
                }
            };

            final UnaryOperation<RandomAccessibleInterval<BitType>, RandomAccessibleInterval<BitType>> op =
                    createOperationBit(m_structElement);

            final Img<BitType> out = ImgUtils.createEmptyCopy(in, new BitType());

            try {
                Img<BitType> inAsBitType = (Img<BitType>)in;
                OutOfBoundsFactory<BitType, RandomAccessibleInterval<BitType>> strategy =
                        OutOfBoundsStrategyFactory.getStrategy(m_smOutOfBoundsStrategy.getStringValue(),
                                                               inAsBitType.firstElement());
                IntervalView<BitType> extend = Views.interval(Views.extend(inAsBitType, strategy), inAsBitType);

                SubsetOperations.iterate(op, m_smDimensions.getSelectedDimIndices(in), new ImgView<BitType>(extend,
                        inAsBitType.factory()), out, getExecutorService());
            } catch (final InterruptedException e) {
                LOGGER.warn("Thread execution was interrupted", e);
            } catch (final ExecutionException e) {
                e.printStackTrace();
                LOGGER.warn("Couldn't retrieve results from thread because execution was interrupted/aborted", e);
            }

            return (ImgPlusCell<T>)m_imgCellFactory.createCell(out, in);
        } else {
            m_fac = new UnaryObjectFactory<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>>() {

                @Override
                public Img<T> instantiate(final RandomAccessibleInterval<T> a) {

                    return in.factory().create(a, in.firstElement().createVariable());
                }
            };

            final Img<T> out = ImgUtils.createEmptyCopy(in);
            final UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> op =
                    createOperationGray(m_structElement, m_smDimensions.getNumSelectedDimLabels());

            try {
                OutOfBoundsFactory<T, RandomAccessibleInterval<T>> strategy =
                        OutOfBoundsStrategyFactory.getStrategy(m_smOutOfBoundsStrategy.getStringValue(),
                                                               in.firstElement());

                IntervalView<T> extend = Views.interval(Views.extend(in, strategy), in);

                SubsetOperations.iterate(op, m_smDimensions.getSelectedDimIndices(in),
                                         new ImgView<T>(extend, in.factory()), out, getExecutorService());
            } catch (final InterruptedException e) {
                LOGGER.warn("Thread execution was interrupted", e);
            } catch (final ExecutionException e) {
                e.printStackTrace();

                LOGGER.warn("Couldn't retrieve results from thread because execution was interrupted/aborted", e);
            }

            return m_imgCellFactory.createCell(out, in);
        }

    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[1].getNumColumns() > 0) {
            m_structurColumnIndex = inSpecs[1].findColumnIndex(m_smStructurColumn.getStringValue());
            if (m_structurColumnIndex == -1) {
                if ((m_structurColumnIndex =
                        NodeUtils.autoOptionalColumnSelection(inSpecs[1], m_smStructurColumn, ImgPlusValue.class)) >= 0) {
                    setWarningMessage("Auto-configure Column: " + m_smStructurColumn.getStringValue());
                } else {
                    throw new InvalidSettingsException("No column selected!");
                }
            }
        } else {
            m_smStructurColumn.setEnabled(false);
            m_structurColumnIndex = -1;
            if (ConnectedType.value(m_smConnectionType.getStringValue()) == ConnectedType.STRUCTURING_ELEMENT) {
                m_smConnectionType.setStringValue(ConnectedType.EIGHT_CONNECTED.toString());
                setWarningMessage("Auto-configure Connection Type: " + m_smConnectionType.getStringValue());
            }
        }
        return super.configure(inSpecs);
    }

    private Img<BitType> createEightConnectedKernel(final int numDims) {
        final long[] dims = new long[numDims];
        Arrays.fill(dims, 3);
        final Img<BitType> kernel = new ArrayImgFactory<BitType>().create(dims, new BitType());

        final Cursor<BitType> kernelCursor = kernel.cursor();
        while (kernelCursor.hasNext()) {
            kernelCursor.fwd();
            kernelCursor.get().set(true);
        }

        return kernel;
    }

    private Img<BitType> createFourConnectedKernel(final int numDims) {
        final long[] dims = new long[numDims];
        Arrays.fill(dims, 3);
        final Img<BitType> kernel = new ArrayImgFactory<BitType>().create(dims, new BitType());

        final Cursor<BitType> kernelCursor = kernel.cursor();
        while (kernelCursor.hasNext()) {
            kernelCursor.fwd();
            boolean valid = false;
            for (int d = 0; d < kernelCursor.numDimensions(); d++) {
                if (kernelCursor.getIntPosition(d) == 1) {
                    valid = true;
                    break;
                }
            }
            if (valid) {
                kernelCursor.get().set(true);
            }
        }

        return kernel;
    }

    @SuppressWarnings("deprecation")
    private UnaryOutputOperation<RandomAccessibleInterval<BitType>, RandomAccessibleInterval<BitType>>
            createOperationBit(final long[][] structuringElement) {
        UnaryOutputOperation<RandomAccessibleInterval<BitType>, RandomAccessibleInterval<BitType>> simpleErode;
        UnaryOutputOperation<RandomAccessibleInterval<BitType>, RandomAccessibleInterval<BitType>> simpleDilate;
        net.imglib2.ops.types.ConnectedType c = net.imglib2.ops.types.ConnectedType.EIGHT_CONNECTED;
        switch (ConnectedType.value(m_smConnectionType.getStringValue())) {
            case STRUCTURING_ELEMENT:
                if (structuringElement == null) {
                    throw new IllegalArgumentException(
                            "No structuring element available but structuring element selected");

                }

                simpleErode =
                        Operations.iterate(Operations.wrap(new ErodeGray<BitType>(structuringElement), m_bitFac),
                                           m_smIterations.getIntValue());

                simpleDilate =
                        Operations.iterate(Operations.wrap(new DilateGray<BitType>(structuringElement), m_bitFac),
                                           m_smIterations.getIntValue());
                break;
            case FOUR_CONNECTED:
                c = net.imglib2.ops.types.ConnectedType.FOUR_CONNECTED;
            case EIGHT_CONNECTED:
            default:

                simpleErode =
                        Operations.iterate(Operations.wrap(new Erode(c, m_smNeighborhood.getIntValue()), m_bitFac),
                                           m_smIterations.getIntValue());

                simpleDilate =
                        Operations.iterate(Operations.wrap(new Dilate(c, m_smNeighborhood.getIntValue()), m_bitFac),
                                           m_smIterations.getIntValue());
        }

        switch (MorphOp.value(m_smOperation.getStringValue())) {
            case OPEN:
                return Operations.concat(simpleErode, simpleDilate);
            case CLOSE:
                return Operations.concat(simpleDilate, simpleErode);
            case DILATE:
                return simpleDilate;
            case ERODE:
            default:
                return simpleErode;
        }
    }

    private UnaryOutputOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>>
            createOperationGray(long[][] structuringElement, final int numDims) {
        UnaryOutputOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> simpleErode;
        UnaryOutputOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> simpleDilate;
        switch (ConnectedType.value(m_smConnectionType.getStringValue())) {
            case STRUCTURING_ELEMENT:
                if (structuringElement == null) {
                    throw new IllegalArgumentException(
                            "No structuring element available but structuring element selected");

                }
                break;
            case FOUR_CONNECTED:
                structuringElement = StructuringElementCursor.createElementFromImg(createFourConnectedKernel(numDims));
                break;
            case EIGHT_CONNECTED:
                structuringElement = StructuringElementCursor.createElementFromImg(createEightConnectedKernel(numDims));
                break;

            default:
                throw new IllegalArgumentException("Can't find strucutring element");
        }

        simpleErode =
                Operations.iterate(Operations.wrap(new ErodeGray<T>(structuringElement), m_fac),
                                   m_smIterations.getIntValue());

        simpleDilate =
                Operations.iterate(Operations.wrap(new DilateGray<T>(structuringElement), m_fac),
                                   m_smIterations.getIntValue());

        switch (MorphOp.value(m_smOperation.getStringValue())) {
            case OPEN:
                return Operations.concat(simpleErode, simpleDilate);
            case CLOSE:
                return Operations.concat(simpleDilate, simpleErode);
            case DILATE:
                return simpleDilate;
            case ERODE:
            default:
                return simpleErode;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        if (ConnectedType.value(m_smConnectionType.getStringValue()) == ConnectedType.STRUCTURING_ELEMENT) {

            if (((BufferedDataTable)inObjects[1]) == null) {
                throw new IllegalArgumentException(
                        "\'Structuring Element\' as connection type selected, but no structuring element in second inport provided");
            }

            final Iterator<DataRow> it = ((BufferedDataTable)inObjects[1]).iterator();
            if (it.hasNext()) {
                @SuppressWarnings("unchecked")
                final ImgPlus<BitType> struct =
                        ((ImgPlusValue<BitType>)it.next().getCell(m_structurColumnIndex)).getImgPlus();

                m_structElement = StructuringElementCursor.createElementFromImg(struct);
                // TODO: Why? Can't we use all of them?
                // Concatenated ... etc :)
                if (it.hasNext()) {
                    setWarningMessage("Ignored all structuring elements except the first one");
                }
            } else {
                throw new Exception("No structuring elements in input table");
            }
        }
        return super.execute(inObjects, exec);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_imgCellFactory = new ImgPlusCellFactory(exec);
    }

}
