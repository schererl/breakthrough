package framework;

import java.util.Random;

/**
 * Created by Tom Pepels (tpepels@gmail.com) on 03/07/15.
 */
public class Options {
    public static final Random r = new Random();
    public double C = .4, shotC = 0.4, imAlpha = 0.1, etWv = 1.3, kq = 2.0;
    public boolean debug = true, fixSimulations = false, timed = false,
            heuristics = false, earlyTerm = false, solver = false,
            nodePriors = false, imm = false, test = false, qualityBonus = false,
            UBLB = false;
    public int timeLimit = 10000, termDepth = 4, etT = 20, npVisits = 100, B = 20;
}
