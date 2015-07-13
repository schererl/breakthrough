package mcts.H_MCTS;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.Options;
import mcts.transpos.ShotTransposTable;
import mcts.transpos.TransposTable;

public class HybridPlayer implements AIPlayer {

    private ShotTransposTable tt = new ShotTransposTable();
    private HybridNode root;
    private int[] bestMove;
    public int total = 0;
    public long totalTime = 0;
    // Fields that must be set
    private Options options = null;

    @Override
    public void getMove(Board board) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        HybridNode.totalPlayouts = 0;
        root = new HybridNode(board.getPlayerToMove(), null, options, board.hash(), tt);
        int[] pl = {0, 0, 0, 0};
        long startT = System.currentTimeMillis();
        root.HybridMCTS(board, 0, options.timeLimit, pl);
        long endT = System.currentTimeMillis();
        // Return the best move found
        HybridNode bestChild = root.selectBestMove();
        bestMove = bestChild.getMove();
        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + board.getPlayerToMove());
            System.out.println("Best child: " + bestChild);
            System.out.println("Play-outs: " + pl[3]);
            System.out.println("Play-outs check: " + HybridNode.totalPlayouts);
            System.out.println((int) ((1000. * HybridNode.totalPlayouts) / (endT - startT)) + " playouts per s");
        }
        total += HybridNode.totalPlayouts;
        totalTime += endT - startT;
        int removed = tt.pack(1);
        if (options.debug)
            System.out.println("Pack cleaned: " + removed + " transpositions");
        root = null;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    @Override
    public int[] getBestMove() {
        return bestMove;
    }
}

