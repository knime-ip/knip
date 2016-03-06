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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.knip.base.nodes.proc.ucm;

import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeModel;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.util.CellUtil;

import net.imagej.ImgPlus;
import net.imagej.ops.MetadataUtil;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * {@link NodeModel} for {@link UCMOp}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 * @param <T>
 * @param <L>
 */
public class UCMNodeModel<T extends RealType<T>, L extends Comparable<L>>
        extends TwoValuesToCellNodeModel<LabelingValue<L>, ImgPlusValue<T>, ImgPlusCell<FloatType>> {

    /**
     * @return SettingsModel to store max number of faces
     */
    static SettingsModelInteger createMaxFacesAmountModel() {
        return new SettingsModelInteger("max_faces_num", 9999);
    }

    /**
     * @return SettingsModel to store max face percent
     */
    static SettingsModelDouble createMaxFacePercentModel() {
        return new SettingsModelDouble("max_face_percent", 100);
    }

    /**
     * @return SettingsModel to store min boundary weight model
     */
    static SettingsModelInteger createMinBoundaryWeightModel() {
        return new SettingsModelInteger("min_boundary_weight", 0);
    }

    /**
     * @return SettingsModel to store boundary label
     */
    static SettingsModelString createBoundaryLabelModel() {
        return new SettingsModelString("boundary_label", "Boundary");
    }

    /**
     * @return SettingsModel to store dimension selection
     */
    static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private SettingsModelInteger m_maxNumFaces = createMaxFacesAmountModel();

    private SettingsModelDouble m_maxFacePercent = createMaxFacePercentModel();

    private SettingsModelInteger m_minBoundaryWeight = createMinBoundaryWeightModel();

    private SettingsModelString m_boundaryLabel = createBoundaryLabelModel();

    private SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

    private ImgPlusCellFactory m_imgCellFactory;

    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_imgCellFactory = new ImgPlusCellFactory(exec);
    }

    @Override
    protected ImgPlusCell<FloatType> compute(final LabelingValue<L> labelingValue, final ImgPlusValue<T> imgValue)
            throws Exception {

        // containers to work on
        final RandomAccessibleInterval<LabelingType<L>> fromCellLabeling = labelingValue.getLabeling();
        final RandomAccessibleInterval<LabelingType<L>> zeroMinFromCellLabeling =
                CellUtil.getZeroMinLabeling(fromCellLabeling);

        final ImgPlus<T> fromCellImg = imgValue.getImgPlus();
        final ImgPlus<T> zeroMinFromCellImg = CellUtil.getZeroMinImgPlus(fromCellImg);

        final Img<FloatType> result = new ArrayImgFactory<FloatType>().create(zeroMinFromCellImg, new FloatType());

        // create new UCMOp with parameters
        final UCMOp<L, T> ucmOp = new UCMOp<L, T>(m_maxNumFaces.getIntValue(), m_maxFacePercent.getDoubleValue(),
                m_minBoundaryWeight.getIntValue(), m_boundaryLabel.getStringValue());

        SubsetOperations.iterate(ucmOp, m_dimSelection.getSelectedDimIndices(zeroMinFromCellImg),
                                 zeroMinFromCellLabeling, zeroMinFromCellImg, result, getExecutorService());

        final ImgPlus<FloatType> res = new ImgPlus<>(result, imgValue.getMetadata());
        MetadataUtil.copySource(imgValue.getMetadata(), res);
        return m_imgCellFactory.createCell(CellUtil.getTranslatedImgPlus(fromCellImg, res));
    }

    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_maxNumFaces);
        settingsModels.add(m_maxFacePercent);
        settingsModels.add(m_minBoundaryWeight);
        settingsModels.add(m_boundaryLabel);
        settingsModels.add(m_dimSelection);
    }
}
