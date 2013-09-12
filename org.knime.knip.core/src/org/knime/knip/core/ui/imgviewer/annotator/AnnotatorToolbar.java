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
package org.knime.knip.core.ui.imgviewer.annotator;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorToolChgEvent;
import org.knime.knip.core.ui.imgviewer.annotator.tools.AnnotatorFreeFormTool;
import org.knime.knip.core.ui.imgviewer.annotator.tools.AnnotatorFreeLineTool;
import org.knime.knip.core.ui.imgviewer.annotator.tools.AnnotatorNoTool;
import org.knime.knip.core.ui.imgviewer.annotator.tools.AnnotatorPointTool;
import org.knime.knip.core.ui.imgviewer.annotator.tools.AnnotatorPolygonTool;
import org.knime.knip.core.ui.imgviewer.annotator.tools.AnnotatorRectangleTool;
import org.knime.knip.core.ui.imgviewer.annotator.tools.AnnotatorSelectionTool;
import org.knime.knip.core.ui.imgviewer.annotator.tools.AnnotatorSplineTool;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class AnnotatorToolbar extends ViewerComponent {

    private static final int BUTTON_WIDHT = 150;

    private static final int BUTTON_HEIGHT = 25;

    private static final long serialVersionUID = 1L;

    private EventService m_eventService;

    public AnnotatorToolbar(final AnnotatorTool<?>... tools) {
        super("Toolbox", false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        final ButtonGroup group = new ButtonGroup();

        for (final AnnotatorTool<?> tool : tools) {
            final JToggleButton jtb = new JToggleButton(tool.getName());
            jtb.setMinimumSize(new Dimension(140, 30));
            jtb.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        m_eventService.publish(new AnnotatorToolChgEvent(tool));
                    }

                }
            });

            setButtonIcon(jtb, "icons/" + tool.getIconPath());
            jtb.setActionCommand(tool.toString());
            group.add(jtb);
            jtb.setMaximumSize(new Dimension(BUTTON_WIDHT, BUTTON_HEIGHT));
            jtb.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(jtb);
        }
    }

    private final void setButtonIcon(final AbstractButton jb, final String path) {
        final URL icon =
                getClass().getClassLoader().getResource(getClass().getPackage().getName().replace('.', '/') + "/"
                                                                + path);
        jb.setHorizontalAlignment(SwingConstants.LEFT);
        if (icon != null) {
            jb.setIcon(new ImageIcon(icon));
        }
    }

    @Override
    public void setEventService(final EventService eventService) {
        m_eventService = eventService;
        eventService.subscribe(this);
    }

    @Override
    public Position getPosition() {
        return Position.EAST;
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // Nothing to save here
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException {
        // Nothing to load here
    }

    public static ViewerComponent createStandardToolbar() {
        return new AnnotatorToolbar(new AnnotatorNoTool("Pan"), new AnnotatorSelectionTool(), new AnnotatorPointTool(),
                new AnnotatorRectangleTool(), new AnnotatorPolygonTool(), new AnnotatorSplineTool(),
                new AnnotatorFreeFormTool(), new AnnotatorFreeLineTool());
    }
}
