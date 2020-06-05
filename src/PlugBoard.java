import java.util.HashMap;

/**
 * The plugboard, called the "steckerboard" in German, is applied both immediately after a key is pressed
 * and just before a lamp is lit up. The plugboard contains 26 sockets, one for each letter. Two sockets may
 * be connected via a cord, this will cause the corresponding letters at each socket to become stecker pairs.
 * Standard procedure was to use 10 cords which would swap 10 pairs of letters (20 letters). If a letter/socket
 * does not have a cord in it, it will not be altered. So for example, let's say that "A" and "B" are steckered,
 * if the user hits "A" on the keyboard, instead it will be as if they pressed "B", and vice versa.
 * Secondly, lets say the user hits "C" on the keyboard, which goes through the full rotor transformation and
 * comes out as a "B", instead of the "B" lamp lighting up, instead the "A"  lamp will light up
 */
public class PlugBoard {

    private final HashMap<Character, Character> swaps = new HashMap<>();

    public PlugBoard() {}

    /**
     * Creates a plug board using a string that lists all of the stecker pairs to initialize
     * the plug board with for convenience. You can use the {@link PlugBoard#add(char, char)}
     * to manually add new stecker pairs.
     * @param pairs A string contains a list of all stecker pairs to initialize the plugboard in in groups
     *              of two. Example: "AB CD EF ZK"
     */
    public PlugBoard(String pairs) {
        String[] tokens = pairs.trim().toLowerCase().split("\\s+");

        for(String s : tokens) {
            if(s.length() != 2) {
                throw new IllegalArgumentException("Stecker pairs must be provided in groupings of two.");
            }

            add(s.charAt(0), s.charAt(1));
        }
    }

    /**
     * Adds a stecker pair to the plugboard.
     * A is steckered to B and equivalently, B is steckered to A
     * @param a Must be lowercase
     * @param b Must be lowercase
     */
    public void add(char a, char b) {
        swaps.put(a, b);
        swaps.put(b, a);
    }

    /**
     * Adds a stecker pair to the plugboard.
     * A is steckered to B and equivalently, B is steckered to A
     * @param a An integer in the range 0-25, representing a letter as an offset from a
     * @param b A integer in the range 0-25, representing a letter as an offset from a
     */
    public void add(int a, int b) {
        add((char)(a + 'a'), (char)(b + 'a'));
    }

    /**
     * Performs the swapping of a letter. If the letter has a stecker pair,
     * it will be replaced with that letter
     * @param c The letter to swap
     * @return The stecker pair of c
     */
    public char swap(char c) {
        return swaps.getOrDefault(c, c);
    }

    /**
     * A convenience function which outputs all stecker pairs existing in the plugboard
     * @return A string in groups of two letters representings all stecker pairs in the plugboard
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for(char c : swaps.keySet()) {
            if(builder.indexOf(String.valueOf(c)) == -1) {
                builder.append(c).append(swaps.get(c)).append(" ");
            }
        }

        return builder.toString();
    }
}
