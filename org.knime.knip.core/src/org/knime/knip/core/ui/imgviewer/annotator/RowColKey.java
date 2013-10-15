package org.knime.knip.core.ui.imgviewer.annotator;

import java.io.Serializable;

/**
 * Unique identifier for table entries based on their row and column name.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class RowColKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String m_rowName;

    private String m_colName;

    public RowColKey(final String rowName, final String colName) {
        m_rowName = rowName;
        m_colName = colName;
    }

    public String getRowName() {
        return m_rowName;
    }

    public String getColName() {
        return m_colName;
    }

    //auto generated member based hashCode and equals

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_colName == null) ? 0 : m_colName.hashCode());
        result = prime * result + ((m_rowName == null) ? 0 : m_rowName.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RowColKey other = (RowColKey)obj;
        if (m_colName == null) {
            if (other.m_colName != null) {
                return false;
            }
        } else if (!m_colName.equals(other.m_colName)) {
            return false;
        }
        if (m_rowName == null) {
            if (other.m_rowName != null) {
                return false;
            }
        } else if (!m_rowName.equals(other.m_rowName)) {
            return false;
        }
        return true;
    }

}
