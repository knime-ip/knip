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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.ops.misc.LabelingDependency;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;
import org.knime.knip.core.util.MiscViews;
import org.knime.knip.features.DataRowUtil;
import org.knime.knip.features.FeaturesGateway;
import org.knime.knip.features.node.model.FeatureSetInfo;
import org.knime.knip.features.sets.FeatureSet;

import net.imagej.ImgPlus;
import net.imagej.ops.slice.SlicesII;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.Operations;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

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
public class PairedFeatureSetGroup<L, T extends RealType<T>, O extends RealType<O>> extends AbstractFeatureSetGroup {

	private final List<FeatureSet<LabelRegion<L>, O>> regionSets;

	private final List<FeatureSet<IterableInterval<T>, O>> iterableSets;

	private final int labdx;

	private final int imgIdx;

	private final boolean append;

	private final boolean appendSegmentInformation;

	private final boolean appendOverlappingSegments;

	private final boolean intersectionMode;

	private final RulebasedLabelFilter<L> labelFilter;
	
	private final RulebasedLabelFilter<L> labelOverlappingFilter;

	private final ExecutionContext exec;

	private final SettingsModelDimSelection dimSelection;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PairedFeatureSetGroup(final List<FeatureSetInfo> infos, final int imgIdx, final int labIdx,
			final boolean append, final boolean appendOverlappingSegments, final boolean appendSegmentInformation,
			final boolean intersectionMode, final RulebasedLabelFilter<L> labelFilter,
			final RulebasedLabelFilter<L> labelOverlappingFilter, final ExecutionContext exec,
			final SettingsModelDimSelection dimSelection) {
		this.regionSets = (List) FeaturesGateway.fs().getValidFeatureSets(LabelRegion.class, Void.class, infos);
		this.iterableSets = (List) FeaturesGateway.fs().getValidFeatureSets(IterableInterval.class, RealType.class,
				infos);
		this.labdx = labIdx;
		this.imgIdx = imgIdx;
		this.append = append;
		this.appendOverlappingSegments = appendOverlappingSegments;
		this.appendSegmentInformation = appendSegmentInformation;
		this.intersectionMode = intersectionMode;
		this.labelFilter = labelFilter;
		this.labelOverlappingFilter = labelOverlappingFilter;
		this.exec = exec;
		this.dimSelection = dimSelection;
	}

	@Override
	public DataTableSpec createSpec(final DataTableSpec inSpec) {

		final HashMap<String, Integer> prefixes = new HashMap<>();

		final List<DataColumnSpec> spec = new ArrayList<>();

		if (append) {
			for (int i = 0; i < inSpec.getNumColumns(); i++) {
				spec.add(inSpec.getColumnSpec(i));
			}
		}

		spec.addAll(createColumnSpec(inSpec, prefixes, regionSets, append, appendOverlappingSegments,
				appendSegmentInformation, dimSelection.getNumSelectedDimLabels()));
		spec.addAll(createColumnSpec(inSpec, prefixes, iterableSets, append, false, false,
				dimSelection.getNumSelectedDimLabels()));

		return new DataTableSpec(spec.toArray(new DataColumnSpec[spec.size()]));
	}

