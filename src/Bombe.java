import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;

/**
 * The Bombe was Turing's device that helped crack Enigma.
 * The Bombe isn't a machine box that instantly decrypts any
 * Nazi message. Instead, it is a tool that helps to invalidate impossible
 * settings and produce a list of several valid settings to be explored
 * manually.
 *
 * Several assumptions are made:
 * - The rotors and rotor order
 * - The ring setting
 *
 * The Bombe will then try every possible rotor rotation setting (26^3 settings)
 * and try to determine plug board settings. The Bombe is only used to determine
 * plug board settings. Instead of discovering what the plug board settings are per se,
 * instead the Bombe will try to prove a logical contradiction for a certain plug board
 * setting. Much like Sherlock's Holmes phrase about eliminating the impossible, the Bombe
 * will try to eliminate the impossible and return a set of settings it couldn't invalidate.
 * These will be manually explored by an operator later.
 *
 * It should also be noted that while testing a specific rotor settings for all three rotors,
 * Enigma ignores stepping the middle and left rotors, and only the rightmost rotor is
 * stepped while processing the menu. This means that a menu shouldn't be too long because
 * it will increase the chance of the middle rotor having stepped.
 *
 * While I did write this class solving it in my own way, I did draw heavy inspiration
 * from https://github.com/gchq/CyberChef/blob/master/src/core/lib/Bombe.mjs, especially
 * for the functions {@link Bombe#energize(int, int)} and {@link Bombe#checkingMachine(int)},
 * so I felt compelled to include a license notice for this:
 *
 * author s2224834
 * author The National Museum of Computing - Bombe Rebuild Project
 * copyright Crown Copyright 2019
 * license Apache-2.0
 *
 */
public class Bombe {

    /**
     * The "menu" is a undirected graph describing a pairing of the cipher text
     * to a crib (the guessed plain text of the cipher text)
     *
     * In the menu, a node represents a letter, either in the cipher text or in
     * the plain text (each letter is only represented once, even if it exists multiple
     * times). Edges correspond to a encoding of a single letter through Enigma. Each
     * edge is labeled with the "offset", in other words, the number of characters from
     * the start of the cipher text. This is important because it implicates a specific
     * rotor setting for the rightmost rotor. Each of these edges is just the Enigma
     * machine with the right rotor's rotation adjusted for the offset of that character
     * within the cipher text or plain text, one can travel either direction along an
     * edge as long as the rotor setting is adjusted.
     *
     * This class will construct a menu from the cipher text. It has two primary goals:
     * - Determine which of the (unconnected) sub-graphs of the complete menu
     *   has the MOST loops (loops allow more plugboard deductions to be made),
     *   and eliminate all other sub-graphs.
     * - Determine which of the nodes within this sub-graph is the MOST CONNECTED,
     *   in other words, which node has the most edges exiting from it. This letter
     *   should be included as one of the plugboard hypothesis'
     *
     *  The graph is represented using the matrix representation. The row and col
     *  represent letters, or nodes, and the values within the matrix represent two things:
     *  - If the value is not along the diagonal, it represents an edge between two letters,
     *    and its value corresponds to the offset of that edge
     *  - If the value is along the diagonal, it represents a node. The depth-first search uses
     *    these for some internal markers to aid the algorithm
     */
    private static class Menu {
        private final Integer[][] matrix = new Integer[26][26];

        // the node within the subgraph with the most connections
        private final int mostConnectedLetter;

        // caches the adjacency lists
        private final ArrayList<Integer[]> adjCache = new ArrayList<>();

