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
package org.knime.knip.base.nodes.features;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.IterableInterval;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.dialog.DialogComponentFilterSelection;
import org.knime.knip.base.nodes.features.IntervalFeatureSetNodeModel.FeatureType;
import org.knime.knip.base.nodes.features.providers.FeatureSetProvider;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class IntervalFeatureSetNodeFactory<L extends Comparable<L>, T extends RealType<T>> extends
        NodeFactory<IntervalFeatureSetNodeModel<L, T>> {

    @SuppressWarnings("unchecked")
    private NodeDialogPane
            createDialog(final FeatureType type,
                         final FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>>[] featSetProvider) {

        final DialogComponentCollection dialogComponentCollection =
                new FeatureDialogComponentCollection(IntervalFeatureSetNodeModel.createActiveFeatureSetModel());

        if ((type == FeatureType.LABELING) || (type == FeatureType.IMAGE_AND_LABELING)) {

            dialogComponentCollection.addDialogComponent("Column selection", "Columns",
                                                         new DialogComponentColumnNameSelection(
                                                                 IntervalFeatureSetNodeModel.createLabColumnModel(),
                                                                 "Labeling", 0, LabelingValue.class));

            dialogComponentCollection.addDialogComponent("ROI Settings", "Settings", new DialogComponentBoolean(
                    IntervalFeatureSetNodeModel.createAppendDependenciesModel(), "Append labels of overlapping ROIs?"));

            dialogComponentCollection.addDialogComponent("ROI Settings", "Settings", new DialogComponentBoolean(
                    IntervalFeatureSetNodeModel.createIntersectionModeModel(),
                    "Dependend ROIs don't need to completely overlap?"));

            dialogComponentCollection.addDialogComponent("ROI Settings",
                                                         "Filter on ROIs",
                                                         new DialogComponentFilterSelection<L>(
                                                                 IntervalFeatureSetNodeModel
                                                                         .<L> createLeftFilterSelectionModel()));

            dialogComponentCollection.addDialogComponent("ROI Settings",
                                                         "Filter depended objects",
                                                         new DialogComponentFilterSelection<L>(
                                                                 IntervalFeatureSetNodeModel
                                                                         .<L> createRightFilterSelectionModel()));

            dialogComponentCollection.addDialogComponent("ROI Settings", "Settings", new DialogComponentBoolean(
                    IntervalFeatureSetNodeModel.createAppendSegmentInfoModel(), "Append ROI information?"));
        }
        if ((type == FeatureType.IMAGE) || (type == FeatureType.IMAGE_AND_LABELING)) {
            dialogComponentCollection.addDialogComponent("Column selection", "Columns",
                                                         new DialogComponentColumnNameSelection(
                                                                 new SettingsModelString("img_column_selection", ""),
                                                                 "Image", 0, ImgPlusValue.class));
        }

        for (final FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>> featFacWrapper : featSetProvider) {
            final List<DialogComponent> dialogComponents = new ArrayList<DialogComponent>();
            featFacWrapper.initAndAddDialogComponents(dialogComponents);
            for (final DialogComponent comp : dialogComponents) {
                dialogComponentCollection.addDialogComponent(featFacWrapper.getFeatureSetName(), comp);
            }
        }
        return dialogComponentCollection.getDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return createDialog(getFeatureType(), getFeatureSetProviders());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntervalFeatureSetNodeModel<L, T> createNodeModel() {
        return new IntervalFeatureSetNodeModel<L, T>(getFeatureType(), getFeatureSetProviders());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<IntervalFeatureSetNodeModel<L, T>>
            createNodeView(final int viewIndex, final IntervalFeatureSetNodeModel<L, T> nodeModel) {
        return null;
    }

    /**
     * @return the feature set providers
     */
    protected abstract FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>>[] getFeatureSetProviders();

    /**
     * @return
     */
    protected abstract FeatureType getFeatureType();

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
