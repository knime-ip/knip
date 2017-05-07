/*
 * ------------------------------------------------------------------------
 *
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
 */
package org.knime.knip.core;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 *
 * @author Christian Dietz, University of Konstanz
 */
@Plugin(type = Service.class, priority = Priority.HIGH_PRIORITY)
public class KNIPLogService extends AbstractService implements LogService {

    private static boolean forwarding = false;

    private final NodeLogger LOGGER = NodeLogger.getLogger(KNIPLogService.class.getSimpleName());

    /**
     * @param forwarding If debug and info messages should be forwarded to the KNIME logs.
     */
    public void setForwarding(final boolean forwarding) {
        KNIPLogService.forwarding = forwarding;
    }

    /**
     * @return If debug and info messages are forwarded to the KNIME logs.
     */
    public boolean getForwarding() {
        return forwarding;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final Object arg0) {
        if (forwarding) {
            LOGGER.debug(arg0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final Throwable arg0) {
        if (forwarding) {
            LOGGER.debug(arg0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final Object arg0, final Throwable arg1) {
        if (forwarding) {
            LOGGER.debug(arg0, arg1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final Object arg0) {
        LOGGER.error(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final Throwable arg0) {
        LOGGER.error(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final Object arg0, final Throwable arg1) {
        LOGGER.error(arg0, arg1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLevel() {
        return LOGGER.getLevel().ordinal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final Object arg0) {
        if (forwarding) {
            LOGGER.info(arg0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final Throwable arg0) {
        if (forwarding) {
            LOGGER.info(arg0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final Object arg0, final Throwable arg1) {
        if (forwarding) {
            LOGGER.info(arg0, arg1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDebug() {
        return LOGGER.isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isError() {
        return LOGGER.isEnabledFor(LEVEL.ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInfo() {
        return LOGGER.isInfoEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTrace() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWarn() {
        return LOGGER.isEnabledFor(LEVEL.WARN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLevel(final int arg0) {
        NodeLogger.setAppenderLevelRange(NodeLogger.KNIME_CONSOLE_APPENDER, NodeLogger.LEVEL.values()[arg0],
                                         NodeLogger.LEVEL.values()[arg0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final Object arg0) {
        if (forwarding) {
            LOGGER.debug(arg0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final Throwable arg0) {
        if (forwarding) {
            LOGGER.debug(arg0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final Object arg0, final Throwable arg1) {
        if (forwarding) {
            LOGGER.debug(arg0, arg1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final Object arg0) {
        LOGGER.warn(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final Throwable arg0) {
        LOGGER.warn(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final Object arg0, final Throwable arg1) {
        LOGGER.warn(arg0, arg1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLevel(final String appender, final int level) {
        //TODO this behavior is not correct. we would have to dig deeper into the KNIME code to understand to fully support the specification of setLevel
        NodeLogger.setAppenderLevelRange(appender, NodeLogger.LEVEL.values()[level], NodeLogger.LEVEL.values()[level]);
    }

}
