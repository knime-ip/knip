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
package org.knime.knip.base.nodes.seg.local;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.core.types.NeighborhoodType;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.util.EnumListProvider;

/**
 * @author friedrichm, dietzc (University of Konstanz)
 */
// TODO Remove with 2.0
/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class LocalThresholderNodeDialog<T extends RealType<T>> extends ImgPlusToImgPlusNodeDialog {

    public LocalThresholderNodeDialog() {
        super(LocalThresholderNodeModel.createDimSelectionModel(), 1, Integer.MAX_VALUE);
    }

    @Override
    public void addDialogComponents() {

        final SettingsModelString method = LocalThresholderNodeModel.createThresholderModel();

        addDialogComponent("Options", "Thresholding method", new DialogComponentStringSelection(method, "Method",
                EnumListProvider.getStringList(LocalThresholdingMethodsEnum.values())));
        addDialogComponent("Options",
                           "Out of bounds strategy",
                           new DialogComponentStringSelection(LocalThresholderNodeModel.createOutOfBoundsModel(),
                                   "Out of bounds strategy", EnumListProvider.getStringList(OutOfBoundsStrategyEnum
                                           .values())));

        final SettingsModelDouble k = LocalThresholderNodeModel.createKModel();

        addDialogComponent("Options", "Parameters", new DialogComponentNumber(k, "K", 0.01));

        final SettingsModelDouble c = LocalThresholderNodeModel.createCModel();
        addDialogComponent("Options", "Parameters", new DialogComponentNumber(c, "C", 0.01));
        final SettingsModelDouble r = LocalThresholderNodeModel.createRModel();
        addDialogComponent("Options", "Parameters", new DialogComponentNumber(r, "R", 0.01));

        final SettingsModelDouble contrastThreshold = LocalThresholderNodeModel.createContrastThreshold();
        addDialogComponent("Options", "Parameters", new DialogComponentNumber(contrastThreshold, "Contrast threshold",
                0.01));

        addDialogComponent("Options", "Parameters",
                           new DialogComponentNumber(LocalThresholderNodeModel.createWindowSize(), "Window size", 1.0));

        addDialogComponent("Options",
                           "Neighborhood type",
                           new DialogComponentStringSelection(LocalThresholderNodeModel
                                   .createNeighborhoodTypeNodeModel(), "Neighborhood Type", EnumListProvider
                                   .getStringList(NeighborhoodType.values())));

        // Init
        contrastThreshold.setEnabled(true);
        k.setEnabled(false);
        c.setEnabled(false);
        r.setEnabled(false);

        method.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent arg0) {
                final LocalThresholdingMethodsEnum methodEnum =
                        Enum.valueOf(LocalThresholdingMethodsEnum.class, method.getStringValue());

                switch (methodEnum) {

                    case BERNSEN:
                        contrastThreshold.setEnabled(true);
                        k.setEnabled(false);
                        c.setEnabled(false);
                        r.setEnabled(false);
                        break;
                    case MEAN:
                        contrastThreshold.setEnabled(false);
                        k.setEnabled(false);
                        c.setEnabled(true);
                        r.setEnabled(false);
                        break;
                    case MEDIAN:
                        contrastThreshold.setEnabled(false);
                        k.setEnabled(false);
                        c.setEnabled(true);
                        r.setEnabled(false);
                        break;
                    case MIDGREY:
                        contrastThreshold.setEnabled(false);
                        k.setEnabled(false);
                        c.setEnabled(true);
                        r.setEnabled(false);
                        break;
                    case NIBLACK:
                        contrastThreshold.setEnabled(false);
                        k.setEnabled(true);
                        c.setEnabled(true);
                        r.setEnabled(false);
                        break;
                    case SAUVOLA:
                        contrastThreshold.setEnabled(false);
                        k.setEnabled(true);
                        c.setEnabled(false);
                        r.setEnabled(true);
                        break;
                    default:
                        throw new RuntimeException();
                }
            }
        });
    }

}
