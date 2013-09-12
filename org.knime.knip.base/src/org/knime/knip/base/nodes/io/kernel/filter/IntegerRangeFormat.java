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
package org.knime.knip.base.nodes.io.kernel.filter;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class IntegerRangeFormat extends Format {

    private static final long serialVersionUID = 1L;

    @Override
    public StringBuffer format(final Object o, final StringBuffer sb, final FieldPosition pos) {
        final int[] values = (int[])o;
        if (values.length == 0) {
            return sb;
        }
        if (values.length == 1) {
            sb.append(values[0]);
            return sb;
        }
        boolean equalStepped = false;
        if (values.length > 2) {
            equalStepped = true;
            final int step = values[1] - values[0];
            for (int i = 2; i < values.length; i++) {
                if (step != (values[i] - values[i - 1])) {
                    equalStepped = false;
                    break;
                }
            }
        }
        if (equalStepped) {
            sb.append(values[0]);
            sb.append(", ");
            sb.append(values[1]);
            sb.append(" ... ");
            sb.append(values[values.length - 1]);
        } else {
            sb.append(values[0]);
            for (int i = 1; i < values.length; i++) {
                sb.append(", ");
                sb.append(values[i]);
            }
        }
        return sb;
    }

    @Override
    public Object parseObject(final String s, final ParsePosition pos) {
        try {
            if (s.indexOf("...") == -1) {
                if (s.indexOf(',') == -1) {
                    pos.setIndex(s.length());
                    return new int[]{Integer.parseInt(s)};
                }
                final String[] elmnts = s.split(",");
                final int[] values = new int[elmnts.length];
                for (int i = 0; i < values.length; i++) {
                    pos.setIndex(s.length());
                    values[i] = Integer.parseInt(elmnts[i].trim());
                }
                return values;
            }
            final String[] range = s.split("\\.\\.\\.");
            if (range.length != 2) {
                return null;
            }
            final String[] startStep = range[0].split(",");
            final int start = Integer.parseInt(startStep[0]);
            final int step = startStep.length < 2 ? 1 : Integer.parseInt(startStep[1]) - start;
            final int end = Integer.parseInt(range[1]);
            final int[] values = new int[((end - start) / step) + 1];
            for (int i = 0; i < values.length; i++) {
                values[i] = start + (i * step);
            }
            pos.setIndex(s.length());
            return values;
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
