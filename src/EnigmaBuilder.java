public class EnigmaBuilder {
    private PlugBoard board;
    private Rotor reflector;
    private Rotor left;
    private Rotor middle;
    private Rotor right;

    public EnigmaBuilder setBoard(PlugBoard board) {
        this.board = board;
        return this;
    }

    public EnigmaBuilder setReflector(Rotor.Mapping mapping) {
        this.reflector = new Rotor(mapping, 0, 0);
        return this;
    }

    public EnigmaBuilder setLeft(Rotor.Mapping mapping, char ringPosition, char rotation) {
        return setLeft(mapping, Utils.a2i(ringPosition), Utils.a2i(rotation));
    }

    public EnigmaBuilder setMiddle(Rotor.Mapping mapping, char ringPosition, char rotation) {
        return setMiddle(mapping, Utils.a2i(ringPosition), Utils.a2i(rotation));
    }

    public EnigmaBuilder setRight(Rotor.Mapping mapping, char ringPosition, char rotation) {
        return setRight(mapping, Utils.a2i(ringPosition), Utils.a2i(rotation));
    }

    public EnigmaBuilder setLeft(Rotor.Mapping mapping, int ringPosition, int rotation) {
        this.left = new Rotor(mapping, ringPosition, rotation);
        return this;
    }

    public EnigmaBuilder setMiddle(Rotor.Mapping mapping, int ringPosition, int rotation) {
        this.middle = new Rotor(mapping, ringPosition, rotation);
        return this;
    }

    public EnigmaBuilder setRight(Rotor.Mapping mapping, int ringPosition, int rotation) {
        this.right = new Rotor(mapping, ringPosition, rotation);
        return this;
    }

    public Enigma build() {
        return new Enigma(board, reflector, left, middle, right);
    }
}