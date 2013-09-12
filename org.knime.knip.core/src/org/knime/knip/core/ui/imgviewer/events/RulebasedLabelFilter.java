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
package org.knime.knip.core.ui.imgviewer.events;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.knip.core.data.labeling.LabelFilter;
import org.knime.knip.core.ui.event.KNIPEvent;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class RulebasedLabelFilter<L extends Comparable<L>> implements LabelFilter<L>, Externalizable, KNIPEvent {

    public enum Operator {
        OR, AND, XOR
    }

    private List<String> m_rules;

    private Operator m_op = Operator.OR;

    private List<L> m_tmpLabeling;

    private BitSet m_ruleValidation;

    private Map<L, Set<Integer>> m_validLabels;

    private Set<L> m_invalidLabels;

    public RulebasedLabelFilter(final String[] rules, final Operator op) {

        this();

        if (rules != null) {
            addRules(rules);
        }

        if (op != null) {
            m_op = op;
        }
    }

    @Override
    public ExecutionPriority getExecutionOrder() {
        return ExecutionPriority.NORMAL;
    }

    /**
     * implements object equality {@inheritDoc}
     */
    @Override
    public <E extends KNIPEvent> boolean isRedundant(final E thatEvent) {
        return this.equals(thatEvent);
    }

    public RulebasedLabelFilter() {
        m_tmpLabeling = new ArrayList<L>();
        m_rules = new ArrayList<String>();
        m_ruleValidation = new BitSet();
        m_validLabels = new HashMap<L, Set<Integer>>();
        m_invalidLabels = new HashSet<L>();
    }

    public final boolean addRules(final String... rules) {
        boolean added = false;
        for (final String r : rules) {
            added = m_rules.add(r) || added;
        }

        m_invalidLabels.clear();
        m_validLabels.clear();
        return added;
    }

    @Override
    public int hashCode() {

        int hashCode = 1;

        for (final String rule : m_rules) {
            hashCode *= 31;
            hashCode += rule.hashCode();
        }

        hashCode = (hashCode * 31) + m_op.hashCode();

        return hashCode;
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final int num = in.readInt();
        for (int i = 0; i < num; i++) {
            m_rules.add(in.readUTF());
        }

        m_op = Operator.values()[in.readInt()];
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(m_rules.size());
        for (int i = 0; i < m_rules.size(); i++) {
            out.writeUTF(m_rules.get(i));
        }

        out.writeInt(m_op.ordinal());
    }

    public void setOp(final Operator op) {
        m_validLabels.clear();
        m_invalidLabels.clear();
        m_ruleValidation.clear();
        m_op = op;
    }

    public List<String> getRules() {
        return m_rules;
    }

    public Operator getOp() {
        return m_op;
    }

    public Collection<L> filterLabeling(final Collection<L> labels, final Operator op, final List<String> rules) {

        if (rules.size() == 0) {
            return labels;
        }

        m_ruleValidation.clear();
        m_tmpLabeling.clear();

        for (final L label : labels) {

            if (m_invalidLabels.contains(label)) {
                continue;
            }

            if (m_validLabels.containsKey(label)) {
                m_tmpLabeling.add(label);

                if (op == Operator.OR) {
                    continue;
                } else if (op == Operator.AND) {
                    for (final int i : m_validLabels.get(label)) {
                        m_ruleValidation.set(i);
                    }
                } else if (op == Operator.XOR) {
                    if (m_tmpLabeling.size() > 1) {
                        m_tmpLabeling.clear();
                        return m_tmpLabeling;
                    }
                }

            } else {

                int r = 0;
                final String labelString = label.toString();

                for (final String rule : rules) {

                    if (labelString.matches(rule)) {
                        m_tmpLabeling.add(label);
                        m_validLabels.put(label, new HashSet<Integer>());
                        m_invalidLabels.remove(label);

                        if (op == Operator.OR) {
                            break;
                        }

                        if (op == Operator.XOR) {
                            if (m_tmpLabeling.size() > 1) {
                                m_tmpLabeling.clear();
                                return m_tmpLabeling;
                            }
                        } else if (op == Operator.AND) {
                            m_ruleValidation.set(r);
                            m_validLabels.get(label).add(r);

                        }
                    }
                    r++;
                }

                if (!m_validLabels.containsKey(label)) {
                    m_invalidLabels.add(label);
                }
            }
        }

        switch (op) {
            case AND:
                if (m_ruleValidation.cardinality() != m_rules.size()) {
                    m_tmpLabeling.clear();
                }

        }

        return m_tmpLabeling;
    }

    public static <L extends Comparable<L>> boolean isValid(final L label, final String rule) {
        return label.toString().matches(rule);
    }

    public boolean isValid(final L label) {
        for (final String rule : m_rules) {
            if (label.toString().matches(rule)) {
                return true;
            }
        }
        return m_rules.size() == 0;
    }

    @Override
    public Collection<L> filterLabeling(final Collection<L> labels) {
        return filterLabeling(labels, m_op, m_rules);
    }

    public static String formatRegExp(String rule) {

        rule = rule.trim();
        rule = rule.replaceAll("\\.", "\\\\.");
        rule = rule.replaceAll("[^a-zA-Z0-9*#-|&_?()\t\r\n:\\.\\ ]", "");
        rule = rule.replaceAll("\\*", ".*");
        rule = rule.replaceAll("\\?", ".");
        rule = rule.replaceAll("\\(", "\\\\\\(");
        rule = rule.replaceAll("\\)", "\\\\\\)");

        String regExp = "(";
        regExp += rule;
        regExp += ")";

        return regExp;
    }

    @Override
    public void clear() {
        m_rules.clear();
    }

    public RulebasedLabelFilter<L> copy() {
        return new RulebasedLabelFilter<L>(m_rules.toArray(new String[m_rules.size()]), m_op);
    }

}
