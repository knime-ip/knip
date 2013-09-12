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
package org.knime.knip.base.nodes.seg;

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003, 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 *
 */

import java.io.IOException;
import java.util.List;

import net.imglib2.img.Img;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.ops.img.UnaryOperationAssignment;
import net.imglib2.ops.operation.real.unary.Convert;
import net.imglib2.ops.operation.real.unary.Convert.TypeConversionTypes;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.Unsigned12BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgUtils;

/**
 * Factory class to produce the Histogram Operations Node.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ConvertLabelingNodeFactory<T extends IntegerType<T> & NativeType<T>, L extends Comparable<L>> extends
        ValueToCellNodeFactory<LabelingValue<L>> {

    private static SettingsModelString createFactorySelectionModel() {
        return new SettingsModelString("factoryselection", ImgFactoryTypes.SOURCE_FACTORY.toString());
    }

    private static SettingsModelString createTargetTypeModel() {
        return new SettingsModelString("targetformat", NativeTypes.BYTETYPE.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Target Type",
                                   new DialogComponentStringSelection(createTargetTypeModel(), "Target format",
                                           EnumListProvider.getStringList(NativeTypes.intTypeValues())));
                addDialogComponent("Options", "Factory Selection",
                                   new DialogComponentStringSelection(createFactorySelectionModel(), "Factory Type",
                                           EnumListProvider.getStringList(ImgFactoryTypes.values())));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>>() {

            private final SettingsModelString m_factorySelection = createFactorySelectionModel();

            private LabelingCellFactory m_labCellFactory;

            private final SettingsModelString m_targetType = createTargetTypeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_factorySelection);
                settingsModels.add(m_targetType);

            }

            /**
             * {@inheritDoc}
             * 
             * @throws IllegalArgumentException
             */
            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            protected LabelingCell<L> compute(final LabelingValue<L> cellValue) throws IOException {

                if (!(cellValue.getLabeling() instanceof NativeImgLabeling)) {
                    throw new IllegalArgumentException("Only native img labelings are supported, yet.");
                }

                final Img<T> img = ((NativeImgLabeling<L, T>)cellValue.getLabeling()).getStorageImg();

                Img<?> res = null;

                final ImgFactoryTypes facType = ImgFactoryTypes.valueOf(m_factorySelection.getStringValue());

                switch (NativeTypes.valueOf(m_targetType.getStringValue().toUpperCase())) {
                    case BITTYPE:
                        res = convert(img, new BitType(), facType);
                        break;
                    case BYTETYPE:
                        res = convert(img, new ByteType(), facType);
                        break;
                    case INTTYPE:
                        res = convert(img, new IntType(), facType);
                        break;
                    case LONGTYPE:
                        res = convert(img, new LongType(), facType);

                        break;
                    case SHORTTYPE:
                        res = convert(img, new ShortType(), facType);

                        break;
                    case UNSIGNED12BITTYPE:
                        res = convert(img, new Unsigned12BitType(), facType);
                        break;
                    case UNSIGNEDBYTETYPE:
                        res = convert(img, new UnsignedByteType(), facType);
                        break;
                    case UNSIGNEDINTTYPE:
                        res = convert(img, new UnsignedIntType(), facType);
                        break;
                    case UNSIGNEDSHORTTYPE:
                        res = convert(img, new UnsignedShortType(), facType);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported conversion. Type might be not IntegerType.");
                }

                final NativeImgLabeling<L, ?> resLab = new NativeImgLabeling(res);

                final LabelingMapping<L> mapping = ((NativeImgLabeling<L, T>)cellValue.getLabeling()).getMapping();
                for (int i = 0; i < mapping.numLists(); i++) {
                    resLab.getMapping().intern(mapping.listAtIndex(i));
                }

                return m_labCellFactory.createCell(resLab, cellValue.getLabelingMetadata());
            }

            public synchronized <TT extends RealType<TT> & NativeType<TT>, O extends RealType<O> & NativeType<O>>
                    Img<O> convert(final Img<TT> img, final O outType, final ImgFactoryTypes facType) {
                final UnaryOperationAssignment<TT, O> map =
                        new UnaryOperationAssignment<TT, O>(new Convert<TT, O>(img.firstElement().createVariable(),
                                outType, TypeConversionTypes.DIRECT));

                @SuppressWarnings("unchecked")
                final Img<O> res =
                        ImgUtils.<TT, O> createEmptyCopy(img, ImgFactoryTypes.<TT> getImgFactory(facType, img), outType);
                map.compute(img, res);

                return res;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labCellFactory = new LabelingCellFactory(exec);
            }

        };
    }

}
