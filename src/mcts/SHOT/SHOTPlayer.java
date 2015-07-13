package mcts.SHOT;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.Options;
import mcts.transpos.ShotTransposTable;

public class SHOTPlayer implements AIPlayer {

    private ShotTransposTable tt = new ShotTransposTable();
    private SHOTNode root;
    private int[] bestMove;
    public int total = 0;
    public long totalTime = 0;
    // Fields that must be set
    private Options options = null;

    public void getMove(Board board) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        //
        SHOTNode.maxDepth = 0;
        SHOTNode.totalPlayouts = 0;
        //
        root = new SHOTNode(board.getPlayerToMove(), null, options, board.hash(), tt);
        int[] pl = {0, 0, 0, 0};
        long startT = System.currentTimeMillis();
        root.SHOT(board.clone(), 0, options.timeLimit, pl);
        long endT = System.currentTimeMillis();
        // Return the best move found
        SHOTNode bestChild = root.selectBestMove();
        bestMove = bestChild.getMove();
        // show information on the best move
        if (options.debug) {
            System.out.println("Player " + board.getPlayerToMove());
            System.out.println("Best child: " + bestChild);
            System.out.println("Play-outs: " + pl[3]);
            System.out.println("Play-outs check: " + SHOTNode.totalPlayouts);
            System.out.println("Max sr depth: " + SHOTNode.maxDepth);
            System.out.println((int) ((1000. * SHOTNode.totalPlayouts) / (endT - startT)) + " playouts per s");
        }
        total += SHOTNode.totalPlayouts;
        totalTime += endT - startT;
        int removed = tt.pack(0);
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

