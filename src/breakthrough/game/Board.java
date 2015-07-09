package breakthrough.game;

import framework.MoveList;
import framework.Options;

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
    private int pieces1, pieces2;
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

        // check for a capture
        if (capture) {
            if (playerToMove == 1)
                pieces2--;
            else
                pieces1--;
        }

        // check for a win
        if (playerToMove == 1 && (to / 8 == startR || pieces2 == 0)) winner = 1;
        else if (playerToMove == 2 && (to / 8 == (endR - 1) || pieces1 == 0)) winner = 2;

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
        //
        if (to / 8 > maxR)
            maxR = to / 8;
        else if (to / 8 < minR)
            minR = to / 8;

        if (to % 8 > maxC)
            maxC = to % 8;
        else if (to % 8 < minC)
            minC = to % 8;
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

    int startC = 0, endC = 8, startR = 0, endR = 8;
    int minR = 64, minC = 64, maxC = 0, maxR = 0;

    public void startSubGame() {

        if (minR == 64 || minC == 64 || maxC == 0 || maxR == 0)
            return;

        int value = Options.r.nextInt(3);
        startR = minR - value;
        endR = maxR + value;
        while (endR - 1 <= startR) {
            endR += 1;
            startR -= 1;
        }
        if (endR > 8)
            endR = 8;
        if (startR < 0)
            startR = 0;

        startC = minC - Options.r.nextInt(3);
        endC = maxC + Options.r.nextInt(3);
        while (endC - 1 <= startC) {
            endC += 1;
            startC -= 1;
        }
        if (endC > 8)
            endC = 8;
        if (startC < 0)
            startC = 0;
//
//        while (getPlayoutMoves().size() == 0) {
//            value = Options.r.nextInt(3);
//            startR = value;
//            endR = 8 - value;
//            //
//            startC = Options.r.nextInt(3);
//            endC = 8 - Options.r.nextInt(3);
//        }
//        for (int r = startR; r < endR; r++) {
//            for (int c = startC; c < endC; c++) {
//                System.out.print(board[r * 8 + c]);
//            }
//            System.out.print("\n");
//        }
//        System.out.println();
    }

    public void increaseSubGame() {
//        for (int r = startR; r < endR; r++) {
//            for (int c = startC; c < endC; c++) {
//                System.out.print(board[r * 8 + c]);
//            }
//            System.out.print("\n");
//        }
//        System.out.println();

        if (startR > 0 && endR < 8) {
            startR--; // Vertical 2
            endR++;
        } else if (startR > 0) {
            startR--; // Vertical 1
        } else if (endR < 8) {
            endR++; // Vertical 1
        } else if (startC > 0) {
            startC--; // Horizontal 1
        } else if (endC < 8) {
            endC++; // Horizontal 1
        }
//        for (int r = startR; r < endR; r++) {
//            for (int c = startC; c < endC; c++) {
//                System.out.print(board[r * 8 + c]);
//            }
//            System.out.print("\n");
//        }
//        System.out.println("----------------");
    }

    public MoveList getPlayoutMoves() {
        MoveList allMoves = new MoveList(96);
        int from, to;
        for (int r = startR; r < endR; r++) {
            for (int c = startC; c < endC; c++) {
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

    private boolean inBounds(int r, int c) {
        return (r >= startR && c >= startC && r < endR && c < endC);
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
                .append(") nMoves: ").append(nMoves).append("\n");
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