package framework;

import java.util.Random;

/**
 * Created by Tom Pepels (tpepels@gmail.com) on 03/07/15.
 */
public class Options {
    public static final Random r = new Random();
    public double C = 1., B = 30;
    public boolean debug = true, fixSimulations = false,
            heuristics = false, earlyTerm = false, solver = true,
            nodePriors = false, tt = false, lorenzEval = false;
    public int timeLimit = 10000, termDepth = 4, etT = 20, npVisits = 5;
    // Schadd etT: 20, Lorentz: 1??
}
