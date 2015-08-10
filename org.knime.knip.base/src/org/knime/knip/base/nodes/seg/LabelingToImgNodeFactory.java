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

import java.io.IOException;
import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumUtils;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * {@link NodeFactory} to convert {@link ImgLabeling} to an {@link ImgPlus} of arbitrary {@link RealType}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <L>
 * @param <V>
 */
public class LabelingToImgNodeFactory<L extends Comparable<L>, V extends IntegerType<V>>
        extends ValueToCellNodeFactory<LabelingValue<L>> {

    private static SettingsModelString createOutputImgModel() {
        return new SettingsModelString("output", NativeTypes.BITTYPE.toString());
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
            @SuppressWarnings("deprecation")
            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "", new DialogComponentStringSelection(createOutputImgModel(),
                        "Img Output Type Selection", EnumUtils.getStringListFromName(NativeTypes.values())));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<LabelingValue<L>, ImgPlusCell<V>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, ImgPlusCell<V>>() {

            private ImgPlusCellFactory m_imgCellFactory;

            private final SettingsModelString m_outputImg = createOutputImgModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_outputImg);
            }

            /**
             * {@inheritDoc}
             *
             * @throws IncompatibleTypeException
             */
            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            protected ImgPlusCell<V> compute(final LabelingValue<L> cellValue)
                    throws IncompatibleTypeException, IOException {
                final RandomAccessibleInterval<LabelingType<L>> lab = cellValue.getLabeling();

                final RealType outType =
                        (RealType)NativeTypes.getTypeInstance(NativeTypes.valueOf(m_outputImg.getStringValue()));

                final ImgPlus<RealType> out = new ImgPlus(KNIPGateway.ops().create().img(lab, outType));

                final Cursor<LabelingType<L>> inC = Views.iterable(lab).cursor();
                final Cursor<RealType> outC = out.cursor();

                double outMax = outType.getMaxValue();

                while (outC.hasNext()) {
                    int val = inC.next().getIndex().getInteger();
                    if (val > outMax) {
                        outC.next().setReal(outMax);
                    } else {
                        outC.next().setReal(val);
                    }
                }

                for (int a = 0; a < cellValue.getDimensions().length; a++) {
                    out.setAxis(cellValue.getLabelingMetadata().axis(a).copy(), a);
                }

                out.setSource(cellValue.getLabelingMetadata().getSource());
                out.setName(cellValue.getLabelingMetadata().getName());

                ImgPlusCell cell = m_imgCellFactory.createCell(out);
                return cell;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_imgCellFactory = new ImgPlusCellFactory(exec);
            }
        };
    }
}
