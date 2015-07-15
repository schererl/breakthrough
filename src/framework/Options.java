package framework;

import java.util.Random;

/**
 * Created by Tom Pepels (tpepels@gmail.com) on 03/07/15.
 */
public class Options {
    public static final Random r = new Random();
    public double C = .8, B = 30;
    public boolean debug = true, fixSimulations = false,
            heuristics = false, earlyTerm = false, solver = true,
            tt = false, lorenzEval = false;
    public int timeLimit = 10000, termDepth = 4, etT = 2;
    // Schadd etT: 20
}
