/**
 * This class simulates the entire Enigma encoding and decoding process.
 * The entire process can be thought of as the following:
 *
 * 1) Configure rotor and plugboard settings
 * 2) Pressing a key (which in turns rotates the rotors)
 * 3) Transforming the pressed letter into another
 *
 * The Enigma machine will coordinate these different mechanisms.
 * Note, the Enigma exhibits a useful reciprocal property: the cipher text
 * can be decoded using the exact same settings it was encoded in and by simply
 * entering the cipher text, the plain text will be outputted.
 *
 * You'll notice Enigma's fatal flaw happens because of the reflector,
 * and letter than enters it may never come out as itself which
 * is one of the properties that can be exploited while cracking the code.
 */
public class Enigma {

    public Rotor left, middle, right, reflector;
    private PlugBoard board;

    Enigma(PlugBoard board, Rotor reflector, Rotor left, Rotor middle, Rotor right) {
        this.board = board;
        this.reflector = reflector;
        this.left = left;
        this.right = right;
        this.middle = middle;
    }

    /**
     * The rotation process is similar to an odometer on a car. Once the right rotor
     * makes a complete rotation, the middle rotor will rotate once, and once it has completed
     * a full rotation, the left rotor will rotate once. However, there are two key differences:
     * 1) Each rotor has a turnover position, i.e., the the turnover is not at 'Z' as you would expect,
     * but instead at an arbitrary position decided on by the designers.
     * 2) A double turnover can occur on the middle rotor when both the left and middle notch line up.
     *
     * Its hard to explain this exactly, so the best way to understand it is to watch the second half
     * of this video: https://www.youtube.com/watch?v=SRHghaww8e8 which gives you a very good visual
     * understanding of how three prawls are trying to turn over each rotor at every key press
     */
    private void rotate() {
        /*
         * Simulate the action of three prawls
         */

        // Perform rotations
        boolean shouldMiddleRotate = right.isAtNotch() || middle.isAtNotch();
        boolean shouldLeftRotate = middle.isAtNotch();

        right.rotate();

        if(shouldMiddleRotate) {
            middle.rotate();
        }

        if(shouldLeftRotate) {
            left.rotate();
        }
    }

    /**
     * This will encode one letter using the current Enigma settings.
     * This simulates a key press, so first the rotors are rotated,
     * and then the letter is transformed
     * @param c a letter that was pressed
     * @return the letter after traveling through the plugboard, rotors, and plugboard again, essentially
     * the lamp that will ligth up
     */
    public char encode(char c) {
        rotate();

        // Run through the plug-board
        c = board.swap(c);

        // Run through the rotors
        c = right.encode(c, true);
        c = middle.encode(c, true);
        c = left.encode(c, true);

        // Run through the reflector
        c = reflector.encode(c, true);

        // Run through the rotors backwards
        c = left.encode(c, false);
        c = middle.encode(c, false);
        c = right.encode(c, false);

        // Run through the plug board again
        return board.swap(c);
    }

    /**
     * A convenience method to encrypt an entire message at once
     * instead of tediously having to type the entire message out
     * @param message The message to encrypt
     * @return The encrypted message
     */
    public String encode(String message) {
        message = message.toLowerCase();

        StringBuilder decrypted = new StringBuilder();

        for(char c : message.toCharArray()) {
            if(c == ' ') {
                decrypted.append(' ');
            } else {
                decrypted.append(encode(c));
            }
        }

        reset();

        return decrypted.toString().toUpperCase();
    }

    public void setRotors(Rotor reflector, Rotor left, Rotor middle, Rotor right) {
        this.reflector = reflector;
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public void setPlugBoard(String s) {
        board = new PlugBoard(s);
    }

    public void setRings(int left, int middle, int right) {
        this.left.ringOffset = left;
        this.middle.ringOffset = middle;
        this.right.ringOffset = right;
    }

    public void setRotations(int left, int middle, int right) {
        this.left.setRotationPermanent(left);
        this.middle.setRotationPermanent(middle);
        this.right.setRotationPermanent(right);
    }

    /**
     * @return returns a string representing the Bombe's configuration
     */
    public String getConfiguration() {
        return "Rotors=["+reflector.mapping.toString() + ", " + left.mapping.toString()+", " + middle.mapping.toString() + ", " + right.mapping.toString()
                +"], Rings=["+Utils.i2a(left.ringOffset)+", "+Utils.i2a(middle.ringOffset)+", "+Utils.i2a(right.ringOffset)+"]"
                +", Rotations=["+Utils.i2a(left.rotation)+", "+Utils.i2a(middle.rotation)+", "+Utils.i2a(right.rotation)+"], Steckerboard: "
                +board.toString();
    }

    /**
     * Will set the rotors to their original rotation
     */
    public void reset() {
        left.reset();
        right.reset();
        middle.reset();
    }
}
