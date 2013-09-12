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
package org.knime.knip.base.nodes.proc.imgjep;

import net.imglib2.img.Img;
import net.imglib2.ops.operation.real.binary.RealAdd;
import net.imglib2.ops.operation.real.binary.RealDivide;
import net.imglib2.ops.operation.real.binary.RealMultiply;
import net.imglib2.ops.operation.real.binary.RealPower;
import net.imglib2.ops.operation.real.binary.RealSubtract;
import net.imglib2.ops.operation.real.unary.RealInvert;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.base.nodes.proc.imgjep.fun.ImgOpEvalRequired;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.Add;
import org.nfunk.jep.function.Divide;
import org.nfunk.jep.function.Multiply;
import org.nfunk.jep.function.Power;
import org.nfunk.jep.function.Subtract;
import org.nfunk.jep.function.UMinus;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgJEP extends JEP implements ImgOpEvalRequired {

    private class ImgOperatorSet extends OperatorSet {

        @Override
        public Operator getAdd() {
            return new Operator("+", new Add() {
                @Override
                public Object add(final Object param1, final Object param2) throws ParseException {
                    if ((param1 instanceof Number) && (param2 instanceof Number)) {
                        return super.add((Number)param1, (Number)param2);
                    } else if ((param1 instanceof Img) || (param2 instanceof Img)) {
                        return m_imgOpEval.doImgBinaryOperation(param1, param2, new RealAdd());
                    }

                    throw new ParseException("Invalid parameter type");

                }
            });

        }

        @Override
        public Operator getAnd() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getAssign() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getCross() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getDivide() {
            return new Operator("/", new Divide() {
                @Override
                public Object div(final Object param1, final Object param2) throws ParseException {

                    if ((param1 instanceof Number) && (param2 instanceof Number)) {
                        return super.div((Number)param1, (Number)param2);
                    } else if ((param1 instanceof Img) || (param2 instanceof Img)) {
                        return m_imgOpEval.doImgBinaryOperation(param1, param2, new RealDivide());
                    }

                    throw new ParseException("Invalid parameter type");

                }
            });
        }

        @Override
        public Operator getDot() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getElement() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getEQ() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getGE() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getGT() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getLE() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getList() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getLT() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getMod() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getMultiply() {
            return new Operator("*", new Multiply() {
                @Override
                public Object mul(final Object param1, final Object param2) throws ParseException {
                    if ((param1 instanceof Number) && (param2 instanceof Number)) {
                        return super.mul((Number)param1, (Number)param2);
                    } else if ((param1 instanceof Img) || (param2 instanceof Img)) {
                        return m_imgOpEval.doImgBinaryOperation(param1, param2, new RealMultiply());
                    }

                    throw new ParseException("Invalid parameter type");

                }
            });
        }

        @Override
        public Operator getNE() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getNot() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getOr() {
            throw new IllegalArgumentException("Unknown operation");
        }

        @Override
        public Operator getPower() {
            return new Operator("^", new Power() {
                @Override
                public Object power(final Object param1, final Object param2) throws ParseException {

                    if ((param1 instanceof Number) && (param2 instanceof Number)) {
                        return super.power((Number)param1, (Number)param2);
                    } else if ((param1 instanceof Img) || (param2 instanceof Img)) {
                        return m_imgOpEval.doImgBinaryOperation(param1, param2, new RealPower());
                    }

                    throw new ParseException("Invalid parameter type");

                }
            });
        }

        @Override
        public Operator getSubtract() {
            return new Operator("-", new Subtract() {
                @Override
                public Object sub(final Object param1, final Object param2) throws ParseException {

                    if ((param1 instanceof Number) && (param2 instanceof Number)) {
                        return super.sub((Number)param1, (Number)param2);
                    } else if ((param1 instanceof Img) || (param2 instanceof Img)) {
                        return m_imgOpEval.doImgBinaryOperation(param1, param2, new RealSubtract());
                    }

                    throw new ParseException("Invalid parameter type");

                }
            });
        }

        @Override
        public Operator getUMinus() {
            return new Operator("-", new UMinus() {
                @Override
                public Object umin(final Object param) throws ParseException {

                    if (param instanceof Number) {
                        return super.umin(param);
                    } else if ((param instanceof Img) && (((Img)param).firstElement() instanceof RealType)) {
                        final RealType firstElement = (RealType)((Img)param).firstElement();
                        final double min = firstElement.getMinValue();
                        final double max = firstElement.getMaxValue();

                        return m_imgOpEval.doImgUnaryOperation((Img)param, new RealInvert(min, max));
                    }

                    throw new ParseException("Invalid parameter type");

                }
            });
        }

    }

    private ImgOperationEval m_imgOpEval;

    public ImgJEP() {
        super();
        opSet = new ImgOperatorSet();
    }

    @Override
    public void setImgOperationEvaluator(final ImgOperationEval imgOpEval) {
        m_imgOpEval = imgOpEval;
        for (final Object fun : funTab.values()) {
            if (fun instanceof ImgOpEvalRequired) {
                ((ImgOpEvalRequired)fun).setImgOperationEvaluator(imgOpEval);
            }
        }
    }

}
