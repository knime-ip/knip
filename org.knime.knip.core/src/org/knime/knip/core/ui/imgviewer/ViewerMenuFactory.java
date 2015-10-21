/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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
 * Created on Sep 18, 2015 by pop210958
 */
package org.knime.knip.core.ui.imgviewer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.panels.LabelOptionPanel;
import org.knime.knip.core.ui.imgviewer.panels.TableOverviewPanel;

/**
 *
 * @author Andreas Burger, University of Konstanz
 */
public class ViewerMenuFactory {

    public static JComponent getCombinedImgViewerImgMenu(final EventService e) {
        JPanel menu = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 5, 0);

        ViewerComponent comp =
                new ExpandingPanel("Plane Selection", ViewerComponents.PLANE_SELECTION.createInstance(), true);
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp = new ExpandingPanel("Image Enhancement", ViewerComponents.IMAGE_ENHANCE.createInstance());
        comp.setEventService(e);
        menu.add(comp, gbc);

        comp = new ExpandingPanel("Renderer Selection", ViewerComponents.RENDERER_SELECTION.createInstance());
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp = new ExpandingPanel("Image Properties", ViewerComponents.IMAGE_PROPERTIES.createInstance());
        comp.setEventService(e);
        menu.add(comp, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        menu.add(Box.createGlue(), gbc);

        return menu;
    }

    public static JComponent getImgViewerImgMenu(final EventService e) {
        JPanel menu = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 5, 0);

        ViewerComponent comp =
                new ExpandingPanel("Plane Selection",ViewerComponents.MINIMAP_PLANE_SELECTION.createInstance(), true);
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp =
                new ExpandingPanel("Image Enhancement",
                                   ViewerComponents.IMAGE_ENHANCE.createInstance(), true);
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp =
                new ExpandingPanel("Renderer Selection",
                                   ViewerComponents.RENDERER_SELECTION.createInstance());
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp =
                new ExpandingPanel("Image Properties",
                                   ViewerComponents.IMAGE_PROPERTIES.createInstance());
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp =
                new ExpandingPanel("Navigation", new TableOverviewPanel(), true);
        menu.add(comp, gbc);
        comp.setEventService(e);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        menu.add(Box.createGlue(), gbc);

        return menu;
    }

    public static JComponent getCombinedImgViewerLabelingMenu(final EventService e) {

        JPanel menu = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 5, 0);

        ViewerComponent comp =
                new ExpandingPanel("Plane Selection", ViewerComponents.PLANE_SELECTION.createInstance(), true);
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp =
                new ExpandingPanel("Labels/Filter", ViewerComponents.LABEL_FILTER.createInstance(),
                                   true);
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp =
                new ExpandingPanel("Label Options", new LabelOptionPanel());
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp =
                new ExpandingPanel("Renderer Selection",
                                   ViewerComponents.RENDERER_SELECTION.createInstance());
        menu.add(comp, gbc);
        comp.setEventService(e);

        comp =
                new ExpandingPanel("Image Properties",
                                   ViewerComponents.IMAGE_PROPERTIES.createInstance());
        menu.add(comp, gbc);
        comp.setEventService(e);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        menu.add(Box.createGlue(), gbc);
        return menu;
    }

}
