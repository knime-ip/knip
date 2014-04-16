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

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.knime.knip.core.util.waitingindicator.libs.SpinningDialWaitIndicator;
import org.knime.knip.core.util.waitingindicator.libs.WaitIndicator;

/**
 * Helper Class that provides a waiting indicator and an error overlay.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public final class WaitingIndicatorUtils {

    /**
     * Hiding constructor
     */
    private WaitingIndicatorUtils() {
    }

    /**
     * Display a waiting indicator on top of the selected JPanel.
     *
     * @param jc The JPanel that the waiting indicator will be placed upon
     * @param display if <code>true</code> the waiting indicator will be displayed, if <code>false</code> any waiting
     *            indicator on the Jpanel is discarded.
     */
    public static final void setWaiting(final JComponent jc, final boolean display) {
        SpinningDialWaitIndicator w = (SpinningDialWaitIndicator)jc.getClientProperty("waiter");
        if (w == null) {
            if (display) {
                w = new SpinningDialWaitIndicator(jc, "loading");
            }
        } else if (!display) {
            w.dispose();
            w = null;
        }
        jc.putClientProperty("waiter", w);
    }

    /**
     * Displays an error on top of the specified JPanel, in the style of {@link BasicErrorIndicator}.
     *
     * @param jc The JPanel that the waiting indicator will be placed upon
     * @param display if <code>true</code> the waiting indicator will be displayed, if <code>false</code> any waiting
     *            indicator on the JPanel is discarded.
     * @param message the error message to display.
     */
    public static final void showError(final JComponent jc, final String[] message, final boolean display) {

        BasicErrorIndicator w = (BasicErrorIndicator)jc.getClientProperty("error");
        if (w == null) {
            if (display) {
                w = new BasicErrorIndicator(jc, message);
            }
        } else if (!display) {
            w.dispose();
            w = null;
        }
        jc.putClientProperty("error", w);
    }

    /**
     * Shows an error overlay on the selected JPanel, using the style of the style class. <br>
     * See {@link BasicErrorIndicator} for an example.
     *
     * @param jc The JPanel that the waiting indicator will be placed upon
     * @param display if <code>true</code> the waiting indicator will be displayed, if <code>false</code> any waiting
     *            indicator on the Jpanel is discarded.
     * @param message the error message to display.
     * @param style Sublclass of {@link WaitIndicator}, must implement a constructor that has exactly the following
     *            signature: <code>Waitindicator(final JComponent target, final String[] message)</code>
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static final void showError(final JComponent jc, final String[] message, final Class<WaitIndicator> style,
                                       final boolean display) throws NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        WaitIndicator w = (WaitIndicator)jc.getClientProperty("error");
        if (w == null) {
            if (display) {
                w = style.getConstructor(new Class[]{JPanel.class, String.class}).newInstance(jc, message);
            }
        } else if (!display) {
            w.dispose();
            w = null;
        }
        jc.putClientProperty("error", w);
    }

}