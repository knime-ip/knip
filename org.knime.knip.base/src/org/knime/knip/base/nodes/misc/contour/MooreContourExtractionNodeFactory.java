/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * Created on Feb 7, 2014 by squareys
 */
package org.knime.knip.base.nodes.misc.contour;

import java.util.List;

import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.data.PolygonCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.data.algebra.ExtendedPolygon;

/**
 *
 * @author Jonathan Hale (University of Konstanz)
 * @param <T>
 */
@SuppressWarnings("deprecation")
public class MooreContourExtractionNodeFactory<T extends RealType<T>, L extends Comparable<L>> extends
ValueToCellNodeFactory<ImgPlusValue<BitType>> {

    class MooreContourExtractionNodeModel extends ValueToCellNodeModel<ImgPlusValue<BitType>, PolygonCell> {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected PolygonCell compute(final ImgPlusValue<BitType> cellValue) throws Exception {
            MooreContourExtractionOp op = new MooreContourExtractionOp();

            ExtendedPolygon output = new ExtendedPolygon();

            op.compute(cellValue.getImgPlus(), output);

            return new PolygonCell(output);
        }

    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<BitType>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<BitType>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Dimensions", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimensions", 2, 2));

            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<BitType>, ? extends DataCell> createNodeModel() {
        return new MooreContourExtractionNodeModel();
    }
}
