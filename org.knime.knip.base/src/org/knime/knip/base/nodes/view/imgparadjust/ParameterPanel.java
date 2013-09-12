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
package org.knime.knip.base.nodes.view.imgparadjust;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgViewerMousePressedEvent;
import org.knime.knip.core.ui.imgviewer.events.NormalizationParametersChgEvent;
import org.knime.knip.core.ui.imgviewer.events.ViewZoomfactorChgEvent;
import org.knime.knip.core.ui.imgviewer.panels.ImgNormalizationPanel;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ParameterPanel extends ViewerComponent {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private EventService m_eventService;

    private int m_mouseCoordX;

    private int m_mouseCoordY;

    private final JTextArea m_parameterList;

    private double m_saturation;

    private double m_thresholdVal;

    private double m_zoomFact;

    public ParameterPanel() {
        super("Parameters", false);

        m_parameterList = new JTextArea(5, 10);
        m_parameterList.setEditable(false);
        add(new JScrollPane(m_parameterList));
        updatedParameterList();

    }

    /**
     * @return the mouseCoordX
     */
    public int getMouseCoordX() {
        return m_mouseCoordX;
    }

    /**
     * @return the mouseCoordY
     */
    public int getMouseCoordY() {
        return m_mouseCoordY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition() {
        return Position.SOUTH;
    }

    /**
     * @return the saturation
     */
    public double getSaturation() {
        return m_saturation;
    }

    /**
     * @return the thresholdVal
     */
    public double getThresholdVal() {
        return m_thresholdVal;
    }

    /**
     * @return the zoomFact
     */
    public double getZoomFact() {
        return m_zoomFact;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // TODO Auto-generated method stub

    }

    @EventListener
    public void onImgViewerMousePressed(final ImgViewerMousePressedEvent e) {
        m_mouseCoordX = e.getPosX();
        m_mouseCoordY = e.getPosY();
        updatedParameterList();
    }

    /**
     * Listens to changes in the {@link ImgNormalizationPanel} of the viewer.
     * 
     * @param np
     */
    @EventListener
    public void onNormalizationParameterChanged(final NormalizationParametersChgEvent np) {
        m_saturation = np.getSaturation();
        if (!np.isNormalized()) {
            m_saturation = 0;
        }
        updatedParameterList();
    }

    @EventListener
    public void onThresholdValChanged(final ThresholdValChgEvent e) {
        m_thresholdVal = e.getValue();
        updatedParameterList();
    }

    @EventListener
    public void onZoomFactorChanged(final ViewZoomfactorChgEvent e) {
        m_zoomFact = e.getZoomFactor();
        updatedParameterList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        m_eventService.subscribe(this);

    }

    private void updatedParameterList() {
        final StringBuilder sb = new StringBuilder();
        sb.append("saturation=");
        sb.append(m_saturation);
        sb.append("\n");
        sb.append("posX=");
        sb.append(m_mouseCoordX);
        sb.append("\n");
        sb.append("posY=");
        sb.append(m_mouseCoordY);
        sb.append("\n");
        sb.append("zoomFactor=");
        sb.append(m_zoomFact);
        sb.append("\n");
        sb.append("threshold=");
        sb.append(m_thresholdVal);
        sb.append("\n");
        m_parameterList.setText(sb.toString());
    }

}