        public Menu(String cipherText, String crib) {
            // Fill the graph
            for(int i = 0; i < cipherText.length(); i++) {
                int t = Utils.a2i(cipherText.charAt(i));
                int b = Utils.a2i(crib.charAt(i));

                if(t == b) {
                    throw new IllegalArgumentException("Crib and cipher text violate no character can be encoded as itself rule.");
                }

                matrix[t][b] = i + 1;
                matrix[b][t] = i + 1;

                matrix[t][t] = 0;
                matrix[b][b] = 0;
            }

            // Find the subgraph with the most loops, in the event of a tie,
            // use nodes as a tie breaker
            HashSet<Integer> exploredEdges = new HashSet<>();
            int mostLoopsSubgraph = -1; // the most connected letter in the subgraph
            int mostLoops = -1;
            int nNodes = -1;

            for(int i = 0; i < 26; i++) {
                if(matrix[i][i] == null) {
                    continue;
                }

                if(matrix[i][i] == 0) {
                    DFSResult result = dfs(exploredEdges, i);
                    if(result.nLoops > mostLoops || (result.nLoops == mostLoops && result.nNodes > nNodes)) {
                        if(mostLoopsSubgraph != -1) {
                            destroy(mostLoopsSubgraph);
                        }

                        mostLoops = result.nLoops;
                        mostLoopsSubgraph = result.mostConnected;
                        nNodes = result.nNodes;
                    } else {
                        destroy(i);
                    }
                }
            }

            if(mostLoopsSubgraph == -1) {
                throw new IllegalStateException("Menu doesn't contain any subgraphs");
            }

            this.mostConnectedLetter = mostLoopsSubgraph;

            // Cache the adjacency lists which will improve the performance of the Bombe
            for(int i = 0; i < 26; i++) {
                ArrayList<Integer> array = new ArrayList<>();

                Iterator it = getIterator(i);

                Integer v;

                while((v = it.next()) != null) {
                    array.add(v);
                }

                adjCache.add(array.toArray(new Integer[0]));
            }
        }

        /**
         * Gets the offset between two letters, to traverse this edge,
         * the rotor's right rotor will need to be incremented by the
         * return of this function
         * @param a a node, represented as in integer in the range 0-25, as an offset from 'a'
         * @param b a node, represented as in integer in the range 0-25, as an offset from 'a'
         * @return a rotation to add to the Enigma's rightmost rotor (the fast rotor)
         */
        public int getCribOffset(int a, int b) {
            return this.matrix[a][b];
        }

        public int getMostConnected() {
            return mostConnectedLetter;
        }

        /**
         * Removes a subgraph once if it isn't the subgraph with
         * the most loops
         * @param letter Any letter within the subgraph
         */
        private void destroy(int letter) {
            Stack<Integer> chain = new Stack<>();

            matrix[letter][letter] = 2;
            chain.push(letter);

            while(!chain.isEmpty()) {
                int u = chain.pop();

                Integer vertex;

                Iterator it = getIterator(u);

                while((vertex = it.next()) != null) {
                    if(matrix[vertex][vertex] == 1) {
                        matrix[vertex][vertex] = 2;
                        chain.push(vertex);
                    }
                }
            }

            for(int i = 0; i < 26; i++) {
                if(matrix[i][i] != null && matrix[i][i] == 2) {
                    // Destroy rows and columns
                    for(int j = 0; j < 26; j++) {
                        matrix[i][j] = null;
                        matrix[j][i] = null;
                    }
                }
            }

        }

        // Returns number of loops and most connected letter of a sub-graph including
        // the provided letter/node
        private DFSResult dfs(HashSet<Integer> exploredEdges, int letter) {
            int nLoops = 0;
            int nNodes = 0;
            int maxConnectedLetter = 0;
            int maxConnections = -1;

            Stack<Integer> chain = new Stack<>();

            matrix[letter][letter] = 1;
            chain.push(letter);

            while(!chain.isEmpty()) {
                int u = chain.pop();
                nNodes++;

                int nConnections = 0;

                Iterator it = getIterator(u);

                Integer vertex;

                while((vertex = it.next()) != null) {
                    nConnections++;

                    if(exploredEdges.contains(this.matrix[u][vertex])) {
                        continue;
                    }

                    exploredEdges.add(this.matrix[u][vertex]);

                    if(matrix[vertex][vertex] == 0) {
                        matrix[vertex][vertex] = 1;
                        chain.push(vertex);
                    } else if(matrix[vertex][vertex] == 1) {
                        nLoops++;
                    }
                }

                if(nConnections > maxConnections) {
                    maxConnections = nConnections;
                    maxConnectedLetter = u;
                }
            }

            return new DFSResult(nLoops, nNodes, maxConnectedLetter);
        }

        private class Iterator {
            private final int letter;

            private int index;

            public Iterator(int letter) {
                this.letter = letter;
            }

            public Integer next() {
                for(int i = index; i < 26; i++) {
                    if(matrix[letter][i] != null && i != letter) {
                        index = i + 1;
                        return i;
                    }
                }

                return null;
            }
        }

        private Iterator getIterator(int letter) {
            return new Iterator(letter);
        }

        public Integer[] getAdjacent(int letter) {
            return adjCache.get(letter);
        }
    }

    private final BombeEnigma enigma;
    private final Menu menu;

