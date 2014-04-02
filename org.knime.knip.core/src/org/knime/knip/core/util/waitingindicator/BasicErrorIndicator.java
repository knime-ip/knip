/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * Created on 02.04.2014 by Gabriel
 */
package org.knime.knip.core.util.waitingindicator;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import org.knime.knip.core.util.waitingindicator.libs.WaitIndicator;

/**
 * Basic error indicator style. Prints a big (50pt) "ERROR" in white and adds the message text in (14pts) bellow it. All
 * on a grey background.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class BasicErrorIndicator extends WaitIndicator {

    private String[] m_errorText = {"Error"};

    /**
     * Basic error indicator style. Prints a big (50pt) "ERROR" in white and adds the message text in (14pts) bellow it.
     * All on a grey background.
     *
     * @param target The {@link JComponent} that the ErrorIndicator will be applied to.
     * @param message The error message to display.
     */
    public BasicErrorIndicator(final JComponent target, final String[] message) {
        super(target);
        getPainter().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        this.m_errorText = message;
    }

    @Override
    public void paint(Graphics g) {
        Rectangle r = getDecorationBounds();
        g = g.create();
        g.setColor(new Color(211, 211, 211, 255));
        g.fillRect(r.x, r.y, r.width, r.height);
        if (m_errorText == null) {
            m_errorText = new String[]{"unknown Error!"};
        }

        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Title Warning
        g.setColor(new Color(0, 0, 0));
        g.setFont(new Font("Helvetica", Font.BOLD, 50));
        g.drawString("ERROR", 10, 130);

        // Error message
        g.setFont(new Font("TimesRoman", Font.BOLD, 14));
        int newline = g.getFontMetrics().getHeight() + 5;
        int y = 200;
        for (int i = 0; i < m_errorText.length; i++) {
            y += newline;
            g.drawString(m_errorText[i], 10, y);
        }
        g.dispose();
    }
}
