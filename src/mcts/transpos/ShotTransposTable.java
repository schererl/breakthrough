package mcts.transpos;

public class ShotTransposTable {
    private final int TT_SIZE = (int) Math.pow(2, 22);
    private final long MASK = TT_SIZE - 1;
    //
    private ShotState[] states;
    private int moveCounter = 0;
    public int collisions = 0, positions = 0, recoveries = 0;

    public ShotTransposTable() {
        this.states = new ShotState[TT_SIZE];
    }

    public ShotState getState(long hash, boolean existingOnly) {
        int hashPos = getHashPos(hash);
        ShotState s = states[hashPos];
        if (s != null) {
            while (true) {
                if (s.hash == hash) {
                    recoveries++;
                    return s;
                }
                if (s.next == null)
                    break;
                s = s.next;
            }
            //
            if (existingOnly)
                return null;
            collisions++;
            positions++;
            // Transposition was not found, i.e. collision
            ShotState newState = new ShotState(hash);
            s.next = newState;
            return newState;
        } else if (!existingOnly) {
            positions++;
            // Transposition was not encountered before
            s = new ShotState(hash);
            states[hashPos] = s;
            return s;
        } else {
            return null;
        }
    }

    public int pack(int offset) {
        recoveries = 0;
        collisions = 0;
        int prePositions = positions;
        ShotState s, ps;
        for (int i = 0; i < TT_SIZE; i++) {
            s = states[i];
            if (s == null)
                continue;
            ps = null;
            // Check if the states were visited this round
            while (true) {
                if (s.visited && offset > 0) {
                    s.visited = false;
                    s.lastVisit = moveCounter;
                    ps = s;
                } else if (moveCounter - s.lastVisit >= offset) {
                    if (ps != null) {
                        ps.next = s.next;
                        positions--;
                        ps = s;
                    } else {
                        positions--;
                        states[i] = s.next;
                        ps = null;
                    }
                }
                if (s.next == null)
                    break;
                s = s.next;
            }
        }
        moveCounter++;
        return (prePositions - positions);
    }

    private int getHashPos(long hash) {
        return (int) (hash & MASK);
    }
}
