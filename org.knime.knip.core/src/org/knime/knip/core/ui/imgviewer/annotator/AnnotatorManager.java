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
package org.knime.knip.core.ui.imgviewer.annotator;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorFilelistChgEvent;
import org.knime.knip.core.ui.imgviewer.overlay.Overlay;

/**
 * Manages overlays and overlay elements ...
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Christian
 */
public class AnnotatorManager<T extends RealType<T>> extends AbstractAnnotatorManager<T> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /* Are serialized */
    private Map<String, Overlay<String>> m_overlayMap;

    public AnnotatorManager() {
        super();
        setOverlayMap(new HashMap<String, Overlay<String>>());
    }

    @EventListener
    public void onFileListChange(final AnnotatorFilelistChgEvent e) {
        for (final String key : new HashSet<String>(m_overlayMap.keySet())) {
            // Switching systems
            boolean contains = false;
            for (final String file : e.getFileList()) {
                // Exactly same path
                if (key.equals(file)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                m_overlayMap.remove(key);
            }
        }
    }

    /*
     * Handling storage
     */

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        out.writeInt(getOverlayMap().size());

        for (final Entry<String, Overlay<String>> entry : getOverlayMap().entrySet()) {
            out.writeUTF(entry.getKey());
            entry.getValue().writeExternal(out);
        }
        out.writeInt(m_selectedLabels.length);

        for (final String s : m_selectedLabels) {
            out.writeUTF(s);
        }

    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        getOverlayMap().clear();
        final int num = in.readInt();
        for (int i = 0; i < num; i++) {
            final String key = in.readUTF();
            final Overlay<String> o = new Overlay<String>();
            o.readExternal(in);
            o.setEventService(m_eventService);
            getOverlayMap().put(key, o);
        }

        m_selectedLabels = new String[in.readInt()];
        for (int i = 0; i < m_selectedLabels.length; i++) {
            m_selectedLabels[i] = in.readUTF();
        }
    }

    @Override
    public Map<String, Overlay<String>> getOverlayMap() {
        return m_overlayMap;
    }

    public void setOverlayMap(final Map<String, Overlay<String>> m_overlayMap) {
        this.m_overlayMap = m_overlayMap;
    }

    /**
     * {@inheritDoc}
     */
    @EventListener
    public void reset2(final AnnotatorResetEvent e) {
        m_overlayMap = new HashMap<String, Overlay<String>>();
    }
}
