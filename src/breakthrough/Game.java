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

        AIPlayer aiPlayer1 = new UCTPlayer();
        Options options1 = new Options();
        options1.solver = true;
        options1.heuristics = true;
        options1.earlyTerm = true;
        options1.nodePriors = true;
        aiPlayer1.setOptions(options1);

        AIPlayer aiPlayer2 = new HybridPlayer();
        Options options2 = new Options();
        options2.solver = true;
        options2.heuristics = true;
        options2.earlyTerm = true;
        options2.nodePriors = true;
        aiPlayer2.setOptions(options2);

        AIPlayer aiPlayer;
        int[] m;
        //
        while (b.checkWin() == Board.NONE_WIN) {
            int player = b.getPlayerToMove();
            System.out.println(b.toString());
            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2);
            System.gc();

            aiPlayer.getMove(b.clone());
            m = aiPlayer.getBestMove();
            b.doMove(m, true);

            if (m != null)
                System.out.println("Player " + player + " played " + Arrays.toString(m));
        }

        System.out.println("Winner is " + b.checkWin());
    }

}


