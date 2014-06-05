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
 * Created on Jun 5, 2014 by Christian Dietz
 */
package org.knime.knip.base.activators;

import java.util.ArrayList;

import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * BundleActivator to active bundles of windows, macosx and linux which require additional native (system-dependend)
 * libraries
 *
 * @author Christian Dietz (University of Konstanz)
 * @author Jonathan Hale (University of Konstanz)
 */
public abstract class NativeLibBundleActivator implements BundleActivator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NativeLibBundleActivator.class);

    private boolean isLoaded = false;

    private String projectName;

    private final ArrayList<SystemLibraryConfig> configs;

    /**
     * @param _projectName name of the project
     */
    public NativeLibBundleActivator(final String _projectName) {
        this.projectName = _projectName;
        this.configs = new ArrayList<SystemLibraryConfig>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        init();
        LOGGER.debug("Trying to load native libraries for " + projectName);

        for (final SystemLibraryConfig config : configs) {
            if (config.matchesOSName(System.getProperty("os.name"))) {
                LOGGER.debug("System Path: " + System.getProperty("java.library.path"));
                try {
                    loadLibs(config);
                    LOGGER.debug(projectName + " libraries successfully loaded");
                    isLoaded = true;
                } catch (final UnsatisfiedLinkError error) {
                    LOGGER.error(error.getMessage());
                    LOGGER.error("Could not load " + projectName);
                }
                break;
            }
        }
    }

    /**
     * Can be overriden. Will be called before anything is called in startup().
     */
    protected void init() {
        // override
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        // Nothing to do here
    }

    /**
     * @return true, if all libs were successfully loaded
     */
    public final boolean isLoaded() {
        return isLoaded;
    }

    /**
     * This method trys to load all libs that are passed to it.<br>
     *
     * To ensure the libs are actually all loaded, the programmer has to make sure that the libs are in correct order.
     *
     * @param libs the system specific libs to load
     *
     * @throws UnsatisfiedLinkError if one or more libs could not be loaded at all
     */
    private void loadLibs(final SystemLibraryConfig config) {
        for (final String s : config.libs()) {
            load(s);
        }
    }

    /**
     * Simply implement "return System.loadLibrary(lib)"! We can't do this is here in the super class, as we then get in
     * trouble with preloaded library paths, as we are working with the wrong classloader.
     *
     * @param lib to be loaded.
     */
    protected abstract void load(String lib);

    /**
     * Adds a new configuration
     *
     * @param _config the {@link SystemLibraryConfig} to be added
     */
    public void addConfig(final SystemLibraryConfig _config) {
        configs.add(_config);
    }

}
