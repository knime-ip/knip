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
package org.knime.knip.base.nodes.proc.imgjep;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.ops.operation.subset.views.ImgPlusView;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.MiscViews;
import org.nfunk.jep.Variable;

/**
 * This is the model implementation of JEP. Math expression parser using JEP (http://www.singularsys.com/jep/)
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ImgJEPNodeModel extends NodeModel implements BufferedDataTableHolder {

    protected static final int APPEND_COLUMN = 0;

    /** NodeSettings key for the option to extend the image */
    protected static final String CFG_ADJUST_IMG_DIM = "adjust_img_dim";

    /** NodeSettings key which column is to be replaced or appended. */
    protected static final String CFG_COLUMN_NAME = "replaced_column";

    /** NodeSettings key for the expression. */
    protected static final String CFG_EXPRESSION = "expression";

    /** NodeSettings key for the reference column selection */
    protected static final String CFG_REF_COLUMN = "ref_column";

    /** NodeSettings key for the result type */
    protected static final String CFG_RESULT_TYPE = "res_type";

    /** NodeSettings key is replace or append column or create new table? */
    protected static final String CFG_TABLE_CREATION_MODE = "table_creation_mode";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgJEPNodeModel.class);

    protected static final int NEW_TABLE = 2;

    protected static final int REPLACE_COLUMN = 1;

    private final SettingsModelBoolean m_adjustImgDim = new SettingsModelBoolean(CFG_ADJUST_IMG_DIM, false);

    private String m_colName;

    /* internal table for table cell viewer */
    private BufferedDataTable m_data;

    private String m_expression;

    /*
     * the parser is created in the configure method and re-used every time
     * configure is called (because it is expensive). Do not depend on it
     * existence.
     */
    private ImgExpressionParser m_exprParser = null;

    private ImgPlusCellFactory m_imgCellFactory;

    private final SettingsModelString m_referenceColumn = new SettingsModelString(CFG_REF_COLUMN, "");

    private final SettingsModelString m_resultType = new SettingsModelString(CFG_RESULT_TYPE,
            NativeTypes.UNSIGNEDBYTETYPE.toString());

    private int m_tableCreationMode;

    /**
     * Constructor for the node model.
     */
    protected ImgJEPNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_colName == null) {
            throw new InvalidSettingsException("No configuration available");
        }

        if ((m_exprParser == null) || !inSpecs[0].equalStructure(m_exprParser.getDataTableSpec())) {
            // if the input spec changed we need to re-create the
            // parser
            m_exprParser = new ImgExpressionParser(m_expression, inSpecs[0], -1);
        }

        final ColumnRearranger a = new ColumnRearranger(inSpecs[0]);

        final CellFactory cellFac = createCellFactory(m_exprParser, inSpecs[0]);
        DataTableSpec outSpec;
        if (m_tableCreationMode == REPLACE_COLUMN) {
            a.replace(cellFac, inSpecs[0].findColumnIndex(m_colName));
            outSpec = a.createSpec();
        } else if (m_tableCreationMode == APPEND_COLUMN) {
            a.append(cellFac);
            outSpec = a.createSpec();
        } else {
            outSpec = new DataTableSpec(createNewColumnSpec(inSpecs[0]));
        }

        return new DataTableSpec[]{outSpec};
    }

    private CellFactory createCellFactory(final ImgExpressionParser expParser, final DataTableSpec spec)
            throws InvalidSettingsException {
        final ImgJEP jep = expParser.getJep();
        final SingleCellFactory cellFac = new SingleCellFactory(createNewColumnSpec(expParser.getDataTableSpec())) {
            // private int m_rowIndex;

            @Override
            public DataCell getCell(final DataRow row) {
                long[] referenceDims = null;
                Object referenceImgIterationOrder = null;
                ImgPlusMetadata referenceMetadata = null;
                Interval referenceInterval = null;
                ImgFactory referenceFactory = null;
                LinkedList<Img<RealType<?>>> imgList = new LinkedList<Img<RealType<?>>>();
                boolean differentIterationOrders = false;

                boolean useAutoGuess = true;
                if (m_referenceColumn.getStringValue() != null) {
                    final int index = spec.findColumnIndex(m_referenceColumn.getStringValue());
                    if ((index != -1) && !row.getCell(index).isMissing()) {
                        // set reference values from
                        // reference image (once per
                        // row)
                        useAutoGuess = false;
                        final ImgPlus tmpImg = ((ImgPlusValue)row.getCell(index)).getImgPlus();

                        referenceDims = new long[tmpImg.numDimensions()];
                        tmpImg.dimensions(referenceDims);
                        referenceImgIterationOrder = tmpImg.getImg().iterationOrder();
                        referenceMetadata = new DefaultImgMetadata(tmpImg);
                        referenceInterval = new FinalInterval(tmpImg);
                        referenceFactory = tmpImg.factory();
                    }
                }

                for (int i = 0; i < row.getNumCells(); i++) {
                    final String col = ImgExpressionParser.createColField(i);
                    final Variable var = jep.getVar(col);
                    if (var != null) {
                        ImgPlus img = null;
                        Number num = null;

                        final DataCell c = row.getCell(i);
                        if (c.isMissing()) {
                            return DataType.getMissingCell();
                        } else if (c instanceof ImgPlusValue) {
                            img = ((ImgPlusValue)c).getImgPlus();
                        } else if (c instanceof DoubleValue) {
                            num = ((DoubleValue)c).getDoubleValue();
                        } else if (c instanceof IntValue) {
                            num = ((IntValue)c).getIntValue();
                        }

                        // if the current column is an image column
                        if (c instanceof ImgPlusValue) {
                            if (useAutoGuess && (referenceDims == null)) {
                                // autoGuess is active and the reference dim has not yet been set => set it
                                // i.e. the first occurring img column is used as reference values
                                referenceDims = new long[img.numDimensions()];
                                img.dimensions(referenceDims);
                                referenceImgIterationOrder = img.getImg().iterationOrder();
                                referenceMetadata = new DefaultImgMetadata(img);
                                referenceInterval = new FinalInterval(img);
                                referenceFactory = img.factory();
                            } else {
                                // at this point we have a reference column either auto-guessed or set by the user
                                if (m_adjustImgDim.getBooleanValue()) {
                                    final RandomAccessibleInterval res =
                                            MiscViews.synchronizeDimensionality(img, img, referenceInterval,
                                                                                referenceMetadata);

                                    img =
                                            new ImgPlusView(res, img.factory(), new DefaultImgMetadata(
                                                    referenceMetadata));
                                }

                                if (!referenceImgIterationOrder.equals(img.iterationOrder())) {
                                    // we can fix that by using flat iteration order
                                    differentIterationOrders = true;
                                }

                                final long[] dims = new long[img.numDimensions()];
                                img.dimensions(dims);
                                if (!Arrays.equals(referenceDims, dims)) {
                                    throw new IllegalStateException("Images are not compatible (dimensions)!");
                                }
                            }

                            // image exists no error occurred
                            jep.getVar(col).setValue(img);
                            imgList.add(img);
                        } else if (num != null) {
                            jep.getVar(col).setValue(num);
                        }
                    }
                }

                // collects the references of the images from
                // the different columns to indicate childs in
                // the parse tree
                final HashSet<Img<RealType<?>>> tableImages = new HashSet<Img<RealType<?>>>();

                //check if one of the images differs in the iteration order in this case all images
                //have to be changed to use flatIterationOrder
                if (differentIterationOrders) {
                    for (Img i : imgList) {
                        tableImages.add(new ImgView(i, i.factory()));
                    }
                } else {
                    tableImages.addAll(imgList);
                }

                final RealType<?> currentType =
                        (RealType<?>)NativeTypes.getTypeInstance(NativeTypes.valueOf(m_resultType.getStringValue()));

                final ImgOperationEval eval = new ImgOperationEval(currentType, referenceFactory, tableImages);
                jep.setImgOperationEvaluator(eval);

                final Img imgRes = (Img)jep.getValueAsObject();

                if ((imgRes == null) || eval.errorOccured()) {
                    LOGGER.error("Result for row " + row.getKey() + "  can't be calculated.");
                    setWarningMessage("Some error occured while executing!");
                    return DataType.getMissingCell();

                }
                try {
                    return m_imgCellFactory.createCell(imgRes, referenceMetadata);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
                                    final ExecutionMonitor exec) {
                // m_rowIndex = curRowNr; // curRowNr refers to
                // last processed
                super.setProgress(curRowNr, rowCount, lastKey, exec);
            }
        };

        return cellFac;
    }

    private DataColumnSpec createNewColumnSpec(final DataTableSpec inSpec) throws InvalidSettingsException {
        DataColumnSpec newColSpec;
        if (m_tableCreationMode == REPLACE_COLUMN) {
            final int targetCol = inSpec.findColumnIndex(m_colName);
            if (targetCol < 0) {
                throw new InvalidSettingsException("No such column: " + m_colName);
            }
            final DataColumnSpecCreator clone = new DataColumnSpecCreator(inSpec.getColumnSpec(targetCol));
            clone.setType(ImgPlusCell.TYPE);
            clone.removeAllHandlers();
            clone.setDomain(null);
            newColSpec = clone.createSpec();
        } else if (m_tableCreationMode == APPEND_COLUMN) {
            if (inSpec.containsName(m_colName)) {
                throw new InvalidSettingsException("Column already exists: " + m_colName);
            }
            newColSpec = new DataColumnSpecCreator(m_colName, ImgPlusCell.TYPE).createSpec();
        } else {
            newColSpec = new DataColumnSpecCreator(m_colName, ImgPlusCell.TYPE).createSpec();
        }
        return newColSpec;
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        m_imgCellFactory = new ImgPlusCellFactory(exec);
        final BufferedDataTable in = inData[0];
        final DataTableSpec spec = in.getDataTableSpec();
        final ImgExpressionParser p = new ImgExpressionParser(m_expression, spec, in.getRowCount());
        final String[] columnsWithConstants = p.getColumnsWithConstants();
        ExecutionMonitor mon = exec;
        if (columnsWithConstants.length > 0) {
            mon = exec.createSubProgress(0.5);
            p.calculateConstants(mon, in);
            mon = exec.createSubProgress(0.5);
        }
        final ColumnRearranger a = new ColumnRearranger(spec);

        final CellFactory cellFac = createCellFactory(p, spec);
        BufferedDataTable out;
        if (m_tableCreationMode == REPLACE_COLUMN) {
            a.replace(cellFac, spec.findColumnIndex(m_colName));
            out = exec.createColumnRearrangeTable(in, a, mon);
        } else if (m_tableCreationMode == APPEND_COLUMN) {
            a.append(cellFac);
            out = exec.createColumnRearrangeTable(in, a, mon);
        } else {
            final DataTableSpec newSpec = new DataTableSpec(createNewColumnSpec(spec));
            final BufferedDataContainer con = exec.createDataContainer(newSpec);
            final RowIterator rowIt = in.iterator();
            int rowNr = 0;
            final int rowCount = in.getRowCount();
            while (rowIt.hasNext()) {
                final DataRow row = rowIt.next();
                con.addRowToTable(new DefaultRow(row.getKey(), cellFac.getCells(row)));
                cellFac.setProgress(rowNr++, rowCount, row.getKey(), exec);
                exec.checkCanceled();
            }
            con.close();
            out = con.getTable();
        }

        m_data = out;
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_expression = settings.getString(CFG_EXPRESSION);
        m_colName = settings.getString(CFG_COLUMN_NAME);
        m_tableCreationMode = settings.getInt(CFG_TABLE_CREATION_MODE);
        // in case we created a parser for an old expression: delete it
        m_exprParser = null;
        m_resultType.loadSettingsFrom(settings);
        m_adjustImgDim.loadSettingsFrom(settings);

        try {
            m_referenceColumn.loadSettingsFrom(settings);
        } catch (final Exception e) {
            m_referenceColumn.setStringValue("");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_expression != null) {
            settings.addString(CFG_EXPRESSION, m_expression);
            settings.addString(CFG_COLUMN_NAME, m_colName);
            settings.addInt(CFG_TABLE_CREATION_MODE, m_tableCreationMode);
        }
        m_resultType.saveSettingsTo(settings);
        m_adjustImgDim.saveSettingsTo(settings);
        m_referenceColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];

    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_EXPRESSION);
        final String newCol = settings.getString(CFG_COLUMN_NAME);
        final int tableCreationMode = settings.getInt(CFG_TABLE_CREATION_MODE);
        if (newCol == null) {
            throw new InvalidSettingsException("Column name can't be null.");
        }
        if ((tableCreationMode != REPLACE_COLUMN) && (newCol.trim().length() == 0)) {
            throw new InvalidSettingsException("An empty string is not a valid column name");
        }
        m_resultType.validateSettings(settings);
        m_adjustImgDim.validateSettings(settings);

        try {
            m_referenceColumn.validateSettings(settings);
        } catch (final Exception e) {
            // Do nothing
        }
    }

}
