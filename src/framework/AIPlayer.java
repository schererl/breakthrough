package framework;


import breakthrough.game.Board;

public interface AIPlayer {
    void getMove(Board board);

    void setOptions(Options options);

    int[] getBestMove();
}

