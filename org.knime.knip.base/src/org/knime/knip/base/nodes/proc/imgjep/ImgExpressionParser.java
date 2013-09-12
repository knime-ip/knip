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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.knime.base.data.statistics.StatisticsTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.proc.imgjep.fun.JEPAbs;
import org.knime.knip.base.nodes.proc.imgjep.fun.JEPAvg;
import org.knime.knip.base.nodes.proc.imgjep.fun.JEPExp;
import org.knime.knip.base.nodes.proc.imgjep.fun.JEPLog;
import org.knime.knip.base.nodes.proc.imgjep.fun.JEPMax;
import org.knime.knip.base.nodes.proc.imgjep.fun.JEPMin;
import org.knime.knip.base.nodes.proc.imgjep.fun.JEPSqrt;

/**
 * Helper class that parses the expression and generates the JEP object.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Bernd Wiswedel, University of Konstanz
 */
final class ImgExpressionParser {

    /**
     * @param col Input column index.
     * @return "col" + col (used for JEP)
     */
    static String createColField(final int col) {
        return "col" + col;
    }

    /**
     * @param c jep constant name.
     * @param col index of column.
     * @return constant identifier for jep.
     */
    static String createConstantName(final ImgJEPConstant c, final int col) {
        return c.getFunctionName() + "_" + createColField(col);
    }

    /**
     * For each of the columns: Do we need to calculate mean, min, max, etc...
     */
    private final boolean[] m_constantFlags;

    /** Input expression. */
    private final String m_expression;

    /** Final JEP object. */
    private final ImgJEP m_jep;

    /** Housing the output expression (as passed to JEP). */
    private final StringBuilder m_jepExprBuilder;

    /** Offset while parsing the expression. */
    private int m_offset;

    /** Input data table spec (used for variable identification. */
    private final DataTableSpec m_spec;

    /**
     * Creates new parser instance, does the parsing.
     * 
     * @param expression Input expression.
     * @param imgJEP the image operations
     * @param spec Respective table spec.
     * @param rowCount Total row count (is a constant).
     * @throws InvalidSettingsException If parsing fails.
     */
    ImgExpressionParser(final String expression, final DataTableSpec spec, final int rowCount)
                                                                                              throws InvalidSettingsException {
        m_expression = expression;
        m_jepExprBuilder = new StringBuilder();
        m_spec = spec;
        m_constantFlags = new boolean[spec.getNumColumns()];
        m_jep = parse(rowCount);
    }

    /**
     * Does the calculation of the constant variables and then puts it into the JEP object.
     * 
     * @param mon for progress / cancel
     * @param bdt the input table.
     * @throws CanceledExecutionException If canceled.
     */
    void calculateConstants(final ExecutionMonitor mon, final BufferedDataTable bdt) throws CanceledExecutionException {
        assert (bdt.getDataTableSpec().equalStructure(m_spec));
        final StatisticsTable t = new StatisticsTable(bdt, mon);
        for (final String s : getColumnsWithConstants()) {
            final int index = m_spec.findColumnIndex(s);
            for (final ImgJEPConstant c : ImgJEPConstant.values()) {
                if (c.getNrArgs() > 0) {
                    final double val = c.readFromStatisticsTable(t, index);
                    m_jep.getVar(createConstantName(c, index)).setValue(val);
                }
            }
        }
    }

    /**
     * Check if we find a constant function identifier from current offset until next dollar sign.
     * 
     * @param end Position of next dollar sign
     * @return end if no constant is found, otherwise end of the constant def.
     * @throws InvalidSettingsException If that fails.
     */
    private int checkForConstantFunction(final int end) throws InvalidSettingsException {
        int min = Integer.MAX_VALUE;
        ImgJEPConstant constant = null;
        for (final ImgJEPConstant c : ImgJEPConstant.values()) {
            if (c.getNrArgs() > 0) {
                final int location = m_expression.indexOf(c.getFunctionName() + "(", m_offset);
                if ((location >= 0) && (location < end) && (location < min)) {
                    constant = c;
                    min = location;
                }
            }
        }
        if (constant == null) {
            return end;
        }
        // now we found something like "...COL_MEAN" (where min points
        // to 'C')
        final int startBracket = min + constant.getFunctionName().length();
        // at least 4 characters follow, e.g. $c$) (where c is the
        // column name)
        if (((startBracket + 4) > (m_expression.length() - 1)) || (m_expression.charAt(startBracket) != '(')) {
            throw new InvalidSettingsException("Can't parse column name " + "of constant function "
                    + constant.toString());
        }
        final int startDollar = startBracket + 1;
        if (m_expression.charAt(startDollar) != '$') {
            throw new InvalidSettingsException("Can't parse column name " + "of constant function "
                    + constant.toString());
        }
        final int endDollar = m_expression.indexOf('$', startDollar + 1);
        if (endDollar < 0) {
            throw new InvalidSettingsException("Can't parse column name" + "of constant function "
                    + constant.toString());
        }
        if (endDollar < startDollar) {
            throw new InvalidSettingsException("No closing $ for: \""
                    + m_expression.substring(startDollar, Math.max(m_expression.length(), startDollar + 10)) + "\"");
        }
        final int endBracket = endDollar + 1;
        if (m_expression.charAt(endBracket) != ')') {
            throw new InvalidSettingsException("Can't parse column name " + "of constant function "
                    + constant.toString());
        }
        final String colIdPound = m_expression.substring(startDollar, endDollar + 1);
        final String colId = colIdPound.substring(1, colIdPound.length() - 1);
        final int colIndex = m_spec.findColumnIndex(colId);
        if (colIndex < 0) {
            throw new InvalidSettingsException("No such column: " + colId);
        }
        final DataType colType = m_spec.getColumnSpec(colIndex).getType();
        if (!colType.isCompatible(DoubleValue.class) && !colType.isCompatible(IntValue.class)) {
            throw new InvalidSettingsException("Can't use column \"" + colId + "\", not numeric!");
        }
        m_constantFlags[colIndex] = true;
        final String colConstantName = createConstantName(constant, colIndex);
        m_jepExprBuilder.append(m_expression.substring(m_offset, min));
        m_jepExprBuilder.append(colConstantName);
        return Math.min(endBracket + 1, m_expression.length());
    }

