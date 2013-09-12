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
package org.knime.knip.core.algorithm.extendedem;

import java.util.Random;

/**
 * <!-- globalinfo-start --> Simple EM (expectation maximisation) class.<br/>
 * <br/>
 * EM assigns a probability distribution to each instance which indicates the probability of it belonging to each of the
 * clusters. EM can decide how many clusters to create by cross validation, or you may specify apriori how many clusters
 * to generate.<br/>
 * <br/>
 * The cross validation performed to determine the number of clusters is done in the following steps:<br/>
 * 1. the number of clusters is set to 1<br/>
 * 2. the training set is split randomly into 10 folds.<br/>
 * 3. EM is performed 10 times using the 10 folds the usual CV way.<br/>
 * 4. the loglikelihood is averaged over all 10 results.<br/>
 * 5. if loglikelihood has increased the number of clusters is increased by 1 and the program continues at step 2. <br/>
 * <br/>
 * The number of folds is fixed to 10, as long as the number of instances in the training set is not smaller 10. If this
 * is the case the number of folds is set equal to the number of instances.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -N &lt;num&gt;
 *  number of clusters. If omitted or -1 specified, then
 *  cross validation is used to select the number of clusters.
 * </pre>
 * 
 * <pre>
 * -I &lt;num&gt;
 *  max iterations.
 * (default 100)
 * </pre>
 * 
 * <pre>
 * -V
 *  verbose.
 * </pre>
 * 
 * <pre>
 * -M &lt;num&gt;
 *  minimum allowable standard deviation for normal density
 *  computation
 *  (default 1e-6)
 * </pre>
 * 
 * <pre>
 * -O
 *  Display model in old format (good when there are many clusters)
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 100)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @version $Revision: 6298 $
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 */
public class ExtendedEM {

    /** for serialization */
    private static final long serialVersionUID = 8348181483812829475L;

    /** hold the normal estimators for each cluster */
    private double m_modelNormal[][][];

    /** default minimum standard deviation */
    private final double m_minStdDev = 1e-6;

    private double[] m_minStdDevPerAtt;

    /** hold the weights of each instance for each cluster */
    private double m_weights[][];

    /** the prior probabilities for clusters */
    private double m_priors[];

    /** training instances */
    private InstancesTmp m_theInstances = null;

    /** number of clusters selected by the user or cross validation */
    private int m_numClusters;

    /** number of attributes */
    private int m_numAttribs;

    /** maximum iterations to perform */
    private int m_maxIterations;

    /** attribute min values */
    private double[] m_minValues;

    /** attribute max values */
    private double[] m_maxValues;

    /** random number generator */
    private Random m_rr;

    /** Verbose? */
    private boolean m_verbose;

    /** the default seed value */
    private int m_seedDefault = 1;

    /** The random number seed. */
    private final int m_seed = m_seedDefault;

    private void normalize(final double[] doubles) {

        double sum = 0;
        for (int i = 0; i < doubles.length; i++) {
            sum += doubles[i];
        }
        normalize(doubles, sum);
    }

