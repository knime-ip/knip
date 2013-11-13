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
 * ---------------------------------------------------------------------
 *
 * Created on 13.11.2013 by Christian Dietz
 */
package org.knime.knip.base.node;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.IterableIntervalsNodeModel.FillingMode;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.util.EnumUtils;

/**
 * This dialogs adds the possibility to optionally select a column containing a labeling which restricts the computation
 * of the underlying operation to certain regions of interest in the image.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 * @param <T> the type of the input
 *
 */
public abstract class IterableIntervalsNodeDialog<T extends RealType<T>> extends ValueToCellNodeDialog<ImgPlusValue<T>> {

    /**
     * To disable/enable from child classes
     */
    protected SettingsModelDimSelection m_dimSelectionModel = IterableIntervalsNodeModel.createDimSelectionModel("X",
                                                                                                                 "Y");

    private boolean hasDimSelection;

    /**
     * Lazily initialize the super constructor
     */
    public IterableIntervalsNodeDialog(final boolean hasDimSelection) {
        super(true);

        this.hasDimSelection = hasDimSelection;
        this.addOwnDcs();

        super.init();
    }

    /*
     * adds all required dialog components
     */
    @SuppressWarnings("unchecked")
    private void addOwnDcs() {
        final SettingsModelString optionalColumn = IterableIntervalsNodeModel.createOptionalColumnModel();
        final SettingsModelString fillingModeModel = IterableIntervalsNodeModel.createFillingModeModel();

        optionalColumn.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if (optionalColumn.getStringValue() == null || optionalColumn.getStringValue().equalsIgnoreCase("")) {
                    if (hasDimSelection) {
                        m_dimSelectionModel.setEnabled(true);
                    }
                    fillingModeModel.setEnabled(false);
                } else {
                    if (hasDimSelection) {
                        m_dimSelectionModel.setEnabled(false);
                    }
                    fillingModeModel.setEnabled(true);
                }
            }
        });

        addDialogComponent("Column Selection", "Choose Labeling Column (optional)",
                           new DialogComponentColumnNameSelection(optionalColumn, getFirstColumnSelectionLabel(), 0,
                                   false, true, LabelingValue.class));

        // IterableIntervals don't rely on dimensions. Should be working for any selection
        if (hasDimSelection) {
            addDialogComponent("Options", "Dimension Selection", new DialogComponentDimSelection(m_dimSelectionModel,
                    "", 1, Integer.MAX_VALUE));
        }

        addDialogComponent("Options", "Filling Mode", new DialogComponentStringSelection(fillingModeModel,
                "Filling Strategy", EnumUtils.getStringCollectionFromName(FillingMode.values())));
    }

    /**
     * Define the label of the optional column (default = Labeling)
     *
     * @return return the displayed name of the column
     */
    protected String getFirstColumnSelectionLabel() {
        return "Labeling";
    }
}