package mcts.uct;

import breakthrough.game.Board;
import framework.MoveList;
import framework.Options;
import framework.util.FastLog;
import mcts.transpos.State;
import mcts.transpos.TransposTable;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

public class UCTNode {
    private static final DecimalFormat df2 = new DecimalFormat("###,##0.000");
    public int player;
    private long hash;
    //
    private final Options options;
    //
    private boolean expanded = false, simulated = false;
    private List<UCTNode> children;
    private final TransposTable tt;
    public final int[] move;
    private State state;

    /**
     * Constructor for the root
     */
    public UCTNode(int player, Options options, Board board, TransposTable tt) {
        this.player = player;
        this.options = options;
        this.tt = tt;
        this.hash = board.hash();
        this.state = tt.getState(hash, true);
        this.move = null;
    }

    /**
     * Constructor for internal node
     */
    public UCTNode(int player, int[] move, Options options, Board board, TransposTable tt) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.tt = tt;
        this.hash = board.hash();
        this.state = tt.getState(hash, true);
    }

    /**
     * Run the MCTS algorithm on the given node.
     *
     * @param board The current board
     * @return the currently evaluated playout value of the node
     */
    public double MCTS(Board board, int depth) {
        if (board.hash() != hash)
            throw new RuntimeException("Incorrect hash");

        UCTNode child = null;
        // First add some leafs if required
        if (!expanded) {
            // Expand returns any node that leads to a win
            child = expand(board.clone());
        }
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null) {
            if (isTerminal())
                child = this;
            else
                child = select();
        }
        double result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.getValue()) != State.INF) {
            // Execute the move represented by the child
            board.doMove(child.move, options.earlyTerm);
            // When a leaf is reached return the result of the playout
            if (!child.simulated) {
                result = child.playOut(board);
                child.updateStats(-result);
                child.simulated = true;
            } else {
                result = -child.MCTS(board, depth + 1);
            }
        } else {
            result = child.getValue();
        }

        // (Solver) If one of the children is a win, then I'm a win
        if (result == State.INF) {
            // If I have a win, my parent has a loss.
            setSolved(false);
            return result;
        } else if (result == -State.INF) {
            // (Solver) Check if all children are a loss
            for (UCTNode tn : children) {
                // Are all children a loss?
                if (tn.getValue() != result) {
                    // Return a single loss, if not all children are a loss
                    updateStats(1);
                    return -1;
                }
            }
            setSolved(true);
            return result; // always return in view of me
        }
        if(Math.abs(getValue()) != State.INF)
            // Update the results for the current node
            updateStats(result);
        else
            return -getValue();
        // Back-propagate the result always return in view of me
        return result;
    }

    private UCTNode expand(Board board) {
        expanded = true;
        int nextPlayer = (3 - board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        UCTNode winNode = null;
        // Generate all moves
        MoveList moves = board.getExpandMoves();
        if (children == null)
            children = new LinkedList<UCTNode>();
        //
        int winner = board.checkWin();
        // Board is terminal, don't expand
        if (winner != Board.NONE_WIN)
            return null;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            Board tempBoard = board.clone();
            // If the game is partial observable, we don't want to do the solver part
            tempBoard.doMove(moves.get(i), false);
            UCTNode child = new UCTNode(nextPlayer, moves.get(i), options, tempBoard, tt);

            if (Math.abs(child.getValue()) != State.INF) {
                // Check for a winner, (Solver)
                winner = tempBoard.checkWin();
                if (winner == player) {
                    winNode = child;
                    child.setSolved(true);
                } else if (winner == nextPlayer) {
                    child.setSolved(false);
                } else if (options.nodePriors && child.state == null) {
                    // Only initialize node priors is the state is not yet initialized somewhere else
                    tempBoard.initNodePriors(player, child.getState(), moves.get(i), options.npVisits);
                }
            }
            children.add(child);
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private UCTNode select() {
        UCTNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        // Use UCT down the tree
        double uctValue, np = getVisits();
        if (options.nodePriors) {
            for (UCTNode c : children)
                np += c.getVisits();
        }
        // Select a child according to the UCT Selection policy
        for (UCTNode c : children) {
            double nc = c.getVisits();
            // Always select a proven win
            if (c.getValue() == State.INF)
                uctValue = State.INF + Options.r.nextDouble();
            else if (c.getVisits() == 0 && c.getValue() != -State.INF) {
                // First, visit all children at least once
                uctValue = 100. + Options.r.nextDouble();
            } else if (c.getValue() == -State.INF) {
                uctValue = -State.INF + Options.r.nextDouble();
            } else {
                // Compute the uct value with the (new) average value
                uctValue = c.getValue() + options.C * Math.sqrt(FastLog.log(np) / nc);
            }
            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    private double playOut(Board board) {
        int winner = board.checkWin(), nMoves = 0;
        int[] move;
        boolean interrupted = false;
        MoveList moves;
        while (winner == Board.NONE_WIN && !interrupted) {
            moves = board.getPlayoutMoves(options.heuristics);
            move = moves.get(Options.r.nextInt(moves.size()));
            board.doMove(move, options.earlyTerm);
            winner = board.checkWin();
            nMoves++;
            if (winner != Board.NONE_WIN && options.earlyTerm && nMoves == options.termDepth)
                interrupted = true;
        }

        double score = 0.;
        if (!interrupted) {
            if (winner == player) score = 1.0;
            else score = -1;
        } else {
            double eval = board.evaluate(player);
            //System.out.println(eval);
            if (eval > options.etT)
                score = 1.;
            else if (eval < -options.etT)
                score = -1.;
        }
        return score;
    }

    public UCTNode getBestChild() {
        if (children == null)
            return null;
        double max = Double.NEGATIVE_INFINITY, value;
        UCTNode bestChild = null;
        for (UCTNode t : children) {
            // If there are children with INF value, choose one of them
            if (t.getValue() == State.INF)
                value = State.INF + Options.r.nextDouble();
            else if (t.getValue() == -State.INF)
                value = -State.INF + t.getVisits() + Options.r.nextDouble();
            else {
                value = t.getVisits();
            }
            if (value > max) {
                max = value;
                bestChild = t;
            }
        }
        return bestChild;
    }

    public String getPV() {
        UCTNode child = getBestChild();
        StringBuilder sb = new StringBuilder();
        while (child != null) {
            sb.append(Board.getMoveString(child.move)).append(" v: ").append(df2.format(child.getValue())).append(" ");
            child = child.getBestChild();
        }
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private void updateStats(double value) {
        if (state == null)
            state = tt.getState(hash, false);
        state.updateStats(value);
    }

    private void setSolved(boolean win) {
        if (state == null)
            state = tt.getState(hash, false);

        if (win) {// win for the parent player
            state.setSolved(3 - player);
        } else {
            state.setSolved(player);
        }
    }

    /**
     * @return The value of this node with respect to the parent
     */
    public double getValue() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getMean(3 - player);
    }

    /**
     * @return The number of visits of the transposition
     */
    private double getVisits() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0.;
        return state.getVisits();
    }

    private State getState() {
        if (state == null)
            state = tt.getState(hash, false);
        return state;
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    @Override
    public String toString() {
        return Board.getMoveString(move) + " " + state.toString();
    }
}
