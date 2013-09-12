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

import java.util.HashSet;
import java.util.LinkedList;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.img.UnaryConstantLeftAssignment;
import net.imglib2.ops.img.UnaryConstantRightAssignment;
import net.imglib2.ops.img.UnaryOperationAssignment;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.node.NodeLogger;
import org.nfunk.jep.ParseException;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgOperationEval {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgOperationEval.class);

    private boolean m_errorOccured;

    private final LinkedList<Img<ComplexType<?>>> m_imgBuffer;

    private final ImgFactory m_imgFactory;

    private final RealType<?> m_resultType;

    // references to the images of the table that should not be part of the
    // recycling
    private final HashSet<Img<RealType<?>>> m_tableImages;

    public ImgOperationEval(final RealType<?> resultType, final ImgFactory imgFactory,
                            final HashSet<Img<RealType<?>>> tableImages) {
        m_resultType = resultType;
        m_imgFactory = imgFactory;
        m_tableImages = tableImages;
        m_imgBuffer = new LinkedList<Img<ComplexType<?>>>();
        m_errorOccured = false;
    }

    public Object doImgBinaryOperation(final Object param1, final Object param2, final BinaryOperation binOp)
            throws ParseException {

        Img img1 = null;
        Img img2 = null;
        Img res = null;

        Number num1 = null;
        Number num2 = null;
        if (param1 instanceof Img) {
            img1 = (Img<ComplexType<?>>)param1;
        }
        if (param2 instanceof Img) {
            img2 = (Img<ComplexType<?>>)param2;
        }

        if ((img1 != null) || (img2 != null)) {
            res = getUsableImage(img1 == null ? img2 : img1);
        }

        if (param1 instanceof Number) {
            num1 = (Number)param1;
        }
        if (param2 instanceof Number) {
            num2 = (Number)param2;
        }

        if ((img1 != null) && (img2 != null)) {
            try { // try necessary to catch runtime exceptions that
                  // are silently handled by JEP otherwise
                new BinaryOperationAssignment(binOp).compute(img1, img2, res);
            } catch (final Exception e) {
                handleEception(e);
            }
        }

        if ((img1 != null) && (num2 != null)) {
            try { // try necessary to catch runtime exceptions that
                  // are silently handled by JEP otherwise
                new UnaryConstantRightAssignment(binOp).compute(img1, new DoubleType(num2.doubleValue()), res);
            } catch (final Exception e) {
                handleEception(e);
            }
        }

        if ((img2 != null) && (num1 != null)) {
            try { // try necessary to catch runtime exceptions that
                  // are silently handled by JEP otherwise
                new UnaryConstantLeftAssignment(binOp).compute(new DoubleType(num1.doubleValue()), img2, res);
            } catch (final Exception e) {
                handleEception(e);
            }

        }

        if (res == null) {
            throw new ParseException("Invalid parameter type");
        } else {

            // // copy meta data
            // Img tmp = (img1 == null ? img2 : img1);
            // ImgMetadataImpl meta = new ImgMetadataImpl(tmp);
            //
            // reuses the images if they are not directly from the
            // table
            if ((img1 != null) && !m_tableImages.contains(img1)) {
                reuseImage(img1);
            }
            if ((img2 != null) && !m_tableImages.contains(img2)) {
                reuseImage(img2);
            }

            return res;
        }

    }

    public Object doImgUnaryOperation(final Img<ComplexType<?>> img,
                                      final UnaryOperation<ComplexType<?>, ComplexType<?>> unaryOp) {

        final Img res = getUsableImage(img);
        try { // try necessary to catch runtime exceptions that
              // are silently handled by JEP otherwise
            new UnaryOperationAssignment(unaryOp).compute(img, res);
        } catch (final Exception e) {
            handleEception(e);
        }
        reuseImage(img);

        return res;
    }

    public boolean errorOccured() {
        return m_errorOccured;
    }

    private Img<?> getUsableImage(final Img<ComplexType<?>> img) {
        if (m_imgBuffer.size() > 0) {
            return m_imgBuffer.poll();
        } else {
            return m_imgFactory.create(img, m_resultType);
        }
    }

    private void handleEception(final Exception e) {
        m_errorOccured = true;
        LOGGER.error(e.getMessage());
        e.printStackTrace();
    }

    private boolean reuseImage(final Img<ComplexType<?>> candidateImg) {
        final boolean reusable = !m_tableImages.contains(candidateImg);
        if (reusable) {
            // not an image from the table can be reused
            m_imgBuffer.add(candidateImg);
        }
        return reusable;
    }

}
