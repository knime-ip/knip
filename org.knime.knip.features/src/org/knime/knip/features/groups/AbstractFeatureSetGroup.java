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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.features.FeatureSetGroup;
import org.knime.knip.features.LabelRegionToBitmaskConverter;
import org.knime.knip.features.sets.FeatureSet;
import org.knime.knip.features.sets.NamedFeature;
import org.knime.knip.features.sets.RequireNumDimensions;

import net.imagej.ImgPlus;
import net.imagej.ops.AbstractComputerOp;
import net.imagej.ops.OpEnvironment;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.type.logic.BitType;
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
public abstract class AbstractFeatureSetGroup implements FeatureSetGroup {

	protected <O, R extends RealType<R>> List<DataColumnSpec> createColumnSpec(final DataTableSpec inSpec,
			final List<FeatureSet<O, R>> featureSets, final boolean append,
			final boolean appendOverlappingSegmentsColumnSpec, final boolean appendSegmentInformation,
			final int expectedDims) {
		return createColumnSpec(inSpec, new HashMap<>(), featureSets, append, appendOverlappingSegmentsColumnSpec,
				appendSegmentInformation, expectedDims);
	}

	protected <O, R extends RealType<R>> List<DataColumnSpec> createColumnSpec(final DataTableSpec inSpec,
			final HashMap<String, Integer> prefixMap, final List<FeatureSet<O, R>> featureSets, final boolean append,
			final boolean appendOverlappingSegmentsColumnSpec, final boolean appendSegmentInformation,
			final int expectedDims) {
		final UniqueNameGenerator colNameGenerator;
		if (append) {
			colNameGenerator = new UniqueNameGenerator(inSpec);
		} else {
			colNameGenerator = new UniqueNameGenerator(new DataTableSpec());
		}
		final List<DataColumnSpec> specs = new ArrayList<>();

		if (appendSegmentInformation) {
			specs.addAll(createSegmentInformationColumnSpec(colNameGenerator));
		}

		if (appendOverlappingSegmentsColumnSpec) {
			specs.add(createOverlappingSegmentsColumnSpec(colNameGenerator));
		}

		for (final FeatureSet<O, R> featureSet : featureSets) {

			if (featureSet instanceof RequireNumDimensions) {
				((RequireNumDimensions) featureSet).setNumDimensions(expectedDims);
			}

			final String featureSetName = KNIPGateway.cs().getCommand(featureSet.getClass()).getLabel() + "";
			final String suffix;

			if (prefixMap.containsKey(featureSetName)) {
				final int ctr = prefixMap.get(featureSetName);
				suffix = " [#" + ctr + "]";
				prefixMap.put(featureSetName, ctr + 1);
			} else {
				prefixMap.put(featureSetName, 1);
				suffix = "";
			}

			for (final NamedFeature featureName : featureSet.getFeatures()) {
				specs.add(new DataColumnSpecCreator(
						colNameGenerator.newName(featureName.getName() + " /(" + featureSetName + ")" + suffix),
						DoubleCell.TYPE).createSpec());
			}
		}

		return specs;
	}

	private DataColumnSpec createOverlappingSegmentsColumnSpec(final UniqueNameGenerator colNameGenerator) {

		return colNameGenerator.newColumn("OverlappingLabels", StringCell.TYPE);

	}

	private List<DataColumnSpec> createSegmentInformationColumnSpec(final UniqueNameGenerator colNameGenerator) {

		List<DataColumnSpec> specs = new ArrayList<DataColumnSpec>();

		specs.add(new DataColumnSpecCreator(colNameGenerator.newName("Bitmask"), ImgPlusCell.TYPE).createSpec());
		specs.add(new DataColumnSpecCreator(colNameGenerator.newName("Label"), StringCell.TYPE).createSpec());

		return specs;

	}

	public abstract class FeatureSetGroupComputer<O extends RealType<O>>
			extends AbstractComputerOp<DataRow, DataContainer> {
		protected <I> boolean initFeatureSet(final List<FeatureSet<I, O>> sets, final I in) {

			boolean allValid = true;
			for (final FeatureSet<I, O> fs : sets) {
				fs.setInput(in);
				if (fs.conforms()) {
					fs.initialize();
				} else {
					allValid = false;
				}
			}

			return allValid;
		}

		protected <I> List<DataCell> computeOnFeatureSets(final List<FeatureSet<I, O>> sets, final I in) {
			final List<DataCell> cells = new ArrayList<>();
			for (final FeatureSet<I, O> fs : sets) {
				fs.setInput(in);
				if (fs.conforms()) {
					final Map<NamedFeature, O> fsRes = fs.compute(in);
					for (final NamedFeature namedFeature : fs.getFeatures()) {
						cells.add(new DoubleCell(fsRes.get(namedFeature).getRealDouble()));
					}
				} else {
					for (int i = 0; i < fs.getFeatures().size(); i++) {
						cells.add(DataType.getMissingCell());
					}

					KNIPGateway.log()
							.warn("Selected FeatureSet " + fs.getClass()
									+ " can't handle the provided input. Please select "
									+ "feature sets suitable for input dimensions. Inserting missing cell.");
				}
			}

			return cells;
		}

	}

	protected <L> void appendRegionOptions(final LabelRegion<L> region, final List<DataCell> cells,
			final ImgPlusCellFactory imgPlusCellFactory, final Map<L, List<L>> dependencies, final OpEnvironment ops) {
		if (imgPlusCellFactory != null) {
			@SuppressWarnings("unchecked")
			Img<BitType> bitmask = (Img<BitType>) ops.run(LabelRegionToBitmaskConverter.class, region);
			try {
				cells.add(imgPlusCellFactory.createCell(new ImgPlus<BitType>(bitmask)));
			} catch (IOException exc) {
				KNIPGateway.log()
						.warn("Could not create bitmask for label " + region.toString() + ". Inserted missing cell.");
				cells.add(DataType.getMissingCell());
			}
			cells.add(new StringCell(region.getLabel().toString()));
		}
		if (dependencies != null) {
			final StringBuffer buf = new StringBuffer();
			if (!dependencies.isEmpty()) {
				for (final L s : dependencies.get(region.getLabel())) {
					buf.append(s.toString());
					buf.append(";");
				}

			} else {
				KNIPGateway.log()
						.warn("No overlapping segment found for segment with label: " + region.getLabel().toString());
			}

			cells.add(new StringCell(buf.toString()));
		}
	}
}
