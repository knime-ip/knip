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
 */

package org.knime.knip.core;

import org.knime.knip.scijava.core.ResourceAwareClassLoader;
import org.scijava.Context;
import org.scijava.cache.CacheService;
import org.scijava.log.LogService;
import org.scijava.module.ModuleService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.thread.ThreadService;

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
 * --------------------------------------------------------------------- *
 *
 */

import net.imagej.ops.OpService;

/**
 * Encapsulates the SciJava instance as singleton.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 */
public class KNIPGateway {

    private static KNIPGateway m_instance;

    private static OpService m_os;

    private static ThreadService m_ts;

    private static LabelingService m_ls;

    private static CacheService m_cs;

    private static ModuleService m_ms;

    private static LogService m_log;

    private final Context m_context;

    private KNIPGateway() {
        // set log level
        System.setProperty("scijava.log.level", "error");

        m_context = new Context(new PluginIndex(
                new DefaultPluginFinder(new ResourceAwareClassLoader(getClass().getClassLoader(), getClass()))));
    }

    /**
     * @return singleton instance of {@link KNIPGateway}
     */
    public static synchronized KNIPGateway getInstance() {
        if (m_instance == null) {
            m_instance = new KNIPGateway();
        }
        return m_instance;
    }

    /**
     * @return singleton instance of {@link OpService}
     */
    public static OpService ops() {
        if (m_os == null) {
            m_os = getInstance().m_context.getService(OpService.class);
        }
        return m_os;
    }

    /**
     * @return singleton instance of {@link ThreadService}
     */
    public static ThreadService threads() {
        if (m_ts == null) {
            m_ts = getInstance().m_context.getService(ThreadService.class);
        }
        return m_ts;
    }

    /**
     * @return singleton instance of {@link LabelingService}
     */
    public static LabelingService regions() {
        if (m_ls == null) {
            m_ls = getInstance().m_context.getService(LabelingService.class);
        }
        return m_ls;
    }

    /**
     * @return singleton instance of {@link CacheService}
     */
    public static CacheService cache() {
        if (m_cs == null) {
            m_cs = getInstance().m_context.getService(CacheService.class);
        }
        return m_cs;
    }

    /**
     * @return singleton instance of {@link ModuleService}
     */
    public static ModuleService ms() {
        if (m_ms == null) {
            m_ms = getInstance().m_context.getService(ModuleService.class);
        }
        return m_ms;
    }

    /**
     * @return singleton instance of {@link LogService}
     */
    public static LogService log() {
        if (m_log == null) {
            m_log = getInstance().m_context.getService(LogService.class);
        }
        return m_log;
    }
}
