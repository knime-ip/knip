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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

import net.imglib2.img.sparse.Ntree;
import net.imglib2.img.sparse.Ntree.NtreeNode;
import net.imglib2.img.sparse.NtreeAccess;
import net.imglib2.img.sparse.NtreeImg;
import net.imglib2.img.sparse.NtreeImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;

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
public class NtreeImgExt0 implements Externalizer<NtreeImg> {

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
    public Class<NtreeImg> getType() {
        return NtreeImg.class;
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
    public NtreeImg read(final BufferedDataInputStream in) throws Exception {

        final long[] dims = new long[in.readInt()];
        in.read(dims);

        final NativeType<?> type = (NativeType<?>)ExternalizerManager.<Class> read(in).newInstance();

        final int numChildren = in.readInt();

        @SuppressWarnings({"rawtypes"})
        final NtreeImg<? extends Type<?>, ? extends NtreeAccess<? extends Comparable<?>, ?>> img =
                new NtreeImgFactory().create(dims, type);
        final Ntree<? extends Comparable<?>> tree = img.update(new NtreeImg.PositionProvider() {

            @Override
            public long[] getPosition() {
                return new long[img.numDimensions()];
            }

        }).getCurrentStorageNtree();

        final NtreeNode<? extends Comparable<?>> root = tree.getRootNode();

        readNtreeNode(new ObjectInputStream(in), (NtreeNode<Object>)root, numChildren);

        return img;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final BufferedDataOutputStream out, final NtreeImg obj) throws Exception {
        // write dimensions
        out.writeInt(obj.numDimensions());
        for (int i = 0; i < obj.numDimensions(); i++) {
            out.writeLong(obj.dimension(i));
        }

        ExternalizerManager.<Class> write(out, obj.firstElement().getClass());

        final int n = obj.numDimensions();
        final int numChildren = 1 << n;

        out.writeInt(numChildren);

        final Ntree<?> tree = obj.update(new NtreeImg.PositionProvider() {

            @Override
            public long[] getPosition() {
                return new long[obj.numDimensions()];
            }

        }).getCurrentStorageNtree();

        writeNtreeNode(new ObjectOutputStream(out), tree.getRootNode());

    }

    @SuppressWarnings("unchecked")
    private void readNtreeNode(final ObjectInputStream in, NtreeNode<Object> current, final int numChildren)
            throws IOException {
        try {
            current.setValue(in.readObject());
            if (!in.readBoolean()) {
                return;
            }

            final LinkedList<NtreeNode<Object>> queue = new LinkedList<NtreeNode<Object>>();
            queue.add(current);

            while (!queue.isEmpty()) {
                current = queue.getFirst();
                final NtreeNode<Object>[] children = new NtreeNode[numChildren];
                for (int i = 0; i < numChildren; i++) {
                    children[i] = new NtreeNode<Object>(current, in.readObject());
                    ;
                    if (((Boolean)in.readObject()).booleanValue()) {
                        queue.add(children[i]);
                    }
                }
                queue.removeFirst().setChildren(children);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeNtreeNode(final ObjectOutputStream out, NtreeNode<? extends Comparable<?>> current)
            throws IOException {

        out.writeObject(current.getValue());
        out.writeBoolean(current.getChildren() != null);

        final LinkedList<NtreeNode<? extends Comparable<?>>> queue =
                new LinkedList<NtreeNode<? extends Comparable<?>>>();
        queue.add(current);

        if (current.getChildren() == null) {
            out.flush();
            return;
        }

        while (!queue.isEmpty()) {

            current = queue.removeFirst();
            for (final NtreeNode<? extends Comparable<?>> child : current.getChildren()) {
                out.writeObject(child.getValue());
                out.writeObject(child.getChildren() != null);

                if (child.getChildren() != null) {
                    queue.add(child);
                }

            }

        }
    }

}
