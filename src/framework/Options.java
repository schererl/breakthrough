package framework;

import java.util.Random;

/**
 * Created by Tom Pepels (tpepels@gmail.com) on 03/07/15.
 */
public class Options {
    public static final Random r = new Random();
    public double C = 1.;
    public boolean debug = true, fixSimulations = false, heuristics = false;
    public int timeLimit = 5000;
}
