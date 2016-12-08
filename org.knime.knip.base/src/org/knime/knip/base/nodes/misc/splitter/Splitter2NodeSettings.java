/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2016
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
 * Created on Dec 8, 2016 by gabriel
 */
package org.knime.knip.base.nodes.misc.splitter;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * Settings for the SplitterNode.
 *
 */
class Splitter2NodeSettings {

    static final String[] COL_CREATION_MODES = new String[]{"New Table", "Append", "Replace"};

    static SettingsModelIntegerBounded[] createAdvancedModels() {
        final SettingsModelIntegerBounded[] models =
                new SettingsModelIntegerBounded[KNIMEKNIPPlugin.parseDimensionLabels().length];
        for (int i = 0; i < models.length; i++) {
            models[i] = new SettingsModelIntegerBounded(i + "_advanced_selection", 0, 0, Integer.MAX_VALUE);
            models[i].setEnabled(false);
        }
        return models;

    }

    static SettingsModelString createColSuffixNodeModel() {
        return new SettingsModelString("Column Suffix", "");
    }

    static SettingsModelString createColumnModel() {
        return new SettingsModelString("column_selection", "");
    }

    /**
     * @return the settings model for the column creation mode
     */
    public static SettingsModelString createColCreationModeModel() {
        return new SettingsModelString("Column Creation Mode", COL_CREATION_MODES[0]);
    }

    static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    static SettingsModelBoolean createIsAdvancedModel() {
        return new SettingsModelBoolean("is_advanced", false);
    }

    enum ColumnCreationMode {
        NEW_TABLE("New Table"), APPEND("Append"), REPLACE("Replace");

        private static String[] modes = {NEW_TABLE.toString(), APPEND.toString(), REPLACE.toString()};

        private String name;

        ColumnCreationMode(final String stringName) {
            this.name = stringName;
        }

        /**
         * @param name the name of the column creation mode
         * @return the ColumnCreationMode corresponding to that name
         * @throws IllegalArgumentException when the name is not associated with any ColumnCreationMode.
         */
        public static ColumnCreationMode fromString(final String name) {
            if (NEW_TABLE.toString().equals(name)) {
                return NEW_TABLE;
            }
            if (APPEND.toString().equals(name)) {
                return APPEND;
            }
            if (REPLACE.toString().equals(name)) {
                return REPLACE;
            }
            throw new IllegalArgumentException(
                    "ColumnCreationMode enum does not contain a value with name \"" + name + "\"");
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * @return an array containing the names of all available modes.
         */
        public static String[] getModes() {
            return modes;
        }
    }
}
