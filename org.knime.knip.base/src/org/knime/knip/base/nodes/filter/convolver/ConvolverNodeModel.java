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
package org.knime.knip.base.nodes.filter.convolver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.subset.views.ImgPlusView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.algorithm.convolvers.KernelTools;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ConvolverNodeModel<T extends RealType<T>, O extends RealType<O>, K extends RealType<K> & NativeType<K>>
        extends ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<O>> {

    protected static SettingsModelString createAdditionalAxisNameModel() {
        return new SettingsModelString("axis_name", "FilterDim");
    }

    protected static SettingsModelBoolean createCalcAsFloatModel() {
        return new SettingsModelBoolean("calc_in_float_space", true);
    }

    protected static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimselection", "X", "Y");
    }

    protected static SettingsModelString createImplementationModel() {
        return new SettingsModelString("implementation", MultiKernelImageConvolverManager.getConvolverNames()
                .iterator().next());
    }

    protected static SettingsModelString createKernelColumnModel() {
        return new SettingsModelString("column_kernel", "");
    }

    protected static SettingsModelString createOutOfBoundsModel() {
        return new SettingsModelString("outofboundsstrategy", OutOfBoundsStrategyEnum.BORDER.toString());
    }

    private final NodeLogger LOGGER = NodeLogger.getLogger(ConvolverNodeModel.class);

    @SuppressWarnings("rawtypes")
    private MultiKernelImageConvolverExt m_convolver;

    private ImgPlusCellFactory m_imgCellFactory;

    private int m_kernelColumnIdx = -1;

    private Img<K>[] m_kernelList;

    private final SettingsModelBoolean m_smCalcAsFloat = createCalcAsFloatModel();

    private final SettingsModelDimSelection m_smDimSelection = createDimSelectionModel();

    private final SettingsModelString m_smImplementation = createImplementationModel();

    private final SettingsModelString m_smKernelColumn = createKernelColumnModel();

    private final SettingsModelString m_smOutOfBoundsStrategy = createOutOfBoundsModel();

    public ConvolverNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE});

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {

        settingsModels.add(m_smCalcAsFloat);
        settingsModels.add(m_smImplementation);
        settingsModels.add(m_smKernelColumn);
        settingsModels.add(m_smDimSelection);
        settingsModels.add(m_smOutOfBoundsStrategy);

        for (final String convolver : MultiKernelImageConvolverManager.getConvolverNames()) {
            settingsModels.addAll(MultiKernelImageConvolverManager.getConvolverByName(convolver)
                    .getAdditionalSettingsModels());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ImgPlusCell<O> compute(final ImgPlusValue<T> cellValue) throws Exception {

        final ImgPlus<T> in =
                new ImgPlusView<T>(SubsetOperations.subsetview(cellValue.getImgPlus(), cellValue.getImgPlus()),
                        cellValue.getImgPlus().getImg().factory(), cellValue.getImgPlus());

        final Img<K>[] currentKernels = new Img[m_kernelList.length];

        final int[] selectedDims = m_smDimSelection.getSelectedDimIndices(in);
        int k = 0;
        for (final Img<K> kernel : m_kernelList) {
            currentKernels[k++] = KernelTools.adjustKernelDimensions(in.numDimensions(), selectedDims, kernel);
        }

        final T inType = in.firstElement().createVariable();
        if (m_smCalcAsFloat.getBooleanValue() && !((inType instanceof DoubleType) || (inType instanceof FloatType))) {
            m_convolver.setResultType(new FloatType());
        } else {
            m_convolver.setResultType(inType);
        }

        return m_imgCellFactory.createCell((ImgPlus<O>)Operations.compute(m_convolver, in, currentKernels));

    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        if (inSpecs[1] != null) {
            m_kernelColumnIdx = ((DataTableSpec)inSpecs[1]).findColumnIndex(m_smKernelColumn.getStringValue());
        }
        if ((m_kernelColumnIdx == -1) && (inSpecs[1] != null)) {
            if ((m_kernelColumnIdx =
                    NodeTools.autoOptionalColumnSelection((DataTableSpec)inSpecs[1], m_smKernelColumn,
                                                          ImgPlusValue.class)) >= 0) {
                setWarningMessage("Auto-configure Column: " + m_smKernelColumn.getStringValue());
            } else {
                throw new InvalidSettingsException("No column selected!");
            }
        }

        return super.configure(inSpecs);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        exec.setProgress("Reading Kernels ...");
        final Iterator<DataRow> it = ((BufferedDataTable)inObjects[1]).iterator();
        final ArrayList<Img<K>> kernelList = new ArrayList<Img<K>>();
        final int numSelectedDims = m_smDimSelection.getNumSelectedDimLabels();

        while (it.hasNext()) {
            final DataRow row = it.next();

            final ImgPlusValue<K> val = ((ImgPlusValue<K>)row.getCell(m_kernelColumnIdx));

            if (val.getDimensions().length != numSelectedDims) {
                LOGGER.warn("Kernel " + row.getKey().getString() + " can't be added as " + numSelectedDims
                        + " Dimensions are selected and the Kernel is " + val.getDimensions().length + " dimensional");
            }

            kernelList.add(((ImgPlusCell<K>)row.getCell(m_kernelColumnIdx)).getImgPlusCopy());
        }
        m_kernelList = kernelList.toArray(new Img[kernelList.size()]);

        m_convolver = MultiKernelImageConvolverManager.getConvolverByName(m_smImplementation.getStringValue());

        m_convolver
                .setOutOfBounds(Enum.valueOf(OutOfBoundsStrategyEnum.class, m_smOutOfBoundsStrategy.getStringValue()));

        exec.setProgress("Convolving ...");

        PortObject[] res = null;

        try {
            res = super.execute(inObjects, exec);
        } catch (final Exception e) {
            LOGGER.warn("Exception during execution of convolver + " + e);
        } finally {
            m_convolver.close();
        }

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_imgCellFactory = new ImgPlusCellFactory(exec);

        m_convolver.load();
    }
}
