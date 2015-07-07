package framework.util;

import breakthrough.game.Board;
import framework.*;

import java.util.Scanner;

public class KeyboardPlayer implements AIPlayer {

    private int[] theMove;

    public void getMove(Board board) {
        MoveList list = board.getExpandMoves();
        theMove = null;
        try {
            Scanner scanner = new Scanner(System.in);
            while (theMove == null) {
                System.out.print("Enter move: ");
                String line = scanner.nextLine();
                // Check if the move is valid
                for (int i = 0; i < list.size(); i++) {
                    int[] move = list.get(i);
                    if (Board.getMoveString(move).equals(line)) {
                        theMove = move;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setOptions(Options options) {
    }

    @Override
    public int[] getBestMove() {
        return theMove;
    }
}

