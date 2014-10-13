package org.knime.knip.io.nodes.annotation.edit.control;

import java.util.Arrays;

import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;

public class LabelingEditorRowKey extends RowColKey {

	private static final long serialVersionUID = 1L;

	private long[] m_labelDims;

	public LabelingEditorRowKey(String rowName, long[] labelDims) {
		super(rowName, "None");
		m_labelDims = labelDims;
	}

	public long[] getLabelingDims() {
		return m_labelDims;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(m_labelDims);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LabelingEditorRowKey other = (LabelingEditorRowKey) obj;
		if (!Arrays.equals(m_labelDims, other.m_labelDims))
			return false;
		return true;
	}

}
