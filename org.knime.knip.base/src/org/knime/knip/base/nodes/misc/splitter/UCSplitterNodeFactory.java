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
package org.knime.knip.base.nodes.misc.splitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.interval.binary.IntervalsFromDimSelection;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.XMLNodeUtils;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.node2012.KnimeNodeDocument;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class UCSplitterNodeFactory<T extends RealType<T>> extends
        DynamicNodeFactory<ValueToCellNodeModel<ImgPlusValue<T>, ListCell>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescription(final KnimeNodeDocument doc) {
        XMLNodeUtils.addXMLNodeDescriptionTo(doc, getClass());
        //     TODO: Add correct description   ValueToCellNodeDialog.addTabsDescriptionTo(doc.getKnimeNode().getFullDescription());
        TableCellViewNodeView.addViewDescriptionTo(doc.getKnimeNode().addNewViews());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimensions selection"));

            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ListCell> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ListCell>() {

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private ImgPlusCellFactory m_imgCellFactory;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_dimSelection);
            }

            @Override
            protected ListCell compute(final ImgPlusValue<T> cellValue) throws Exception {
                final ImgPlus<T> img = cellValue.getImgPlus();
                final Interval[] tmp =
                        IntervalsFromDimSelection
                                .compute(m_dimSelection.getSelectedDimIndices(img.numDimensions(), img), img);
                final Collection<ImgPlusCell<T>> cells = new ArrayList<ImgPlusCell<T>>(tmp.length);

                for (int i = 0; i < tmp.length; i++) {

                    // TODO Test it here
                    final Img<T> subImg = new ImgView<T>(SubsetOperations.subsetview(img, tmp[i]), img.factory());

                    final CalibratedSpace<CalibratedAxis> cSpace = new DefaultCalibratedSpace(subImg.numDimensions());
                    final CalibratedAxis[] axes = new CalibratedAxis[img.numDimensions()];
                    img.axes(axes);
                    int d = 0;
                    for (int d0 = 0; d0 < axes.length; d0++) {
                        if (tmp[i].dimension(d0) != 1) {
                            cSpace.setAxis(axes[d0], d++);
                        }
                    }

                    final ImgPlusMetadata metadata = new DefaultImgMetadata(cSpace, img, img, img);

                    cells.add(m_imgCellFactory.createCell(subImg, metadata));
                }

                return CollectionCellFactory.createListCell(cells);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected DataType getOutDataCellListCellType() {
                return ImgPlusCell.TYPE;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<ValueToCellNodeModel<ImgPlusValue<T>, ListCell>>
            createNodeView(final int viewIndex, final ValueToCellNodeModel<ImgPlusValue<T>, ListCell> nodeModel) {
        return new TableCellViewNodeView<ValueToCellNodeModel<ImgPlusValue<T>, ListCell>>(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
