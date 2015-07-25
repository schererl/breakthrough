package framework.util;
/*
 * Stores stats without keep track of the actual numbers.
 *
 * Implements Knuth's online algorithm for variance, first one
 * found under "Online Algorithm" of
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
 *
 */

public class StatCounter {
    public double m_sum, m_m2, m_mean;
    private int m_n;

    public StatCounter() {
        this.reset();
    }

    public void reset() {
        m_sum = 0.0;
        m_m2 = 0.0;
        m_mean = 0.0;
        m_n = 0;
    }

    public void push(double num) {
        if (Double.isInfinite(m_sum) || Double.isInfinite(m_mean))
            throw new RuntimeException("Something is infinite in push");
        m_n++;

        m_sum += num;
        double delta = num - m_mean;
        m_mean += delta / m_n;
        m_m2 += delta * (num - m_mean);
        //
        if (Double.isInfinite(m_sum) || Double.isInfinite(m_mean))
            throw new RuntimeException("Something is infinite in push");
        if (Double.isNaN(m_mean))
            throw new RuntimeException("Mean is NaN in push");
    }

    public double variance() {
        return m_m2 / (double) m_n;
    }

    public double stddev() {
        return Math.sqrt(variance());
    }

    public double mean() {
        if (Double.isNaN(m_mean))
            throw new RuntimeException("Mean is NaN in getMean");
        return m_mean;
    }

    public int visits() {
        return m_n;
    }

    public String toString() {
        return "value: " + mean() + " visits: " + visits();
    }
}
