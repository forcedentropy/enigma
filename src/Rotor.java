/**
 * The rotor handles the brunt of the encoding process.
 * A rotor can be thought of as a substitution cipher, one
 * letter comes in, and a different letter comes out. There
 * are several different properties a rotor may have.
 *
 * 1) First, the rotor has an inherent mapping that is
 * configured when the rotor is built. On the right
 * side of a rotor, there are 26 metal contacts. On the
 * left side of the rotor, there are 26 more metal contacts.
 * The core of the rotor contains a random spaghetti mapping
 * between each of these contacts. You can see this
 * in the {@link Mapping} enum, for example on mapping I,
 * A is mapped to E, B is mapped to K, C is mapped to M,
 * and so on. Each rotor also has a turn over position,
 * which is the point at which the prawl can engage and
 * rotate a rotor.
 * 2) Next, the rotor has a ring setting. Think of this
 * as a car tire. In this analogy, the core mapping
 * of the rotor would be the hub of the tire, and the tire
 * would by the ring of letter labels around it. This tire can
 * be rotated relative to the core. You can basically think
 * of this ring as being used to label what a particular
 * metal contact should be called. Think of the mappings
 * as "metal contact 1 maps to metal contact 17", and you
 * can use the ring setting to call "metal contact 1" 'A',
 * 'B', 'C', or whatever you like.
 * 3) The rotation of a rotor represents what letter
 * appears facing up on the rotor. Each key press
 * will cause 3 prawls to move up and try to rotate each
 * rotor, which will rotate if it is in its turnover position.
 *
 * Note that I also use the rotor class to handle the reflector.
 * A reflector however does not rotate or support a ring setting,
 * so they are both left at 0.
 *
 */
public class Rotor {

    public enum Mapping {
        I("EKMFLGDQVZNTOWYHXUSPAIBRCJ", 'q'),
        II("AJDKSIRUXBLHWTMCQGZNPYFVOE", 'e'),
        III("BDFHJLCPRTXVZNYEIWGAKMUSQO", 'v'),
        IV("ESOVPZJAYQUIRHXLNFTGKDCMWB", 'j'),
        V("VZBRGITYUPSDNHLXAWMJQOFECK", 'z'),
        ReflectorB("YRUHQSLDPXNGOKMIEBFZCWVJAT"),
        ReflectorC("FVPJIAOYEDRZXWGCTKUQSBNMHL");

        private final String text;
        private char turnOver;

        Mapping(String text) {
            this.text = text.toLowerCase();
        }

        Mapping(String text, char turnOver) {
            this.text = text.toLowerCase();
            this.turnOver = turnOver;
        }

        public static Mapping getMapping(int index) {
            return values()[index];
        }
    }

    public final Mapping mapping;

    public int ringOffset;

    private int originalRotation;
    public int rotation;

    /**
     * Creates a rotor
     * @param mapping The mapping to use
     * @param ringOffset The ring offset, an integer 0-25 representing an offset from 'a'
     * @param rotation The rotation, an integer 0-25 representing an offset from 'a'
     */
    public Rotor(Mapping mapping, int ringOffset, int rotation) {
        this.mapping = mapping;
        this.ringOffset = ringOffset;
        this.rotation = rotation;
        this.originalRotation = rotation;
    }

    public void setRotationPermanent(int rotation) {
        this.originalRotation = rotation;
        this.reset();
    }

    /**
     * Rotates the rotor forward one position
     */
    public void rotate() {
        rotation = (rotation + 1) % 26;
    }

    // this will return true if the next rotate operation should
    // also rotate its neighbor

    /**
     * Returns if the notch on this rotor lines up with the rotor's prawl,
     * if this returns true, the rotor to the right should be rotated.
     * Note, if the middle rotor is at its turnover notch, but the right rotor
     * isn't, the middle rotor will still turnover.
     * @return Returns true if the rotor and the rotor to its left should both rotate
     */
    public boolean isAtNotch() {
        return Utils.i2a(rotation) == mapping.turnOver;
    }

    /**
     * Substitutes the character, forwards should be true if going from right to left,
     * and false after it hits the reflector and goes back
     * @param c the letter to encode, an integer in the range 0-25 as an offset from 'a'
     * @param forwards true if right-to-left, false if left-to-right
     * @return the encoded letter, as an offset from 'a'
     */
    public int encode(int c, boolean forwards) {
        // ring offset and rotation work in opposite directions from one another
        int val = (26 + c - ringOffset + rotation) % 26;

        int mapped = forwards ? Utils.a2i(mapping.text.charAt(val)) :  mapping.text.indexOf(Utils.i2a(val));
        return (26 + mapped + ringOffset - rotation) % 26;
    }

    public char encode(char c, boolean forwards) {
        return Utils.i2a(encode(Utils.a2i(c), forwards));
    }

    /**
     * Resets to the original rotation
     */
    public void reset() {
        this.rotation = originalRotation;
    }

    public Rotor copy() {
        return new Rotor(mapping, 0, 0);
    }
}