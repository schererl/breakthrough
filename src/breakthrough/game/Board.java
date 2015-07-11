package breakthrough.game;

import framework.MoveList;
import framework.Options;
import mcts.transpos.State;

import java.util.Random;

public class Board {
    public static final int P1 = 1, NONE_WIN = -1, CAPTURED = -1, PIECES = 16;
    private static final int[] lorentzValues =
            {5, 15, 15, 5, 5, 15, 15, 5,
                    2, 3, 3, 3, 3, 3, 3, 2,
                    4, 6, 6, 6, 6, 6, 6, 4,
                    7, 10, 10, 10, 10, 10, 10, 7,
                    11, 15, 15, 15, 15, 15, 15, 11,
                    16, 21, 21, 21, 21, 21, 21, 16,
                    20, 28, 28, 28, 28, 28, 28, 20,
                    36, 36, 36, 36, 36, 36, 36, 36};
    private static final String rowLabels = "87654321", colLabels = "abcdefgh";
    // Zobrist stuff
    private static long[][] zbnums = null;
    private static long blackHash, whiteHash;
    // Board stuff
    public int[] board, pieces[];
    public short nMoves, winner, playerToMove;
    private int nPieces1, progress1, lorentzPV1, nPieces2, progress2, lorentzPV2;
    private long zbHash = 0;

