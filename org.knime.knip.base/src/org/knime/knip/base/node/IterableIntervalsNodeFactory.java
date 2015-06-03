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

import net.imglib2.type.numeric.RealType;

import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.node.v210.KnimeNodeDocument.KnimeNode;
import org.knime.node.v210.OptionDocument.Option;
import org.knime.node.v210.TabDocument.Tab;
import org.knime.node.v210.UlDocument.Ul;

/**
 * NodeFactory for {@link IterableIntervalsNodeModel}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T> Type of Input
 * @param <V> Type of Output
 * @param <L> Type of Labeling
 */
public abstract class IterableIntervalsNodeFactory<T extends RealType<T>, V extends RealType<V>, L extends Comparable<L>>
        extends ValueToCellNodeFactory<ImgPlusValue<T>> {


    /**
     * Variable holding information whether this node has a dimSelection or not.
     */
    protected boolean m_hasDimensionSelection = true;

    /**
     * {@inheritDoc}
     */
    @Override
    protected abstract IterableIntervalsNodeDialog<T> createNodeDialog();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract IterableIntervalsNodeModel<T, V, L> createNodeModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescriptionContent(final KnimeNode node) {
        if(m_hasDimensionSelection) {
            DialogComponentDimSelection.createNodeDescription(node.getFullDescription().getTabList().get(0).addNewOption());
        }

        Tab tab = node.getFullDescription().addNewTab();
        tab.setName("ROI Options");

        createLabelingColumnSelectionDescription(tab.addNewOption());
        createFillingModeDescription(tab.addNewOption());
    }

    /**
     * @param addNewOption
     */
    private void createFillingModeDescription(final Option opt) {
        opt.setName("Filling Mode");
        opt.addNewP()
                .newCursor()
                .setTextValue("The FillingMode determines how all values, which lie outside your defined region of interest, will be set. This option is only needed if you choose a labeling column, such that the node operates on ROIs instead of the entire image. There are currently four FillingModes:");
        Ul list = opt.addNewUl();
        list.addLi("Value of Source: In this mode, pixels outside of the ROIs remain unchanged. ");
        list.addLi("Minimum of Result Type: Here, values outside of the ROI are set to the smallest legal value of the output image type. ");
        list.addLi("Maximum of Result Type: All values outside of the ROI are set to the largest posible value of the output image type. ");
        list.addLi("No Filling: No action is taken after initializing the target image, thus all pixels outside the ROIs remain zero. ");

    }

    /**
     * @param addNewOption
     */
    private void createLabelingColumnSelectionDescription(final Option opt) {
        opt.setName("Labeling Column");
        opt.addNewP()
                .newCursor()
                .setTextValue("If you choose a column with a labeling here, the node will only operate on the Regions of Interests (ROIs) which are defined by the incoming labeling!");
    }
}