    private void normalize(final double[] doubles, final double sum) {

        if (Double.isNaN(sum)) {
            throw new IllegalArgumentException("Can't normalize array. Sum is NaN.");
        }
        if (sum == 0) {
            // Maybe this should just be a return.
            throw new IllegalArgumentException("Can't normalize array. Sum is zero.");
        }
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] /= sum;
        }
    }

    /**
     * Set the number of clusters (-1 to select by CV).
     * 
     * @param n the number of clusters
     * @throws Exception if n is 0
     */

    public void setNumClusters(final int n) {

        if (n == 0) {
            throw new RuntimeException("Number of clusters must be > 0. (or -1 to " + "select by cross validation).");
        }

        if (n < 0) {
            m_numClusters = -1;
        } else {
            m_numClusters = n;
        }
    }

    /**
     * Set start centers of the EM Algo
     * 
     * @param inst
     */

    private InstancesTmp m_centers;

    public void setCenters(final InstancesTmp inst) {
        m_centers = inst;
    }

    private int[] m_clusterSizes;

    public void setClusterSizes(final int[] clusterSizes) {
        m_clusterSizes = clusterSizes.clone();
    }

    public void setMaxInterations(final int max) {
        m_maxIterations = max;
    }

    /**
     * Initialise estimators and storage.
     * 
     * @param inst the instances
     * @throws Exception if initialization fails
     **/
    private void EM_Init(final InstancesTmp inst) {
        int i, j;

        m_weights = new double[inst.numInstances()][m_numClusters];
        m_modelNormal = new double[m_numClusters][m_numAttribs][3];
        m_priors = new double[m_numClusters];

        final int[] clusterSizes = m_clusterSizes;
        final InstancesTmp centers = m_centers;

        for (i = 0; i < m_numClusters; i++) {
            final InstanceTmp center = centers.instance(i);
            for (j = 0; j < m_numAttribs; j++) {
                final double minStdD = (m_minStdDevPerAtt != null) ? m_minStdDevPerAtt[j] : m_minStdDev;
                final double mean = (center.isMissing(j)) ? inst.meanOrMode(j) : center.value(j);
                m_modelNormal[i][j][0] = mean;
                double stdv = ((m_maxValues[j] - m_minValues[j]) / (2 * m_numClusters));
                if (stdv < minStdD) {
                    stdv = inst.attributeStats(j).m_numericStats.getStdDev();
                    if (Double.isInfinite(stdv)) {
                        stdv = minStdD;
                    }
                    if (stdv < minStdD) {
                        stdv = minStdD;
                    }
                }
                if (stdv <= 0) {
                    stdv = m_minStdDev;
                }

                m_modelNormal[i][j][1] = stdv;
                m_modelNormal[i][j][2] = 1.0;
            }
        }
        for (j = 0; j < m_numClusters; j++) {
            m_priors[j] = clusterSizes[j];
        }
        normalize(m_priors);
    }

    /**
     * calculate prior probabilites for the clusters
     * 
     * @param inst the instances
     * @throws Exception if priors can't be calculated
     **/
    private void estimate_priors(final InstancesTmp inst) {

        for (int i = 0; i < m_numClusters; i++) {
            m_priors[i] = 0.0;
        }

        for (int i = 0; i < inst.numInstances(); i++) {
            for (int j = 0; j < m_numClusters; j++) {
                m_priors[j] += inst.instance(i).weight() * m_weights[i][j];
            }
        }

        normalize(m_priors);
    }

    /**
     * New probability estimators for an iteration
     */
    private void new_estimators() {
        for (int i = 0; i < m_numClusters; i++) {
            for (int j = 0; j < m_numAttribs; j++) {
                m_modelNormal[i][j][0] = m_modelNormal[i][j][1] = m_modelNormal[i][j][2] = 0.0;
            }
        }
    }

    /**
     * The M step of the EM algorithm.
     * 
     * @param inst the training instances
     * @throws Exception if something goes wrong
     */
    private void M(final InstancesTmp inst) {

        int i, j, l;

        new_estimators();
        estimate_priors(inst);

        for (i = 0; i < m_numClusters; i++) {
            for (j = 0; j < m_numAttribs; j++) {
                for (l = 0; l < inst.numInstances(); l++) {
                    final InstanceTmp in = inst.instance(l);
                    if (!in.isMissing(j)) {
                        m_modelNormal[i][j][0] += (in.value(j) * in.weight() * m_weights[l][i]);
                        m_modelNormal[i][j][2] += in.weight() * m_weights[l][i];
                        m_modelNormal[i][j][1] += (in.value(j) * in.value(j) * in.weight() * m_weights[l][i]);
                    }
                }
            }
        }

        // calcualte mean and std deviation for numeric attributes
        for (j = 0; j < m_numAttribs; j++) {
            if (!inst.attribute(j).isNominal()) {
                for (i = 0; i < m_numClusters; i++) {
                    if (m_modelNormal[i][j][2] <= 0) {
                        m_modelNormal[i][j][1] = Double.MAX_VALUE;
                        m_modelNormal[i][j][0] = m_minStdDev;
                    } else {

                        // variance
                        m_modelNormal[i][j][1] =
                                (m_modelNormal[i][j][1] - ((m_modelNormal[i][j][0] * m_modelNormal[i][j][0]) / m_modelNormal[i][j][2]))
                                        / (m_modelNormal[i][j][2]);

                        if (m_modelNormal[i][j][1] < 0) {
                            m_modelNormal[i][j][1] = 0;
                        }

                        // std dev
                        final double minStdD = (m_minStdDevPerAtt != null) ? m_minStdDevPerAtt[j] : m_minStdDev;

                        m_modelNormal[i][j][1] = Math.sqrt(m_modelNormal[i][j][1]);

                        if ((m_modelNormal[i][j][1] <= minStdD)) {
                            m_modelNormal[i][j][1] = inst.attributeStats(j).m_numericStats.getStdDev();
                            if ((m_modelNormal[i][j][1] <= minStdD)) {
                                m_modelNormal[i][j][1] = minStdD;
                            }
                        }
                        if ((m_modelNormal[i][j][1] <= 0)) {
                            m_modelNormal[i][j][1] = m_minStdDev;
                        }
                        if (Double.isInfinite(m_modelNormal[i][j][1])) {
                            m_modelNormal[i][j][1] = m_minStdDev;
                        }

                        // mean
                        m_modelNormal[i][j][0] /= m_modelNormal[i][j][2];
                    }
                }
            }
        }
    }

    /**
     * The E step of the EM algorithm. Estimate cluster membership probabilities.
     * 
     * @param inst the training instances
     * @param change_weights whether to change the weights
     * @return the average log likelihood
     * @throws Exception if computation fails
     */
    private double E(final InstancesTmp inst, final boolean change_weights) {

        double loglk = 0.0, sOW = 0.0;

        for (int l = 0; l < inst.numInstances(); l++) {

            final InstanceTmp in = inst.instance(l);

            loglk += in.weight() * logDensityForInstance(in);
            sOW += in.weight();

            if (change_weights) {
                m_weights[l] = distributionForInstance(in);
            }
        }

        return loglk / sOW;
    }

    /**
     * Constructor.
     * 
     **/
    public ExtendedEM() {
        m_seedDefault = 100;
        m_maxIterations = 100;
    }

    /**
     * Return the normal distributions for the cluster models
     * 
     * @return a <code>double[][][]</code> value
     */
    public double[][][] getClusterModelsNumericAtts() {
        return m_modelNormal;
    }

    /**
     * Updates the minimum and maximum values for all the attributes based on a new instance.
     * 
     * @param instance the new instance
     */
    private void updateMinMax(final InstanceTmp instance) {

        for (int j = 0; j < m_theInstances.numAttributes(); j++) {
            if (!instance.isMissing(j)) {
                if (Double.isNaN(m_minValues[j])) {
                    m_minValues[j] = instance.value(j);
                    m_maxValues[j] = instance.value(j);
                } else {
                    if (instance.value(j) < m_minValues[j]) {
                        m_minValues[j] = instance.value(j);
                    } else {
                        if (instance.value(j) > m_maxValues[j]) {
                            m_maxValues[j] = instance.value(j);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates a clusterer. Has to initialize all fields of the clusterer that are not being set via options.
     * 
     * @param data set of instances serving as training data
     * @throws Exception if the clusterer has not been generated successfully
     */
    public void buildClusterer(final InstancesTmp data) {
        m_theInstances = data;

        // calculate min and max values for attributes
        m_minValues = new double[m_theInstances.numAttributes()];
        m_maxValues = new double[m_theInstances.numAttributes()];
        for (int i = 0; i < m_theInstances.numAttributes(); i++) {
            m_minValues[i] = m_maxValues[i] = Double.NaN;
        }
        for (int i = 0; i < m_theInstances.numInstances(); i++) {
            updateMinMax(m_theInstances.instance(i));
        }

        doEM();

        // save memory
        m_theInstances = new InstancesTmp(m_theInstances, 0);
    }

    /**
     * Perform the EM algorithm
     * 
     * @throws Exception if something goes wrong
     */
    private void doEM() {

        m_rr = new Random(getSeed());

        // throw away numbers to avoid problem of similar initial
        // numbers
        // from a similar seed
        for (int i = 0; i < 10; i++) {
            m_rr.nextDouble();
        }

        m_numAttribs = m_theInstances.numAttributes();

        // fit full training set
        EM_Init(m_theInstances);
        iterate(m_theInstances, m_verbose);
    }

    /**
     * iterates the E and M steps until the log likelihood of the data converges.
     * 
     * @param inst the training instances.
     * @param report be verbose.
     * @return the log likelihood of the data
     * @throws Exception if something goes wrong
     */
    private double iterate(final InstancesTmp inst, final boolean report) {

        int i;
        double llkold = 0.0;
        double llk = 0.0;

        boolean ok = false;
        int seed = getSeed();
        int restartCount = 0;
        while (!ok) {
            try {
                for (i = 0; i < m_maxIterations; i++) {
                    llkold = llk;
                    llk = E(inst, true);

                    if (report) {
                        System.out.println("Loglikely: " + llk);
                    }

                    if (i > 0) {
                        if ((llk - llkold) < 1e-6) {
                            break;
                        }
                    }
                    M(inst);
                }
                ok = true;
            } catch (final Exception ex) {
                ex.printStackTrace();
                seed++;
                restartCount++;
                m_rr = new Random(seed);
                for (int z = 0; z < 10; z++) {
                    m_rr.nextDouble();
                    m_rr.nextInt();
                }
                if (restartCount > 5) {
                    m_numClusters--;
                    restartCount = 0;
                }
                EM_Init(m_theInstances);
            }
        }

        return llk;
    }

    /**
     * Gets the seed for the random number generations
     * 
     * @return the seed for the random number generation
     */
    public int getSeed() {
        return m_seed;
    }

    /**
     * Computes the density for a given instance.
     * 
     * @param instance the instance to compute the density for
     * @return the density.
     * @exception Exception if the density could not be computed successfully
     */
    private int maxIndex(final double[] doubles) {

        double maximum = 0;
        int maxIndex = 0;

        for (int i = 0; i < doubles.length; i++) {
            if ((i == 0) || (doubles[i] > maximum)) {
                maxIndex = i;
                maximum = doubles[i];
            }
        }

        return maxIndex;
    }

    public double logDensityForInstance(final InstanceTmp instance) {

        final double[] a = logJointDensitiesForInstance(instance);
        final double max = a[maxIndex(a)];
        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            sum += Math.exp(a[i] - max);
        }

        return max + Math.log(sum);
    }

    private double[] logs2probs(final double[] a) {

        final double max = a[maxIndex(a)];
        double sum = 0.0;

        final double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = Math.exp(a[i] - max);
            sum += result[i];
        }

        normalize(result, sum);

        return result;
    }

    /**
     * Computes the log of the conditional density (per cluster) for a given instance.
     * 
     * @param inst the instance to compute the density for
     * @return an array containing the estimated densities
     * @throws Exception if the density could not be computed successfully
     */
    public double[] logDensityPerClusterForInstance(final InstanceTmp inst) {

        int i, j;
        double logprob;
        final double[] wghts = new double[m_numClusters];

        for (i = 0; i < m_numClusters; i++) {
            logprob = 0.0;

            for (j = 0; j < m_numAttribs; j++) {
                if (!inst.isMissing(j)) {
                    logprob += logNormalDens(inst.value(j), m_modelNormal[i][j][0], m_modelNormal[i][j][1]);
                }
            }

            wghts[i] = logprob;
        }
        return wghts;
    }

    /**
     * Returns the cluster priors.
     * 
     * @return the cluster priors
     */
    public double[] clusterPriors() {

        final double[] n = new double[m_priors.length];

        System.arraycopy(m_priors, 0, n, 0, n.length);
        return n;
    }

    /** Constant for normal distribution. */
    private static double m_normConst = Math.log(Math.sqrt(2 * Math.PI));

    /**
     * Density function of normal distribution.
     * 
     * @param x input value
     * @param mean mean of distribution
     * @param stdDev standard deviation of distribution
     * @return the density
     */
    private double logNormalDens(final double x, final double mean, final double stdDev) {

        final double diff = x - mean;
        return -((diff * diff) / (2 * stdDev * stdDev)) - m_normConst - Math.log(stdDev);
    }

    public class DiscreteEstimator {
        private final double[] m_Counts;

        private double m_SumOfCounts;

        public DiscreteEstimator(final int numSymbols, final boolean laplace) {

            m_Counts = new double[numSymbols];
            m_SumOfCounts = 0;
            if (laplace) {
                for (int i = 0; i < numSymbols; i++) {
                    m_Counts[i] = 1;
                }
                m_SumOfCounts = numSymbols;
            }
        }

        public void addValue(final double data, final double weight) {

            m_Counts[(int)data] += weight;
            m_SumOfCounts += weight;
        }

        public double getProbability(final double data) {

            if (m_SumOfCounts == 0) {
                return 0;
            }
            return m_Counts[(int)data] / m_SumOfCounts;
        }
    }

    public double[] distributionForInstance(final InstanceTmp instance) {

        return logs2probs(logJointDensitiesForInstance(instance));
    }

    /**
     * Returns the logs of the joint densities for a given instance.
     * 
     * @param inst the instance
     * @return the array of values
     * @exception Exception if values could not be computed
     */
    public double[] logJointDensitiesForInstance(final InstanceTmp inst) {

        final double[] weights = logDensityPerClusterForInstance(inst);
        final double[] priors = clusterPriors();

        for (int i = 0; i < weights.length; i++) {
            if (priors[i] > 0) {
                weights[i] += Math.log(priors[i]);
            }
        }
        return weights;
    }
}
