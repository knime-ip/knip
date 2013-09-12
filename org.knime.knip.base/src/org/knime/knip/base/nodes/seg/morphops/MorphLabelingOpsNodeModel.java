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
package org.knime.knip.base.nodes.seg.morphops;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imglib2.labeling.Labeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.labeling.unary.DilateLabeling;
import net.imglib2.ops.operation.labeling.unary.ErodeLabeling;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.StructuringElementCursor;
import net.imglib2.type.logic.BitType;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MorphLabelingOpsNodeModel<L extends Comparable<L>> extends
        ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>> {

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

    enum LabelHandling {
        BINARY("Binary"), LABELING("Individual Labeling");
        static final List<String> NAMES = new ArrayList<String>();

        static {
            for (final LabelHandling e : LabelHandling.values()) {
                NAMES.add(e.toString());
            }
        }

        static LabelHandling value(final String name) {
            return values()[NAMES.indexOf(name)];
        }

        private final String m_name;

        private LabelHandling(final String name) {
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

    static SettingsModelString createConnectionTypeModel() {
        return new SettingsModelString("connection_type", ConnectedType.FOUR_CONNECTED.toString());
    }

    static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimension_selection", "X", "Y");
    }

    static SettingsModelIntegerBounded createIterationsModel() {
        return new SettingsModelIntegerBounded("iterations", 1, 1, Integer.MAX_VALUE);
    }

    static SettingsModelString createLabelingBasedModel() {
        return new SettingsModelString("labeling_basd", LabelHandling.LABELING.toString());
    }

    static SettingsModelString createOperationModel() {
        return new SettingsModelString("operation", MorphOp.ERODE.toString());
    }

    static SettingsModelString createStructureColModel() {
        return new SettingsModelString("structure_col", "<none>");
    }

    private ImgPlus<BitType> m_currentStruct;

    private UnaryObjectFactory<Labeling<L>, Labeling<L>> m_fac;

    private LabelingCellFactory m_labCellFactory;

    private UnaryOperation<Labeling<L>, Labeling<L>> m_operation;

    private final SettingsModelString m_smConnectionType = createConnectionTypeModel();

    private final SettingsModelDimSelection m_smDimensions = createDimSelectionModel();

    private final SettingsModelInteger m_smIterations = createIterationsModel();

    private final SettingsModelString m_smLabelingBased = createLabelingBasedModel();

    private final SettingsModelString m_smOperation = createOperationModel();

    private final SettingsModelString m_smStructurColumn = createStructureColModel();

    private int m_structurColumnIndex;

    protected MorphLabelingOpsNodeModel() {
        super(new PortType[]{new PortType(BufferedDataTable.class, true)});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_smConnectionType);
        settingsModels.add(m_smStructurColumn);
        settingsModels.add(m_smOperation);
        settingsModels.add(m_smLabelingBased);
        settingsModels.add(m_smIterations);
        settingsModels.add(m_smDimensions);

    }

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     */
    @Override
    protected LabelingCell<L> compute(final LabelingValue<L> cellValue) throws IOException {

        m_fac = new UnaryObjectFactory<Labeling<L>, Labeling<L>>() {

            @Override
            public Labeling<L> instantiate(final Labeling<L> a) {

                return a.<L> factory().create(a);
            }
        };

        final Labeling<L> in = cellValue.getLabeling();
        final Labeling<L> out = in.<L> factory().create(in);

        if (m_currentStruct != null) {
            m_operation = createOperation(StructuringElementCursor.createElementFromImg(m_currentStruct));
        } else {
            m_operation = createOperation(null);
        }

        // TODO: Logger
        try {
            SubsetOperations.iterate(m_operation,
                                     m_smDimensions.getSelectedDimIndices(cellValue.getLabelingMetadata()), in, out,
                                     getExecutorService());
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }

        return m_labCellFactory.createCell(out, cellValue.getLabelingMetadata());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[1] != null) {
            m_structurColumnIndex = ((DataTableSpec)inSpecs[1]).findColumnIndex(m_smStructurColumn.getStringValue());
            if (m_structurColumnIndex == -1) {
                if ((m_structurColumnIndex =
                        NodeTools.autoOptionalColumnSelection((DataTableSpec)inSpecs[1], m_smStructurColumn,
                                                              ImgPlusValue.class)) >= 0) {
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

    /**
     * TODO: Missing erode dialte (binary)
     * 
     * @param structuringElement
     * @return
     */
    private UnaryOperation<Labeling<L>, Labeling<L>> createOperation(final long[][] structuringElement) {

        final boolean labelingBased = LabelHandling.value(m_smLabelingBased.getStringValue()) == LabelHandling.LABELING;
        long[][] localStruct = structuringElement;
        switch (ConnectedType.value(m_smConnectionType.getStringValue())) {
            case STRUCTURING_ELEMENT:
                break;
            case FOUR_CONNECTED:
                localStruct = new long[][]{{0, -1}, {-1, 0}, {0, 0}, {1, 0}, {0, 1}};
                break;
            case EIGHT_CONNECTED:
                localStruct =
                        new long[][]{{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
                break;
            default:
                if (localStruct == null) {
                    throw new IllegalArgumentException(
                            "No structuring element available but structuring element selected");

                }
        }

        UnaryOutputOperation<Labeling<L>, Labeling<L>> simpleErode =
                Operations.iterate(Operations.wrap(new ErodeLabeling<L>(localStruct, labelingBased), m_fac),
                                   m_smIterations.getIntValue());

        UnaryOutputOperation<Labeling<L>, Labeling<L>> simpleDilate =
                Operations.iterate(Operations.wrap(new DilateLabeling<L>(localStruct, labelingBased), m_fac),
                                   m_smIterations.getIntValue());

        switch (MorphOp.value(m_smOperation.getStringValue())) {
            case OPEN:
                return Operations.concat(simpleErode, simpleDilate);
            case CLOSE:
                return Operations.concat(simpleDilate, simpleErode);
            case DILATE:
                return simpleDilate;
            case ERODE:
                return simpleErode;
            default:
                throw new IllegalArgumentException("Unknown element type morphological type");
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        if (ConnectedType.value(m_smConnectionType.getStringValue()) == ConnectedType.STRUCTURING_ELEMENT) {

            if (((BufferedDataTable)inObjects[1]) == null) {
                throw new IllegalArgumentException(
                        "\'Structuring Element\' as connection type selected, but no structuring element in second inport provided");
            }

            final Iterator<DataRow> it = ((BufferedDataTable)inObjects[1]).iterator();
            if (it.hasNext()) {
                m_currentStruct = ((ImgPlusValue<BitType>)it.next().getCell(m_structurColumnIndex)).getImgPlus();

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
        m_labCellFactory = new LabelingCellFactory(exec);
    }

}
