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
package org.knime.knip.base.nodes.proc.imgjep.fun;

import java.util.Stack;

import net.imglib2.img.Img;
import net.imglib2.ops.operation.real.binary.RealAvg;

import org.knime.knip.base.nodes.proc.imgjep.ImgOperationEval;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class JEPAvg extends PostfixMathCommand implements ImgOpEvalRequired {

    private ImgOperationEval m_imgOpEval;

    public JEPAvg() {
        numberOfParameters = -1;
    }

    public Double avg(final Number d1, final Number d2) {
        return new Double((d1.doubleValue() + d2.doubleValue()) / 2.0);
    }

    public Object avg(final Object param1, final Object param2) throws ParseException {
        if ((param1 instanceof Number) && (param2 instanceof Number)) {
            return avg((Number)param1, (Number)param2);
        } else if ((param1 instanceof Img) || (param2 instanceof Img)) {
            return m_imgOpEval.doImgBinaryOperation(param1, param2, new RealAvg());
        }

        throw new ParseException("Invalid parameter type");
    }

    @Override
    public void run(final Stack stack) throws ParseException {
        checkStack(stack);// check the stack

        Object sum = stack.pop();
        Object param;
        int i = 1;

        // repeat summation for each one of the current parameters
        while (i < curNumberOfParameters) {
            // get the parameter from the stack
            param = stack.pop();

            // add it to the sum (order is important for String
            // arguments)
            sum = avg(param, sum);

            i++;
        }

        stack.push(sum);

        return;
    }

    @Override
    public void setImgOperationEvaluator(final ImgOperationEval imgOpEval) {
        m_imgOpEval = imgOpEval;

    }

}
