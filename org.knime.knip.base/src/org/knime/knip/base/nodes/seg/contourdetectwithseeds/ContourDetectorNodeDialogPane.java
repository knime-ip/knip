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
package org.knime.knip.base.nodes.seg.contourdetectwithseeds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.dialog.DialogComponentFilterSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.util.EnumListProvider;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class ContourDetectorNodeDialogPane extends DefaultNodeSettingsPane {
    public enum GradientDirection {
        DECREASING {
            @Override
            public String toString() {
                return "Decreasing";
            }
        },
        INCREASING {
            @Override
            public String toString() {
                return "Increasing";
            }
        };
        private static final List<String> NAMES = new ArrayList<String>();
        static {
            for (final GradientDirection e : GradientDirection.values()) {
                NAMES.add(e.toString());
            }
        }
    }

    static SettingsModelDimSelection createAngularDimension() {
        final SettingsModelDimSelection model = new SettingsModelDimSelection("angulardimension", new String[0]);
        model.setEnabled(false);
        return model;
    }

    static SettingsModelBoolean createCalculateGradient() {
        return new SettingsModelBoolean("calcgradient", false);
    }

    static SettingsModelString createGradientDirection() {
        return new SettingsModelString("gradientdirection", GradientDirection.DECREASING.name());
    }

    static SettingsModelString createImgColumn() {
        return new SettingsModelString("imgcolumn", "");
    }

    static SettingsModelDimSelection createImgDimesions() {
        return new SettingsModelDimSelection("dimensions", "Dimselection", "X", "Y");
    }

    @SuppressWarnings("rawtypes")
    static SettingsModelFilterSelection createLeftFilterSelection() {
        return new SettingsModelFilterSelection("leftfilter");
    }

    static SettingsModelInteger createLineVariance() {
        return new SettingsModelInteger("linevariance", 1);
    }

    static SettingsModelInteger createMinArea() {
        return new SettingsModelInteger("minarea", 20);
    }

    static SettingsModelDoubleBounded createMinScore() {
        return new SettingsModelDoubleBounded("minscore", .5, 0, 1);
    }

    static SettingsModelInteger createNumAngles() {
        return new SettingsModelInteger("numangles", 100);
    }

    static SettingsModelString createOutOfBoundsSelectionModel() {
        return new SettingsModelString("outofbounds", OutOfBoundsStrategyEnum.BORDER.toString());
    }

    static SettingsModelDoubleBounded createOverlap() {
        return new SettingsModelDoubleBounded("overlap", 0, 0, 1);
    }

    static SettingsModelBoolean createPartialProjection() {
        final SettingsModelBoolean model = new SettingsModelBoolean("angularpartialprojection", false);
        model.setEnabled(false);
        return model;
    }

    static SettingsModelInteger createRadius() {
        return new SettingsModelInteger("radius", 40);
    }

    static SettingsModelString createSeedColumn() {
        return new SettingsModelString("seedcolumn", "");
    }

    static SettingsModelBoolean createSmoothGradient() {
        return new SettingsModelBoolean("smoothgradient", false);
    }

    static SettingsModelBoolean createUseAngularDimension() {
        return new SettingsModelBoolean("useangulardimension", false);
    };

    private final DialogComponentDimSelection m_AngularDim;

    private final DialogComponentBoolean m_calcGradient = new DialogComponentBoolean(createCalculateGradient(),
            "Calc Gradient");

    private final DialogComponentStringSelection m_graDirection = new DialogComponentStringSelection(
            createGradientDirection(), "Gradient direction", GradientDirection.NAMES);

    @SuppressWarnings("unchecked")
    private final DialogComponentColumnNameSelection m_imgColumn = new DialogComponentColumnNameSelection(
            createImgColumn(), "", 0, ImgPlusValue.class);

    private final DialogComponentDimSelection m_imgDimensions = new DialogComponentDimSelection(createImgDimesions(),
            "Dimension selection", 2, 2);

    @SuppressWarnings({"rawtypes", "unchecked"})
    private final DialogComponentFilterSelection m_leftFilter = new DialogComponentFilterSelection(
            createLeftFilterSelection());

    private final DialogComponentNumber m_lineVariance = new DialogComponentNumber(createLineVariance(),
            "Line variance", 1);

    private final DialogComponentNumber m_minArea = new DialogComponentNumber(createMinArea(), "Minimal area", 1);

    private final DialogComponentNumber m_minScore = new DialogComponentNumber(createMinScore(), "Minimal score", .1);

    private final DialogComponentNumber m_numAngles = new DialogComponentNumber(createNumAngles(), "Angles", 1);

    private final DialogComponentStringSelection m_outOfBoundsFactory = new DialogComponentStringSelection(
            createOutOfBoundsSelectionModel(), "Out of bounds strategy",
            EnumListProvider.getStringList(OutOfBoundsStrategyEnum.values()));

    private final DialogComponentNumber m_overlap = new DialogComponentNumber(createOverlap(), "Overlap", .1);

    private final DialogComponentBoolean m_partialProjection;

    private final DialogComponentNumber m_radius = new DialogComponentNumber(createRadius(), "Radius", 1);

    private final DialogComponentColumnNameSelection m_seedColumn = new DialogComponentColumnNameSelection(
            createSeedColumn(), "Column", 0, true, LabelingValue.class);

    private final DialogComponentBoolean m_smooth = new DialogComponentBoolean(createSmoothGradient(),
            "Smooth contour (requires the angles to be a power of 2!)");

    private final SettingsModelBoolean m_smPartialProjection;

    private final DialogComponentBoolean m_useAngular;

    /**
     * Create new dialog pane with default components.
     */
    public ContourDetectorNodeDialogPane() {
        super();
        createNewGroup("Image");
        addDialogComponent(m_imgColumn);
        addDialogComponent(m_seedColumn);
        addDialogComponent(m_imgDimensions);

        closeCurrentGroup();

        createNewGroup("Basic Settings");
        addDialogComponent(m_outOfBoundsFactory);
        addDialogComponent(m_radius);
        addDialogComponent(m_numAngles);
        addDialogComponent(m_lineVariance);
        addDialogComponent(m_overlap);
        addDialogComponent(m_minScore);
        addDialogComponent(m_minArea);
        addDialogComponent(m_smooth);
        closeCurrentGroup();

        final SettingsModelBoolean useAngularModel = createUseAngularDimension();
        m_useAngular = new DialogComponentBoolean(useAngularModel, "Use Angular Dimension");
        final SettingsModelDimSelection angularDimensionsModel = createAngularDimension();
        m_AngularDim = new DialogComponentDimSelection(angularDimensionsModel, "Angular dimension", 1, 1);

        m_smPartialProjection = createPartialProjection();
        m_partialProjection = new DialogComponentBoolean(m_smPartialProjection, "Do partial projection?");

        useAngularModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                angularDimensionsModel.setEnabled(useAngularModel.getBooleanValue());
                m_smPartialProjection.setEnabled(useAngularModel.getBooleanValue());
                if (!useAngularModel.getBooleanValue()) {
                    angularDimensionsModel.setDimSelectionValue(new HashSet<String>());
                }
            }
        });

        createNewTab("Angular");
        addDialogComponent(m_useAngular);
        addDialogComponent(m_AngularDim);
        addDialogComponent(m_partialProjection);

        createNewTab("Gradient Options");
        addDialogComponent(m_calcGradient);
        addDialogComponent(m_graDirection);
        addDialogComponent(m_leftFilter);
    }
}
