package mcts.transpos;

import java.text.DecimalFormat;

public class State {

    private static final DecimalFormat df2 = new DecimalFormat("###,##0.000");

    public static float INF = 999999;
    public long hash;
    public int visits = 0, lastVisit = 0;
    private float sum;
    public short solvedPlayer = 0;
    public boolean visited = false;
    //
    public State next = null;

    public State(long hash) {
        this.hash = hash;
    }

    public void updateStats(double score) {
        visited = true;
        if (solvedPlayer != 0)
            throw new RuntimeException("updateStats called on solved position!");
        sum += score;
//        addSample(winner);
        this.visits++;
    }

    public void init(int player, int wins, int visits) {
        // WTF?
    }

    public float getMean(int player) {
        visited = true;
        if (solvedPlayer == 0) { // Position is not solved, return mean
            if (visits > 0)
                return sum / visits;
            else
                return 0;
        } else    // Position is solved, return inf
            return (player == solvedPlayer) ? INF : -INF;
    }

    public void setSolved(int player) {
        visited = true;
        if (solvedPlayer > 0 && player != solvedPlayer)
            throw new RuntimeException("setSolved with different player!");
        this.solvedPlayer = (short) player;
    }

    public int getVisits() {
        return visits;
    }

    public String toString() {
        if (solvedPlayer == 0)
            return df2.format(sum) + "\tn:" + visits; // + "\tKL:" + df2.format(getKL());
        else
            return "solved win P" + solvedPlayer;
    }

//    private boolean[] samples = new boolean[2];
//
//    private void addSample(int winner) {
//        if (visits >= samples.length) {
//            boolean[] newSamples = new boolean[samples.length * 2];
//            System.arraycopy(samples, 0, newSamples, 0, samples.length);
//            samples = newSamples;
//        }
//        samples[visits] = (winner == 1);
//    }
//
//    public double getKL() {
//        double p = 0, q = 0;
//        int vis = visits / 2;
//        for (int i = 0; i < vis; i++) {
//            if (samples[i])
//                p++;
//            if (samples[vis + i])
//                q++;
//        }
////        p = p / vis;
//        p = (p + q) / visits;
//        q = q / vis;
//
//        return p * Math.log(p / q) + (1 - p) * Math.log((1 - p) / (1 - q));
//    }
}
