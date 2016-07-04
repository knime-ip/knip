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
 * Created on Jun 3, 2015 by squareys
 */
package org.knime.knip.base.node;

import java.lang.reflect.Method;

import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDescription210Proxy;
import org.knime.core.node.NodeDescription27Proxy;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;
import org.knime.knip.cellviewer.CellNodeView;
import org.knime.node.v210.KnimeNodeDocument;
import org.knime.node.v210.KnimeNodeDocument.KnimeNode;

/**
 * {@link AbstractValueToCellNodeFactory} is a abstract implementation of {@link DynamicNodeFactory} for
 * {@link BufferedDataTableHolder} {@link NodeModel}, which have a {@link NodeDialogPane} and adds a
 * {@link TableCellViewNodeView} to it.
 *
 * @param <DIALOG> dialog pane type
 * @param <MODEL> node model type
 *
 * @see TwoValuesToCellNodeFactory
 * @see ValueToCellsNodeFactory
 * @see GenericValueToCellNodeFactory
 *
 * @author <a href="mailto:jonathan.hale@uni.kn">Jonathan Hale</a>
 */
public abstract class AbstractValueToCellNodeFactory<DIALOG extends NodeDialogPane, MODEL extends NodeModel & BufferedDataTableHolder>
        extends DynamicNodeFactory<MODEL> {

    /**
     * Check if the subclass is using deprecated node descriptions.
     *
     * @return true if the derived class overrides
     *         {@link #addNodeDescriptionContent(org.knime.node2012.KnimeNodeDocument.KnimeNode)}.
     * @deprecated Remove when support for deprecated node descriptions is dropped.
     */
    @Deprecated
    private boolean usingDeprecatedDoc() {
        try {
            // get the deprecated addNodeDescriptionContent method
            Method method =
                    getClass().getMethod("addNodeDescriptionContent",
                                         org.knime.node2012.KnimeNodeDocument.KnimeNode.class);

            if (method.getDeclaringClass() != AbstractValueToCellNodeFactory.class) {
                // subclass overrides deprecated addNodeDescriptionContent(...)
                return true;
            }

            method = getClass().getMethod("createNodeDescription", org.knime.node2012.KnimeNodeDocument.class);

            // return whether subclass overrides deprecated createNodeDescription(...)
            return method.getDeclaringClass() != AbstractValueToCellNodeFactory.class;
        } catch (NoSuchMethodException | SecurityException e) {
            // will not happen.
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final NodeDescription createNodeDescription() {
        if (usingDeprecatedDoc()) {
            // TODO: remove when support for deprecated node descriptions is dropped.
            return createNodeDescriptionDeprecated();
        }

        final KnimeNodeDocument doc = KnimeNodeDocument.Factory.newInstance();
        createNodeDescription(doc);

        KnimeNode node = doc.getKnimeNode();
        if (node == null) {
            XMLNodeUtils.addXMLNodeDescriptionTo(doc, this.getClass());
            node = doc.getKnimeNode();
        }

        // Load if possible
        if (node != null) {

            // add description of "this" dialog
            ValueToCellNodeDialog.addTabsDescriptionTo(node.getFullDescription());
            TableCellViewNodeView.addViewDescriptionTo(node.addNewViews());

            if (node.getPorts() == null) {
                ValueToCellNodeDialog.addPortsDescriptionTo(node);
            }

            // Add user stuff
            addNodeDescriptionContent(node);
        }

        return new NodeDescription210Proxy(doc);
    }

    /**
     * @deprecated Keep until {@link #addNodeDescription(org.knime.node2012.KnimeNodeDocument)} is removed.
     */
    @Deprecated
    protected final NodeDescription createNodeDescriptionDeprecated() {
        final org.knime.node2012.KnimeNodeDocument doc = org.knime.node2012.KnimeNodeDocument.Factory.newInstance();
        createNodeDescription(doc);

        org.knime.node2012.KnimeNodeDocument.KnimeNode node = doc.getKnimeNode();
        if (node == null) {
            XMLNodeUtils.addXMLNodeDescriptionTo(doc, this.getClass());
            node = doc.getKnimeNode();
        }

        // Load if possible
        if (node != null) {

            // add description of "this" dialog
            ValueToCellNodeDialog.addTabsDescriptionTo(node.getFullDescription());
            TableCellViewNodeView.addViewDescriptionTo(node.addNewViews());

            if (node.getPorts() == null) {
                ValueToCellNodeDialog.addPortsDescriptionTo(node);
            }

            // Add user stuff
            addNodeDescriptionContent(node);
        }

        return new NodeDescription27Proxy(doc);
    }

    /**
     * Overwrite this method to add additional details programmatically to the already existing node description
     * (created either from an xml file or in
     * {@link GenericValueToCellNodeFactory#createNodeDescription(KnimeNodeDocument)}.
     *
     * @param node
     * @see #createNodeDescription(KnimeNodeDocument)
     */
    protected void addNodeDescriptionContent(final KnimeNode node) {
        // Nothing to do here
    }

    /**
     * Overwrite this method to add additional details programmatically to the already existing node description
     * (created either from an xml file or in
     * {@link GenericValueToCellNodeFactory#createNodeDescription(KnimeNodeDocument)}.
     *
     * @param node
     * @see #createNodeDescription(org.knime.node2012.KnimeNodeDocument)
     * @deprecated Consider using Consider using {@link org.knime.node.v210.KnimeNodeDocument.KnimeNode} and override
     *             {@link #createNodeDescription(org.knime.node.v210.KnimeNodeDocument)} and
     *             {@link #addNodeDescriptionContent(org.knime.node.v210.KnimeNodeDocument.KnimeNode)} instead.
     */
    @Deprecated
    protected void addNodeDescriptionContent(final org.knime.node2012.KnimeNodeDocument.KnimeNode node) {
        // Nothing to do here
    }

    /**
     * Overwrite this method if you want to create the node description programmatically. A description in the xml file
     * named after the derived class will not be used.
     *
     * @param doc
     * @see #addNodeDescriptionContent(KnimeNode)
     */
    protected void createNodeDescription(final KnimeNodeDocument doc) {
        // May be overwritten
    }

    /**
     * Overwrite this method if you want to create the node description programmatically. A description in the xml file
     * named after the derived class will not be used.
     *
     * @param doc
     * @see #addNodeDescriptionContent(org.knime.node2012.KnimeNodeDocument.KnimeNode)
     * @deprecated Consider using {@link org.knime.node.v210.KnimeNodeDocument} and override
     *             {@link #createNodeDescription(org.knime.node.v210.KnimeNodeDocument)} and
     *             {@link #addNodeDescriptionContent(org.knime.node.v210.KnimeNodeDocument.KnimeNode)} instead.
     *
     */
    @Deprecated
    protected void createNodeDescription(final org.knime.node2012.KnimeNodeDocument doc) {
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
    public NodeView<MODEL> createNodeView(final int viewIndex, final MODEL nodeModel) {
        return new CellNodeView<MODEL>(nodeModel);
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
     */
    @Override
    protected DIALOG createNodeDialogPane() {
        return createNodeDialog();
    }

    /**
     * @return the new dialog
     * @deprecated Override {@link #createNodeDialogPane()} instead (renaming should do the job).
     */
    @Deprecated
    /* TODO: Remove with next major API breaking release. #createNodeDialogPane()
     * is just fine and has better naming (since returns a NodeDialogPane) */
    protected abstract DIALOG createNodeDialog();

}
