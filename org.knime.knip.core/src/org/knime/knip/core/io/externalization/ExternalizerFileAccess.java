/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2016
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
 * Created on Jan 20, 2016 by hornm
 */
package org.knime.knip.core.io.externalization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.knime.core.data.filestore.FileStore;
import org.knime.knip2.core.ext.Externalizer;
import org.knime.knip2.core.storage.FileStoreStorage;
import org.knime.knip2.core.tree.ext.AbstractFlushableExtAccess;

public class ExternalizerFileAccess<O>
        extends AbstractFlushableExtAccess<FileStoreStorage, O, BufferedDataInputStream, BufferedDataOutputStream> {

    private O m_obj;

    private String m_key;

    private boolean m_useFile;

    public ExternalizerFileAccess() {
        //deserialisation
    }

    ExternalizerFileAccess(final FileStoreStorage storage, final O obj,
                           final Externalizer<BufferedDataInputStream, BufferedDataOutputStream, O> ext,
                           final boolean useFile) {
        super(storage, useFile ? obj : null, ext);
        if (!useFile) {
            m_obj = obj;
        }
        m_useFile = useFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public O get() {
        if(m_useFile) {
            //if object is read from the file we can make use of the caching (super class)
            return super.get();
        }else {
            //otherwise the obj is not read from the file we don't need to cache and return it directly
            return m_obj;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void releaseToCache() {
        if(m_useFile) {
            super.releaseToCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readState(final ObjectInput in) throws IOException {
        m_useFile = in.readBoolean();
        if(m_useFile) {
            try {
                m_key = (String) in.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            BufferedDataInputStream inStream = new BufferedDataInputStream((InputStream)in);
            m_obj = getExt().read(inStream);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeState(final ObjectOutput out) throws IOException {
        out.writeBoolean(m_useFile);
        if(m_useFile) {
            out.writeObject(m_key);
        } else {
            BufferedDataOutputStream outStream = new BufferedDataOutputStream((OutputStream)out);
            getExt().write(outStream, m_obj);
            outStream.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected O read(final FileStoreStorage storage, final Externalizer<BufferedDataInputStream, BufferedDataOutputStream, O> ext)
            throws IOException {
        if(m_useFile) {
            FileStore fs = getStorage().getFileStore(m_key);
            BufferedDataInputStream inStream = StreamUtil.createInputStream(fs.getFile(), 0);
            return ext.read(inStream);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void write(final FileStoreStorage storage, final Externalizer<BufferedDataInputStream, BufferedDataOutputStream, O> ext)
            throws IOException {
        if (m_useFile) {
            FileStore fs = getStorage().getFileStore(null);
            m_key = fs.toString();
            BufferedDataOutputStream outStream = StreamUtil.createOutStream(fs.getFile());
            ext.write(outStream, get());
        }
    }

}
