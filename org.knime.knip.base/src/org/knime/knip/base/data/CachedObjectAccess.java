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
 * Created on Oct 9, 2015 by dietzc
 */
package org.knime.knip.base.data;

import java.util.UUID;

import org.knime.core.data.filestore.FileStore;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.ExternalizerManager;
import org.scijava.cache.CacheService;

/**
 * @author Christian Dietz, University of Konstanz
 * @param <O>
 */
public class CachedObjectAccess<O> {

    private final CacheService CACHE = KNIPGateway.cache();

    private final UUID ID = UUID.randomUUID();

    private O m_obj;

    private long m_offset;

    private FileStore m_fileStore;

    // NB: This hack is required due to backwards compatibility.
    private StreamSkipper m_skipper;

    /**
     * Create {@link CachedObjectAccess} given file an obj
     *
     * @param fileStore used to write the object
     * @param obj object to manage
     */
    public CachedObjectAccess(final FileStore fileStore, final O obj) {
        this(fileStore, obj, -1, null);
    }

    /**
     * Create {@link CachedObjectAccess}
     *
     * @param fileStore used to read/write the object
     * @param obj to manage
     * @param offset of total cell content
     * @param skipper skips bytes in streams until correct position was determined
     */
    public CachedObjectAccess(final FileStore fileStore, final O obj, final long offset, final StreamSkipper skipper) {
        m_obj = obj;
        m_fileStore = fileStore;
        m_offset = offset;
        m_skipper = skipper;
    }

    /**
     * Writes the object on disc
     */
    public synchronized void serialize() {
        try {
            assert (m_obj != null);
            //  Object can only be null if it was written
            m_offset = m_fileStore.getFile().length();
            final BufferedDataOutputStream stream = StreamUtil.createOutStream(m_fileStore.getFile());
            ExternalizerManager.write(stream, m_obj);
            stream.flush();
            stream.close();
            CACHE.put(ID, m_obj);
            m_obj = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the object. either directly or from cache or from disc
     */
    public synchronized O get() {
        try {
            //NB: this logic could be part of the cache
            if (m_obj != null) {
                return m_obj;
            } else {
                @SuppressWarnings("unchecked")
                final O object = (O)CACHE.get(ID);
                if (object != null) {
                    return object;
                } else {
                    final BufferedDataInputStream stream =
                            StreamUtil.createInputStream(m_fileStore.getFile(), m_offset);

                    // NB: This hack is required to be backwards compatible.
                    // NB: It's not slower than before. However, in the future we may want to avoid this.
                    if (m_skipper != null) {
                        m_skipper.skip(stream);
                    }

                    final O obj = ExternalizerManager.read(stream);
                    stream.close();
                    CACHE.put(ID, obj);
                    return obj;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param obj to set.
     */
    public void setObject(final O obj) {
        if (m_obj == null) {
            CACHE.put(ID, obj);
            return;
        }
        m_obj = obj;
    }

    /**
     * Hack to provide backward compatibility
     *
     * @author Christian Dietz, University of Kontanz
     */
    public static interface StreamSkipper {
        /**
         * Skip bytes in stream
         *
         * @param in stream on which the bytes will be skipped
         */
        void skip(final BufferedDataInputStream in);
    }

}