    private final boolean check;
    private final boolean[] wires;

    private int liveWires;
    private final int testRegister;
    private final int testRegisterPair;

    public Bombe(Enigma enigma, String cipherText, String crib, boolean check) {
        this.enigma = new BombeEnigma(enigma);
        this.check = check;

        this.menu = new Menu(cipherText.toLowerCase(), crib.toLowerCase());
        this.wires = new boolean[26 * 26];

        this.testRegister = menu.getMostConnected();
        this.testRegisterPair = 1;
    }

    // assumption is that bombeLetter is a letter in the bombe's menu
    // and the steckerLetter is one that its attached to

    // called when i & j should connect (when they are stecker partners)
    // energize will energize all other wires that are derived from the logical
    // conclusion of i & j being steckered, for example, m & n might be forced
    // to be live when i & j are

    /**
     * The energize function is basically the core function of the Bombe,
     * A great visualization can be found here:
     * http://www.ellsbury.com/bombe4.htm
     *
     * Essentially, there are 26 cables each with 26 wires in them. Each cable represents
     * a letter from a-z, and each wire within the cable represents a letter from a-z. The Bombe
     * takes a stecker hypothesis (a hypothesis of two steckered letters, one
     * of which is the most frequent letter within the cipher text / plain text,
     * the other of which is arbitrary) and tries to disprove it. This is where
     * the cables come in, a test register is placed over the cable corresponding to
     * first stecker hypothesis letter. Basically the Bombe uses this clever wire setup to determine logical
     * implications of which letters must be steckered if the hypothesis is true.
     * So for example, if the test register is on cable A, all live wires within this cable correspond to
     * letters A is steckered to. So let's say to start our hypothesis is A is steckered to B,
     * first, make the B wire live in the A cable, and the A wire live in the B cable. Next,
     * scramblers are attached to cables corresponding to edges in the menu. Let's assume
     * we have an edge from A - C, and B - C, so Enigma machines from the Bombe are connected
     * between these cables (for example, for A-C, all 26 wires in cable A are attached to the Enigma
     * machine, and all 26 output wires are attached to cable C, the Enigma is then configured
     * using the rotor offset as defined by the edge). Then, once our hypothesis letters become live,
     * they are run through the scrambler Enigmas, which will in turn make other wires live and shoot
     * through all the cables. This explanation continues at the {@link Bombe#checkStop()} function.
     * @param i first stecker pair letter
     * @param j second stecker pair letter
     */
    private void energize(int i, int j) {
        int idx = 26 * i + j;

        /*
         * This wire is already live, so we assume
         * that all the downstream wires of this wire
         * will also get electrified
         */
        if(this.wires[idx]) {
            return;
        }

        // Welchman's diagonal board, if i is steckered to j, j is also
        // steckered to i
        this.wires[idx] = true;

        int complement = 26 * j + i;

        this.wires[complement] = true;

        // One of the bundles is a test register
        /*
         * The test register sits over one cable and counts how many live
         * wires this cable has, if either letter is equal to the test register,
         * we can expect a wire to become live
         */
        if(i == this.testRegister || j == this.testRegister) {
            this.liveWires++;
            if(this.liveWires == 26) {
                return;
            }
        }

        /*
         * Okay so i and j represent two letters that
         * are energized. These letters surely
         * connect to other letters via scramblers,
         * so this next phase will energize all
         * other wires that are connected via
         * the scramblers. Additionally, after the
         * scrambler, repeat the process again
         *
         * First, do wire j in bundle i
         */
        Integer[] nodes = this.menu.getAdjacent(i);

        for(int k : nodes) {
            /*
             * Get all the scramblers rooted at i,
             */
            int offset = this.menu.getCribOffset(i, k); // effectively the scrambler setting
            int encoded = this.enigma.encode(j, offset);

            int other = 26 * k + encoded;

            if(!this.wires[other]) {
                this.energize(k, encoded);
                if(this.liveWires == 26) {
                    return;
                }
            }
        }

        // Reverse operation would be identical, skip it
        if(i == j) {
            return;
        }

        nodes = this.menu.getAdjacent(j);

        // Second, do wire i in bundle j
        for(int k : nodes) {
            int offset = this.menu.getCribOffset(j, k);
            int encoded = this.enigma.encode(i, offset);

            int other = 26 * k + encoded;

            if(!this.wires[other]) {
                this.energize(k, encoded);
                if(this.liveWires == 26) {
                    return;
                }
            }
        }
    }

