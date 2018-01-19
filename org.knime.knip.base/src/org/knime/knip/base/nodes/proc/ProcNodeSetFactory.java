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
package org.knime.knip.base.nodes.proc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.config.ConfigRO;
import org.knime.knip.base.nodes.misc.dimswap.DimensionSwapperNodeFactory;
import org.knime.knip.base.nodes.misc.merger.MergerNodeFactory;
import org.knime.knip.base.nodes.misc.splitter.Splitter2NodeFactory;
import org.knime.knip.base.nodes.misc.splitter.UCSplitterNodeFactory;
import org.knime.knip.base.nodes.orientationj.measure.OrientationJMeasurementNodeFactory;
import org.knime.knip.base.nodes.proc.binner.IntensityBinnerNodeFactory;
import org.knime.knip.base.nodes.proc.clahe.ClaheNodeFactory;
import org.knime.knip.base.nodes.proc.dogdetector.DoGDetector3NodeFactory;
import org.knime.knip.base.nodes.proc.imgjep.ImgJEPNodeFactory;
import org.knime.knip.base.nodes.proc.maxfinder.MaximumFinderNodeFactory;
import org.knime.knip.base.nodes.proc.multilvlthresholding.MultilevelThresholderNodeFactory;
import org.knime.knip.base.nodes.proc.resizer.LabelingResizerNodeFactory;
import org.knime.knip.base.nodes.proc.resizer.ResizerNodeFactory;
import org.knime.knip.base.nodes.proc.spotdetection.WaveletSpotDetectionNodeFactory;
import org.knime.knip.base.nodes.proc.thinning.ThinningNodeFactory;
import org.knime.knip.base.nodes.proc.ucm.UCMNodeFactory;
import org.knime.knip.base.nodes.seg.local.LocalThresholderNodeFactory2;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ProcNodeSetFactory implements NodeSetFactory {

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
        return "";
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
    @SuppressWarnings("unchecked")
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
        m_nodeFactories.put(LocalThresholderNodeFactory2.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(MultilevelThresholderNodeFactory.class.getCanonicalName(), "/community/knip/image/process");
        //        m_nodeFactories.put(AlignerNodeFactory.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(ThinningNodeFactory.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(ImgJEPNodeFactory.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(ConvertImgNodeFactory.class.getCanonicalName(), "/community/knip/image");
        m_nodeFactories.put(LocalMaximaForDistanceMapNodeFactory.class.getCanonicalName(),
                            "/community/knip/image/process");

        m_nodeFactories.put(ProjectorNodeFactory.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(Rotation2DNodeFactory.class.getCanonicalName(), "/community/knip/image");
        m_nodeFactories.put(ResizerNodeFactory.class.getCanonicalName(), "/community/knip/image");
        m_nodeFactories.put(LabelingResizerNodeFactory.class.getCanonicalName(), "/community/knip/image");
        m_nodeFactories.put(UCSplitterNodeFactory.class.getCanonicalName(), "/community/knip/image");
        m_nodeFactories.put(DimensionSwapperNodeFactory.class.getCanonicalName(), "/community/knip/image");
        m_nodeFactories.put(Splitter2NodeFactory.class.getCanonicalName(), "/community/knip/image");
        m_nodeFactories.put(MergerNodeFactory.class.getCanonicalName(), "/community/knip/image");
        //        m_nodeFactories.put(ImgCropperNodeFactory.class.getCanonicalName(), "/community/knip/image");
        m_nodeFactories.put(AutoCropNodeFactory.class.getCanonicalName(), "/community/knip/image");

        m_nodeFactories.put(GrayscaleReconstructionNodeFactory.class.getCanonicalName(),
                            "/community/knip/image/process");

        m_nodeFactories.put(HDomeNodeFactory.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(MaximumFinderNodeFactory.class.getCanonicalName(), "/community/knip/image/process");

        m_nodeFactories.put(WaveletSpotDetectionNodeFactory.class.getCanonicalName(), "/community/knip/image/process");

        // new node: UCM
        m_nodeFactories.put(UCMNodeFactory.class.getCanonicalName(), "/community/knip/image/process");

        // ROI based nodes (which deprecated the old implementations)
        m_nodeFactories.put(ImgNormalizerNodeFactory.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(InverterNodeFactory2.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(ThresholderNodeFactory3.class.getCanonicalName(), "/community/knip/image/process");

        // Deprecated ClaheNodeFactory
        m_nodeFactories.put(ClaheNodeFactory.class.getCanonicalName(), "/community/knip/image/process");

        // DoG Detection
//        m_nodeFactories.put(DoGDetectorNodeFactory.class.getCanonicalName(), "/community/knip/image/process");
//        m_nodeFactories.put(DoGDetector2NodeFactory.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(DoGDetector3NodeFactory.class.getCanonicalName(), "/community/knip/image/process");

        // new version
        m_nodeFactories.put(ImgCropperNodeFactory2.class.getCanonicalName(), "/community/knip/image/process");
        m_nodeFactories.put(AlignerNodeFactory2.class.getCanonicalName(), "/community/knip/image/process");

        m_nodeFactories.put(ColorspaceConvertNodeFactory.class.getCanonicalName(), "/community/knip/image/process");

        m_nodeFactories.put(IntensityBinnerNodeFactory.class.getCanonicalName(), "/community/knip/image/process");

        m_nodeFactories.put(OrientationJMeasurementNodeFactory.class.getCanonicalName(), "/community/knip/image/process");

        return m_nodeFactories.keySet();
    }
}
