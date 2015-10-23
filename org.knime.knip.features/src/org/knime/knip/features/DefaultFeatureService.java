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
package org.knime.knip.features;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;
import org.knime.knip.features.groups.ImgLabelingFeatureSetGroup;
import org.knime.knip.features.groups.ImgPlusFeatureSetGroup;
import org.knime.knip.features.groups.PairedFeatureSetGroup;
import org.knime.knip.features.node.model.FeatureSetInfo;
import org.knime.knip.features.sets.FeatureSet;
import org.scijava.module.Module;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import net.imglib2.type.numeric.RealType;

/**
 * Default implementation of {@link FeatureService}
 * 
 * @author Christian Dietz, University of Konstanz
 */
@Plugin(type = FeatureService.class)
public class DefaultFeatureService extends AbstractService implements FeatureService {

	@Override
	public <L> FeatureSetGroup getFeatureGroup(final List<FeatureSetInfo> infos, final int imgColIndex,
			final int labColIdx, final boolean append, final boolean appendOverlappingSegments,
			final boolean appendSegmentInformation, final boolean intersectionMode,
			final RulebasedLabelFilter<L> rulebasedFilter, final ExecutionContext exec,
			final SettingsModelDimSelection dimSelection) {

		// just labeling
		if (imgColIndex == -1 && labColIdx != -1) {
			return new ImgLabelingFeatureSetGroup<>(infos, labColIdx, append, appendOverlappingSegments,
					appendSegmentInformation, intersectionMode, rulebasedFilter, exec, dimSelection);
			// just img
		} else if (imgColIndex != -1 && labColIdx == -1) {
			return new ImgPlusFeatureSetGroup<>(infos, imgColIndex, append, dimSelection);
			// img and labeling
		} else {
			return new PairedFeatureSetGroup<>(infos, imgColIndex, labColIdx, append, appendOverlappingSegments,
					appendSegmentInformation, intersectionMode, rulebasedFilter, exec, dimSelection);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <I, O extends RealType<O>> List<FeatureSet<I, O>> getValidFeatureSets(final Class<?> object,
			final Class<I> type, final List<FeatureSetInfo> infos) {

		final List<FeatureSet<I, O>> validSets = new ArrayList<>();
		for (final FeatureSetInfo info : infos) {
			Module load = info.load();
			@SuppressWarnings("rawtypes")
			final FeatureSet fs = (FeatureSet) load.getDelegateObject();

			// FIXME: isCompatible is just a workaround because I was not able
			// to get the desired information from the generic types, yet.
			if (fs.isCompatible(object, type)) {
				fs.setEnvironment(KNIPGateway.ops());
				validSets.add(fs);
			} else {
				// FIXME: Nicer name for featureset
				KNIPGateway.log().warn("Selected feature set " + load.getInfo().getLabel()
						+ " can't handle the provided input. The feature set will be ignored!");
			}

		}
		return validSets;
	}
}
