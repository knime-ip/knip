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
package org.knime.knip.core.ops.misc;

import net.imglib2.histogram.Histogram1d;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.knip.core.algorithm.types.ThresholdingType;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class FindThreshold<T extends RealType<T>> implements UnaryOperation<Histogram1d<T>, DoubleType> {

    private int m_maxValue;

    private final ThresholdingType m_ttype;

    private final T m_type;

    public FindThreshold(final ThresholdingType ttype, final T type) {
        m_ttype = ttype;
        m_type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DoubleType compute(final Histogram1d<T> hist, final DoubleType r) {
        if (hist.getBinCount() > Integer.MAX_VALUE) {
            throw new RuntimeException("to many histogram bins can't allocate a big enought array.");
        }

        //testing for histograms with only one filled bin
        int filled = 0;
        for (Long binVal : hist.toLongArray()) {
            if (binVal > 0) {
                filled++;
            }
        }
        if (filled == 1) {
            //only one bin contains content
            r.setReal(m_type.getMaxValue());
            return r;
        }

        m_maxValue = (int)hist.getBinCount() - 1;
        int bin = 0;
        if (m_ttype == ThresholdingType.HUANG) {
            bin = Huang(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.INTERMODES) {
            bin = Intermodes(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.ISODATA) {
            bin = IsoData(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.LI) {
            bin = Li(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.MAXENTROPY) {
            bin = MaxEntropy(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.MEAN) {
            bin = Mean(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.MINERROR) {
            bin = MinErrorI(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.MINIMUM) {
            bin = Minimum(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.MOMENTS) {
            bin = Moments(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.OTSU) {
            bin = Otsu(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.PERCENTILE) {
            bin = Percentile(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.RENYIENTROPY) {
            bin = RenyiEntropy(hist.toLongArray());
        } else if ((m_ttype == ThresholdingType.SHANBAG)) {
            bin = Shanbhag(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.TRIANGLE) {
            bin = Triangle(hist.toLongArray());
        } else if (m_ttype == ThresholdingType.YEN) {
            bin = Yen(hist.toLongArray());
        } else {
            bin = -Integer.MAX_VALUE;
        }

        if (bin < 0) {
            //an error occurred e.g. signaled by returning -1
            throw new RuntimeException("thresholding method " + m_ttype + " failed.");
        }

        hist.getCenterValue(bin, m_type);
        r.setReal(m_type.getRealDouble());

        return r;
    }

    /**
     * @param data histogramm
     * @return
     */
    private int Huang(final long[] data) {
        // Implements Huang's fuzzy thresholding method
        // Uses Shannon's entropy function (one can also use Yager's
        // entropy
        // function)
        // Huang L.-K. and Wang M.-J.J. (1995) "Image Thresholding by
        // Minimizing
        // the Measures of Fuzziness" Pattern Recognition, 28(1): 41-51
        // M. Emre Celebi 06.15.2007
        // Ported to ImageJ plugin by G. Landini from E Celebi's
        // fourier_0.8
        // routines
        int threshold = -1;
        int ih, it;
        int first_bin;
        int last_bin;
        int sum_pix;
        int num_pix;
        double term;
        double ent; // entropy
        double min_ent; // min entropy
        double mu_x;

        /* Determine the first non-zero bin */
        first_bin = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            if (data[ih] != 0) {
                first_bin = ih;
                break;
            }
        }

        /* Determine the last non-zero bin */
        last_bin = m_maxValue;
        for (ih = m_maxValue; ih >= first_bin; ih--) {
            if (data[ih] != 0) {
                last_bin = ih;
                break;
            }
        }
        term = 1.0 / (last_bin - first_bin);
        final double[] mu_0 = new double[m_maxValue + 1];
        sum_pix = num_pix = 0;
        for (ih = first_bin; ih < (m_maxValue + 1); ih++) {
            sum_pix += ih * data[ih];
            num_pix += data[ih];
            /* NUM_PIX cannot be zero ! */
            mu_0[ih] = sum_pix / (double)num_pix;
        }

        final double[] mu_1 = new double[m_maxValue + 1];
        sum_pix = num_pix = 0;
        for (ih = last_bin; ih > 0; ih--) {
            sum_pix += ih * data[ih];
            num_pix += data[ih];
            /* NUM_PIX cannot be zero ! */
            mu_1[ih - 1] = sum_pix / (double)num_pix;
        }

        /* Determine the threshold that minimizes the fuzzy entropy */
        threshold = -1;
        min_ent = Double.MAX_VALUE;
        for (it = 0; it < (m_maxValue + 1); it++) {
            ent = 0.0;
            for (ih = 0; ih <= it; ih++) {
                /* Equation (4) in Ref. 1 */
                mu_x = 1.0 / (1.0 + (term * Math.abs(ih - mu_0[it])));
                if (!((mu_x < 1e-06) || (mu_x > 0.999999))) {
                    /* Equation (6) & (8) in Ref. 1 */
                    ent += data[ih] * ((-mu_x * Math.log(mu_x)) - ((1.0 - mu_x) * Math.log(1.0 - mu_x)));
                }
            }

            for (ih = it + 1; ih < (m_maxValue + 1); ih++) {
                /* Equation (4) in Ref. 1 */
                mu_x = 1.0 / (1.0 + (term * Math.abs(ih - mu_1[it])));
                if (!((mu_x < 1e-06) || (mu_x > 0.999999))) {
                    /* Equation (6) & (8) in Ref. 1 */
                    ent += data[ih] * ((-mu_x * Math.log(mu_x)) - ((1.0 - mu_x) * Math.log(1.0 - mu_x)));
                }
            }
            /* No need to divide by NUM_ROWS * NUM_COLS * LOG(2) ! */
            if (ent < min_ent) {
                min_ent = ent;
                threshold = it;
            }
        }
        return threshold;
    }

    private static boolean bimodalTest(final double[] y) {
        final int len = y.length;
        boolean b = false;
        int modes = 0;

        for (int k = 1; k < (len - 1); k++) {
            if ((y[k - 1] < y[k]) && (y[k + 1] < y[k])) {
                modes++;
                if (modes > 2) {
                    return false;
                }
            }
        }
        if (modes == 2) {
            b = true;
        }
        return b;
    }

    private int Intermodes(final long[] data) {
        // J. M. S. Prewitt and M. L. Mendelsohn,
        // "The analysis of cell images,"
        // in
        // Annals of the New York Academy of Sciences, vol. 128, pp.
        // 1035-1053,
        // 1966.
        // ported to ImageJ plugin by G.Landini from Antti Niemisto's
        // Matlab
        // code (GPL)
        // Original Matlab code Copyright (C) 2004 Antti Niemisto
        // See http://www.cs.tut.fi/~ant/histthresh/ for an excellent
        // slide
        // presentation
        // and the original Matlab code.
        //
        // Assumes a bimodal histogram. The histogram needs is smoothed
        // (using a
        // running average of size 3, iteratively) until there are only
        // two
        // local maxima.
        // j and k
        // Threshold t is (j+k)/2.
        // Images with histograms having extremely unequal peaks or a
        // broad and
        // flat valley are unsuitable for this method.
        double[] iHisto = new double[m_maxValue + 1];
        int iter = 0;
        int threshold = -1;
        for (int i = 0; i < (m_maxValue + 1); i++) {
            iHisto[i] = data[i];
        }

        final double[] tHisto = iHisto;

        while (!bimodalTest(iHisto)) {
            // smooth with a 3 point running mean filter
            for (int i = 1; i < m_maxValue; i++) {
                tHisto[i] = (iHisto[i - 1] + iHisto[i] + iHisto[i + 1]) / 3;
            }
            tHisto[0] = (iHisto[0] + iHisto[1]) / 3; // 0 outside
            tHisto[m_maxValue] = (iHisto[254] + iHisto[m_maxValue]) / 3; // 0
            // outside
            iHisto = tHisto;
            iter++;
            if (iter > 10000) {
                threshold = -1;
                return threshold;
            }
        }

        // The threshold is the mean between the two peaks.
        int tt = 0;
        for (int i = 1; i < m_maxValue; i++) {
            if ((iHisto[i - 1] < iHisto[i]) && (iHisto[i + 1] < iHisto[i])) {
                tt += i;
            }
        }
        threshold = (int)Math.floor(tt / 2.0);
        return threshold;
    }

    private int IsoData(final long[] data) {
        // Also called intermeans
        // Iterative procedure based on the isodata algorithm [T.W.
        // Ridler, S.
        // Calvard, Picture
        // thresholding using an iterative selection method, IEEE Trans.
        // System,
        // Man and
        // Cybernetics, SMC-8 (1978) 630-632.]
        // The procedure divides the image into objects and background
        // by taking
        // an initial threshold,
        // then the averages of the pixels at or below the threshold and
        // pixels
        // above are computed.
        // The averages of those two values are computed, the threshold
        // is
        // incremented and the
        // process is repeated until the threshold is larger than the
        // composite
        // average. That is,
        // threshold = (average background + average objects)/2
        // The code in ImageJ that implements this function is the
        // getAutoThreshold() method in the ImageProcessor class.
        //
        // From: Tim Morris (dtm@ap.co.umist.ac.uk)
        // Subject: Re: Thresholding method?
        // posted to sci.image.processing on 1996/06/24
        // The algorithm implemented in NIH Image sets the threshold as
        // that
        // grey
        // value, G, for which the average of the averages of the grey
        // values
        // below and above G is equal to G. It does this by initialising
        // G to
        // the
        // lowest sensible value and iterating:

        // L = the average grey value of pixels with intensities < G
        // H = the average grey value of pixels with intensities > G
        // is G = (L + H)/2?
        // yes => exit
        // no => increment G and repeat
        //
        // There is a discrepancy with IJ because they are slightly
        // different
        // methods
        int i, h, g = 0;
        long totl, l, toth;
        for (i = 1; i < (m_maxValue + 1); i++) {
            if (data[i] > 0) {
                g = i + 1;
                break;
            }
        }
        while (true) {
            l = 0;
            totl = 0;
            for (i = 0; i < g; i++) {
                totl = totl + data[i];
                l = l + (data[i] * i);
            }
            h = 0;
            toth = 0;
            for (i = g + 1; i < (m_maxValue + 1); i++) {
                toth += data[i];
                h += (data[i] * i);
            }
            if ((totl > 0) && (toth > 0)) {
                l /= totl;
                h /= toth;
                if (g == (int)Math.round((l + h) / 2.0)) {
                    break;
                }
            }
            g++;
            if (g > (m_maxValue - 1)) { // was g > 254
                return -1;
            }
        }
        return g;
    }

    private int Li(final long[] data) {
        // Implements Li's Minimum Cross Entropy thresholding method
        // This implementation is based on the iterative version (Ref.
        // 2) of the
        // algorithm.
        // 1) Li C.H. and Lee C.K. (1993)
        // "Minimum Cross Entropy Thresholding"
        // Pattern Recognition, 26(4): 617-625
        // 2) Li C.H. and Tam P.K.S. (1998) "An Iterative Algorithm for
        // Minimum
        // Cross Entropy Thresholding"Pattern Recognition Letters,
        // 18(8):
        // 771-776
        // 3) Sezgin M. and Sankur B. (2004) "Survey over Image
        // Thresholding
        // Techniques and Quantitative Performance Evaluation" Journal
        // of
        // Electronic Imaging, 13(1): 146-165
        // http://citeseer.ist.psu.edu/sezgin04survey.html
        // Ported to ImageJ plugin by G.Landini from E Celebi's
        // fourier_0.8
        // routines
        int threshold;
        int ih;
        int numPixels;
        int sumBack; /*
                     * sum of the background pixels at a given
                     * threshold
                     */
        int sumObj; /* sum of the object pixels at a given threshold */
        int numBack; /* number of background pixels at a given threshold */
        int numObj; /* number of object pixels at a given threshold */
        double oldThresh;
        double newThresh;
        double meanBack; /*
                         * mean of the background pixels at a given
                         * threshold
                         */
        double meanObj; /*
                        * mean of the object pixels at a given
                        * threshold
                        */
        double mean; /* mean gray-level in the image */
        double tolerance; /* threshold tolerance */
        double temp;

        tolerance = 0.5;
        numPixels = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            numPixels += data[ih];
        }

        /* Calculate the mean gray-level */
        mean = 0.0;
        for (ih = 0 + 1; ih < (m_maxValue + 1); ih++) {
            // 0 + 1?
            mean += ih * data[ih];
        }
        mean /= numPixels;
        /* Initial estimate */
        newThresh = mean;

        do {
            oldThresh = newThresh;
            threshold = (int)(oldThresh + 0.5); /* range */
            /* Calculate the means of background and object pixels */
            /* Background */
            sumBack = 0;
            numBack = 0;
            for (ih = 0; ih <= threshold; ih++) {
                sumBack += ih * data[ih];
                numBack += data[ih];
            }
            meanBack = (numBack == 0 ? 0.0 : (sumBack / (double)numBack));
            /* Object */
            sumObj = 0;
            numObj = 0;
            for (ih = threshold + 1; ih < (m_maxValue + 1); ih++) {
                sumObj += ih * data[ih];
                numObj += data[ih];
            }
            meanObj = (numObj == 0 ? 0.0 : (sumObj / (double)numObj));

            /* Calculate the new threshold: Equation (7) in Ref. 2 */
            temp = (meanBack - meanObj) / (Math.log(meanBack) - Math.log(meanObj));

            if (temp < -2.220446049250313E-16) {
                newThresh = (int)(temp - 0.5);
            } else {
                newThresh = (int)(temp + 0.5);
                /*
                 * Stop the iterations when the difference
                 * between the new and old threshold values is
                 * less than the tolerance
                 */
            }
        } while (Math.abs(newThresh - oldThresh) > tolerance);
        return threshold;
    }

    private int MaxEntropy(final long[] data) {
        // Implements Kapur-Sahoo-Wong (Maximum Entropy) thresholding
        // method
        // Kapur J.N., Sahoo P.K., and Wong A.K.C. (1985) "A New Method
        // for
        // Gray-Level Picture Thresholding Using the Entropy of the
        // Histogram"
        // Graphical Models and Image Processing, 29(3): 273-285
        // M. Emre Celebi
        // 06.15.2007
        // Ported to ImageJ plugin by G.Landini from E Celebi's
        // fourier_0.8
        // routines
        int threshold = -1;
        int ih, it;
        int firstBin;
        int lastBin;
        double totEnt; /* total entropy */
        double maxEnt; /* max entropy */
        double entBack; /*
                        * entropy of the background pixels at a given
                        * threshold
                        */
        double entObj; /*
                       * entropy of the object pixels at a given
                       * threshold
                       */
        final double[] norm_histo = new double[m_maxValue + 1]; /*
                                                                * normalized
                                                                * histogram
                                                                */
        final double[] P1 = new double[m_maxValue + 1]; /*
                                                        * cumulative normalized
                                                        * histogram
                                                        */
        final double[] P2 = new double[m_maxValue + 1];

        int total = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            total += data[ih];
        }

        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            norm_histo[ih] = (double)data[ih] / total;
        }

        P1[0] = norm_histo[0];
        P2[0] = 1.0 - P1[0];
        for (ih = 1; ih < (m_maxValue + 1); ih++) {
            P1[ih] = P1[ih - 1] + norm_histo[ih];
            P2[ih] = 1.0 - P1[ih];
        }

        /* Determine the first non-zero bin */
        firstBin = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            if (!(Math.abs(P1[ih]) < 2.220446049250313E-16)) {
                firstBin = ih;
                break;
            }
        }

        /* Determine the last non-zero bin */
        lastBin = m_maxValue;
        for (ih = m_maxValue; ih >= firstBin; ih--) {
            if (!(Math.abs(P2[ih]) < 2.220446049250313E-16)) {
                lastBin = ih;
                break;
            }
        }

        // Calculate the total entropy each gray-level
        // and find the threshold that maximizes it
        maxEnt = Double.MIN_VALUE;

        for (it = firstBin; it <= lastBin; it++) {
            /* Entropy of the background pixels */
            entBack = 0.0;
            for (ih = 0; ih <= it; ih++) {
                if (data[ih] != 0) {
                    entBack -= (norm_histo[ih] / P1[it]) * Math.log(norm_histo[ih] / P1[it]);
                }
            }

            /* Entropy of the object pixels */
            entObj = 0.0;
            for (ih = it + 1; ih < (m_maxValue + 1); ih++) {
                if (data[ih] != 0) {
                    entObj -= (norm_histo[ih] / P2[it]) * Math.log(norm_histo[ih] / P2[it]);
                }
            }

            /* Total entropy */
            totEnt = entBack + entObj;

            if (maxEnt < totEnt) {
                maxEnt = totEnt;
                threshold = it;
            }
        }
        return threshold;
    }

    private int Mean(final long[] data) {
        // C. A. Glasbey,
        // "An analysis of histogram-based thresholding algorithms,"
        // CVGIP: Graphical Models and Image Processing, vol. 55, pp.
        // 532-537,
        // 1993.
        //
        // The threshold is the mean of the greyscale data
        int threshold = -1;
        double tot = 0, sum = 0;
        for (int i = 0; i < (m_maxValue + 1); i++) {
            tot += data[i];
            sum += (i * data[i]);
        }
        threshold = (int)Math.floor(sum / tot);
        return threshold;
    }

    int MinErrorI(final long[] data) {
        // Kittler and J. Illingworth, "Minimum error thresholding,"
        // Pattern
        // Recognition, vol. 19, pp. 41-47, 1986.
        // C. A. Glasbey,
        // "An analysis of histogram-based thresholding algorithms,"
        // CVGIP:
        // Graphical Models and Image Processing, vol. 55, pp. 532-537,
        // 1993.
        // Ported to ImageJ plugin by G.Landini from Antti Niemisto's
        // Matlab
        // code (GPL)
        // Original Matlab code Copyright (C) 2004, 2010 Antti Niemisto
        // See http://www.cs.tut.fi/~ant/histthresh/ for an excellent
        // slide
        // presentation
        // and the original Matlab code.

        int threshold = Mean(data); // Initial estimate for the
        // threshold is
        // found with the MEAN algorithm.
        int Tprev = -2;
        double mu, nu, p, q, sigma2, tau2, w0, w1, w2, sqterm, temp;
        while (threshold != Tprev) {
            // Calculate some statistics.
            mu = B(data, threshold) / A(data, threshold);
            nu = (B(data, m_maxValue) - B(data, threshold)) / (A(data, m_maxValue) - A(data, threshold));
            p = A(data, threshold) / A(data, m_maxValue);
            q = (A(data, m_maxValue) - A(data, threshold)) / A(data, m_maxValue);
            sigma2 = (C(data, threshold) / A(data, threshold)) - (mu * mu);
            tau2 =
                    ((C(data, m_maxValue) - C(data, threshold)) / (A(data, m_maxValue) - A(data, threshold)))
                            - (nu * nu);

            // The terms of the quadratic equation to be solved.
            w0 = (1.0 / sigma2) - (1.0 / tau2);
            w1 = (mu / sigma2) - (nu / tau2);
            w2 = (((mu * mu) / sigma2) - ((nu * nu) / tau2)) + Math.log10((sigma2 * (q * q)) / (tau2 * (p * p)));

            // If the next threshold would be imaginary, return with
            // the current
            // one.
            sqterm = (w1 * w1) - (w0 * w2);
            if (sqterm < 0) {
                return threshold;
            }

            // The updated threshold is the integer part of the
            // solution of the
            // quadratic equation.
            Tprev = threshold;
            temp = (w1 + Math.sqrt(sqterm)) / w0;

            if (Double.isNaN(temp)) {
                threshold = Tprev;
            } else {
                threshold = (int)Math.floor(temp);
            }
        }
        return threshold;
    }

    static double A(final long[] y, final int j) {
        double x = 0;
        for (int i = 0; i <= j; i++) {
            x += y[i];
        }
        return x;
    }

    static double B(final long[] y, final int j) {
        double x = 0;
        for (int i = 0; i <= j; i++) {
            x += i * y[i];
        }
        return x;
    }

    static double C(final long[] y, final int j) {
        double x = 0;
        for (int i = 0; i <= j; i++) {
            x += i * i * y[i];
        }
        return x;
    }

    int Minimum(final long[] data) {
        // J. M. S. Prewitt and M. L. Mendelsohn,
        // "The analysis of cell images,"
        // in
        // Annals of the New York Academy of Sciences, vol. 128, pp.
        // 1035-1053,
        // 1966.
        // ported to ImageJ plugin by G.Landini from Antti Niemisto's
        // Matlab
        // code (GPL)
        // Original Matlab code Copyright (C) 2004 Antti Niemisto
        // See http://www.cs.tut.fi/~ant/histthresh/ for an excellent
        // slide
        // presentation
        // and the original Matlab code.
        //
        // Assumes a bimodal histogram. The histogram needs is smoothed
        // (using a
        // running average of size 3, iteratively) until there are only
        // two
        // local maxima.
        // Threshold t is such that yt-1 > yt <= yt+1.
        // Images with histograms having extremely unequal peaks or a
        // broad and
        // flat valley are unsuitable for this method.
        int iter = 0;
        int threshold = -1;
        double[] iHisto = new double[m_maxValue + 1];

        for (int i = 0; i < (m_maxValue + 1); i++) {
            iHisto[i] = data[i];
        }

        final double[] tHisto = iHisto;

        while (!bimodalTest(iHisto)) {
            // smooth with a 3 point running mean filter
            for (int i = 1; i < m_maxValue; i++) {
                tHisto[i] = (iHisto[i - 1] + iHisto[i] + iHisto[i + 1]) / 3;
            }
            tHisto[0] = (iHisto[0] + iHisto[1]) / 3; // 0 outside
            tHisto[m_maxValue] = (iHisto[254] + iHisto[m_maxValue]) / 3; // 0
            // outside
            iHisto = tHisto;
            iter++;
            if (iter > 10000) {
                threshold = -1;
                return threshold;
            }
        }
        // The threshold is the minimum between the two peaks.
        for (int i = 1; i < m_maxValue; i++) {
            if ((iHisto[i - 1] > iHisto[i]) && (iHisto[i + 1] >= iHisto[i])) {
                threshold = i;
            }
        }
        return threshold;
    }

    int Moments(final long[] data) {
        // W. Tsai, "Moment-preserving thresholding: a new approach,"
        // Computer
        // Vision,
        // Graphics, and Image Processing, vol. 29, pp. 377-393, 1985.
        // Ported to ImageJ plugin by G.Landini from the the open source
        // project
        // FOURIER 0.8
        // by M. Emre Celebi , Department of Computer Science, Louisiana
        // State
        // University in Shreveport
        // Shreveport, LA 71115, USA
        // http://sourceforge.net/projects/fourier-ipal
        // http://www.lsus.edu/faculty/~ecelebi/fourier.htm
        double total = 0;
        final double m0 = 1.0;
        double m1 = 0.0, m2 = 0.0, m3 = 0.0, sum = 0.0, p0 = 0.0;
        double cd, c0, c1, z0, z1; /* auxiliary variables */
        int threshold = -1;

        final double[] histo = new double[m_maxValue + 1];

        for (int i = 0; i < (m_maxValue + 1); i++) {
            total += data[i];
        }

        for (int i = 0; i < (m_maxValue + 1); i++) {
            histo[i] = (data[i] / total); // normalised histogram
        }

        /* Calculate the first, second, and third order moments */
        for (int i = 0; i < (m_maxValue + 1); i++) {
            m1 += i * histo[i];
            m2 += i * i * histo[i];
            m3 += i * i * i * histo[i];
        }
        /*
         * First 4 moments of the gray-level image should match the
         * first 4 moments of the target binary image. This leads to 4
         * equalities whose solutions are given in the Appendix of Ref.
         * 1
         */
        cd = (m0 * m2) - (m1 * m1);
        c0 = ((-m2 * m2) + (m1 * m3)) / cd;
        c1 = ((m0 * -m3) + (m2 * m1)) / cd;
        z0 = 0.5 * (-c1 - Math.sqrt((c1 * c1) - (4.0 * c0)));
        z1 = 0.5 * (-c1 + Math.sqrt((c1 * c1) - (4.0 * c0)));
        p0 = (z1 - m1) / (z1 - z0); /*
                                    * Fraction of the object pixels in
                                    * the target binary image
                                    */

        // The threshold is the gray-level closest
        // to the p0-tile of the normalized histogram
        sum = 0;
        for (int i = 0; i < (m_maxValue + 1); i++) {
            sum += histo[i];
            if (sum > p0) {
                threshold = i;
                break;
            }
        }
        return threshold;
    }

    int Otsu(final long[] data) {
        // Otsu's threshold algorithm
        // C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
        // ported to ImageJ plugin by G.Landini
        int k, kStar; // k = the current threshold; kStar = optimal
        // threshold
        long n1, n; // N1 = # points with intensity <=k; N = total number
        // of
        // points
        double BCV, BCVmax; // The current Between Class Variance and
        // maximum
        // BCV
        double num, denom; // temporary bookeeping
        int sk; // The total intensity for all histogram points <=k
        int s; // The total intensity of the image
        final int L = m_maxValue + 1;

        // Initialize values:
        s = 0;
        n = 0;
        for (k = 0; k < L; k++) {
            s += k * data[k]; // Total histogram intensity
            n += data[k]; // Total number of data points
        }

        sk = 0;
        n1 = data[0]; // The entry for zero intensity
        BCV = 0;
        BCVmax = 0;
        kStar = 0;

        // Look at each possible threshold value,
        // calculate the between-class variance, and decide if it's a
        // max
        for (k = 1; k < (L - 1); k++) { // No need to check endpoints k =
            // 0 or k =
            // L-1
            sk += k * data[k];
            n1 += data[k];

            // The float casting here is to avoid compiler warning
            // about loss of
            // precision and
            // will prevent overflow in the case of large saturated
            // images
            denom = (double)(n1) * (n - n1); // Maximum value of
            // denom is
            // (N^2)/4 = approx. 3E10

            if (denom != 0) {
                // Float here is to avoid loss of precision when
                // dividing
                num = (((double)n1 / n) * s) - sk; // Maximum
                // value of
                // num =
                // MAX_VALUE*N = approx 8E7
                BCV = (num * num) / denom;
            } else {
                BCV = 0;
            }

            if (BCV >= BCVmax) { // Assign the best threshold found
                // so far
                BCVmax = BCV;
                kStar = k;
            }
        }
        // kStar += 1; // Use QTI convention that intensity -> 1 if
        // intensity >=
        // k
        // (the algorithm was developed for I-> 1 if I <= k.)
        return kStar;
    }

    int Percentile(final long[] data) {
        // W. Doyle,
        // "Operation useful for similarity-invariant pattern recognition,"
        // Journal of the Association for Computing Machinery, vol.
        // 9,pp.
        // 259-267, 1962.
        // ported to ImageJ plugin by G.Landini from Antti Niemisto's
        // Matlab
        // code (GPL)
        // Original Matlab code Copyright (C) 2004 Antti Niemisto
        // See http://www.cs.tut.fi/~ant/histthresh/ for an excellent
        // slide
        // presentation
        // and the original Matlab code.

        int threshold = -1;
        final double ptile = 0.5; // default fraction of foreground pixels
        final double[] avec = new double[m_maxValue + 1];

        for (int i = 0; i < (m_maxValue + 1); i++) {
            avec[i] = 0.0;
        }

        final double total = partialSum(data, m_maxValue);
        double temp = 1.0;
        for (int i = 0; i < (m_maxValue + 1); i++) {
            avec[i] = Math.abs((partialSum(data, i) / total) - ptile);
            if (avec[i] < temp) {
                temp = avec[i];
                threshold = i;
            }
        }
        return threshold;
    }

    static double partialSum(final long[] y, final int j) {
        double x = 0;
        for (int i = 0; i <= j; i++) {
            x += y[i];
        }
        return x;
    }

    int RenyiEntropy(final long[] data) {
        // Kapur J.N., Sahoo P.K., and Wong A.K.C. (1985) "A New Method
        // for
        // Gray-Level Picture Thresholding Using the Entropy of the
        // Histogram"
        // Graphical Models and Image Processing, 29(3): 273-285
        // M. Emre Celebi
        // 06.15.2007
        // Ported to ImageJ plugin by G.Landini from E Celebi's
        // fourier_0.8
        // routines

        int threshold;
        int optThreshold;

        int ih, it;
        int firstBin;
        int lastBin;
        int tmp_var;
        int tStar1, tStar2, tStar3;
        int beta1, beta2, beta3;
        double alpha;/* alpha parameter of the method */
        double term;
        double totEnt; /* total entropy */
        double maxEnt; /* max entropy */
        double entBack; /*
                        * entropy of the background pixels at a given
                        * threshold
                        */
        double entObj; /*
                       * entropy of the object pixels at a given
                       * threshold
                       */
        double omega;
        final double[] normHisto = new double[m_maxValue + 1]; /*
                                                               * normalized
                                                               * histogram
                                                               */
        final double[] P1 = new double[m_maxValue + 1]; /*
                                                        * cumulative normalized
                                                        * histogram
                                                        */
        final double[] P2 = new double[m_maxValue + 1];

        int total = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            total += data[ih];
        }

        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            normHisto[ih] = (double)data[ih] / total;
        }

        P1[0] = normHisto[0];
        P2[0] = 1.0 - P1[0];
        for (ih = 1; ih < (m_maxValue + 1); ih++) {
            P1[ih] = P1[ih - 1] + normHisto[ih];
            P2[ih] = 1.0 - P1[ih];
        }

        /* Determine the first non-zero bin */
        firstBin = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            if (!(Math.abs(P1[ih]) < 2.220446049250313E-16)) {
                firstBin = ih;
                break;
            }
        }

        /* Determine the last non-zero bin */
        lastBin = m_maxValue;
        for (ih = m_maxValue; ih >= firstBin; ih--) {
            if (!(Math.abs(P2[ih]) < 2.220446049250313E-16)) {
                lastBin = ih;
                break;
            }
        }

        /* Maximum Entropy Thresholding - BEGIN */
        /* ALPHA = 1.0 */
        /*
         * Calculate the total entropy each gray-level and find the
         * threshold that maximizes it
         */
        threshold = 0; // was MIN_INT in original code, but if an empty
        // image is
        // processed it gives an error later on.
        maxEnt = 0.0;

        for (it = firstBin; it <= lastBin; it++) {
            /* Entropy of the background pixels */
            entBack = 0.0;
            for (ih = 0; ih <= it; ih++) {
                if (data[ih] != 0) {
                    entBack -= (normHisto[ih] / P1[it]) * Math.log(normHisto[ih] / P1[it]);
                }
            }

            /* Entropy of the object pixels */
            entObj = 0.0;
            for (ih = it + 1; ih < (m_maxValue + 1); ih++) {
                if (data[ih] != 0) {
                    entObj -= (normHisto[ih] / P2[it]) * Math.log(normHisto[ih] / P2[it]);
                }
            }

            /* Total entropy */
            totEnt = entBack + entObj;

            if (maxEnt < totEnt) {
                maxEnt = totEnt;
                threshold = it;
            }
        }
        tStar2 = threshold;

        /* Maximum Entropy Thresholding - END */
        threshold = 0; // was MIN_INT in original code, but if an empty
        // image is
        // processed it gives an error later on.
        maxEnt = 0.0;
        alpha = 0.5;
        term = 1.0 / (1.0 - alpha);
        for (it = firstBin; it <= lastBin; it++) {
            /* Entropy of the background pixels */
            entBack = 0.0;
            for (ih = 0; ih <= it; ih++) {
                entBack += Math.sqrt(normHisto[ih] / P1[it]);
            }

            /* Entropy of the object pixels */
            entObj = 0.0;
            for (ih = it + 1; ih < (m_maxValue + 1); ih++) {
                entObj += Math.sqrt(normHisto[ih] / P2[it]);
            }

            /* Total entropy */
            totEnt = term * ((entBack * entObj) > 0.0 ? Math.log(entBack * entObj) : 0.0);

            if (totEnt > maxEnt) {
                maxEnt = totEnt;
                threshold = it;
            }
        }

        tStar1 = threshold;

        threshold = 0; // was MIN_INT in original code, but if an empty
        // image is
        // processed it gives an error later on.
        maxEnt = 0.0;
        alpha = 2.0;
        term = 1.0 / (1.0 - alpha);
        for (it = firstBin; it <= lastBin; it++) {
            /* Entropy of the background pixels */
            entBack = 0.0;
            for (ih = 0; ih <= it; ih++) {
                entBack += (normHisto[ih] * normHisto[ih]) / (P1[it] * P1[it]);
            }

            /* Entropy of the object pixels */
            entObj = 0.0;
            for (ih = it + 1; ih < (m_maxValue + 1); ih++) {
                entObj += (normHisto[ih] * normHisto[ih]) / (P2[it] * P2[it]);
            }

            /* Total entropy */
            totEnt = term * ((entBack * entObj) > 0.0 ? Math.log(entBack * entObj) : 0.0);

            if (totEnt > maxEnt) {
                maxEnt = totEnt;
                threshold = it;
            }
        }

        tStar3 = threshold;

        /* Sort t_star values */
        if (tStar2 < tStar1) {
            tmp_var = tStar1;
            tStar1 = tStar2;
            tStar2 = tmp_var;
        }
        if (tStar3 < tStar2) {
            tmp_var = tStar2;
            tStar2 = tStar3;
            tStar3 = tmp_var;
        }
        if (tStar2 < tStar1) {
            tmp_var = tStar1;
            tStar1 = tStar2;
            tStar2 = tmp_var;
        }

        /* Adjust beta values */
        if (Math.abs(tStar1 - tStar2) <= 5) {
            if (Math.abs(tStar2 - tStar3) <= 5) {
                beta1 = 1;
                beta2 = 2;
                beta3 = 1;
            } else {
                beta1 = 0;
                beta2 = 1;
                beta3 = 3;
            }
        } else {
            if (Math.abs(tStar2 - tStar3) <= 5) {
                beta1 = 3;
                beta2 = 1;
                beta3 = 0;
            } else {
                beta1 = 1;
                beta2 = 2;
                beta3 = 1;
            }
        }
        /* Determine the optimal threshold value */
        omega = P1[tStar3] - P1[tStar1];
        optThreshold =
                (int)((tStar1 * (P1[tStar1] + (0.25 * omega * beta1))) + (0.25 * tStar2 * omega * beta2) + (tStar3 * (P2[tStar3] + (0.25 * omega * beta3))));

        return optThreshold;
    }

    int Shanbhag(final long[] data) {
        // Shanhbag A.G. (1994) "Utilization of Information Measure as a
        // Means
        // of
        // Image Thresholding" Graphical Models and Image Processing,
        // 56(5):
        // 414-419
        // Ported to ImageJ plugin by G.Landini from E Celebi's
        // fourier_0.8
        // routines
        int threshold;
        int ih, it;
        int first_bin;
        int last_bin;
        double term;
        double tot_ent; /* total entropy */
        double min_ent; /* max entropy */
        double ent_back; /*
                         * entropy of the background pixels at a given
                         * threshold
                         */
        double ent_obj; /*
                        * entropy of the object pixels at a given
                        * threshold
                        */
        final double[] norm_histo = new double[m_maxValue + 1]; /*
                                                                * normalized
                                                                * histogram
                                                                */
        final double[] P1 = new double[m_maxValue + 1]; /*
                                                        * cumulative normalized
                                                        * histogram
                                                        */
        final double[] P2 = new double[m_maxValue + 1];

        int total = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            total += data[ih];
        }

        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            norm_histo[ih] = (double)data[ih] / total;
        }

        P1[0] = norm_histo[0];
        P2[0] = 1.0 - P1[0];
        for (ih = 1; ih < (m_maxValue + 1); ih++) {
            P1[ih] = P1[ih - 1] + norm_histo[ih];
            P2[ih] = 1.0 - P1[ih];
        }

        /* Determine the first non-zero bin */
        first_bin = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            if (!(Math.abs(P1[ih]) < 2.220446049250313E-16)) {
                first_bin = ih;
                break;
            }
        }

        /* Determine the last non-zero bin */
        last_bin = m_maxValue;
        for (ih = m_maxValue; ih >= first_bin; ih--) {
            if (!(Math.abs(P2[ih]) < 2.220446049250313E-16)) {
                last_bin = ih;
                break;
            }
        }

        // Calculate the total entropy each gray-level
        // and find the threshold that maximizes it
        threshold = -1;
        min_ent = Double.MAX_VALUE;

        for (it = first_bin; it <= last_bin; it++) {
            /* Entropy of the background pixels */
            ent_back = 0.0;
            term = 0.5 / P1[it];
            for (ih = 1; ih <= it; ih++) { // 0+1?
                ent_back -= norm_histo[ih] * Math.log(1.0 - (term * P1[ih - 1]));
            }
            ent_back *= term;

            /* Entropy of the object pixels */
            ent_obj = 0.0;
            term = 0.5 / P2[it];
            for (ih = it + 1; ih < (m_maxValue + 1); ih++) {
                ent_obj -= norm_histo[ih] * Math.log(1.0 - (term * P2[ih]));
            }
            ent_obj *= term;

            /* Total entropy */
            tot_ent = Math.abs(ent_back - ent_obj);

            if (tot_ent < min_ent) {
                min_ent = tot_ent;
                threshold = it;
            }
        }
        return threshold;
    }

    int Triangle(final long[] data) {
        // Zack, G. W., Rogers, W. E. and Latt, S. A., 1977,
        // Automatic Measurement of Sister Chromatid Exchange Frequency,
        // Journal of Histochemistry and Cytochemistry 25 (7), pp.
        // 741-753
        //
        // modified from Johannes Schindelin plugin
        //
        // find min and max
        long dmax = 0;
        int min = 0, max = 0, min2 = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] > 0) {
                min = i;
                break;
            }
        }
        if (min > 0) {
            min--; // line to the (p==0) point, not to data[min]
        }

        // The Triangle algorithm cannot tell whether the data is skewed
        // to one
        // side or another.
        // This causes a problem as there are 2 possible thresholds
        // between the
        // max and the 2 extremes
        // of the histogram.
        // Here I propose to find out to which side of the max point the
        // data is
        // furthest, and use that as
        // the other extreme.
        for (int i = m_maxValue; i > 0; i--) {
            if (data[i] > 0) {
                min2 = i;
                break;
            }
        }
        if (min2 < m_maxValue) {
            min2++; // line to the (p==0) point, not to data[min]
        }

        for (int i = 0; i < (m_maxValue + 1); i++) {
            if (data[i] > dmax) {
                max = i;
                dmax = data[i];
            }
        }
        // find which is the furthest side
        boolean inverted = false;
        if ((max - min) < (min2 - max)) {
            // reverse the histogram
            inverted = true;
            int left = 0; // index of leftmost element
            int right = m_maxValue; // index of rightmost element
            while (left < right) {
                // exchange the left and right elements
                final long temp = data[left];
                data[left] = data[right];
                data[right] = temp;
                // move the bounds toward the center
                left++;
                right--;
            }
            min = m_maxValue - min2;
            max = m_maxValue - max;
        }

        if (min == max) {
            return min;
        }

        // describe line by nx * x + ny * y - d = 0
        double nx, ny, d;
        // nx is just the max frequency as the other point has freq=0
        nx = data[max]; // -min; // data[min]; // lowest value bmin =
        // (p=0)%
        // in
        // the image
        ny = min - max;
        d = Math.sqrt((nx * nx) + (ny * ny));
        nx /= d;
        ny /= d;
        d = (nx * min) + (ny * data[min]);

        // find split point
        int split = min;
        double splitDistance = 0;
        for (int i = min + 1; i <= max; i++) {
            final double newDistance = ((nx * i) + (ny * data[i])) - d;
            if (newDistance > splitDistance) {
                split = i;
                splitDistance = newDistance;
            }
        }
        split--;

        if (inverted) {
            // The histogram might be used for something else, so
            // let's reverse
            // it back
            int left = 0;
            int right = m_maxValue;
            while (left < right) {
                final long temp = data[left];
                data[left] = data[right];
                data[right] = temp;
                left++;
                right--;
            }
            return (m_maxValue - split);
        }
        return split;
    }

    int Yen(final long[] data) {
        // Implements Yen thresholding method
        // 1) Yen J.C., Chang F.J., and Chang S. (1995) "A New Criterion
        // for Automatic Multilevel Thresholding" IEEE Trans. on Image
        // Processing, 4(3): 370-378
        // 2) Sezgin M. and Sankur B. (2004) "Survey over Image
        // Thresholding
        // Techniques and Quantitative Performance Evaluation" Journal
        // of
        // Electronic Imaging, 13(1): 146-165
        // http://citeseer.ist.psu.edu/sezgin04survey.html
        //
        // M. Emre Celebi
        // 06.15.2007
        // Ported to ImageJ plugin by G.Landini from E Celebi's
        // fourier_0.8
        // routines
        int threshold;
        int ih, it;
        double crit;
        double maxCrit;
        final double[] normHisto = new double[m_maxValue + 1]; /*
                                                               * normalized
                                                               * histogram
                                                               */
        final double[] p1 = new double[m_maxValue + 1]; /*
                                                        * cumulative normalized
                                                        * histogram
                                                        */
        final double[] p1Sq = new double[m_maxValue + 1];
        final double[] p2Sq = new double[m_maxValue + 1];

        int total = 0;
        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            total += data[ih];
        }

        for (ih = 0; ih < (m_maxValue + 1); ih++) {
            normHisto[ih] = (double)data[ih] / total;
        }

        p1[0] = normHisto[0];
        for (ih = 1; ih < (m_maxValue + 1); ih++) {
            p1[ih] = p1[ih - 1] + normHisto[ih];
        }

        p1Sq[0] = normHisto[0] * normHisto[0];
        for (ih = 1; ih < (m_maxValue + 1); ih++) {
            p1Sq[ih] = p1Sq[ih - 1] + (normHisto[ih] * normHisto[ih]);
        }

        p2Sq[m_maxValue] = 0.0;
        for (ih = 254; ih >= 0; ih--) {
            p2Sq[ih] = p2Sq[ih + 1] + (normHisto[ih + 1] * normHisto[ih + 1]);
        }

        /* Find the threshold that maximizes the criterion */
        threshold = -1;
        maxCrit = Double.MIN_VALUE;
        for (it = 0; it < (m_maxValue + 1); it++) {
            crit =
                    (-1.0 * ((p1Sq[it] * p2Sq[it]) > 0.0 ? Math.log(p1Sq[it] * p2Sq[it]) : 0.0))
                            + (2 * ((p1[it] * (1.0 - p1[it])) > 0.0 ? Math.log(p1[it] * (1.0 - p1[it])) : 0.0));
            if (crit > maxCrit) {
                maxCrit = crit;
                threshold = it;
            }
        }
        return threshold;
    }

    @Override
    public UnaryOperation<Histogram1d<T>, DoubleType> copy() {
        return new FindThreshold<T>(m_ttype, m_type);
    }
}
