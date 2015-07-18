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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;

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
@SuppressWarnings("rawtypes")
public class ImgLabelingExt0 implements Externalizer<ImgLabeling> {

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
    public Class<ImgLabeling> getType() {
        return ImgLabeling.class;
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
    @SuppressWarnings("unchecked")
    @Override
    public ImgLabeling read(final BufferedDataInputStream in) throws Exception {
        // Create Labeling
        final ImgLabeling res = new ImgLabeling<>(ExternalizerManager.<Img> read(in));

        readMapping(in, res.getMapping());

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final BufferedDataOutputStream out, final ImgLabeling obj) throws Exception {

        // write the backed image
        ExternalizerManager.write(out, obj.getIndexImg());

        writeMapping(out, obj.getMapping());
    }

    /**
     * @param out
     * @param obj
     * @throws IOException
     * @throws Exception
     */
    public static void writeMapping(final BufferedDataOutputStream out, final LabelingMapping obj) throws IOException,
            Exception {
        final LabelingMappingAccess access = new LabelingMappingAccess(obj);

        final List<Set<?>> sets = access.get();

        out.writeInt(sets.size());

        for (final Set<?> set : sets) {
            out.writeInt(set.size());
            for (final Object type : set) {
                ExternalizerManager.write(out, type);
            }
        }
    }

    /**
     * @param in
     * @param obj
     * @throws IOException
     * @throws Exception
     */
    @SuppressWarnings({"unchecked"})
    public static void readMapping(final BufferedDataInputStream in, final LabelingMapping obj) throws IOException,
            Exception {
        final LabelingMappingAccess access = new LabelingMappingAccess(obj);

        final int numSets = in.readInt();

        final List<Set<?>> sets = new ArrayList<>(numSets);
        for (int i = 0; i < numSets; i++) {
            final int size = in.readInt();
            final Set set = new HashSet(size);
            for (int j = 0; j < size; j++) {
                set.add(ExternalizerManager.read(in));
            }
            sets.add(set);
        }

        access.set(sets);
    }

    @SuppressWarnings({"unchecked"})
    static class LabelingMappingAccess extends LabelingMapping.SerialisationAccess {

        private LabelingMapping mapping;

        /**
         * @param mapping
         */
        protected LabelingMappingAccess(@SuppressWarnings("hiding") final LabelingMapping mapping) {
            super(mapping);
            this.mapping = mapping;

        }

        public void set(final List<Set<?>> labels) {
            super.setLabelSets(labels);
        }

        public List<Set<?>> get() {
            return super.getLabelSets();
        }

        public LabelingMapping getMapping() {
            return mapping;
        }
    }
}
