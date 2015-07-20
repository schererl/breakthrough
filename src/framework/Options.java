package framework;

import java.util.Random;

/**
 * Created by Tom Pepels (tpepels@gmail.com) on 03/07/15.
 */
public class Options {
    public static final Random r = new Random();
    public double C = .8, imAlpha = 0.025;
    public boolean debug = true, fixSimulations = false,
            heuristics = false, earlyTerm = false, solver = false,
            nodePriors = false, imm = false;
    public int timeLimit = 10000, termDepth = 4, etT = 25, npVisits = 100, B = 20;
}
