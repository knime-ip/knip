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
 * Created on Sep 9, 2013 by dietzc
 */
package org.knime.knip.base.nodes.seg.colormanager;

import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.XMLNodeUtils;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;
import org.knime.node2012.KnimeNodeDocument;

/**
 * TODO
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ApplyColorSettingsToLabelsNodeFactory<L extends Comparable<L>> extends
        DynamicNodeFactory<ApplyColorSettingsToLabelsNodeModel<L>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplyColorSettingsToLabelsNodeModel<L> createNodeModel() {
        return new ApplyColorSettingsToLabelsNodeModel<L>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescription(final KnimeNodeDocument doc) {
        XMLNodeUtils.addXMLNodeDescriptionTo(doc, getClass());
        TableCellViewNodeView.addViewDescriptionTo(doc.getKnimeNode().addNewViews());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new DefaultNodeSettingsPane() {
            {
                addDialogComponent(new DialogComponentColumnNameSelection(
                        ApplyColorSettingsToLabelsNodeModel.createLabelingColumnModel(), "Labeling Column", 0,
                        LabelingValue.class));

                addDialogComponent(new DialogComponentBoolean(
                        ApplyColorSettingsToLabelsNodeModel.createOverrideExistingColorTablesModel(),
                        "Override Existing Color Tables?"));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<ApplyColorSettingsToLabelsNodeModel<L>>
            createNodeView(final int viewIndex, final ApplyColorSettingsToLabelsNodeModel<L> nodeModel) {
        return new TableCellViewNodeView<ApplyColorSettingsToLabelsNodeModel<L>>(nodeModel);
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
    protected boolean hasDialog() {
        return true;
    }

}
