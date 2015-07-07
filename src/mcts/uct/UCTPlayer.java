package mcts.uct;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.Options;
import mcts.transpos.State;
import mcts.transpos.TransposTable;

public class UCTPlayer implements AIPlayer {

    private TransposTable tt = new TransposTable();
    public UCTNode root;
    private int[] bestMove;
    //
    private Options options;

    @Override
    public void getMove(Board board) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");

        root = new UCTNode(board.getPlayerToMove(), options, board, tt);

        if (options == null)
            throw new RuntimeException("MCTS Options not set.");

        int simulations = 0;
        long startT = System.currentTimeMillis();
        if (!options.fixSimulations) {
            // Search for timeInterval seconds
            long endTime = System.currentTimeMillis() + options.timeLimit;
            // Run the MCTS algorithm while time allows it
            while (true) {
                simulations++;
                if (System.currentTimeMillis() >= endTime)
                    break;
                // Make one simulation from root to leaf.
                if (Math.abs(root.MCTS(board.clone())) == State.INF)
                    break; // Break if you find a winning move

                if (options.debug && simulations % 5000 == 0)
                    System.out.println("PV: " + root.getPV());
            }
        } else {
            // Run as many simulations as allowed
            while (simulations <= options.timeLimit) {
                simulations++;
                // Make one simulation from root to leaf.
                // Note: stats at the root node are in view of the root player (also never used)
                if (Math.abs(root.MCTS(board.clone())) == State.INF)
                    break; // Break if you find a winning move
            }
        }
        long endT = System.currentTimeMillis();
        // Return the best move found
        UCTNode bestChild = root.getBestChild();
        bestMove = bestChild.move;
        // Pack the transpositions
        int removed = tt.pack(1);
        // show information on the best move
        if (options.debug) {
            System.out.println("- PV: " + root.getPV());
            System.out.println("- Player " + board.getPlayerToMove());
            System.out.println("- Did " + simulations + " simulations");
            System.out.println("- Best child: " + bestChild);
            System.out.println("- " + (int) Math.round((1000. * simulations) / (endT - startT)) + " playouts per sec.");
            System.out.println(":: Pack cleaned: " + removed + " transpositions");
            System.out.println(":: Collisions: " + tt.collisions + ", tps: " + tt.positions);
            System.out.println(":: Recoveries: " + tt.recoveries);
        }
        // Set the root to the best child, so in the next move, the opponent's move can become the new root
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

