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
package org.knime.knip.base.nodes.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;

/**
 * 
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class TableCellViewsManager {

    /**
     * The attribute of the table cell view extension point pointing to the factory class
     */
    public static final String EXT_POINT_ATTR_DF = "TableCellViewFactory";

    /** The id of the TableCellView extension point. */
    public static final String EXT_POINT_ID = "org.knime.knip.base.TableCellView";

    private static TableCellViewsManager instance;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableCellViewsManager.class);

    /**
     * @return the only instance of this class
     */
    public static TableCellViewsManager getInstance() {
        if (instance == null) {
            instance = new TableCellViewsManager();
        }
        return instance;
    }

    private final Map<Class<? extends DataValue>, List<String>> m_viewDescriptions =
            new HashMap<Class<? extends DataValue>, List<String>>();

    private final Map<Class<? extends DataValue>, List<TableCellViewFactory>> m_viewFactories =
            new HashMap<Class<? extends DataValue>, List<TableCellViewFactory>>();

    /**
     * Singleton, use getInstance()
     */
    private TableCellViewsManager() {
        addTableCellViewFactory(new ImgCellViewFactory());
        addTableCellViewFactory(new LabelingCellViewFactory());
        registerExtensionPoints();
    }

    /**
     * @param fac
     */
    public void addTableCellViewFactory(final TableCellViewFactory fac) {
        final Class<? extends DataValue> value = fac.getDataValueClass();
        List<TableCellViewFactory> facs = m_viewFactories.get(value);
        List<String> descs = m_viewDescriptions.get(value);
        if (facs == null) {
            facs = new ArrayList<TableCellViewFactory>();
            m_viewFactories.put(value, facs);
            descs = new ArrayList<String>();
            m_viewDescriptions.put(value, descs);
        }
        facs.add(fac);
        final TableCellView[] views = fac.createTableCellViews();
        for (final TableCellView tcv : views) {
            descs.add(tcv.getName() + ": " + tcv.getDescription());
        }
    }

    /**
     * @param <V>
     * @param dataValue
     * @return
     */
    public List<TableCellView> createTableCellViews(final List<Class<? extends DataValue>> valueClasses) {
        final List<TableCellView> views = new ArrayList<TableCellView>();
        for (final Class<? extends DataValue> valueClass : valueClasses) {
            final List<TableCellViewFactory> facs = m_viewFactories.get(valueClass);
            if (facs == null) {
                continue;
            }

            for (final TableCellViewFactory fac : facs) {
                for (final TableCellView view : fac.createTableCellViews()) {
                    views.add(view);
                }
            }
        }
        return views;

    }

    /**
     * @return descriptions of the table cell views (format: "[name]: [description]");
     */
    public Map<Class<? extends DataValue>, List<String>> getTableCellViewDescriptions() {
        return m_viewDescriptions;
    }

    /**
     * Registers all extension point implementations.
     */
    private void registerExtensionPoints() {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: " + " --> Invalid extension point: " + EXT_POINT_ID);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
                final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

                if ((operator == null) || operator.isEmpty()) {
                    LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                            + EXT_POINT_ATTR_DF + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }

                try {
                    final TableCellViewFactory factory =
                            (TableCellViewFactory)elem.createExecutableExtension(EXT_POINT_ATTR_DF);
                    addTableCellViewFactory(factory);
                } catch (final Exception t) {
                    LOGGER.error("Problems during initialization of " + "Table Cell View (with id '" + operator + "'.)");
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", t);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering " + "TableCellView extensions");
        }
    }

}
