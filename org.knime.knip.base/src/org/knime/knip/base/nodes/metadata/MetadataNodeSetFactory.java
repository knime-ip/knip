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
package org.knime.knip.base.nodes.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.config.ConfigRO;
import org.knime.knip.base.nodes.metadata.setimgmetadata.SetImgMetadataNodeFactory;
import org.knime.knip.base.nodes.metadata.transferimgmetadata.TransferImgMetadataNodeFactory;
import org.knime.knip.base.nodes.proc.DimExtendNodeFactory;
import org.knime.knip.base.nodes.proc.Remove1DimNodeFactory;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MetadataNodeSetFactory implements NodeSetFactory {

    private final Map<String, String> m_nodeFactories = new HashMap<String, String>();

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigRO getAdditionalSettings(final String id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAfterID(final String id) {
        return "/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCategoryPath(final String id) {
        return m_nodeFactories.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends NodeFactory<? extends NodeModel>> getNodeFactory(final String id) {
        try {
            return (Class<? extends NodeFactory<? extends NodeModel>>)Class.forName(id);
        } catch (final ClassNotFoundException e) {
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getNodeFactoryIds() {
        m_nodeFactories.put(SetImgMetadataNodeFactory.class.getCanonicalName(), "/community/knip/image/metadata");
        m_nodeFactories.put(TransferImgMetadataNodeFactory.class.getCanonicalName(), "/community/knip/image/metadata");
        m_nodeFactories.put(Remove1DimNodeFactory.class.getCanonicalName(), "/community/knip/image/metadata");
        m_nodeFactories.put(DimExtendNodeFactory.class.getCanonicalName(), "/community/knip/image/metadata");
        return m_nodeFactories.keySet();
    }

}
