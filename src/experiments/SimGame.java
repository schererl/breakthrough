package experiments;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.Options;
import framework.util.FastLog;
import mcts.H_MCTS.HybridPlayer;
import mcts.SHOT.SHOTPlayer;
import mcts.uct.UCTPlayer;

/**
 * Runs a single experiment. Options are sent by command-line.
 */

public class SimGame {

    private String p1label, p2label;
    private AIPlayer player1, player2;
    private int timeLimit;
    private long seed;
    private boolean printBoard;
    private boolean mctsDebug;

    public SimGame() {
        p1label = "none specified";
        p2label = "none specified";
        player1 = null;
        player2 = null;
        timeLimit = 1000;
        printBoard = false;
        mctsDebug = false;

        seed = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SimGame sim = new SimGame();
        sim.parseArgs(args);
        sim.run();
    }

    public void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--p1")) {
                i++;
                p1label = args[i];
            } else if (args[i].equals("--p2")) {
                i++;
                p2label = args[i];
            } else if (args[i].equals("--timelimit")) {
                i++;
                timeLimit = Integer.parseInt(args[i]);
            } else if (args[i].equals("--seed")) {
                i++;
                seed = Long.parseLong(args[i]);
                Options.r.setSeed(seed);
            } else if (args[i].equals("--printboard")) {
                printBoard = true;
            } else if (args[i].equals("--game")) {
                i++;
            } else {
                throw new RuntimeException("Unknown option: " + args[i]);
            }
        }
    }

    public void loadPlayer(int player, String label) {
        AIPlayer playerRef;

        String[] parts = label.split("_");
        Options options = new Options();

        if (parts[0].equals("uct")) {

            playerRef = new UCTPlayer();
            options.debug = mctsDebug; // false by default
            options.timeLimit = timeLimit;
        } else if (parts[0].equals("shot")) {
            playerRef = new SHOTPlayer();
            options.debug = mctsDebug; // false by default
            options.timeLimit = timeLimit;
        } else if (parts[0].equals("hybrid")) {
            playerRef = new HybridPlayer();
            options.debug = mctsDebug; // false by default
            options.timeLimit = timeLimit;
        } else {
            throw new RuntimeException("Unrecognized player: " + label);
        }

        // now, parse the tags
        for (int i = 1; i < parts.length; i++) {
            String tag = parts[i];
            if (tag.startsWith("h")) {
                options.heuristics = true;
            } else if (tag.startsWith("b")) {
                options.B = Integer.parseInt(tag.substring(1));
            } else if (tag.startsWith("s")) {
                options.solver = true;
            } else if (tag.startsWith("et")) {
                options.earlyTerm = true;
                options.termDepth = Integer.parseInt(tag.substring(2));
            } else if (tag.startsWith("ert")) {
                options.earlyTerm = true;
                options.etT = Integer.parseInt(tag.substring(3));
            } else if (tag.startsWith("c")) {
                options.C = Double.parseDouble(tag.substring(1));
            } else if (tag.equals("le")) {
                options.lorenzEval = true;
            } else if (tag.equals("tt")) {
                options.tt = true;
            } else {
                throw new RuntimeException("Unrecognized tag: " + tag);
            }
        }


        // Now, set the player
        if (player == 1) {
            player1 = playerRef;
            player1.setOptions(options);
        } else if (player == 2) {
            player2 = playerRef;
            player2.setOptions(options);
        }
    }

    public void run() {

        System.out.println("Starting game simulation...");

        System.out.println("P1: " + p1label);
        System.out.println("P2: " + p2label);
        System.out.println("");

        Board board = new Board();
        board.initialize();

        loadPlayer(1, p1label);
        loadPlayer(2, p2label);

        // Initialize the fast... stuff
        FastLog.log(1.);

        int[] m;
        while (board.checkWin() == Board.NONE_WIN) {

            if (printBoard)
                System.out.println(board.toString());

            int p = board.getPlayerToMove();
            AIPlayer aiPlayer = (p == 1 ? player1 : player2);
            System.gc();

            aiPlayer.getMove(board.clone());
            m = aiPlayer.getBestMove();
            board.doMove(m, true);

        }

        // Do not change the format of this line. Used by results aggregator scripts/parseres.perl
        System.out.println("Game over. Winner is " + board.checkWin());
    }
}