    /**
     * As mentioned earlier, for the cable representing letter A,
     * every live wire in it represents a letter that A is steckered to.
     * The plug board however ONLY allows a letter to be steckered once.
     * For example, A can be steckered to ONLY one other letter.
     * So if we find more than 1 live wire in a cable, we know the
     * plug board is invalid. Interestingly enough, if 25 wires are live,
     * we can assume the hypothesis is wrong, but instead we can flip it
     * to get a valid stecker pair. A stop in the Bombe occurs when
     * the Bombe couldn't invalidate a stop, so the settings corresponding
     * to that stop could be valid.
     * @return null if the Bombe won't stop, otherwise a plug board will all deduced
     * steckerboard settings
     */
    public PlugBoard checkStop() {
        if(this.liveWires == 26) {
            return null;
        }

        int steckerPair = -1;

        // Hypothesis should actually be the opposite
        if(this.liveWires == 25) {
            for(int j = 0; j < 26; j++) {
                if(!this.wires[26 * this.testRegister + j]) {
                    steckerPair = j;
                    break;
                }
            }
        } else if(this.liveWires == 1) {
            // hypothesis is correct
            steckerPair = this.testRegisterPair;
        } else {
            if(!this.check) {
                return new PlugBoard();
            }

            PlugBoard stecker = null;
            for(int i = 0; i < 26; i++) {
                PlugBoard newStecker = this.checkingMachine(i);
                if(newStecker != null) {
                    if(stecker != null) {
                        return new PlugBoard();
                    }
                    stecker = newStecker;
                }
            }

            return stecker;
        }

        if(this.check) {
            return this.checkingMachine(steckerPair);
        } else {
            PlugBoard board = new PlugBoard();
            board.add(this.testRegister, steckerPair);
            return board;
        }
    }

    /**
     * The checking machine was implemented manually back
     * in the day with a thing called a "machine gun".
     * The Bombe only tries to disprove a stecker hypothesis of
     * two letters, the checking machine will figure out several
     * more plug board deductions automatically, but might not be able
     * to get all of the stops.
     * @param pair The most connected letter of the test hypothesis
     * @return returns deduced stecker board pairs
     */
    public PlugBoard checkingMachine(int pair) {
        if(pair != this.testRegisterPair) {
            Arrays.fill(this.wires, false);
            this.liveWires = 0;

            this.energize(this.testRegister, pair);
        }

        PlugBoard board = new PlugBoard();
        board.add(this.testRegister, pair);
        for(int i = 0; i < 26; i++) {
            int count = 0;
            int other = -1;
            for(int j = 0; j < 26; j++) {
                if(this.wires[i * 26 + j]) {
                    count++;
                    other = j;
                }
            }

            if(count > 1) {
                return null;
            } else if(count == 0) {
                continue;
            }

            board.add(i, other);
        }

        return board;
    }


    /**
     * Runs cracking operation
     * @return ArrayList of all Bombe stops, each entry is a string array with three entries, the rotation
     * settings, the string of plugboard deductions, and the Bombe configuration
     */
    public ArrayList<String[]> run() {
        ArrayList<String[]> result = new ArrayList<>();

        /*
         * Check every possible initial rotor setting
         */
        enigma.setRotation(0, 0, 0);

        for(int i = 0; i < 26; i++) {
            for(int j = 0 ; j < 26; j++) {
                for(int k = 0; k < 26; k++) {
                    enigma.setRotation(i, j, k);

                    /*
                     * Shoot electricity through the menu, this is effectively the
                     * attempt to invalidate a certain plugboard setting
                     */
                    Arrays.fill(this.wires, false);
                    this.liveWires = 0;

                    this.energize(this.testRegister, this.testRegisterPair);

                    /*
                     * Check if the machine would have stopped
                     */

                    PlugBoard deductions = checkStop();

                    // A stop occurred
                    if(deductions != null) {
                        result.add(new String[]{enigma.getIndicator(), deductions.toString(), enigma.getConfiguration()});
                    }
                }
            }
        }

        return result;
    }

    /*
     * Utilities
     */

    private static class DFSResult {
        private final int nLoops;
        private final int nNodes;
        private final int mostConnected;

        public DFSResult(int nLoops, int nNodes, int mostConnected) {
            this.nLoops = nLoops;
            this.nNodes = nNodes;
            this.mostConnected = mostConnected;
        }
    }
}