    /** @return name of columns for which constants need to be calculated. */
    String[] getColumnsWithConstants() {
        final List<String> result = new ArrayList<String>();
        for (int i = 0; i < m_constantFlags.length; i++) {
            if (m_constantFlags[i]) {
                result.add(m_spec.getColumnSpec(i).getName());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Getter for table spec (as passed in constructor).
     * 
     * @return The spec.
     */
    DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @return The final jep object.
     */
    ImgJEP getJep() {
        return m_jep;
    }

    /**
     * @param rowCount
     */
    private ImgJEP parse(final int rowCount) throws InvalidSettingsException {
        final LinkedHashSet<String> variableHash = new LinkedHashSet<String>();
        int numImgCols = 0;
        while (m_offset < m_expression.length()) {
            final int start = m_expression.indexOf('$', m_offset);
            if (start < m_offset) {
                break;
            }
            final int newOffset = checkForConstantFunction(start);
            if (newOffset > start) {
                m_offset = newOffset;
                continue; // encountered constant function,
                // everything done
            }

            final int end = m_expression.indexOf('$', start + 1);
            if (end < start) {
                throw new InvalidSettingsException("No closing $ for: \""
                        + m_expression.substring(start, Math.max(m_expression.length(), start + 10)) + "\"");
            }
            final String colIdPound = m_expression.substring(start, end + 1);
            final String colId = colIdPound.substring(1, colIdPound.length() - 1);
            m_jepExprBuilder.append(m_expression.substring(m_offset, start));
            String colFieldName;
            final int colIndex = m_spec.findColumnIndex(colId);
            if (colIndex < 0) {
                throw new InvalidSettingsException("No such column: " + colId);
            }
            colFieldName = createColField(colIndex);
            final DataType colType = m_spec.getColumnSpec(colIndex).getType();
            if (!colType.isCompatible(ImgPlusValue.class) && !colType.isCompatible(DoubleValue.class)
                    && !colType.isCompatible(IntValue.class)) {
                throw new InvalidSettingsException("Can't use column \"" + colId + "\", not compatible!");
            }
            m_jepExprBuilder.append(colFieldName);
            m_offset = Math.min(end + 1, m_expression.length());
            variableHash.add(colFieldName);
            if (colType.isCompatible(ImgPlusValue.class)) {
                numImgCols++;
            }
        }
        if (numImgCols == 0) {
            throw new InvalidSettingsException("No image column in the expression.");
        }
        m_jepExprBuilder.append(m_expression.substring(Math.min(m_offset, m_expression.length())));
        final ImgJEP result = new ImgJEP();
        result.addStandardConstants();
        result.addFunction(ImgJEPFunction.max.toString(), new JEPMax());
        result.addFunction(ImgJEPFunction.min.toString(), new JEPMin());
        result.addFunction(ImgJEPFunction.average.toString(), new JEPAvg());
        result.addFunction(ImgJEPFunction.exp.toString(), new JEPExp());
        result.addFunction(ImgJEPFunction.log.toString(), new JEPLog());
        result.addFunction(ImgJEPFunction.sqrt.toString(), new JEPSqrt());
        result.addFunction(ImgJEPFunction.abs.toString(), new JEPAbs());

        // result.addFunction(ImgJEPFunction.min_in_args.toString(), new
        // Min());
        // result.addFunction(ImgJEPFunction.average.toString(), new
        // Avg());
        result.setImplicitMul(true);
        // result.addConstant(ImgJEPConstant.ROWCOUNT.toString(),
        // rowCount);
        for (final String s : variableHash) {
            result.addVariable(s, 0.0);
        }
        for (int i = 0; i < m_constantFlags.length; i++) {
            if (m_constantFlags[i]) {
                for (final ImgJEPConstant c : ImgJEPConstant.values()) {
                    if (c.getNrArgs() > 0) {
                        result.addVariable(createConstantName(c, i), 0.0);
                    }
                }
            }
        }
        // result.addVariable(ImgJEPConstant.ROWINDEX.toString(), 0);

        result.parseExpression(m_jepExprBuilder.toString());
        if (result.hasError()) {
            throw new InvalidSettingsException("Unable to parse m_expression\n" + result.getErrorInfo());
        }
        return result;

    }

}
