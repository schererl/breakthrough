package mcts.transpos;

import java.text.DecimalFormat;

/**
 * Created by Tom Pepels (tpepels@gmail.com) on 13/07/15.
 */
public class ShotState {
    private static final DecimalFormat df2 = new DecimalFormat("###,##0.000");

    public static double INF = 999999;
    public long hash;
    public double visits = 0, lastVisit = 0, budgetSpent = 0;
    private double[] wins = {0, 0};
    private short solvedPlayer = 0;
    public boolean visited = false;
    //
    public ShotState next = null;

    public ShotState(long hash) {
        this.hash = hash;
    }

//    public void updateStats(int winner) {
//        visited = true;
//        if (solvedPlayer != 0)
//            throw new RuntimeException("updateStats called on solved position!");
//        this.wins[winner - 1]++;
//        this.visits++;
//    }

    public void init(int wins, int player, int visits) {
        this.wins[player - 1] += wins;
        this.visits += visits;
    }

    public void setValue(ShotState s) {
        if (s == null)
            throw new NullPointerException("State is null");
        visited = true;
        this.visits = s.visits;
        this.wins[0] = s.wins[0];
        this.wins[1] = s.wins[1];
    }

    public void updateStats(double n, double p1, double p2) {
        visited = true;
        this.visits += n;
        wins[0] += p1;
        wins[1] += p2;
    }

    public double getMean(int player) {
        if (player != 1 && player != 2)
            throw new RuntimeException("Invalid player " + player + " in getMean");
        visited = true;
        if (solvedPlayer == 0) { // Position is not solved, return mean
            if (visits > 0)
                return (wins[player - 1] - wins[(3 - player) - 1]) / visits;
            else
                return 0;
        } else    // Position is solved, return inf
            return (player == solvedPlayer) ? INF : -INF;
    }

    public void setSolved(int player) {
        if (player != 1 && player != 2)
            throw new RuntimeException("Invalid player " + player + " in setSolved");
        visited = true;
        if (solvedPlayer > 0 && player != solvedPlayer)
            throw new RuntimeException("setSolved with different player!");
        this.solvedPlayer = (short) player;
    }

    public void incrBudgetSpent(double incr) {
        this.budgetSpent += incr;
    }

    public double getBudgetSpent() {
        return budgetSpent;
    }

    public double getVisits() {
        return visits;
    }

    public String toString() {
        if (solvedPlayer == 0)
            return df2.format(getMean(1)) + "\tn:" + visits;
        else
            return "solved win P" + solvedPlayer;
    }
}
