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
package org.knime.knip.core.awt;

import java.util.Set;

import org.knime.knip.core.awt.converter.LabelingTypeARGBConverter;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.ExtendedLabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableRenderer;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.awt.parametersupport.RendererWithHilite;
import org.knime.knip.core.awt.parametersupport.RendererWithLabels;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter.Operator;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.view.Views;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ColorLabelingRenderer<L> extends ProjectingRenderer<LabelingType<L>>
        implements RendererWithLabels<L>, RendererWithHilite, LabelingColorTableRenderer {

    /**
     * RENDERER_NAME.
     */
    public static final String RENDERER_NAME = "Random Color Labeling Renderer";

    private LabelingTypeARGBConverter<L> m_converter;

    //    private LabelingMapping<L> m_labelMapping;

    private boolean m_rebuildRequired;

    private LabelingColorTable m_colorMapping;

    public ColorLabelingRenderer() {
        m_rebuildRequired = true;
    }

    @Override public void setOperator(final Operator operator) {
        rebuildLabelConverter();
        m_converter.setOperator(operator);
        m_rebuildRequired = true;
    }

    @Override
    public void setActiveLabels(final Set<String> activeLabels) {
        rebuildLabelConverter();
        m_converter.setActiveLabels(activeLabels);
        m_rebuildRequired = true;
    }

    @Override
    public void setHilitedLabels(final Set<String> hilitedLabels) {
        rebuildLabelConverter();
        m_converter.setHilitedLabels(hilitedLabels);
        m_rebuildRequired = true;
    }

    @Override
    public void setHiliteMode(final boolean isHiliteMode) {
        rebuildLabelConverter();
        m_converter.setHiliteMode(isHiliteMode);
        m_rebuildRequired = true;
    }

    @Override
    public String toString() {
        return "Random Color Labeling Renderer";
    }

    @Override
    protected IterableIntervalProjector2D<LabelingType<L>, ARGBType>
              getProjector(final int dimX, final int dimY, final RandomAccessibleInterval<LabelingType<L>> source,
                           final RandomAccessibleInterval<ARGBType> target) {

        rebuildLabelConverter();

        return new IterableIntervalProjector2D<LabelingType<L>, ARGBType>(dimX, dimY, source, Views.iterable(target),
                m_converter);
    }

    // create the converter

    private void rebuildLabelConverter() {
        if (m_rebuildRequired) {
            m_rebuildRequired = false;
            //check whether all necessary fields are set, if not, use the default values
            if (m_colorMapping == null) {
                m_colorMapping = new ExtendedLabelingColorTable(new DefaultLabelingColorTable(),
                        new RandomMissingColorHandler());
            }
            m_converter = new LabelingTypeARGBConverter<L>(m_colorMapping);
        }
    }

    @Override
    public void setRenderingWithLabelStrings(final boolean withNumbers) {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLabelingColorTable(final LabelingColorTable mapping) {
        m_rebuildRequired = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLabelMapping(final LabelingMapping<L> labelMapping) {
        // TODO Auto-generated method stub

    }

}
