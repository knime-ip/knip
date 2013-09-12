/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.knime.knip.core.data.algebra;

/*************************************************************************
 * Compilation: javac Complex.java Execution: java Complex
 * 
 * Data type for complex numbers.
 * 
 * The data type is "immutable" so once you create and initialize a Complex object, you cannot change it. The "final"
 * keyword when declaring re and im enforces this rule, making it a compile-time error to change the .re or .im fields
 * after they've been initialized.
 * 
 * % java Complex a = 5.0 + 6.0i b = -3.0 + 4.0i Re(a) = 5.0 Im(a) = 6.0 b + a = 2.0 + 10.0i a - b = 8.0 + 2.0i a * b =
 * -39.0 + 2.0i b * a = -39.0 + 2.0i a / b = 0.36 - 1.52i (a / b) * b = 5.0 + 6.0i conj(a) = 5.0 - 6.0i |a| =
 * 7.810249675906654 tan(a) = -6.685231390246571E-6 + 1.0000103108981198i
 * 
 *************************************************************************/

public class Complex {
    private final double m_re; // the real part

    private final double m_im; // the imaginary part

    /** the maximum value that can be stored in an unsigned byte as integer. */
    private static final int UNSIGNEDBYTE_MAX = (1 << Byte.SIZE) - 1;

    // create a new object with the given real and imaginary parts
    public Complex(final double real, final double imag) {
        m_re = real;
        m_im = imag;
    }

    // return a string representation of the invoking Complex object
    @Override
    public String toString() {
        if (m_im == 0) {
            return m_re + "";
        }
        if (m_re == 0) {
            return m_im + "i";
        }
        if (m_im < 0) {
            return m_re + " - " + (-m_im) + "i";
        }
        return m_re + " + " + m_im + "i";
    }

    // return abs/modulus/magnitude and angle/phase/argument
    public double abs() {
        return Math.hypot(m_re, m_im);
    } // Math.sqrt(re*re + im*im)

    public double phase() {
        return Math.atan2(m_im, m_re);
    } // between -pi and pi

    // return a new Complex object whose value is (this + b)
    public Complex plus(final Complex b) {
        final Complex a = this; // invoking object
        final double real = a.m_re + b.m_re;
        final double imag = a.m_im + b.m_im;
        return new Complex(real, imag);
    }

    // return a new Complex object whose value is (this - b)
    public Complex minus(final Complex b) {
        final Complex a = this;
        final double real = a.m_re - b.m_re;
        final double imag = a.m_im - b.m_im;
        return new Complex(real, imag);
    }

    // return a new Complex object whose value is (this * b)
    public Complex times(final Complex b) {
        final Complex a = this;
        final double real = (a.m_re * b.m_re) - (a.m_im * b.m_im);
        final double imag = (a.m_re * b.m_im) + (a.m_im * b.m_re);
        return new Complex(real, imag);
    }

    // scalar multiplication
    // return a new object whose value is (this * alpha)
    public Complex times(final double alpha) {
        return new Complex(alpha * m_re, alpha * m_im);
    }

    // return a new Complex object whose value is the conjugate of this
    public Complex conjugate() {
        return new Complex(m_re, -m_im);
    }

    // return a new Complex object whose value is the reciprocal of this
    public Complex reciprocal() {
        final double scale = (m_re * m_re) + (m_im * m_im);
        return new Complex(m_re / scale, -m_im / scale);
    }

    // return the real or imaginary part
    public double re() {
        return m_re;
    }

    public double im() {
        return m_im;
    }

    // return a / b
    public Complex divides(final Complex b) {
        final Complex a = this;
        return a.times(b.reciprocal());
    }

    // return a new Complex object whose value is the complex exponential of
    // this
    public Complex exp() {
        return new Complex(Math.exp(m_re) * Math.cos(m_im), Math.exp(m_re) * Math.sin(m_im));
    }

    // return a new Complex object whose value is the complex sine of this
    public Complex sin() {
        return new Complex(Math.sin(m_re) * Math.cosh(m_im), Math.cos(m_re) * Math.sinh(m_im));
    }

    // return a new Complex object whose value is the complex cosine of this
    public Complex cos() {
        return new Complex(Math.cos(m_re) * Math.cosh(m_im), -Math.sin(m_re) * Math.sinh(m_im));
    }

    // return a new Complex object whose value is the complex tangent of
    // this
    public Complex tan() {
        return sin().divides(cos());
    }

    // a static version of plus
    public static Complex plus(final Complex a, final Complex b) {
        final double real = a.m_re + b.m_re;
        final double imag = a.m_im + b.m_im;
        final Complex sum = new Complex(real, imag);
        return sum;
    }

    public static Complex[] makeComplexVector(final double[] signal) {
        final int M = signal.length;
        final Complex[] g = new Complex[M];
        for (int i = 0; i < M; i++) {
            g[i] = new Complex(signal[i], 0);
        }
        return g;
    }

    public static Complex[] makeComplexVector(final int[] signal) {
        final int M = signal.length;
        final Complex[] g = new Complex[M];
        for (int i = 0; i < M; i++) {
            g[i] = new Complex(signal[i], 0);
        }
        return g;
    }

    public static Complex[] makeComplexVector(final byte[] signal) {
        final int M = signal.length;
        final Complex[] g = new Complex[M];
        for (int i = 0; i < M; i++) {
            g[i] = new Complex(UNSIGNEDBYTE_MAX & signal[i], 0);
        }
        return g;
    }

    public static Complex[] makeComplexVector(final double[] real, final double[] imag) {
        final int M = real.length;
        final Complex[] g = new Complex[M];
        for (int i = 0; i < M; i++) {
            g[i] = new Complex(real[i], imag[i]);
        }
        return g;
    }

    public double getMagnitude() {
        return Math.sqrt((m_re * m_re) + (m_im * m_im));
    }

    public static void printComplexVector(final Complex[] g, final String title) {
        System.out.println("Printing " + title);
        for (int i = 0; i < g.length; i++) {
            if (g[i] == null) {
                System.out.println(i + ": ******");
            } else {
                double gr = g[i].m_re;
                double gi = g[i].m_im;
                gr = (Math.rint(gr * 1000) / 1000);
                gi = (Math.rint(gi * 1000) / 1000);
                if (gi >= 0) {
                    System.out.println(i + ": " + gr + " + " + Math.abs(gi) + "i");
                } else {
                    System.out.println(i + ": " + gr + " - " + Math.abs(gi) + "i");
                }
            }
        }
    }

    // sample client for testing
    public static void main(final String[] args) {
        final Complex a = new Complex(5.0, 6.0);
        final Complex b = new Complex(-3.0, 4.0);

        System.out.println("a            = " + a);
        System.out.println("b            = " + b);
        System.out.println("Re(a)        = " + a.re());
        System.out.println("Im(a)        = " + a.im());
        System.out.println("b + a        = " + b.plus(a));
        System.out.println("a - b        = " + a.minus(b));
        System.out.println("a * b        = " + a.times(b));
        System.out.println("b * a        = " + b.times(a));
        System.out.println("a / b        = " + a.divides(b));
        System.out.println("(a / b) * b  = " + a.divides(b).times(b));
        System.out.println("conj(a)      = " + a.conjugate());
        System.out.println("|a|          = " + a.abs());
        System.out.println("tan(a)       = " + a.tan());
    }

}
