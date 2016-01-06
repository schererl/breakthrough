package breakthrough;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.Options;
import framework.util.KeyboardPlayer;
import mcts.H_MCTS.HybridPlayer;
import mcts.SHOT.SHOTPlayer;
import mcts.uct.UCTPlayer;

import java.util.Arrays;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        AIPlayer aiPlayer1 = new SHOTPlayer();
        Options options1 = new Options();
        options1.fixSimulations = true;
        options1.solver = true;
        options1.timeLimit = 300000;
        options1.heuristics = true;
        options1.earlyTerm = true;
        aiPlayer1.setOptions(options1);

        AIPlayer aiPlayer2 = new SHOTPlayer();
        Options options2 = new Options();
        options2.solver = true;
        options2.heuristics = true;
        options2.fixSimulations = true;
        options2.timeLimit = 300000;
        options2.earlyTerm = true;
        options2.UBLB = true;
        aiPlayer2.setOptions(options2);


        int timedPlayer = -1;
        boolean timed = options1.timed || options2.timed;
        if (timed) {
            // Initially, both players are assigned the same budget
            options1.fixSimulations = true;
            options2.fixSimulations = true;
            timedPlayer = (options1.timed) ? 1 : 2;
        }

        AIPlayer aiPlayer;
        int[] m;
        //
        while (b.checkWin() == Board.NONE_WIN) {
            int player = b.getPlayerToMove();
            System.out.println(b.toString());
            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2);
            System.gc();

            long startTime = System.currentTimeMillis();
            aiPlayer.getMove(b.clone());
            long time = System.currentTimeMillis() - startTime;
            m = aiPlayer.getBestMove();
            b.doMove(m, true);

            if (m != null)
                System.out.println("Player " + player + " played " + Arrays.toString(m));

            if (timed && player == timedPlayer) {
                // Allocate the time spent to the non-fixed player
                Options opt = (timedPlayer == 1) ? options2 : options1;
                opt.fixSimulations = false;
                opt.timeLimit = (int)Math.max(100, time);
            }
        }

        System.out.println("Winner is " + b.checkWin());
    }

}


