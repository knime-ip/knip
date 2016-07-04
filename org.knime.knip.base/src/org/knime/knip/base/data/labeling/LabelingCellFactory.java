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
package org.knime.knip.base.data.labeling;

import java.io.IOException;

import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.base.data.KNIPCellFactory;
import org.knime.knip.core.data.img.LabelingMetadata;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingCellFactory extends KNIPCellFactory {

    /**
     * {@inheritDoc}
     */
    public LabelingCellFactory() {
        // NB: don't call.
    }

    /**
     * @param exec
     */
    public LabelingCellFactory(final ExecutionContext exec) {
        super(exec);
    }

    /**
     * @param fsFactory
     */
    public LabelingCellFactory(final FileStoreFactory fsFactory) {
        super(fsFactory);
    }

    /**
     * @param lab
     * @param metadata
     * @return {@link LabelingCell}
     * @throws IOException
     */
    public final <L> LabelingCell<L> createCell(final RandomAccessibleInterval<LabelingType<L>> lab,
                                                final LabelingMetadata metadata) throws IOException {
        return new LabelingCell<L>(lab, metadata, getFileStore(getLabelingSize(lab)));

    }

    @SuppressWarnings("rawtypes")
    private final <L> double getLabelingSize(final RandomAccessibleInterval<LabelingType<L>> lab) {
        long bitsPerPixel = 8;
        if (lab instanceof ImgLabeling) {
            @SuppressWarnings("unchecked")
            final Type t = Util.getTypeFromInterval(((ImgLabeling)lab).getIndexImg());
            if (t instanceof RealType) {
                bitsPerPixel = ((RealType)t).getBitsPerPixel();
            }

        }
        //defensive estimation of the size of the mapping (number of lists * 4 bytes per list)
        long mapSize = Util.getTypeFromInterval(lab).getMapping().numSets() * 4;
        return (bitsPerPixel / 8) * Views.iterable(lab).size() + mapSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getDataType() {
        return LabelingCell.TYPE;
    }

}
