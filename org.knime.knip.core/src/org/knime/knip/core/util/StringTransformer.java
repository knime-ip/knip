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
package org.knime.knip.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.util.ValuePair;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class StringTransformer {

    private final List<ValuePair<String, Boolean>> m_parsedList;

    public StringTransformer(final String expression, final String delim) throws IllegalArgumentException {
        m_parsedList = parse(expression, delim);
    }

    /*
     * Pre-calculates the parsing of the expression, which can be used later
     * on
     */
    private List<ValuePair<String, Boolean>> parse(final String expression, final String delim)
            throws IllegalArgumentException {
        int current = 0;
        final List<ValuePair<String, Boolean>> res = new ArrayList<ValuePair<String, Boolean>>();

        while (current < expression.length()) {

            final int start = expression.indexOf(delim, current);

            if (start == -1) {
                res.add(new ValuePair<String, Boolean>(expression.substring(current, expression.length()), true));
                break;
            }

            if (start != current) {
                res.add(new ValuePair<String, Boolean>(expression.substring(current, start), true));
                current = start;
                continue;
            }

            final int end = expression.indexOf(delim, start + 1);

            if (end < start) {
                throw new IllegalArgumentException("No closing $ for: \""
                        + expression.substring(start, Math.max(expression.length(), start + 10)) + "\"");
            }

            current = end + 1;

            res.add(new ValuePair<String, Boolean>(expression.substring(start + 1, end), false));
        }
        return res;
    }

    /**
     * Given a map from String to Object, the resulting String is created, given the expression set in the constructor.
     * 
     * @param input
     * @return
     * @throws InvalidSettingsException
     */
    public String transform(final Map<String, Object> input) throws IllegalArgumentException {
        final StringBuffer bf = new StringBuffer();
        for (final ValuePair<String, Boolean> ValuePair : m_parsedList) {
            if (ValuePair.b) {
                bf.append(ValuePair.a);
            } else {
                bf.append(input.get(ValuePair.a).toString());
            }
        }

        return bf.toString();
    }

    public static void main(final String[] args) throws IllegalArgumentException {
        final Map<String, Object> map = new HashMap<String, Object>();

        map.put("name", "Name");
        map.put("label", "myLabel");

        System.out.println(new StringTransformer("$name$#_chrome", "$").transform(map).toString());
    }

}
