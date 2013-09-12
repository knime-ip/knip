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
package org.knime.knip.base.nodes.io.kernel;

import java.util.LinkedHashMap;
import java.util.Map;

import net.imglib2.type.numeric.RealType;

import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.knip.base.node.XMLNodeUtils;
import org.knime.knip.base.nodes.io.kernel.filter.DerivativeOfGaussianConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.FreiChenConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.GaborCircularConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.GaborConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.GaborCurvedConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.GaussianConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.KirschConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.LaplacianConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.LaplacianOfGaussianConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.PrewittConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.RobertsConfiguration;
import org.knime.knip.base.nodes.io.kernel.filter.SobelConfiguration;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;
import org.knime.node2012.KnimeNodeDocument;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ConvolutionKernelNodeFactory<T extends RealType<T>> extends DynamicNodeFactory<KernelCreatorNodeModel<T>> {

    private final Map<String, Class<?>> m_pool;

    public ConvolutionKernelNodeFactory() {
        m_pool = createPool();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescription(final KnimeNodeDocument doc) {
        XMLNodeUtils.addXMLNodeDescriptionTo(doc, getClass());
        TableCellViewNodeView.addViewDescriptionTo(doc.getKnimeNode().addNewViews());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new DefaultNodeSettingsPane() {
            {

                addDialogComponent(new DialogComponentSerializableConfiguration(
                        KernelCreatorNodeModel.createKernelListModel(), m_pool));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KernelCreatorNodeModel<T> createNodeModel() {
        return new KernelCreatorNodeModel<T>(m_pool);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<KernelCreatorNodeModel<T>> createNodeView(final int viewIndex,
                                                              final KernelCreatorNodeModel<T> nodeModel) {
        return new TableCellViewNodeView<KernelCreatorNodeModel<T>>(nodeModel);
    }

    private Map<String, Class<?>> createPool() {
        final Map<String, Class<?>> pool = new LinkedHashMap<String, Class<?>>();
        pool.put("Gabor", GaborConfiguration.class);
        pool.put("Gabor Circular", GaborCircularConfiguration.class);
        pool.put("Gabor Curved", GaborCurvedConfiguration.class);
        pool.put("Derivative of Gaussian", DerivativeOfGaussianConfiguration.class);
        pool.put("Gaussian", GaussianConfiguration.class);
        pool.put("Laplacian Of Gaussian", LaplacianOfGaussianConfiguration.class);
        // constant filters
        pool.put("Sobel", SobelConfiguration.class);
        pool.put("Roberts", RobertsConfiguration.class);
        pool.put("Kirsch", KirschConfiguration.class);
        pool.put("Prewitt", PrewittConfiguration.class);
        pool.put("Frei & Chen", FreiChenConfiguration.class);
        pool.put("Laplacian", LaplacianConfiguration.class);
        return pool;
    }

    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
}
