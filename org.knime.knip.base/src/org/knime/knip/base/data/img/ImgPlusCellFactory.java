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
package org.knime.knip.base.data.img;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.KNIPCellFactory;
import org.knime.knip.base.data.img2.FileStoreImgPlusCell;
import org.knime.knip.base.data.img2.StreamImgPlusCell;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip2.core.storage.FileStoreStorage;
import org.knime.knip2.core.storage.SimpleStorage;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.ops.operation.img.unary.ImgCopyOperation;
import net.imglib2.type.numeric.RealType;

/**
 *
 * Factory to create ImgPlus cells.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class ImgPlusCellFactory extends KNIPCellFactory {

    /**
     * The type of the {@link DataCell}s this factory is going to create.
     */
    public static final DataType TYPE  = FileStoreImgPlusCell.TYPE;

    /**
     * @param exec
     */
    public ImgPlusCellFactory(final ExecutionContext exec) {
        super(exec);
    }

    /**
     * @param fsFactory
     */
    public ImgPlusCellFactory(final FileStoreFactory fsFactory) {
        super(fsFactory);
    }

    /**
     * @param <T>
     * @param imgPlus
     *
     * @return {@link ImgPlusCell}
     * @throws IOException
     *
     * @Deprecated use {@link #createDataCell(ImgPlus)} instead
     */
    @Deprecated
    public final <T extends RealType<T>> ImgPlusCell<T> createCell(final ImgPlus<T> imgPlus) throws IOException {
        return new ImgPlusCell<T>(imgPlus.getImg(), imgPlus, getFileStore(getImgSize(imgPlus.getImg())));
    }

    /**
     * Creates a new {@link DataCell} of type {@link ImgPlusCellFactory#TYPE}. Where as the {@link DataType} is
     * guaranteed the type of the actual {@link DataCell}-implementation is NOT.
     *
     * @param imgPlus
     * @param copy if <code>true</code> the passed img will be copied before the cell is created. This essentially
     *            executes ('burns in') any possibly virtual operation that represents the actual image.
     * @return an arbitrary {@link DataCell}-implementation of typer {@link ImgPlusCellFactory#TYPE}.
     * @throws IOException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final <T extends RealType<T>> DataCell createDataCell(final ImgPlus<T> imgPlus, final boolean copy) throws IOException {
        //the call is delegated to the cell factory of the knip2 framework
        long[] dims = new long[imgPlus.numDimensions()];
        imgPlus.dimensions(dims);
        T type = imgPlus.firstElement().createVariable();
        ImgPlusCellMetadata cellMetadata =
                new ImgPlusCellMetadata(imgPlus, imgPlus.size(), null, dims, type.getClass(), null);

        //depending on the image size either an ordinary data cell or a file store cell is created
        Img<T> img = imgPlus.getImg();
        if(copy) {
            img = burnIn(img);
        }
        if (imgPlus.size() < KNIMEKNIPPlugin.getMinNumPixelsForFileStoreImgCell()) {
            SimpleStorage ss = new SimpleStorage();
            return new StreamImgPlusCell(KNIPGateway.aps().getAccess(ss, img, Img.class),
                    KNIPGateway.aps().getAccess(ss, cellMetadata, ImgPlusCellMetadata.class));
        } else {
            FileStoreStorage fss = new FileStoreStorage(getExecutionContext());
            return new FileStoreImgPlusCell(KNIPGateway.aps().getAccess(fss, img, Img.class),
                    KNIPGateway.aps().getAccess(fss, cellMetadata, ImgPlusCellMetadata.class));
        }
    }

    private final <T extends RealType<T>> double getImgSize(final Img<T> img) {
        // calculate the approx. size of the current image
        return (img.firstElement().getBitsPerPixel() / 8.0) * img.size();
    }

    private <T extends RealType<T>> Img<T> burnIn(final Img<T> img) {
        return (Img<T>)new ImgCopyOperation<T>()
                .compute(img, img.factory().create(img, img.firstElement().createVariable()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getDataType() {
        return ImgPlusCell.TYPE;
    }

}
