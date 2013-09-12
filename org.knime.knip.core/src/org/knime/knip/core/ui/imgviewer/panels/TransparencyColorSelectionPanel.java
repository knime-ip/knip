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
package org.knime.knip.core.ui.imgviewer.panels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;

/**
 * composite of a {@link TransparencyPanel} and a {@link LabelOptionPanel}.
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class TransparencyColorSelectionPanel extends ViewerComponent {

    private final TransparencyPanel m_transparencyPanel;

    private final LabelOptionPanel m_colorSelectionPanel;

    public TransparencyColorSelectionPanel() {
        super("Color Options", false);

        m_transparencyPanel = new TransparencyPanel(true);
        m_colorSelectionPanel = new LabelOptionPanel(true);

        setLayout(new GridBagLayout());
        final GridBagConstraints gc = new GridBagConstraints();

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridy = 0;

        add(m_transparencyPanel, gc);

        gc.gridy = 1;
        add(m_colorSelectionPanel, gc);

        setMinimumSize(new Dimension(250, 150));
        setPreferredSize(new Dimension(250, 150));
        setMaximumSize(new Dimension(250, 150));

    }

    @Override
    public void setEventService(final EventService eventService) {
        m_transparencyPanel.setEventService(eventService);
        m_colorSelectionPanel.setEventService(eventService);
        eventService.subscribe(this);
    }

    @Override
    public Position getPosition() {
        return Position.SOUTH;
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        m_transparencyPanel.saveComponentConfiguration(out);
        m_colorSelectionPanel.saveComponentConfiguration(out);
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_transparencyPanel.loadComponentConfiguration(in);
        m_colorSelectionPanel.loadComponentConfiguration(in);
    }
}
