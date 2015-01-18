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
package org.knime.knip.base.nodes.features.providers;

import java.util.List;

import net.imagej.space.CalibratedSpace;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.knip.core.features.FeatureFactory;
import org.knime.knip.core.features.seg.ZernikeFeatureSet;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ZernikeDescriptorFeatureSetProvider<T extends RealType<T>> implements
        FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>> {

    private static SettingsModelBoolean createMagnitudeModel() {
        return new SettingsModelBoolean("zernike_magnitude", true);
    }

    private FeatureFactory m_featFac;

    private SettingsModelBoolean m_magnitude;

    private SettingsModelInteger m_zernikeOrder;

    @Override
    public void
            calcAndAddFeatures(final ValuePair<IterableInterval<T>, CalibratedSpace> roi, final List<DataCell> cells) {
        m_featFac.updateFeatureTarget(roi.a);
        m_featFac.updateFeatureTarget(roi.b);

        // Features
        for (int featID = 0; featID < m_featFac.getNumFeatures(); featID++) {
            cells.add(new DoubleCell(m_featFac.getFeatureValue(featID)));

        }
    }

    private SettingsModelInteger createOrderModel() {
        return new SettingsModelInteger("zernike_order", 5);
    }

    @Override
    public String getFeatureSetName() {
        return "Zernike";
    }

    @Override
    public String getFeatureSetId() {
        return "Zernike";
    }

    @Override
    public void initAndAddColumnSpecs(final List<DataColumnSpec> specs) {
        // create outspec according to the selected features

        m_featFac =
                new FeatureFactory(true, new ZernikeFeatureSet<T>(m_zernikeOrder.getIntValue(),
                        m_magnitude.getBooleanValue()));

        final String[] featNames = m_featFac.getFeatureNames();
        for (int i = 0; i < featNames.length; i++) {
            specs.add(new DataColumnSpecCreator(featNames[i], DoubleCell.TYPE).createSpec());

        }

    }

    @Override
    public void initAndAddDialogComponents(final List<DialogComponent> dialogComponents) {

        dialogComponents.add(new DialogComponentNumber(createOrderModel(), "order", 1));

        dialogComponents
                .add(new DialogComponentBoolean(createMagnitudeModel(), "calc magnitude of the complex result"));

    }

    @Override
    public void initAndAddSettingsModels(final List<SettingsModel> model) {
        model.add(m_magnitude = createMagnitudeModel());
        model.add(m_zernikeOrder = createOrderModel());

    }
}
