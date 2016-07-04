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
package org.knime.knip.base.nodes.seg.labeltopng;

import java.util.Arrays;

import javax.swing.ListSelectionModel;

import org.knime.core.data.collection.ListCell;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColorChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;

import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;

/**
 * Converts Labelings with (optional) background images to PNGImageCell using the view renderes
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingToPNGValueNodeFactory<T extends RealType<T>, L extends Comparable<L> & Type<L>> extends
        TwoValuesToCellNodeFactory<ImgPlusValue<T>, LabelingValue<L>> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected TwoValuesToCellNodeDialog<ImgPlusValue<T>, LabelingValue<L>> createNodeDialog() {
        return new TwoValuesToCellNodeDialog<ImgPlusValue<T>, LabelingValue<L>>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void addDialogComponents() {
                DialogComponentNumber transparency =
                        new DialogComponentNumber(LabelingToPNGValueNodeModel.createTransparencySM(),
                                "Transparency of labels", 1);
                DialogComponentStringListSelection rendererSelection =
                        new DialogComponentStringListSelection(LabelingToPNGValueNodeModel.createRendererSM(),
                                "Select a renderer", Arrays.asList(LabelingToPNGValueNodeModel.RENDERER_NAMES),
                                ListSelectionModel.SINGLE_SELECTION, true, 3);
                DialogComponentColorChooser boundingBoxColor =
                        new DialogComponentColorChooser(LabelingToPNGValueNodeModel.createColorSM(),
                                "Select bounding box color", true);
                DialogComponentBoolean showLabelNames =
                        new DialogComponentBoolean(LabelingToPNGValueNodeModel.createShowBoundingBoxNameSM(),
                                "Show bounding box names");

                //image settings
                addDialogComponent("Image Settings", "", transparency);
                addDialogComponent("Image Settings", "", rendererSelection);
                addDialogComponent("Image Settings", "", boundingBoxColor);
                addDialogComponent("Image Settings", "", showLabelNames);

                //dim selection
                addDialogComponent("Options", "Dimension selection", new DialogComponentDimSelection(
                        LabelingToPNGValueNodeModel.createDimSelectionModelPlane(), "Dimensions Plane", 2, 2));

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_ltPNG";
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TwoValuesToCellNodeModel<ImgPlusValue<T>, LabelingValue<L>, ListCell> createNodeModel() {
        return new LabelingToPNGValueNodeModel<T, L>();
    }

}
