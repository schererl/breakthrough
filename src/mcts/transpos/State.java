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
        this.visits++;
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
            return df2.format(getMean(1)) + "\tn:" + visits; // + "\tKL:" + df2.format(getKL());
        else
            return "solved win P" + solvedPlayer;
    }
}