	@Override
	public UnaryComputerOp<DataRow, DataContainer> createComputerOp() {
		final ImgPlusCellFactory imgPlusCellFactory;
		final LabelingDependency<L> dependencyOp;

		if (appendOverlappingSegments) {
			dependencyOp = new LabelingDependency<L>(labelFilter, labelOverlappingFilter, intersectionMode);
		} else {
			dependencyOp = null;
		}

		if (appendSegmentInformation) {
			imgPlusCellFactory = new ImgPlusCellFactory(exec);
		} else {
			imgPlusCellFactory = null;
		}

		final UnaryComputerOp<DataRow, DataContainer> op = new FeatureSetGroupComputer<O>() {

			private boolean isInitialized = false;

			@SuppressWarnings("unchecked")
			@Override
			public void compute1(final DataRow row, final DataContainer container) {

				final DataCell labCell = row.getCell(labdx);
				final DataCell imgPlusCell = row.getCell(imgIdx);

				if (imgPlusCell.isMissing()) {
					KNIPGateway.log().warn("Skipping missing Image at Row: " + row.getKey() + ".");
					return;
				}

				if (labCell.isMissing()) {
					KNIPGateway.log().warn("Skipping missing Labeling at Row: " + row.getKey() + ".");
					return;
				}

				final LabelingValue<L> val = ((LabelingValue<L>) labCell);

				final ImgPlus<T> imgPlus = ((ImgPlusValue<T>) imgPlusCell).getImgPlus();
				RandomAccessibleInterval<LabelingType<L>> labeling = val.getLabeling();

				if (!Intervals.equalDimensions(imgPlus, labeling)) {
					KNIPGateway.log().warn("The dimensions of Labeling and Image in Row " + row.getKey()
							+ " are not compatible. Dimensions of labeling are virtually adjusted to match size.");

					labeling = MiscViews.synchronizeDimensionality(labeling, val.getLabelingMetadata(), imgPlus,
							imgPlus);
				}

				final SlicesII<LabelingType<L>> slicerLab = new SlicesII<>(labeling,
						dimSelection.getSelectedDimIndices(val.getLabelingMetadata()), true);

				final SlicesII<T> slicerImg = new SlicesII<>(imgPlus, dimSelection.getSelectedDimIndices(imgPlus),
						true);

				final Cursor<RandomAccessibleInterval<T>> slicingImgCursor = slicerImg.cursor();
				final Cursor<RandomAccessibleInterval<LabelingType<L>>> slicingLabCursor = slicerLab.cursor();

				boolean slicingActive = (slicerImg.size() == 1);
				while (slicingImgCursor.hasNext() && slicingLabCursor.hasNext()) {
					final RandomAccessibleInterval<LabelingType<L>> sliceLab = ((RandomAccessibleInterval<LabelingType<L>>) slicingLabCursor
							.next());

					final RandomAccessibleInterval<T> sliceImgPlus = ((RandomAccessibleInterval<T>) slicingImgCursor
							.next());

					final Map<L, List<L>> dependencies;
					if (dependencyOp != null) {
						dependencies = Operations.compute(dependencyOp, sliceLab);
					} else {
						dependencies = null;
					}
					final LabelRegions<L> regions = KNIPGateway.regions().regions(sliceLab);

					if (regions.getExistingLabels().size() == 0) {
						continue;
					}

					if (!isInitialized) {
						final LabelRegion<L> region = regions.iterator().next();
						boolean isAllValid = initFeatureSet(iterableSets, Regions.sample(region, sliceImgPlus));
						isAllValid &= initFeatureSet(regionSets, region);
						if (isAllValid) {
							KNIPGateway.log().warn(
									"Not all features can be calculated on the given inputs. Missing cells are inserted!");
						}
						;
						isInitialized = true;
					}

					String intervalDef = null;
					if (!slicingActive) {

						long[] pos = new long[slicingImgCursor.numDimensions()];
						slicingImgCursor.localize(pos);
						intervalDef = " Pos[" + Arrays.toString(pos) + "]";

					}

					final ArrayList<Future<Pair<String, List<DataCell>>>> futures = new ArrayList<>();
					for (final LabelRegion<L> region : regions) {
						if (!labelFilter.getRules().isEmpty() && !labelFilter.isValid(region.getLabel())) {
							continue;
						}

						futures.add(KNIPGateway.threads().run(new Callable<Pair<String, List<DataCell>>>() {

							@Override
							public Pair<String, List<DataCell>> call() throws Exception {
								final List<DataCell> cells = new ArrayList<DataCell>();

								appendRegionOptions(region, cells, imgPlusCellFactory, dependencies, appendOverlappingSegments, ops());

								cells.addAll(computeOnFeatureSets(regionSets, region));
								cells.addAll(computeOnFeatureSets(iterableSets, Regions.sample(region, sliceImgPlus)));

								return new ValuePair<>(region.getLabel().toString(), cells);
							}
						}));
					}

					for (final Future<Pair<String, List<DataCell>>> future : futures) {
						try {
							final Pair<String, List<DataCell>> res = future.get();
							final String newKey = row.getKey().getString() + KNIPConstants.IMGID_LABEL_DELIMITER
									+ res.getA()
									+ (intervalDef != null ? KNIPConstants.IMGID_LABEL_DELIMITER + intervalDef : "");
							container.addRowToTable(append ? DataRowUtil.appendCells(newKey, row, res.getB())
									: new DefaultRow(newKey, res.getB()));
						} catch (InterruptedException | ExecutionException e) {
							KNIPGateway.log().error(e);
						}
					}
				}
			}

		};

		op.setEnvironment(KNIPGateway.ops());
		return op;
	}

}
