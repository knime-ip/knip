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
package org.knime.knip.base.node;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;
import org.knime.node2012.KnimeNodeDocument;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;

/**
 * Node factory mapping two data values to a data cell. Please note that if this factory is used, the node has to be
 * registered at a extension point using ONLY the {@link NodeSetFactory} class. Registering this class directly will NOT
 * work so far.
 * 
 * 
 * @param <VIN1>
 * @param <VIN2>
 * @param <COUT>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class TwoValuesToCellNodeFactory<VIN1 extends DataValue, VIN2 extends DataValue> extends
        DynamicNodeFactory<TwoValuesToCellNodeModel<VIN1, VIN2, ? extends DataCell>> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void addNodeDescription(final KnimeNodeDocument doc) {
        createNodeDescription(doc);

        KnimeNode node = doc.getKnimeNode();
        if (node == null) {
            XMLNodeUtils.addXMLNodeDescriptionTo(doc, this.getClass());
            node = doc.getKnimeNode();
        }

        // Load if possible
        if (node != null) {

            // add description of this dialog
            //TODO!
            TableCellViewNodeView.addViewDescriptionTo(node.addNewViews());

            if (node.getPorts() == null) {
                //add default port description
                //TODO!
            }

            // Add user stuff
            addNodeDescriptionContent(node);
        }
    }

    /**
     * Overwrite this method to add additional details programmatically to the already existing node description
     * (created either from an xml file or in
     * {@link TwoValuesToCellNodeFactory#createNodeDescription(KnimeNodeDocument)}.
     * 
     * @param node
     */
    protected void addNodeDescriptionContent(final KnimeNode node) {
        // Nothing to do here
    }

    /**
     * Overwrite this method if you want to create the node description programmatically. A description in the xml file
     * named after the derived class will not be used.
     * 
     * @param doc
     * @return
     */
    protected void createNodeDescription(final KnimeNodeDocument doc) {
        // May be overwritten
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
            NodeView<TwoValuesToCellNodeModel<VIN1, VIN2, ? extends DataCell>>
            createNodeView(final int viewIndex, final TwoValuesToCellNodeModel<VIN1, VIN2, ? extends DataCell> nodeModel) {
        return new TableCellViewNodeView<TwoValuesToCellNodeModel<VIN1, VIN2, ? extends DataCell>>(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override
    protected TwoValuesToCellNodeDialog<VIN1, VIN2> createNodeDialogPane() {
        return createNodeDialog();
    }

    /**
     * 
     * @return the new dialog
     */
    protected abstract TwoValuesToCellNodeDialog<VIN1, VIN2> createNodeDialog();

}
