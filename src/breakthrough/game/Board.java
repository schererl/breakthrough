package breakthrough.game;

import framework.MoveList;
import framework.Options;
import framework.util.FastTanh;

import java.util.Random;

public class Board {
    public static final int P1 = 1, NONE_WIN = -1;
    private static final String rowLabels = "87654321";
    private static final String colLabels = "abcdefgh";
    // Zobrist stuff
    private static long[][] zbnums = null;
    private static long blackHash, whiteHash;
    // Board stuff
    public char[] board;
    public int nMoves, winner, playerToMove;
    private int pieces1, pieces2, progress1, progress2;
    private long zbHash = 0;

    public void initialize() {
        board = new char[64];
        pieces1 = pieces2 = 16;
        playerToMove = P1;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (r == 0 || r == 1)
                    board[r * 8 + c] = 'b'; // player 2 is black
                else if (r == 6 || r == 7)
                    board[r * 8 + c] = 'w'; // player 1 is white
                else board[r * 8 + c] = '.';
            }
        }

        nMoves = 0;
        winner = NONE_WIN;
        progress1 = progress2 = 1;

        // initialize the zobrist numbers
        if (zbnums == null) {
            // init the zobrist numbers
            Random rng = new Random();

            // 64 locations, 3 states for each location = 192
            zbnums = new long[8 * 8][3];

            for (int i = 0; i < 8 * 8; i++) {
                zbnums[i][0] = rng.nextLong();
                zbnums[i][1] = rng.nextLong();
                zbnums[i][2] = rng.nextLong();
            }
            whiteHash = rng.nextLong();
            blackHash = rng.nextLong();
        }
        // now build the initial hash
        zbHash = 0;
        for (int i = 0; i < 8 * 8; i++) {
            if (board[i] == '.')
                zbHash ^= zbnums[i][0];
            else if (board[i] == 'w')
                zbHash ^= zbnums[i][1];
            else if (board[i] == 'b')
                zbHash ^= zbnums[i][2];
        }
        zbHash ^= whiteHash;
    }

    public void doMove(int[] move) {
        int from = move[0], to = move[1];

        zbHash ^= zbnums[from][playerToMove];

        boolean capture = board[to] != '.';

        if (!capture)
            zbHash ^= zbnums[to][0];
        else
            zbHash ^= zbnums[to][3 - playerToMove];

        board[to] = board[from];
        board[from] = '.';
        int rp = to / 8;

        // check for a capture
        if (capture) {
            if (playerToMove == 1) {
                pieces2--;
                // wiping out this piece could reduce the player's progress
                if (progress2 == rp && pieces2 > 0)
                    recomputeProgress(2);
            } else {
                if (progress1 == 7 - rp && pieces1 > 0)
                    recomputeProgress(1);
                pieces1--;
            }
        }

        // check for a win
        if (playerToMove == 1 && (rp == 0 || pieces2 == 0)) winner = 1;
        else if (playerToMove == 2 && (rp == (8 - 1) || pieces1 == 0)) winner = 2;

        // check for progress (furthest pawn)
        if (playerToMove == 1 && (7 - rp) > progress1) progress1 = 7 - rp;
        else if (playerToMove == 2 && rp > progress2) progress2 = rp;

        zbHash ^= zbnums[to][playerToMove];
        zbHash ^= zbnums[from][0];

        nMoves++;
        playerToMove = 3 - playerToMove;

        if (playerToMove == Board.P1) {
            zbHash ^= blackHash;
            zbHash ^= whiteHash;
        } else {
            zbHash ^= whiteHash;
            zbHash ^= blackHash;
        }
    }

    public MoveList getExpandMoves() {
        MoveList allMoves = new MoveList(96);
        int from, to;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                from = r * 8 + c;
                if (playerToMove == 1 && board[from] == 'w') {
                    if (inBounds(r - 1, c - 1)) {
                        to = (r - 1) * 8 + (c - 1);
                        // northwest
                        if (board[to] != 'w')
                            allMoves.add(from, to);
                    }
                    if (inBounds(r - 1, c + 1)) {
                        to = (r - 1) * 8 + (c + 1);
                        // northeast
                        if (board[to] != 'w')
                            allMoves.add(from, to);
                    }
                    if (inBounds(r - 1, c)) {
                        to = (r - 1) * 8 + c;
                        // north
                        if (board[to] == '.')
                            allMoves.add(from, to);
                    }
                } else if (playerToMove == 2 && board[from] == 'b') {
                    if (inBounds(r + 1, c - 1)) {
                        to = (r + 1) * 8 + (c - 1);
                        // southwest
                        if (board[to] != 'b')
                            allMoves.add(from, to);
                    }
                    if (inBounds(r + 1, c + 1)) {
                        to = (r + 1) * 8 + (c + 1);
                        // southeast
                        if (board[to] != 'b')
                            allMoves.add(from, to);
                    }
                    if (inBounds(r + 1, c)) {
                        to = (r + 1) * 8 + c;
                        // south
                        if (board[to] == '.')
                            allMoves.add(from, to);
                    }
                }
            }
        }
        return allMoves;
    }

    public double evaluate(int player) {
        // inspired by ion function in Maarten's thesis
        double p1eval;
        if (progress1 == 7 || pieces2 == 0) p1eval = 1;
        else if (progress2 == 7 || pieces1 == 0) p1eval = -1;
        else {
            //double delta = (pieces1 * 10 + progress1 * 2.5 + capBonus1) - (pieces2 * 10 + progress2 * 2.5 + capBonus2);
            double delta = (pieces1 * 10 + progress1 * 2.5) - (pieces2 * 10 + progress2 * 2.5);
            if (delta < -100) delta = -100;
            if (delta > 100) delta = 100;
            // now pass it through tanh;
            p1eval = FastTanh.tanh(delta / 60.0);
        }
        return (player == 1 ? p1eval : -p1eval);
    }

    private void recomputeProgress(int player) {
        if (player == 1) {
            // white, start from top
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (board[r * 8 + c] == 'w') {
                        progress1 = 7 - r;
                        return;
                    }
                }
            }
        } else if (player == 2) {
            // black, start from bottom
            for (int r = 7; r >= 0; r--) {
                for (int c = 0; c < 8; c++) {
                    if (board[r * 8 + c] == 'b') {
                        progress2 = r;
                        return;
                    }
                }
            }
        }
    }

    public MoveList getPlayoutMoves(boolean heuristics) {
        MoveList moveList = getExpandMoves();
        if(heuristics) {
            MoveList decisive = new MoveList(16);
            MoveList antiDecisive = new MoveList(16);
            for (int i = 0; i < moveList.size(); i++) {
                int[] move = moveList.get(i);
                // Decisive / anti-decisive moves
                if (playerToMove == 1 && (move[1] / 8 == 0)) {
                    decisive.add(move[0], move[1]);
                } else if (playerToMove == 2 && (move[1] / 8 == 7)) {
                    decisive.add(move[0], move[1]);
                } else if (board[move[1]] != '.' && (move[0] / 8 == 7 || move[0] / 8 == 0)) {
                    antiDecisive.add(move[0], move[1]);
                }
            }
            if (antiDecisive.size() > 0) {
                return antiDecisive;
            }
            if (decisive.size() > 0) {
                return decisive;
            }
        }
        return moveList;
    }

    private boolean inBounds(int r, int c) {
        return (r >= 0 && c >= 0 && r < 8 && c < 8);
    }

    public int checkWin() {
        return winner;
    }

    public int getPlayerToMove() {
        return playerToMove;
    }

    public long hash() {
        return zbHash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        //
        for (int r = 0; r < 8; r++) {
            sb.append(rowLabels.charAt(r));
            for (int c = 0; c < 8; c++) {
                sb.append(board[r * 8 + c]);
            }
            sb.append("\n");
        }
        sb.append(" ").append(colLabels).append("\n");
        sb.append("\nPieces: (").append(pieces1).append(", ").append(pieces2)
                .append(") nMoves: ").append(nMoves).append("\n").append("Progresses: ")
                .append(progress1).append(" ").append(progress2);
        return sb.toString();
    }

    @Override
    public Board clone() {
        Board b = new Board();
        b.board = new char[64];
        System.arraycopy(board, 0, b.board, 0, board.length);
        b.pieces1 = this.pieces1;
        b.pieces2 = this.pieces2;
        b.nMoves = this.nMoves;
        b.winner = this.winner;
        b.progress1 = this.progress1;
        b.progress2 = this.progress2;
        b.playerToMove = this.playerToMove;
        b.zbHash = zbHash;
        return b;
    }

    public static String getMoveString(int[] move) {
        int c = move[0] % 8, cp = move[1] % 8;
        int r = move[0] / 8, rp = move[1] / 8;

        char cc = (char) (c + 97);
        char cpc = (char) (cp + 97);
        return String.format("%c%d%c%d", cc, 8 - r, cpc, 8 - rp);
    }
}