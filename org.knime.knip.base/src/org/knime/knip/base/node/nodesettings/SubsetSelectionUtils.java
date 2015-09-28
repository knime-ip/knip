/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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
 * ---------------------------------------------------------------------
 *
 * Created on Sep 15, 2015 by seebacher
 */
package org.knime.knip.base.node.nodesettings;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for the SubsetSelection Dialog and Model.
 *
 * @author Daniel Seebacher, University of Konstanz.
 */
public class SubsetSelectionUtils {

    public static int[] parseString(final String subsetSelectionString) {
        String intext = subsetSelectionString.replaceAll("\\s", "");

        TreeSet<Integer> indices = new TreeSet<>();

        // first split by commas
        for (String commaSeperatedToken : intext.split(",")) {
            if (commaSeperatedToken.length() == 0) {
                throw new IllegalArgumentException("Malformed input! Please check your commas.");
            }

            // second split by dash, only two numbers can be seperated by a dash.
            String[] numbersAsString = commaSeperatedToken.split("-");
            if (numbersAsString.length == 0 && numbersAsString.length > 2) {
                throw new IllegalArgumentException(
                        "Malformed input! Only two numbers can be seperated by a dash! Please check '"
                                + commaSeperatedToken + "' again");
            }

            int firstIndex = -1;
            int secondIndex = -1;

            try {
                firstIndex = Integer.parseInt(numbersAsString[0]);
                secondIndex = (numbersAsString.length == 2) ? Integer.parseInt(numbersAsString[1]) : -1;
            } catch (NumberFormatException exc) {
                throw new IllegalArgumentException(
                        "The numbers " + Arrays.toString(numbersAsString) + " couldn't be parsed as integers.");
            }

            if (-1 == firstIndex && firstIndex == secondIndex) {
                throw new IllegalArgumentException(
                        "Both indices we're -1, this shouldn't happen. Please check your input string again '"
                                + commaSeperatedToken + "'");
            }

            if (firstIndex == secondIndex || secondIndex == -1) {
                indices.add(firstIndex);
            } else {
                for (int i = Math.min(firstIndex, secondIndex); i <= Math.max(firstIndex, secondIndex); i++) {
                    indices.add(i);
                }
            }

        }

        int[] indicesArray = new int[indices.size()];
        int i = 0;
        Iterator<Integer> it = indices.iterator();
        while (it.hasNext()) {
            indicesArray[i++] = it.next();
        }

        return indicesArray;
    }

    public static void checkInput(final String text) throws IllegalArgumentException {
        String intext = text.replaceAll("\\s", "");

        Pattern p = Pattern.compile("[^0-9,-]");
        Matcher matcher = p.matcher(intext);

        if (matcher.find()) {
            throw new IllegalArgumentException("Illegal characters found! Only numbers, dashes and commas allowed.");
        }
    }
}
