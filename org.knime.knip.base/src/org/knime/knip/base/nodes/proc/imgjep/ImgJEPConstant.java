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

import org.knime.base.data.statistics.StatisticsTable;

/**
 * Some constant values that are shown in the "Constants" list in the dialog.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Bernd Wiswedel, University of Konstanz
 */
enum ImgJEPConstant {

    /** Value of mathematical E. */
    e {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "Value of e (base of natural logarithm)";
        }

        /** {@inheritDoc} */
        @Override
        int getNrArgs() {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        boolean isJEPConstant() {
            return true;
        }
    },
    // /** Row Count. */
    // ROWCOUNT {
    // /** {@inheritDoc} */
    // String getDescription() { return "Total row count in table"; }
    // /** {@inheritDoc} */
    // int getNrArgs() { return 0; }
    // },
    // /** Current row number, starting with 0. */
    // ROWINDEX {
    // /** {@inheritDoc} */
    // String getDescription() {
    // return "Current row index, starting with 0"; }
    // /** {@inheritDoc} */
    // int getNrArgs() { return 0; }
    // },
    /** Value of mathematical Pi. */
    pi {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "Value of  \u03C0";
        }

        /** {@inheritDoc} */
        @Override
        int getNrArgs() {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        boolean isJEPConstant() {
            return true;
        }
    };
    // /** minimum in column. */
    // COL_MIN {
    // /** {@inheritDoc} */
    // String getDescription() {
    // return "minimum in column: COL_MIN($col_name$)";
    // }
    // /** {@inheritDoc} */
    // double readFromStatisticsTable(final StatisticsTable t, final int
    // col) {
    // DataCell minVal = t.getMin(col);
    // return minVal instanceof DoubleValue
    // ? ((DoubleValue)minVal).getDoubleValue() : Double.NaN;
    // }
    // },
    // /** maximum in column. */
    // COL_MAX {
    // /** {@inheritDoc} */
    // String getDescription() {
    // return "maximum in column: COL_MAX($col_name$)";
    // }
    // /** {@inheritDoc} */
    // double readFromStatisticsTable(final StatisticsTable t, final int
    // col) {
    // DataCell maxVal = t.getMax(col);
    // return maxVal instanceof DoubleValue
    // ? ((DoubleValue)maxVal).getDoubleValue() : Double.NaN;
    // }
    // },
    // /** mean in column. */
    // COL_MEAN {
    // /** {@inheritDoc} */
    // String getDescription() {
    // return "mean in column: COL_MEAN($col_name$)";
    // }
    // /** {@inheritDoc} */
    // double readFromStatisticsTable(final StatisticsTable t, final int
    // col) {
    // return t.getMean(col);
    // }
    // },
    // /** standard deviation in column. */
    // COL_STDDEV {
    // /** {@inheritDoc} */
    // String getDescription() { return
    // "standard deviation in column: COL_STDDEV($col_name$)"; }
    // /** {@inheritDoc} */
    // double readFromStatisticsTable(final StatisticsTable t, final int
    // col) {
    // return t.getStandardDeviation(col);
    // }
    // },
    // /** variance in column. */
    // COL_VAR {
    // /** {@inheritDoc} */
    // String getDescription() {
    // return "variance in column: COL_VAR($col_name$)";
    // }
    // /** {@inheritDoc} */
    // double readFromStatisticsTable(final StatisticsTable t, final int
    // col) {
    // return t.getVariance(col);
    // }
    // };

    /**
     * Tooltip text for a constant.
     * 
     * @return A short description.
     */
    abstract String getDescription();

    /**
     * The full name as it is shown in the dialog (including dummy args).
     * 
     * @return Function full name.
     */
    String getFunctionFullName() {
        final StringBuilder b = new StringBuilder(getFunctionName());
        if (getNrArgs() == 0) {
            return b.toString();
        }
        b.append('(');
        b.append("col_name");
        for (int i = 1; i < getNrArgs(); i++) {
            b.append(i > 0 ? ", " : "");
        }
        b.append(')');
        return b.toString();
    }

    /**
     * Short name of the function, typically toString().
     * 
     * @return The function's name.
     */
    String getFunctionName() {
        return toString();
    }

    /**
     * Get number of arguments, defaults to 1.
     * 
     * @return Number of arguments (e.g. 1 for col_min and 0 for PI).
     */
    int getNrArgs() {
        return 1;
    }

    /**
     * If this constitutes a JEP constant, mostly false, only true for pi & e.
     * 
     * @return If this is a JEP constant.
     */
    boolean isJEPConstant() {
        return false;
    }

    /**
     * Getter method that reads the constant from a statistics table. Makes only sense for min_col, max, mean, etc.
     * 
     * @param table The statistics table to read from.
     * @param col The column index of interest
     * @return The value of the constant.
     * @throws IllegalStateException In this default impl. always.
     */
    double readFromStatisticsTable(final StatisticsTable table, final int col) {
        assert (table != null);
        assert (col >= 0);
        throw new IllegalStateException("Not implemented for " + toString());
    }

}
