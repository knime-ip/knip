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
package org.knime.knip.base.nodes.testing.TableCellViewer;

import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.data.DataValue;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.base.nodes.view.TableCellViewFactory;
import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.RendererFactory;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelColoringChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.LabelOptionsChangeEvent;
import org.knime.knip.core.ui.imgviewer.events.RendererSelectionChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ResetCacheEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewClosedEvent;

/**
 * This Class is an implementation of the {@link TableCellViewFactory} interface providing additional Methods for
 * testing labelings.
 *
 * @author Andreas Burger, University of Konstanz
 */
public class TestLabelingCellViewFactory<L extends Comparable<L>, II extends IntegerType<II>> implements
        TableCellViewFactory {

    @Override
    public TableCellView[] createTableCellViews() {
        return new TableCellView[]{new TableCellView() {

            // Lazy loading due to headless mode
            private ImgViewer m_view = null;

            /**
             * {@inheritDoc}
             */
            @Override
            public String getDescription() {
                return "Logging view on a labeling/segmentation";
            }

            @Override
            public String getName() {
                return "Labeling Test-Viewer";
            }

            @Override
            public ImgViewer getViewComponent() {
                if (m_view == null) {
                    m_view = TestViewerFactory.createLabelingViewer(KNIMEKNIPPlugin.getCacheSizeForBufferedImages());
                }
                return m_view;
            }

            @Override
            public void loadConfigurationFrom(final ConfigRO config) {
                //

            }

            @Override
            public void onClose() {
                getViewComponent().getEventService().publish(new ViewClosedEvent());
            }

            @Override
            public void onReset() {
                getViewComponent().getEventService().publish(new ResetCacheEvent());
            }

            @Override
            public void saveConfigurationTo(final ConfigWO config) {
                //

            }

            @Override
            public void updateComponent(final DataValue valueToView) {

                final LabelingValue<L> labelingValue = (LabelingValue<L>)valueToView;
                getViewComponent().setLabeling(labelingValue.getLabeling(), labelingValue.getLabelingMetadata());
                testComponent(labelingValue);
            }

            /**
             * This method handles the actual testing.
             *
             * @param labelingValue The labeling to be displayed, used in tests.
             */
            private void testComponent(final LabelingValue<L> labelingValue) {
                EventService service = m_view.getEventService();

                // Coloring

                RandomMissingColorHandler.resetColorMap();
                RandomMissingColorHandler.setSeed(12345l);
                service.publish(new LabelColoringChangeEvent(LabelingColorTableUtils.getBoundingBoxColor(),
                        RandomMissingColorHandler.getGeneration()));
                service.publish(new ImgRedrawEvent());
                service.publish(new TestCompleteEvent());
                RandomMissingColorHandler.setSeed(-1l);

                /* Disabled for the time being due to inability to hold on to seed.

                RandomMissingColorHandler.resetColorMap();
                service.publish(new LabelColoringChangeEvent(LabelingColorTableUtils.getBoundingBoxColor(),
                        RandomMissingColorHandler.getGeneration()));
                service.publish(new ImgRedrawEvent());
                service.publish(new TestCompleteEvent());
                  */

                // Renderer

                ImageRenderer<LabelingType<L>>[] tmp =
                        RendererFactory.createSuitableRenderer(labelingValue.getLabeling());
                for (ImageRenderer<LabelingType<L>> ir : tmp) {
                    service.publish(new RendererSelectionChgEvent(ir));
                    service.publish(new ImgRedrawEvent());
                    service.publish(new TestCompleteEvent());
                }

                // Rendering with Numbers
                service.publish(new LabelOptionsChangeEvent(true));
                service.publish(new ImgRedrawEvent());
                service.publish(new TestCompleteEvent());

                service.publish(new RendererSelectionChgEvent(tmp[0]));
                service.publish(new ImgRedrawEvent());

                service.publish(new LabelOptionsChangeEvent(false));
                service.publish(new ImgRedrawEvent());
                service.publish(new TestCompleteEvent());
            }

        }};

    }

    @Override
    public Class<? extends DataValue> getDataValueClass() {
        return LabelingValue.class;
    }

}
