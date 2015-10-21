/*
 * ------------------------------------------------------------------------
 *
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
  ---------------------------------------------------------------------
 *
 */
package org.knime.knip.features.groups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.features.DataRowUtil;
import org.knime.knip.features.FeaturesGateway;
import org.knime.knip.features.node.model.FeatureSetInfo;
import org.knime.knip.features.sets.FeatureSet;

import net.imagej.ImgPlus;
import net.imagej.ops.ComputerOp;
import net.imagej.ops.slicewise.Hyperslice;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * FIXME: Design of FeatureGroups is really weak. However, we can redesign it
 * whenever we have more time, without destroying backwards compatibility.
 * 
 * @author Christian Dietz, University of Konstanz
 *
 * @param <L>
 * @param <T>
 * @param <O>
 */
public class ImgPlusFeatureSetGroup<T extends RealType<T>, R extends RealType<R>> extends AbstractFeatureSetGroup {

	private final int imgIdx;

	private final List<FeatureSet<RandomAccessibleInterval<T>, R>> featureSets;

	private final boolean append;

	private final SettingsModelDimSelection dimSelection;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ImgPlusFeatureSetGroup(final List<FeatureSetInfo> infos, final int imgIdx, final boolean append,
			final SettingsModelDimSelection dimSelection) {
		this.append = append;
		this.imgIdx = imgIdx;
		this.dimSelection = dimSelection;
		this.featureSets = (List) FeaturesGateway.fs().getValidFeatureSets(ImgPlus.class, RealType.class, infos);
	}

	@Override
	public DataTableSpec createSpec(final DataTableSpec inSpec) {
		List<DataColumnSpec> spec = new ArrayList<>();

		if (append) {
			for (int i = 0; i < inSpec.getNumColumns(); i++) {
				spec.add(inSpec.getColumnSpec(i));
			}
		}

		spec.addAll(
				createColumnSpec(inSpec, featureSets, append, false, false, dimSelection.getNumSelectedDimLabels()));

		return new DataTableSpec(spec.toArray(new DataColumnSpec[spec.size()]));
	}

	@Override
	public ComputerOp<DataRow, DataContainer> createComputerOp() {

		final ComputerOp<DataRow, DataContainer> op = new FeatureSetGroupComputer<R>() {

			private boolean initialized = false;

			@Override
			public void compute(final DataRow row, final DataContainer container) {
				final DataCell cell = row.getCell(imgIdx);
				if (cell.isMissing()) {
					KNIPGateway.log().warn("Skipping missing Image at Row: " + row.getKey() + ".");
					return;
				}

				@SuppressWarnings("unchecked")
				final ImgPlus<T> imgPlus = ((ImgPlusValue<T>) cell).getImgPlus();

				final Hyperslice slicer = new Hyperslice(KNIPGateway.ops(), imgPlus,
						dimSelection.getSelectedDimIndices(imgPlus), true);
				final Cursor<RandomAccessibleInterval<?>> slicingCursor = slicer.cursor();
				boolean slicingActive = (slicer.size() == 1);

				while (slicingCursor.hasNext()) {
					@SuppressWarnings("unchecked")
					final RandomAccessibleInterval<T> slice = (RandomAccessibleInterval<T>) slicingCursor.next();

					if (!initialized) {
						initFeatureSet(featureSets, slice);
						initialized = true;
					}

					final List<DataCell> cells = computeOnFeatureSets(featureSets, slice);

					String intervalDef = null;
					if (!slicingActive) {

						long[] pos = new long[slicingCursor.numDimensions()];
						slicingCursor.localize(pos);
						intervalDef = " Pos[" + Arrays.toString(pos) + "]";

					}

					final String newKey = row.getKey().getString()
							+ (intervalDef != null ? KNIPConstants.IMGID_LABEL_DELIMITER + intervalDef : "");

					container.addRowToTable(
							append ? DataRowUtil.appendCells(newKey, row, cells) : new DefaultRow(newKey, cells));
				}
			}
		};

		op.setEnvironment(KNIPGateway.ops());
		return op;
	}

}