    public void initialize() {
        board = new int[64];
        pieces = new int[2][PIECES];
        playerToMove = P1;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (r == 0 || r == 1) {
                    board[r * 8 + c] = 200 + nPieces2; // player 2 is black
                    pieces[1][nPieces2] = r * 8 + c;
                    nPieces2++;
                    lorentzPV2 += getLorentzPV(2, r * 8 + c);
                } else if (r == 6 || r == 7) {
                    board[r * 8 + c] = 100 + nPieces1; // player 1 is white
                    pieces[0][nPieces1] = r * 8 + c;
                    nPieces1++;
                    lorentzPV1 += getLorentzPV(1, r * 8 + c);
                }
            }
        }

        nMoves = 0;
        winner = NONE_WIN;
        progress1 = 1;
        progress2 = 1;

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
        for (int i = 0; i < 8 * 8; i++)
            zbHash ^= zbnums[i][board[i] / 100];
        zbHash ^= whiteHash;
    }

    public void doMove(int[] move, boolean updateEval) {
        int from = move[0], to = move[1];

        zbHash ^= zbnums[from][playerToMove];

        boolean capture = board[to] != 0;
        int pieceCap = board[to] % 100;

        if (!capture)
            zbHash ^= zbnums[to][0];
        else
            zbHash ^= zbnums[to][3 - playerToMove];

        // Move the piece in the reverse lookup table
        pieces[playerToMove - 1][board[from] % 100] = to;

        board[to] = board[from];
        board[from] = 0;

        if(updateEval) {
            // lorentz piece value updates:
            // subtract off from where you came, add where you ended up
            if (playerToMove == 1) {
                lorentzPV1 -= getLorentzPV(1, from);
                lorentzPV1 += getLorentzPV(1, to);
            } else {
                lorentzPV2 -= getLorentzPV(2, from);
                lorentzPV2 += getLorentzPV(2, to);
            }
        }

        int rp = to / 8;

        // check for a capture
        if (capture) {
            if (playerToMove == 1) {
                nPieces2--;
                pieces[1][pieceCap] = CAPTURED;
                // wiping out this piece could reduce the player's progress
                if (progress2 == rp && nPieces2 > 0)
                    recomputeProgress(2);
                if(updateEval) {
                    // The player loses the piece's lorentz value
                    lorentzPV2 -= getLorentzPV(2, to);
                }
            } else {
                nPieces1--;
                pieces[0][pieceCap] = CAPTURED;
                //
                if (progress1 == 7 - rp && nPieces1 > 0)
                    recomputeProgress(1);
                if(updateEval) {
                    // The player loses the piece's lorentz value
                    lorentzPV1 -= getLorentzPV(1, to);
                }
            }

        }

        // check for a win
        if (playerToMove == 1 && (rp == 0 || nPieces2 == 0)) winner = 1;
        else if (playerToMove == 2 && (rp == (8 - 1) || nPieces1 == 0)) winner = 2;

        // check for progress (furthest pawn)
        if (playerToMove == 1 && (7 - rp) > progress1) progress1 = 7 - rp;
        else if (playerToMove == 2 && rp > progress2) progress2 = rp;

        zbHash ^= zbnums[to][playerToMove];
        zbHash ^= zbnums[from][0];

        nMoves++;
        playerToMove = (short) (3 - playerToMove);

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
        int moveMode = (playerToMove == 1) ? -1 : 1;
        int[] playerPieces = pieces[playerToMove - 1];
        for (int playerPiece : playerPieces) {
            if (playerPiece == CAPTURED)
                continue;
            generateMovesForPiece(playerPiece, moveMode, allMoves);
        }
        return allMoves;
    }

    public void generateMovesForPiece(int from, int moveMode, MoveList moveList) {
        int r = from / 8, c = from % 8, to;
        // Generate the moves!
        if (inBounds(r + moveMode, c - 1)) {
            to = (r + moveMode) * 8 + (c - 1);
            // northwest
            if (board[to] / 100 != playerToMove)
                moveList.add(from, to);
        }
        if (inBounds(r + moveMode, c + 1)) {
            to = (r + moveMode) * 8 + (c + 1);
            // northeast
            if (board[to] / 100 != playerToMove)
                moveList.add(from, to);
        }
        if (inBounds(r + moveMode, c)) {
            to = (r + moveMode) * 8 + c;
            // north
            if (board[to] == 0)
                moveList.add(from, to);
        }
    }

    public int evaluate(int player) {
        int p1eval = 10 * (nPieces1 - nPieces2);
        p1eval += lorentzPV1 - lorentzPV2;
        return (player == 1 ? p1eval : -p1eval);
    }

    private void recomputeProgress(int player) {
        int[] playerPieces = pieces[player - 1];
        if (player == 1) {
            int min = 100;
            for (int piece : playerPieces) {
                if (piece == CAPTURED)
                    continue;
                if (piece / 8 < min) {
                    min = piece / 8;
                    progress1 = 7 - min;
                }
            }
        } else if (player == 2) {
            int max = -1;
            for (int piece : playerPieces) {
                if (piece == CAPTURED)
                    continue;
                if (piece / 8 > max) {
                    max = piece / 8;
                    progress2 = max;
                }
            }
        }
    }

    private int getLorentzPV(int player, int position) {
        if (player == 2) {
            if (isSafe(position))
                return lorentzValues[position] + (int) (0.5 * lorentzValues[position]);
            else
                return lorentzValues[position];
        } else {
            if (isSafe(position))
                return lorentzValues[63 - position] + (int) (0.5 * lorentzValues[63 - position]);
            else
                return lorentzValues[63 - position];
        }
    }


    public MoveList getPlayoutMoves(boolean heuristics) {
        // Check for decisive / anti-decisive moves
        if (heuristics && (progress1 >= 6 || progress2 >= 6)) {
            MoveList moveList = getExpandMoves();
            MoveList decisive = new MoveList(32);
            MoveList antiDecisive = new MoveList(32);
            for (int i = 0; i < moveList.size(); i++) {
                int[] move = moveList.get(i);
                // Decisive / anti-decisive moves
                if (playerToMove == 1 && (move[1] / 8 == 0)) {
                    decisive.add(move[0], move[1]);
                } else if (playerToMove == 2 && (move[1] / 8 == 7)) {
                    decisive.add(move[0], move[1]);
                } else if (board[move[1]] != 0 && (move[0] / 8 == 7 || move[0] / 8 == 0)) {
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
        // Select a piece uniformly random and generate its moves
        // This should remove any bias towards selecting pieces with more available moves
        int[] playerPieces = pieces[playerToMove - 1];
        int pieceI;
        MoveList moveList = new MoveList(3);
        while (moveList.size() == 0) {
            pieceI = Options.r.nextInt(PIECES);
            //
            if (playerPieces[pieceI] != CAPTURED) {
                generateMovesForPiece(playerPieces[pieceI], (playerToMove == 1) ? -1 : 1, moveList);
                if(heuristics && moveList.size() > 1) {
                    MoveList heurMoves = new MoveList(32);
                    for(int i = 0; i < moveList.size(); i++) {
                        int[] move = moveList.get(i);
                        if (board[move[1]] != 0) {
                            heurMoves.add(move[0], move[1]);
                            heurMoves.add(move[0], move[1]);
                            heurMoves.add(move[0], move[1]);
                        }
                        if (isSafe1(move[1], playerToMove)) {
                            heurMoves.add(move[0], move[1]);
                            heurMoves.add(move[0], move[1]);
                        }
                        heurMoves.add(move[0], move[1]);
                    }
                    return heurMoves;
                }
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
                int player = board[r * 8 + c] / 100;
                switch (player) {
                    case 1:
                        sb.append('w');
                        break;
                    case 2:
                        sb.append('b');
                        break;
                    case 0:
                        sb.append('.');
                }
            }
            sb.append("\n");
        }
        sb.append(" ").append(colLabels).append("\n");
        sb.append("\nPieces: (").append(nPieces1).append(", ").append(nPieces2)
                .append(") nMoves: ").append(nMoves).append("\n").append("Lorentz: ")
                .append(lorentzPV1).append(" ").append(lorentzPV2);
        return sb.toString();
    }

    @Override
    public Board clone() {
        Board b = new Board();
        b.board = new int[64];
        // Only copy the actual pieces on the board, instead of the full board
        int piece;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < PIECES; j++) {
                piece = pieces[i][j];
                if (piece == CAPTURED)
                    continue;
                b.board[piece] = board[piece];
            }
        }
        b.pieces = new int[2][PIECES];
        System.arraycopy(pieces[0], 0, b.pieces[0], 0, 16);
        System.arraycopy(pieces[1], 0, b.pieces[1], 0, 16);
        b.nPieces1 = this.nPieces1;
        b.nPieces2 = this.nPieces2;
        b.nMoves = this.nMoves;
        b.winner = this.winner;
        b.progress1 = this.progress1;
        b.progress2 = this.progress2;
        b.lorentzPV1 = this.lorentzPV1;
        b.lorentzPV1 = this.lorentzPV2;
        b.playerToMove = this.playerToMove;
        b.zbHash = zbHash;
        return b;
    }

    private boolean isSafe1(int position, int player) {
        // White moves up (-1) black down (+1)
        int moveMode = (player == 1) ? -1 : 1;
        int opp = 3 - player;
        int r = position / 8, c = position % 8, to;
        // Check neighbouring positions for an opponent's piece
        if (inBounds(r + moveMode, c - 1)) {
            to = (r + moveMode) * 8 + (c - 1);
            // northwest
            if (board[to] / 100 == opp)
                return false;
        }
        if (inBounds(r + moveMode, c + 1)) {
            to = (r + moveMode) * 8 + (c + 1);
            // northeast
            if (board[to] / 100 == opp)
                return false;
        }
        return true;
    }


    private boolean isSafe(int position) {
        int rp = position / 8;
        int cp = position % 8;

        //assert(inBounds(rp,cp));
        int piece = board[position] / 100;

        // count immediate attackers and defenders
        int attackers = 0, defenders = 0;

        int[] rowOffset = {-1, -1, +1, +1};
        int[] colOffset = {-1, +1, -1, +1};

        for (int oi = 0; oi < 4; oi++) {
            int rpp = rp + rowOffset[oi];
            int cpp = cp + colOffset[oi];

            if (inBounds(rpp, cpp) && board[rpp * 8 + cpp] / 100 != 0) {
                if(piece == 1) {
                    if (oi < 2 && board[rpp * 8 + cpp] / 100 == 2)
                        attackers++;
                    if (oi >= 2 && board[rpp * 8 + cpp] / 100 == 1)
                        defenders++;
                } else {
                    if (oi < 2 && board[rpp * 8 + cpp] / 100 == 2)
                        defenders++;
                    if (oi >= 2 && board[rpp * 8 + cpp] / 100 == 1)
                        attackers++;
                }
            }
        }
        return attackers <= defenders;
    }

    public void initNodePriors(int parentPlayer, State state, int[] move, int npVisits) {
        boolean safeMove = isSafe1(move[1], parentPlayer);
        int rp = move[1] / 8;
        int distToGoal = (parentPlayer == 1 ? rp : (7 - rp));
        double winRate = 0.3;

        if (safeMove) {
            if (distToGoal == 0)
                winRate = 1.;
            else if (distToGoal == 1)
                winRate = 0.95;
            else if (distToGoal == 2)
                winRate = 0.85;
            else if (distToGoal == 3)
                winRate = 0.7;
            else if (distToGoal == 4)
                winRate = 0.6;
        } else {
            if (board[move[1]] != 0)
                winRate = 0.6;
        }

        //if (!safeMove) {
        //    System.out.println("unsafe move! " + bmove + "\n" + toString());
        // }
        //System.out.println("Node priors, wins = " + wins);

        // this causes a significant slowdown
        /*for (int i = 0; i < winrate*npvisits; i++)
            stats.push(1.0);
        for (int i = 0; i < (1.0-winrate)*npvisits; i++)
            stats.push(-1.0);*/

        state.init((int)(winRate * npVisits), npVisits);
    }


    public static String getMoveString(int[] move) {
        int c = move[0] % 8, cp = move[1] % 8;
        int r = move[0] / 8, rp = move[1] / 8;

        char cc = (char) (c + 97);
        char cpc = (char) (cp + 97);
        return String.format("%c%d%c%d", cc, 8 - r, cpc, 8 - rp);
    }
}