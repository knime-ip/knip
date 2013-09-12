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
package org.knime.knip.core.ui.imgviewer.panels;

import java.awt.Dimension;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.imglib2.IterableInterval;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.TypedAxis;
import net.imglib2.meta.TypedSpace;
import net.imglib2.type.Type;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.IntervalWithMetadataChgEvent;

/**
 * Panel containing image properties.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImagePropertiesPanel<T extends Type<T>, I extends IterableInterval<T>> extends ViewerComponent {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final JTable m_propertiesTable;

    public ImagePropertiesPanel() {
        super("Image Properties", false);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(100, getPreferredSize().height));

        m_propertiesTable = new JTable();
        m_propertiesTable.setLayout(new BoxLayout(m_propertiesTable, BoxLayout.X_AXIS));
        add(new JScrollPane(m_propertiesTable));
    }

    /**
     * @param axes
     * @param name
     */
    @EventListener
    public void onImgUpdated(final IntervalWithMetadataChgEvent<T> e) {
        final String[][] properties = new String[2 + e.getRandomAccessibleInterval().numDimensions()][2];
        properties[0][0] = "Type";
        properties[0][1] = e.getIterableInterval().firstElement().createVariable().getClass().getCanonicalName();
        properties[1][0] = "Image type";
        if (e.getRandomAccessibleInterval() instanceof ImgPlus) {
            properties[1][1] = ((ImgPlus<T>)e.getRandomAccessibleInterval()).getImg().getClass().getCanonicalName();
        } else {
            properties[1][1] = e.getRandomAccessibleInterval().getClass().getCanonicalName();
        }

        if (e.getRandomAccessibleInterval() instanceof TypedSpace) {
            for (int i = 0; i < e.getRandomAccessibleInterval().numDimensions(); i++) {
                properties[2 + i][0] =
                        "Size "
                                + ((TypedSpace<? extends TypedAxis>)e.getRandomAccessibleInterval()).axis(i).type()
                                        .getLabel();
                properties[2 + i][1] = "" + e.getRandomAccessibleInterval().dimension(i);
            }
        } else {
            for (int i = 0; i < e.getRandomAccessibleInterval().numDimensions(); i++) {
                properties[2 + i][0] = "Size " + e.getTypedSpace().axis(i).type().getLabel();
                properties[2 + i][1] = "" + e.getRandomAccessibleInterval().dimension(i);
            }
        }
        m_propertiesTable.setModel(new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getColumnCount() {
                return properties[0].length;
            }

            @Override
            public int getRowCount() {
                return properties.length;
            }

            @Override
            public Object getValueAt(final int rowIndex, final int columnIndex) {
                return properties[rowIndex][columnIndex];
            }

            @Override
            public String getColumnName(final int index) {
                if (index == 0) {
                    return "prop";
                } else {
                    return "value";
                }
            }
        });
    }

    @Override
    public Position getPosition() {
        return Position.SOUTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEventService(final EventService eventService) {
        eventService.subscribe(this);

    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // Nothing to do here
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException {
        // Nothing to do here
    }

}
