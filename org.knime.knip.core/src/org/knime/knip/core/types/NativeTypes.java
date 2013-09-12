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
package org.knime.knip.core.types;

import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.Unsigned12BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public enum NativeTypes {

    /**
     *
     */
    BITTYPE(false),

    /**
     *
     */
    BYTETYPE(true),
    /**
     *
     */
    SHORTTYPE(true),
    /**
     *
     */
    INTTYPE(true),
    /**
     *
     */
    LONGTYPE(true),
    /**
     *
     */
    DOUBLETYPE(true),
    /**
     *
     */
    FLOATTYPE(true),
    /**
     *
     */
    UNSIGNED12BITTYPE(false),
    /**
     *
     */
    UNSIGNEDBYTETYPE(false),
    /**
     *
     */
    UNSIGNEDINTTYPE(false),
    /**
     *
     */
    UNSIGNEDSHORTTYPE(false);

    // indicates if type is signed
    private boolean m_isSigned;

    public final Type<?> getTypeInstance() {
        return NativeTypes.getTypeInstance(this);
    }

    private NativeTypes(final boolean isSigned) {
        m_isSigned = isSigned;
    }

    public static final Type<?> getTypeInstance(final NativeTypes type) {
        switch (type) {
            case BITTYPE:
                return new BitType();
            case BYTETYPE:
                return new ByteType();
            case DOUBLETYPE:
                return new DoubleType();
            case FLOATTYPE:
                return new FloatType();
            case INTTYPE:
                return new IntType();
            case LONGTYPE:
                return new LongType();
            case SHORTTYPE:
                return new ShortType();
            case UNSIGNED12BITTYPE:
                return new Unsigned12BitType();
            case UNSIGNEDBYTETYPE:
                return new UnsignedByteType();
            case UNSIGNEDINTTYPE:
                return new UnsignedIntType();
            case UNSIGNEDSHORTTYPE:
                return new UnsignedShortType();
            default:
                return null;
        }
    }

    public static final NativeTypes getPixelType(final Object val) {
        if (val instanceof BitType) {
            return BITTYPE;
        } else if (val instanceof ByteType) {
            return BYTETYPE;
        } else if (val instanceof IntType) {
            return INTTYPE;
        } else if (val instanceof ShortType) {
            return SHORTTYPE;
        } else if (val instanceof FloatType) {
            return FLOATTYPE;
        } else if (val instanceof DoubleType) {
            return DOUBLETYPE;
        } else if (val instanceof LongType) {
            return LONGTYPE;
        } else if (val instanceof UnsignedByteType) {
            return UNSIGNEDBYTETYPE;
        } else if (val instanceof UnsignedIntType) {
            return UNSIGNEDINTTYPE;
        } else if (val instanceof UnsignedShortType) {
            return UNSIGNEDSHORTTYPE;
        } else if (val instanceof Unsigned12BitType) {
            return NativeTypes.UNSIGNED12BITTYPE;
        } else {
            return null;
        }
    }

    public boolean isSigned() {
        return m_isSigned;
    }

    public static NativeTypes[] intTypeValues() {
        return new NativeTypes[]{BITTYPE, BYTETYPE, SHORTTYPE, INTTYPE, UNSIGNEDBYTETYPE, UNSIGNEDINTTYPE,
                UNSIGNEDSHORTTYPE, LONGTYPE};
    }
}
