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
package org.knime.knip.core.io.externalization.externalizers;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCell;
import net.imglib2.img.cell.AbstractCellImg.CellContainerSampler;
import net.imglib2.img.cell.CellCursor;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;

import org.knime.knip.core.io.externalization.BufferedDataInputStream;
import org.knime.knip.core.io.externalization.BufferedDataOutputStream;
import org.knime.knip.core.io.externalization.Externalizer;
import org.knime.knip.core.io.externalization.ExternalizerManager;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class CellImgExt0 implements Externalizer<CellImg> {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellImg> getType() {
        return CellImg.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CellImg read(final BufferedDataInputStream in) throws Exception {

        final long[] dims = new long[in.readInt()];
        in.read(dims);

        final NativeType<?> type = (NativeType<?>)ExternalizerManager.<Class> read(in).newInstance();

        final CellImg<? extends NativeType<?>, ? extends ArrayDataAccess<?>, ? extends AbstractCell<?>> cellImg =
                new CellImgFactory().create(dims, type);

        final DirectCellCursor<? extends NativeType<?>, ? extends ArrayDataAccess<?>, ? extends AbstractCell<?>> cursor =
                new DirectCellCursor(cellImg.cursor());

        boolean indicateStop = cursor.isLastCell();
        while (true) {

            in.readLArray(((ArrayDataAccess<? extends ArrayDataAccess<?>>)((CellContainerSampler<? extends NativeType<?>, ? extends ArrayDataAccess<?>, ? extends AbstractCell<?>>)cursor)
                    .getCell().getData()).getCurrentStorageArray());

            if (indicateStop) {
                break;
            }

            cursor.moveToNextCell();

            if (cursor.isLastCell()) {
                indicateStop = true;
            }
        }

        return cellImg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final BufferedDataOutputStream out, final CellImg obj) throws Exception {

        // write dimensions
        out.writeInt(obj.numDimensions());
        for (int i = 0; i < obj.numDimensions(); i++) {
            out.writeLong(obj.dimension(i));
        }

        ExternalizerManager.<Class> write(out, obj.firstElement().getClass());

        final DirectCellCursor<? extends NativeType<?>, ? extends ArrayDataAccess<?>, ? extends AbstractCell<?>> cursorOnCells =
                new DirectCellCursor(
                        ((CellImg<? extends NativeType<?>, ? extends ArrayDataAccess<?>, ? extends AbstractCell<?>>)obj)
                                .cursor());

        boolean indicateStop = cursorOnCells.isLastCell();
        while (true) {
            // TODO extend for other types
            out.writeArray(((ArrayDataAccess<? extends ArrayDataAccess<?>>)((CellContainerSampler<? extends NativeType<?>, ? extends ArrayDataAccess<?>, ? extends AbstractCell<?>>)cursorOnCells)
                    .getCell().getData()).getCurrentStorageArray());

            if (indicateStop) {
                break;
            }

            cursorOnCells.moveToNextCell();

            if (cursorOnCells.isLastCell()) {
                indicateStop = true;
            }

        }

    }

    private class DirectCellCursor<T extends NativeType<T>, A extends ArrayDataAccess<A>, C extends AbstractCell<A>>
            extends CellCursor<T, A, C> {

        protected DirectCellCursor(final CellCursor<T, A, C> cursor) {
            super(cursor);
        }

        /**
         * Move cursor right before the first element of the next cell. Update type and index variables.
         */
        public synchronized void moveToNextCell() {
            cursorOnCells.fwd();
            isNotLastCell = cursorOnCells.hasNext();
            lastIndexInCell = (int)(getCell().size() - 1);
            index = -1;
            type.updateContainer(this);
        }

        public synchronized boolean isLastCell() {
            return !isNotLastCell;
        }
    }

}
