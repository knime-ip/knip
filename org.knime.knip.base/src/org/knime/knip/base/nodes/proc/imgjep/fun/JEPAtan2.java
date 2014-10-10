/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * ---------------------------------------------------------------------
 *
 * Created on Sep 25, 2014 by seebacher
 */
package org.knime.knip.base.nodes.proc.imgjep.fun;

import java.util.Stack;

import net.imglib2.img.Img;

import org.knime.knip.base.nodes.proc.imgjep.ImgOperationEval;
import org.knime.knip.base.nodes.proc.imgjep.op.RealAtan2;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.ArcTangent2;

/**
 *
 * @author seebacher
 */
public class JEPAtan2 extends ArcTangent2 implements ImgOpEvalRequired {

    private ImgOperationEval m_imgOpEval;

    public JEPAtan2() {
        numberOfParameters = 2;
    }

    @Override
    public void run(final Stack inStack) throws ParseException {
        checkStack(inStack);// check the stack
        Object param2 = inStack.pop();
        Object param1 = inStack.pop();

        if ((param1 instanceof Number) && (param2 instanceof Number)) {
            double y = ((Number)param1).doubleValue();
            double x = ((Number)param2).doubleValue();
            inStack.push(new Double(Math.atan2(y, x)));//push the result on the inStack
        } else if ((param1 instanceof Img) && (param2 instanceof Img)) {
            inStack.push(m_imgOpEval.doImgBinaryOperation(param1, param2, new RealAtan2()));
        } else {
            throw new ParseException("Invalid parameter type");
        }
        return;
    }

    @Override
    public void setImgOperationEvaluator(final ImgOperationEval imgOpEval) {
        m_imgOpEval = imgOpEval;

    }

}
