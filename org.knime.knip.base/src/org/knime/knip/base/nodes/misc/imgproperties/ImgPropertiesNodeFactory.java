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
package org.knime.knip.base.nodes.misc.imgproperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import net.imglib2.meta.TypedAxis;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.util.Pair;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellsNodeDialog;
import org.knime.knip.base.node.ValueToCellsNodeFactory;
import org.knime.knip.base.node.ValueToCellsNodeModel;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgPropertiesNodeFactory<T extends RealType<T>> extends ValueToCellsNodeFactory<ImgPlusValue<T>> {

    private static final DataType[] FEATURE_DATA_TYPES = new DataType[]{IntCell.TYPE,
            ListCell.getCollectionType(LongCell.TYPE), IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE,
            StringCell.TYPE, StringCell.TYPE, ListCell.getCollectionType(DoubleCell.TYPE)};

    private static final String[] FEATURE_NAMES = new String[]{"Number of Dimensions", "Dimensions",
            "Number of Pixels", "Type", "Factory Type", "Axes", "Name", "Source", "Calibration"};

    private static SettingsModelStringArray createFeatSelectionModel() {
        return new SettingsModelStringArray("feature_seleciton", new String[]{});
    }

    @Override
    protected ValueToCellsNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellsNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Features", "", new DialogComponentStringListSelection(createFeatSelectionModel(),
                        "Image Features", Arrays.asList(FEATURE_NAMES), true, 5));
            }

        };
    }

    @Override
    public ValueToCellsNodeModel<ImgPlusValue<T>> createNodeModel() {
        return new ValueToCellsNodeModel<ImgPlusValue<T>>() {

            SettingsModelStringArray m_featureSelection = createFeatSelectionModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_featureSelection);
            }

            @Override
            protected DataCell[] compute(final ImgPlusValue<T> imgPlusValue) throws Exception {

                final BitSet selection = initSelectedFeatures();

                final DataCell[] cells = new DataCell[selection.cardinality()];

                final long[] dimsArray = imgPlusValue.getDimensions();

                int ctr = 0;
                for (int feat = selection.nextSetBit(0); feat >= 0; feat = selection.nextSetBit(feat + 1)) {
                    switch (feat) {
                        case 0:
                            cells[ctr++] = new IntCell(dimsArray.length);
                            break;
                        case 1:
                            final List<LongCell> dims = new ArrayList<LongCell>(dimsArray.length);
                            for (int i = 0; i < dimsArray.length; i++) {
                                dims.add(new LongCell(dimsArray[i]));
                            }
                            cells[ctr++] = CollectionCellFactory.createListCell(dims);
                            break;
                        case 2:

                            int size = 1;
                            for (final long d : dimsArray) {
                                size *= d;
                            }

                            cells[ctr++] = new IntCell(size);
                            break;
                        case 3:
                            cells[ctr++] = new StringCell(imgPlusValue.getPixelType().getSimpleName());
                            break;

                        case 4:
                            cells[ctr++] =
                                    new StringCell(imgPlusValue.getImgPlus().factory().getClass().getSimpleName());
                            break;
                        case 5:
                            final StringBuffer buf = new StringBuffer();
                            final TypedAxis[] axes = new TypedAxis[imgPlusValue.getMetadata().numDimensions()];

                            for (int d = 0; d < axes.length; d++) {
                                axes[d] = imgPlusValue.getMetadata().axis(d);
                            }

                            for (int i = 0; i < (axes.length - 1); i++) {
                                buf.append(axes[i].type().getLabel() + ";");
                            }
                            buf.append(axes[axes.length - 1]);
                            cells[ctr++] = new StringCell(buf.toString());
                            break;
                        case 6:
                            cells[ctr++] = new StringCell(imgPlusValue.getMetadata().getName());
                            break;
                        case 7:
                            cells[ctr++] = new StringCell(imgPlusValue.getMetadata().getSource());
                            break;

                        case 8:
                            final List<DoubleCell> calibration =
                                    new ArrayList<DoubleCell>(imgPlusValue.getMetadata().numDimensions());
                            for (int i = 0; i < imgPlusValue.getMetadata().numDimensions(); i++) {
                                calibration.add(new DoubleCell(imgPlusValue.getMetadata().calibration(i)));
                            }
                            cells[ctr++] = CollectionCellFactory.createListCell(calibration);
                            break;

                        default:
                            cells[ctr++] = DataType.getMissingCell();
                    }
                }
                return cells;
            }

            @Override
            protected Pair<DataType[], String[]> getDataOutTypeAndName() {

                final BitSet enabled = initSelectedFeatures();
                final DataType[] types = new DataType[enabled.cardinality()];
                final String[] names = new String[enabled.cardinality()];
                int ctr = 0;
                for (int feat = enabled.nextSetBit(0); feat >= 0; feat = enabled.nextSetBit(feat + 1)) {
                    types[ctr] = FEATURE_DATA_TYPES[feat];
                    names[ctr] = FEATURE_NAMES[feat];
                    ctr++;
                }

                return new Pair<DataType[], String[]>(types, names);
            }

            private BitSet initSelectedFeatures() {
                final String[] selectedFeatures = m_featureSelection.getStringArrayValue();

                final BitSet selection = new BitSet(FEATURE_NAMES.length);
                int j = 0;
                for (int i = 0; i < FEATURE_NAMES.length; i++) {
                    if ((j < selectedFeatures.length) && selectedFeatures[j].equals(FEATURE_NAMES[i])) {
                        selection.set(i);
                        j++;
                    }
                }
                return selection;
            }
        };
    }

}
