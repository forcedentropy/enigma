/**
 * The Bombe, which was Turing's machine used to crack Enigma,
 * included many Enigmas of its own that were slightly modified.
 * This class also emulates Enigma behavior, but with a few modifications
 * that help the Bombe crack things efficiently.
 *
 * The modifications that the Bombe's Enigmas used were the following:
 * - Ring settings were ignored and needed to be manually determined after running the Bombe
 * - Letters are not passed through a plug board as the Bombe's goal is to deduce what the plug board
 *   settings could be
 * - Here, every letter through every possible rotation of the rotors is cached to make the Bombe
 *   speedier. This is literally caching half a million integers, isn't modern hardware great? Technically,
 *   you wouldn't need to cache all three rotations keys and could use something fancy like Cantor's
 *   pairing function, but we're too lazy for that.
 */
public class BombeEnigma {

    private final Rotor reflector;
    private final Rotor left;
    private final Rotor middle;
    private final Rotor right;

    // Indexed in the following order: left rotation, middle rotation,
    // right rotation, and the letter to encode
    private final int[][][][] CACHE = new int[26][26][26][26];

    public BombeEnigma(Enigma enigma) {
        this.reflector = enigma.reflector.copy();
        this.left = enigma.left.copy();
        this.middle = enigma.middle.copy();
        this.right = enigma.right.copy();

        // Cache the entire possible state
        for(int i = 0; i < 26; i++) {
            for(int j = 0; j < 26; j++) {
                for(int k = 0; k < 26; k++) {
                    for(int l = 0; l < 26; l++) {
                        int letter = l;

                        this.left.rotation = i;
                        this.middle.rotation = j;
                        this.right.rotation = k;

                        letter = right.encode(letter, true);
                        letter = middle.encode(letter, true);
                        letter = left.encode(letter, true);

                        // Run through the reflector
                        letter = reflector.encode(letter, true);

                        // Run through the rotors backwards
                        letter = left.encode(letter, false);
                        letter = middle.encode(letter, false);
                        letter = right.encode(letter, false);

                        CACHE[i][j][k][l] = letter;
                    }
                }
            }
        }
    }

    /**
     * Sets the rotation of the left, middle, and right rotors
     * @param left An integer in the range 0-25 corresponding to the rotor's rotation, as an offset from 'a'
     * @param middle An integer in the range 0-25 corresponding to the rotor's rotation, as an offset from 'a'
     * @param right An integer in the range 0-25 corresponding to the rotor's rotation, as an offset from 'a'
     */
    public void setRotation(int left, int middle, int right) {
        this.left.rotation = left;
        this.middle.rotation = middle;
        this.right.rotation = right;
    }

    /**
     * Encodes a letter, the plug board is not taken into effect
     * @param letter The letter to encode, an integer in the range 0-25 representing an offset from 'a'
     * @param rightRotation The rotation of the right/fast rotor to use, this is non-destructive, i.e., the rotation
     *                      is only applied for this one encoding. As you might recall, the menu's edges correspond to states of this
     *                      right rotor, so this makes it easy for the Bombe to jump around
     * @return the encoded letter as an offset from 'a'
     */
    public int encode(int letter, int rightRotation) {
        return CACHE[this.left.rotation][this.middle.rotation][(right.rotation + rightRotation) % 26][letter];
    }

    /**
     * Returns the three rotations for each of the rotors, this is used to identify which rotor position was used
     * if the Bombe stops so it can be reported as a particular candidate Enigma setting
     * @return A three letter string representing the three rotor positions
     */
    public String getIndicator() {
        return String.valueOf(Utils.i2a(left.rotation)) + Utils.i2a(middle.rotation) + Utils.i2a(right.rotation);
    }

    /**
     * @return returns a string representing the Bombe's configuration
     */
    public String getConfiguration() {
        return reflector.mapping.toString() + ", " + left.mapping.toString()+", " + middle.mapping.toString() + ", " + right.mapping.toString();
    }

}
