package org.knime.knip.featurenode.model;

import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;

/**
 * 
 * @author Daniel Seebacher, University of Konstanz.
 */
public class LabelSettings<L extends Comparable<L>> {

	private final boolean appendLabelSegments;
	private final boolean intersectionMode;
	private final boolean appendSegmentInformation;
	private final RulebasedLabelFilter<L> ruleBasedLabelFilter;

	public LabelSettings(boolean appendLabelSegments, boolean intersectionMode,
			boolean appendSegmentInformation,
			RulebasedLabelFilter<L> ruleBasedLabelFilter) {
		this.appendLabelSegments = appendLabelSegments;
		this.intersectionMode = intersectionMode;
		this.appendSegmentInformation = appendSegmentInformation;
		this.ruleBasedLabelFilter = ruleBasedLabelFilter;
	}

	public boolean isAppendLabelSegments() {
		return appendLabelSegments;
	}

	public boolean isIntersectionMode() {
		return intersectionMode;
	}

	public boolean isAppendSegmentInformation() {
		return appendSegmentInformation;
	}

	public RulebasedLabelFilter<L> getRuleBasedLabelFilter() {
		return ruleBasedLabelFilter;
	}

}
