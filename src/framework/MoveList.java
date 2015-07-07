package framework;

public class MoveList {

    private int[] movesFrom, movesTo;
    private int size;

    public MoveList(int maxSize) {
        movesFrom = new int[maxSize];
        movesTo = new int[maxSize];
        size = 0;
    }

    public void add(int from, int to) {
        movesFrom[size] = from;
        movesTo[size++] = to;
    }

    public int[] get(int index) {
        return new int[] {movesFrom[index], movesTo[index]};
    }

    public int size() {
        return size;
    }
}
