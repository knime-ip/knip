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
package org.knime.knip.features.sets;

import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Contingent;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;

/**
 * {@link FeatureSet} for {@link ImageMomentFeatureSet}s
 * 
 * @author Christian Dietz, University of Konstanz
 * @param <T>
 * @param <O>
 */
@Plugin(type = FeatureSet.class, label = "Image Moments", description = "<h1> Image Moments </h1> In image processing, computer vision and related fields, an image moment is a certain particular weighted average (moment) of the image pixels' intensities, or a function of such moments, usually chosen to have some attractive property or interpretation. </br>Image moments are useful to describe objects after segmentation. Simple properties of the image which are found via image moments include area (or total intensity), its centroid, and information about its orientation. There are four kind of image moments supported by this feature set <ul> 	<li>Raw Moments</li> 	<li> Central Moments</li> 	<li> Normalized Central Moments</li> 	<li>Hu Moments</li> </ul> For more information see <a href=\"https://en.wikipedia.org/wiki/Image_moment\">Image moment</a>")
public class ImageMomentFeatureSet<T extends RealType<T>, O extends RealType<O>>
		extends AbstractOpRefFeatureSet<IterableInterval<T>, O> implements Contingent, RequireNumDimensions {

	private static final String PKG = "net.imagej.ops.Ops$ImageMoments$";

	@Parameter(required = false, label = "Moment 00", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Moment00") })
	private boolean isMoment00Active = true;

	@Parameter(required = false, label = "Moment 10", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Moment10") })
	private boolean isMoment10Active = true;

	@Parameter(required = false, label = "Moment 01", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Moment01") })
	private boolean isMoment01Active = true;

	@Parameter(required = false, label = "Moment 11", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Moment11") })
	private boolean isMoment11Active = true;

	@Parameter(required = false, label = "Central Moment 00", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment00") })
	private boolean isCentralMoment00Active = true;

	@Parameter(required = false, label = "Central Moment 01", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment01") })
	private boolean isCentralMoment01Active = true;

	@Parameter(required = false, label = "Central Moment 02", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment02") })
	private boolean isCentralMoment02Active = true;

	@Parameter(required = false, label = "Central Moment 03", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment03") })
	private boolean isCentralMoment03Active = true;

	@Parameter(required = false, label = "Central Moment 10", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment10") })
	private boolean isCentralMoment10Active = true;

	@Parameter(required = false, label = "Central Moment 11", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment11") })
	private boolean isCentralMoment11Active = true;

	@Parameter(required = false, label = "Central Moment 12", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment12") })
	private boolean isCentralMoment12Active = true;

	@Parameter(required = false, label = "Central Moment 20", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment20") })
	private boolean isCentralMoment20Active = true;

	@Parameter(required = false, label = "Central Moment 21", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment21") })
	private boolean isCentralMoment21Active = true;

	@Parameter(required = false, label = "Central Moment 30", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "CentralMoment30") })
	private boolean isCentralMoment30Active = true;

	@Parameter(required = false, label = "Normalized CentralMoment 02", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "NormalizedCentralMoment02") })
	private boolean isNormalizedCentralMoment02Active = true;

	@Parameter(required = false, label = "Normalized CentralMoment 03", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "NormalizedCentralMoment03") })
	private boolean isNormalizedCentralMoment03Active = true;

	@Parameter(required = false, label = "Normalized Central Moment 11", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "NormalizedCentralMoment11") })
	private boolean isNormalizedCentralMoment11Active = true;

	@Parameter(required = false, label = "Normalized Central Moment 12", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "NormalizedCentralMoment12") })
	private boolean isNormalizedCentralMoment12Active = true;

	@Parameter(required = false, label = "Normalized CentralMoment 20", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "NormalizedCentralMoment20") })
	private boolean isNormalizedCentralMoment20Active = true;

	@Parameter(required = false, label = "Normalized Central Moment 21", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "NormalizedCentralMoment21") })
	private boolean isNormalizedCentralMoment21Active = true;

	@Parameter(required = false, label = "Normalized Central Moment 30", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "NormalizedCentralMoment30") })
	private boolean isNormalizedCentralMoment30Active = true;

	@Parameter(required = false, label = "Hu Moment 1", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "HuMoment1") })
	private boolean isHuMoment1Active = true;

	@Parameter(required = false, label = "Hu Moment 2", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "HuMoment2") })
	private boolean isHuMoment2Active = true;

	@Parameter(required = false, label = "Hu Moment 3", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "HuMoment3") })
	private boolean isHuMoment3Active = true;

	@Parameter(required = false, label = "Hu Moment 4", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "HuMoment4") })
	private boolean isHuMoment4Active = true;

	@Parameter(required = false, label = "Hu Moment 5", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "HuMoment5") })
	private boolean isHuMoment5Active = true;

	@Parameter(required = false, label = "Hu Moment 6", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "HuMoment6") })
	private boolean isHuMoment6Active = true;

	@Parameter(required = false, label = "Hu Moment 7", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "HuMoment7") })
	private boolean isHuMoment7Active = true;

	private int numDims = -1;

	public ImageMomentFeatureSet() {
		// NB: Empty Constructor
	}

	@Override
	public boolean conforms() {
		return numDims < 0 ? in().numDimensions() == 2 : numDims == 2;
	}

	@Override
	public boolean isCompatible(final Class<?> object, final Class<?> type) {
		return IterableInterval.class.isAssignableFrom(object) && RealType.class.isAssignableFrom(type);
	}

	@Override
	public void setNumDimensions(int numDims) {
		this.numDims = numDims;
	}
}
