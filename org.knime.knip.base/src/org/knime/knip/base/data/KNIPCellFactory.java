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
package org.knime.knip.base.data;

import java.io.IOException;
import java.util.UUID;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCellFactory;

/**
 * 
 * 
 * TODO: I needed to remove the ability to write all cells into one file! Reasons: KNIME can't handle this! Sorting
 * won't work for 2.8 release as well as writing to tables would produces gigantic large files in the worst case. We
 * need to review this for 1.1 (dietzc)
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class KNIPCellFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgPlusCellFactory.class);

    /* the estimated current file size in bytes */
    private long m_currentFileSize = 0;

    private FileStore m_currentFileStore = null;

    private final ExecutionContext m_exec;

    private final FileStoreFactory m_fsFactory;

    public KNIPCellFactory(final ExecutionContext exec) {
        m_exec = exec;
        m_fsFactory = null;
    }

    public KNIPCellFactory(final FileStoreFactory fsFactory) {
        m_fsFactory = fsFactory;
        m_exec = null;

    }

    /**
     * Helper to create the file store. Whether a new file store is created (i.e. a new file) depends on the size of the
     * images already written into the last file.
     * 
     * @param objSize the approx. number of bytes of the next cell to be written to the file
     */
    protected FileStore getFileStore(final long objSize) throws IOException {

        // Each cell needs to be associated with  one filestore as KNIME can't handle n cells to one filestore since now. Needs to be discussed for 1.1.0 (dietzc)

        if ((m_currentFileStore == null) || ((m_currentFileSize + objSize) > KNIMEKNIPPlugin.getMaxFileSizeInByte())) {

            LOGGER.debug("New file created. Last file has approx. " + (m_currentFileSize / (1024.0 * 1024.0)) + "MB.");

            String zipSuffix = "";
            if (KNIMEKNIPPlugin.compressFiles()) {
                zipSuffix = KNIPConstants.ZIP_SUFFIX;
            }

            if (m_exec != null) {
                m_currentFileStore = m_exec.createFileStore(UUID.randomUUID().toString() + "");
            } else {
                m_currentFileStore = m_fsFactory.createFileStore(UUID.randomUUID().toString() + "");
            }

            m_currentFileSize = 0;
        }
        m_currentFileSize += objSize;

        return m_currentFileStore;

    }

}
