/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
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
 * Created on Jan 9, 2017 by gabriel
 */
package org.knime.knip.base.nodes.seg.cropper;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;

/**
 *
 * @author gabriel
 */
class SegmentCropperNodeSettings {

    enum BACKGROUND_OPTION {
        MIN("Min Value of Result"), MAX("Max Value of Result"), ZERO("Zero"), SOURCE("Source");

        private String name;

        private BACKGROUND_OPTION(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Helper
     *
     * @return SettingsModel to store img column
     */
    static SettingsModelBoolean createAddOverlappingLabels() {
        return new SettingsModelBoolean("cfg_add_dependendcy", false);
    }

    static SettingsModelBoolean createNotEnforceCompleteOverlapModel(final boolean enabled) {
        SettingsModelBoolean sm = new SettingsModelBoolean("no_complete_overlap", false);
        sm.setEnabled(enabled);
        return sm;
    }

    /**
     * Helper
     *
     * @return SettingsModel to store img column
     */
    static SettingsModelString createImgColumnSelectionModel() {
        return new SettingsModelString("cfg_img_col", "");
    }

    /**
     * @return selected value for the background (parts of a bounding box that do not belong to the label.
     */
    static SettingsModelString createBackgroundSelectionModel() {
        return new SettingsModelString("backgroundOptions", BACKGROUND_OPTION.MIN.toString());
    }

    /**
     * Helper
     *
     * @return SettingsModelFilterSelection to store left filter selection
     */
    static <LL extends Comparable<LL>> SettingsModelFilterSelection<LL> createLabelFilterModel() {
        return new SettingsModelFilterSelection<>("cfg_label_filter_left");
    }

    /**
     * Helper
     *
     * @return SettingsModelFilterSelection to store right filter selection
     */
    static <LL extends Comparable<LL>> SettingsModelFilterSelection<LL>
           createOverlappingLabelFilterModel(final boolean isEnabled) {
        final SettingsModelFilterSelection<LL> sm = new SettingsModelFilterSelection<>("cfg_label_filter_right");
        sm.setEnabled(isEnabled);
        return sm;
    }

    /**
     * Helper
     *
     * @return SettingsModel to store labeling column
     */
    static SettingsModelString createSMLabelingColumnSelection() {
        return new SettingsModelString("cfg_labeling_column", "");
    }
}
