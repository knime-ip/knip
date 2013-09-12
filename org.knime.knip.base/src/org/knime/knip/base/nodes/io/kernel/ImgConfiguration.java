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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.img.Img;
import net.imglib2.meta.Axes;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.core.ui.imgviewer.ImgCanvas;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.ViewerComponents;
import org.knime.knip.core.ui.imgviewer.events.ViewZoomfactorChgEvent;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;
import org.knime.knip.core.ui.imgviewer.panels.infobars.ImgViewInfoPanel;
import org.knime.knip.core.ui.imgviewer.panels.providers.BufferedImageProvider;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class ImgConfiguration<T extends RealType<T>> extends SerializableConfiguration<Img<T>[]> {

    protected final static int STANDARD_PREF_WIDTH = 150;

    protected final static int STANDARD_TEXTFIELD_COLUMNS = 10;

    private static <T extends RealType<T>, I extends Img<T>> ImgViewer createImgViewer() {
        final ImgViewer viewer = new ImgViewer();
        final BufferedImageProvider<T> realProvider =
                new BufferedImageProvider<T>(KNIMEKNIPPlugin.getCacheSizeForBufferedImages());
        realProvider.setEventService(viewer.getEventService());
        viewer.addViewerComponent(new ImgViewInfoPanel<T>());
        viewer.addViewerComponent(new ImgCanvas<T, Img<T>>());
        viewer.addViewerComponent(ViewerComponents.MINIMAP.createInstance());
        viewer.addViewerComponent(ViewerComponents.PLANE_SELECTION.createInstance());
        viewer.addViewerComponent(new ImgNormalizationPanel<T, Img<T>>(0, true));
        // set normalization true as default and higher zoom level
        // TODO: build constructor for minimap
        viewer.getEventService().publish(new ViewZoomfactorChgEvent(10));
        return viewer;
    }

    private JPanel m_applyButtonPanel;

    private Img<T>[] m_imgs;

    private JPanel m_jpViewPanel;

    private JSlider m_jsIndex;

    private JPanel m_sliderPanel;

    private JLabel m_errorMessage;

    protected ActionListener m_updatePreviewListener = new ActionListener() {

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            updatePreview();
        }
    };

    private ImgViewer m_view;

    /**
     * @return the inner configuration panel without variant slider or apply button just config ui items like textfields
     *         etc.
     */
    protected abstract JPanel getConfigContentPanel();

    @Override
    public JComponent getConfigurationPanel() {
        final JComponent center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));

        center.add(getVariantSliderPanel());
        center.add(getConfigContentPanel());
        center.add(Box.createHorizontalGlue());
        center.add(getRefreshConfigPanel());

        JPanel res = new JPanel(new BorderLayout());
        res.setBorder(BorderFactory.createTitledBorder("Configuration"));
        res.add(center, BorderLayout.CENTER);

        m_errorMessage = new JLabel();
        m_errorMessage.setForeground(Color.RED);
        res.add(m_errorMessage, BorderLayout.SOUTH);

        return res;
    }

    @Override
    public JComponent getPreviewPanel() {
        if (m_jpViewPanel == null) {
            // ensure that the slider is created before
            // it is used or updated
            getVariantSliderPanel();

            m_view = createImgViewer();
            m_jpViewPanel = new JPanel(new BorderLayout());
            m_jpViewPanel.add(m_view, BorderLayout.CENTER);
            updatePreview();
        }
        return m_jpViewPanel;
    }

    protected JPanel getRefreshConfigPanel() {
        if (m_applyButtonPanel == null) {
            m_applyButtonPanel = new JPanel(new BorderLayout());

            final JButton applyButton = new JButton("Refresh");
            applyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    m_updatePreviewListener.actionPerformed(null);
                }
            });

            final JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.add(applyButton, BorderLayout.EAST);

            m_applyButtonPanel.add(bottomPanel, BorderLayout.SOUTH);
        }
        return m_applyButtonPanel;
    }

    protected JPanel getVariantSliderPanel() {
        if (m_sliderPanel == null) {
            m_sliderPanel = new JPanel();
            m_sliderPanel.setLayout(new BoxLayout(m_sliderPanel, BoxLayout.Y_AXIS));

            m_sliderPanel.add(new JLabel("Variant Browser"));
            m_jsIndex = new JSlider();
            m_jsIndex.setPreferredSize(new Dimension(STANDARD_PREF_WIDTH, m_jsIndex.getPreferredSize().height));

            m_jsIndex.setSnapToTicks(true);
            m_jsIndex.setMaximum(0);// will be overwritten set to
            // have a secure base value

            m_sliderPanel.add(m_jsIndex);

            m_jsIndex.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    updatePreview(m_jsIndex.getValue());
                }
            });
        }
        return m_sliderPanel;
    }

    protected void updatePreview() {
        try {
            validate();
        } catch (InvalidSettingsException e) {
            m_errorMessage.setText(e.getMessage());
            return;
        }
        if (m_errorMessage != null) {
            m_errorMessage.setText("");
        }
        m_imgs = getSetting().get();
        m_jsIndex.setMaximum(m_imgs.length - 1);
        m_jsIndex.setValue(Math.min(m_jsIndex.getValue(), m_imgs.length - 1));
        m_jsIndex.setEnabled(m_imgs.length > 1);
        updatePreview(m_jsIndex.getValue());
    }

    protected void updatePreview(final int index) {
        if ((index < 0) || (index > m_imgs.length)) {
            return;
        }
        final Img<T> img = m_imgs[index];
        final DefaultCalibratedSpace cs = new DefaultCalibratedSpace(img.numDimensions());
        for (int i = 0; i < img.numDimensions(); i++) {
            cs.setAxis(new DefaultCalibratedAxis(Axes.get("X" + i)), i);
        }
        m_view.setImg(new ImgPlus<T>(img));
    }
}
