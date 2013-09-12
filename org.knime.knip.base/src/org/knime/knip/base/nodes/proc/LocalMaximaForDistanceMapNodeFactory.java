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
package org.knime.knip.base.nodes.proc;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.img.sparse.NtreeImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.LocalMaximaForDistanceMap;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.LocalMaximaForDistanceMap.NeighborhoodType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.core.ops.bittype.PositionsToBitTypeImage;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * LocalMaxima for DistanceMap Node Factory
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author metznerj
 */
public class LocalMaximaForDistanceMapNodeFactory<T extends RealType<T>> extends
        ImgPlusToImgPlusNodeFactory<T, BitType> {

    private class CombinedLocalMaximaOp implements UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> {

        private final LocalMaximaForDistanceMap<T, ImgPlus<T>> m_localMaximaOp;

        private final NeighborhoodType m_neighborhood;

        private final PositionsToBitTypeImage m_posToBitType;

        public CombinedLocalMaximaOp(final NeighborhoodType neighborhood) {
            m_neighborhood = neighborhood;
            m_localMaximaOp = new LocalMaximaForDistanceMap<T, ImgPlus<T>>(neighborhood);
            m_posToBitType = new PositionsToBitTypeImage();
        }

        @Override
        public UnaryObjectFactory<ImgPlus<T>, ImgPlus<BitType>> bufferFactory() {
            return new UnaryObjectFactory<ImgPlus<T>, ImgPlus<BitType>>() {

                @Override
                public ImgPlus<BitType> instantiate(final ImgPlus<T> in) {
                    final long[] dims = new long[in.numDimensions()];
                    in.dimensions(dims);
                    return new ImgPlus<BitType>(new NtreeImgFactory<BitType>().create(dims, new BitType()), in);
                }
            };
        }

        @Override
        public ImgPlus<BitType> compute(final ImgPlus<T> op, final ImgPlus<BitType> r) {
            m_posToBitType.compute(m_localMaximaOp.compute(op, new ArrayList<long[]>()), r);
            return r;
        }

        @Override
        public UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> copy() {
            return new CombinedLocalMaximaOp(m_neighborhood);
        }
    }

    private static SettingsModelString createNeighborhoodModel() {
        return new SettingsModelString("neighborhood", NeighborhoodType.EIGHT.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusToImgPlusNodeDialog<T> createNodeDialog() {
        return new ImgPlusToImgPlusNodeDialog<T>(2, 3, "X", "Y") {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "Options", new DialogComponentStringSelection(createNeighborhoodModel(),
                        "Neighboorhood", EnumListProvider.getStringList(NeighborhoodType.values())));

            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusToImgPlusNodeModel<T, BitType> createNodeModel() {
        return new ImgPlusToImgPlusNodeModel<T, BitType>("X", "Y") {

            private final SettingsModelString m_neighborhood = createNeighborhoodModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_dimSelection);

                settingsModels.add(m_neighborhood);
            }

            @Override
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> op(final ImgPlus<T> imgPlus) {
                return Operations.wrap(new CombinedLocalMaximaOp(NeighborhoodType.valueOf(m_neighborhood
                                               .getStringValue())), ImgPlusFactory.<T, BitType> get(new BitType()));
            }

            @Override
            protected int getMinDimensions() {
                return 2;
            }
        };

    }
}
