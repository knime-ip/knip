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
package org.knime.knip.io.node.dialog;

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003, 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   12 Nov 2009 (hornm): created
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.BoxLayout;
import javax.swing.filechooser.FileFilter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.knip.io.nodes.imgreader.FileChooserPanel;

/**
 * Dialog components to select multiple image files.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class DialogComponentMultiFileChooser extends DialogComponent {

    private static NodeLogger LOGGER = NodeLogger
            .getLogger(DialogComponentMultiFileChooser.class);

    public final static String KNIME_WORKFLOW_RELPATH =
            "knime://knime.workflow";

    private String m_workflowCanonicalPath;

    /*
     * The file chooser
     */
    private final FileChooserPanel m_fileChooser;

    /*
     * Settings to save the selected file list.
     */
    private final SettingsModelStringArray m_stringArrayModel;

    // private StringHistory m_dirHistory;

    /**
     * TODO: Use History
     * 
     * Creates new a DialogComponent.
     * 
     * @param stringArrayModel
     */
    public DialogComponentMultiFileChooser(
            final SettingsModelStringArray stringArrayModel,
            final FileFilter fileFilter, final String historyID) {

        super(stringArrayModel);

        m_workflowCanonicalPath = null;
        try {
            m_workflowCanonicalPath =
                    ResolverUtil.resolveURItoLocalFile(
                            new URI(KNIME_WORKFLOW_RELPATH)).getCanonicalPath();
        } catch (URISyntaxException e) {
            LOGGER.warn("could not resolve the workflow directory as local file");
        } catch (IOException e) {
            LOGGER.warn("could not resolve the workflow directory as local file");
        }

        // m_dirHistory = StringHistory.getInstance(historyID);
        m_fileChooser = new FileChooserPanel(fileFilter);
        // m_fileChooser.setDirectoryHistory(m_dirHistory.getHistory());
        // m_fileChooser.setCurrentDirectory(System
        // .getProperty("user.home"));
        m_stringArrayModel = stringArrayModel;

        /*
         * // when the user input changes we need to update the model.
         * m_fileChooser.addChangeListener(new ChangeListener() { public void
         * stateChanged(final ChangeEvent e) { updateModel(); } });
         */
        updateModel();

        getComponentPanel().add(m_fileChooser);
        // m_fileChooser.setPreferredSize(new Dimension(1500, 600));

        // updateComponent();

        getComponentPanel().setLayout(
                new BoxLayout(getComponentPanel(), BoxLayout.X_AXIS));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Nothing to do here
    }

    public FileChooserPanel getFileChooserPanel() {
        return m_fileChooser;
    }

    /**
     * @return the list of the currently selected files.
     */
    public String[] getSelectFiles() {
        return m_fileChooser.getSelectedFiles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        String[] paths = m_stringArrayModel.getStringArrayValue();
        for (int i = 0; i < paths.length; i++) {
            paths[i] = convertToFilePath(paths[i], m_workflowCanonicalPath);
        }
        m_fileChooser.update(paths);
    }

    private void updateModel() {
        String[] paths = m_fileChooser.getSelectedFiles();
        for (int i = 0; i < paths.length; i++) {
            paths[i] = convertToKNIMEPath(paths[i], m_workflowCanonicalPath);
        }
        m_stringArrayModel.setStringArrayValue(paths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // m_dirHistory.add(m_fileChooser.getCurrentDirectory());
        updateModel();
    }

    /**
     * expands relative knime://knime.workflow URI parts of pathes to normal
     * local paths to allow handling as normal files.
     * 
     * @param paths
     */
    public static String convertToFilePath(String path,
            String canonicalWorkflowPath) {
        if (path.startsWith(KNIME_WORKFLOW_RELPATH)) {
            path = path.replace(KNIME_WORKFLOW_RELPATH, canonicalWorkflowPath);
        }
        return path;
    }

    /**
     * changes the string path identifiers to use the knime://knime.workflow URI
     * syntax if possible. This is used before storing into the model and allows
     * to move such workflows with relative references to data.
     * 
     * @param path
     */
    public static String convertToKNIMEPath(String path,
            String canonicalWorkflowPath) {
        String canonicalPath = null;
        try {
            canonicalPath = new File(path).getCanonicalPath();
        } catch (IOException e) {
            LOGGER.warn("Could not convert path to canonical path. This may affect workflow relative file paths.");
        }

        if (canonicalPath != null
                && canonicalPath.startsWith(canonicalWorkflowPath)) {
            path =
                    canonicalPath.replace(canonicalWorkflowPath,
                            KNIME_WORKFLOW_RELPATH);
            path = path.replaceAll("\\\\", "/");
        }
        return path;
    }

}
