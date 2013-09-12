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

/**
 * Functions that are available in JEP, shown in the "Mathematical Function" list in the dialog.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Bernd Wiswedel, University of Konstanz
 */
enum ImgJEPFunction {

    /** Absolute Value / Magnitude. */
    abs {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "Absolute Value / Magnitude";
        }
    },

    /** average in argument list. */
    average {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "average in argument list: avg(x, y)";
        }

        /** {@inheritDoc} */
        @Override
        int getNrArgs() {
            return 2;
        }
    },

    div {
        @Override
        String getDescription() {
            return "division operation (infix notation)";
        }

        @Override
        String getFunctionName() {
            return "/";
        }

        @Override
        int getNrArgs() {
            return 2;
        }

        @Override
        boolean isInfixOperation() {
            return true;
        }
    },

    /** Exponential. */
    exp {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "Exponential";
        }
    },
    /** Logarithm */
    log {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "Logarithm";
        }
    },
    // /** Random number (between 0 and 1). */
    // rand {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Random number (between 0 and 1)";
    // }
    //
    // /** {@inheritDoc} */
    // @Override
    // int getNrArgs() {
    // return 0;
    // }
    // },
    // /** Modulus. */
    // mod {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Modulus";
    // }
    //
    // /** {@inheritDoc} */
    // @Override
    // int getNrArgs() {
    // return 2;
    // }
    // },
    // /** If-Condition. */
    // iff {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "If-Condition: if (cond, trueval, falseval)";
    // }
    //
    // /** {@inheritDoc} */
    // @Override
    // int getNrArgs() {
    // return 3;
    // }
    //
    // /** {@inheritDoc} */
    // @Override
    // String getFunctionName() {
    // return "if";
    // }
    // },
    // /** Round. */
    // round {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Round (to integer)";
    // }
    // },
    // /** round(value, precision). */
    // round_precision {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Round: round(value, precision)";
    // }
    //
    // /** {@inheritDoc} */
    // @Override
    // int getNrArgs() {
    // return 2;
    // }
    //
    // /** {@inheritDoc} */
    // @Override
    // String getFunctionName() {
    // return "round";
    // }
    // },
    // /** Ceil. */
    // ceil {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Smallest integer above the "
    // + "number; e.g. ceil(pi) returns 4";
    // }
    // },
    // /** Floor. */
    // floor {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Largest integer below the "
    // + "number; e.g. floor(pi) returns 3";
    // }
    // },
    // /** Binomial coefficients. */
    // binom {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Binomial coefficients";
    // }
    //
    // /** {@inheritDoc} */
    // @Override
    // int getNrArgs() {
    // return 2;
    // }
    // },
    // /** Sine. */
    // sin {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Sine";
    // }
    // },
    // /** Cosine. */
    // cos {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Cosine";
    // }
    // },
    // /** Tangent. */
    // tan {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Tangent";
    // }
    // },
    // /** Arc Sine. */
    // asin {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Arc Sine";
    // }
    // },
    // /** Arc Cosine. */
    // acos {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Arc Cosine";
    // }
    // },
    // /** Arc Tangent. */
    // atan {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Arc Tangent";
    // }
    // },
    // /** Arc Tangent 2 parameters. */
    // atan2 {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Arc Tangent (with 2 parameters)";
    // }
    // },
    // /** Hyperbolic Sine. */
    // sinh {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Hyperbolic Sine";
    // }
    // },
    // /** Hyperbolic Cosine. */
    // cosh {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Hyperbolic Cosine";
    // }
    // },
    // /** Hyperbolic Tangent. */
    // tanh {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Hyperbolic Tangent";
    // }
    // },
    // /** Inverse Hyperbolic Sine. */
    // asinh {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Inverse Hyperbolic Sine";
    // }
    // },
    // /** Inverse Hyperbolic Cosine. */
    // acosh {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Inverse Hyperbolic Cosine";
    // }
    // },
    // /** Inverse Hyperbolic Tangent. */
    // atanh {
    // /** {@inheritDoc} */
    // @Override
    // String getDescription() {
    // return "Inverse Hyperbolic Tangent";
    // }
    // },
    /** maximum in argument list. */
    max {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "maximum in argument list: max(x, y)";
        }

        /** {@inheritDoc} */
        @Override
        int getNrArgs() {
            return 2;
        }
    },

    /** minimum in argument list. */
    min {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "minimum in argument list: min(x, y)";
        }

        /** {@inheritDoc} */
        @Override
        int getNrArgs() {
            return 2;
        }
    },
    mul {
        @Override
        String getDescription() {
            return "multiplication operation (infix notation)";
        }

        @Override
        String getFunctionName() {
            return "*";
        }

        @Override
        int getNrArgs() {
            return 2;
        }

        @Override
        boolean isInfixOperation() {
            return true;
        }
    },
    neg {
        @Override
        String getDescription() {
            return "negative sign operation (prefix notation)";
        }

        @Override
        String getFunctionName() {
            return "-";
        }

        @Override
        int getNrArgs() {
            return 1;
        }

        @Override
        boolean isInfixOperation() {
            return false;
        }
    },
    /*********** basic operations *************/
    plus {
        @Override
        String getDescription() {
            return "plus operation (infix notation)";
        }

        @Override
        String getFunctionName() {
            return "+";
        }

        @Override
        int getNrArgs() {
            return 2;
        }

        @Override
        boolean isInfixOperation() {
            return true;
        }
    },
    pow {
        @Override
        String getDescription() {
            return "powern operation (infix notation)";
        }

        @Override
        String getFunctionName() {
            return "^";
        }

        @Override
        int getNrArgs() {
            return 2;
        }

        @Override
        boolean isInfixOperation() {
            return true;
        }
    },
    /** Square Root. */
    sqrt {
        /** {@inheritDoc} */
        @Override
        String getDescription() {
            return "Square Root";
        }
    },
    sub {
        @Override
        String getDescription() {
            return "substraction operation (infix notation)";
        }

        @Override
        String getFunctionName() {
            return "-";
        }

        @Override
        int getNrArgs() {
            return 2;
        }

        @Override
        boolean isInfixOperation() {
            return true;
        }
    };

    /** @return short description for function (tooltip). */
    abstract String getDescription();

    /** @return The function full name (incl. arguments) */
    String getFunctionFullName() {
        StringBuilder b;
        if (isInfixOperation()) {
            b = new StringBuilder();
            b.append("x");
            b.append(getFunctionName());
            b.append("y");
        } else {
            b = new StringBuilder(getFunctionName());
            b.append('(');
            for (int i = 0; i < getNrArgs(); i++) {
                b.append(i > 0 ? ", " : "");
                b.append((char)('x' + i));
            }
            b.append(')');
        }
        return b.toString();
    }

    /** @return The function short name. */
    String getFunctionName() {
        return toString();
    }

    /**
     * Number of arguments for this function (just used in dialog).
     * 
     * @return Number of default arguments.
     */
    int getNrArgs() {
        return 1;
    }

    /**
     * @return true if the operation is notated like "x+y"
     */
    boolean isInfixOperation() {
        return false;
    }

}
