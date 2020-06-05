import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Enigma enigma = new EnigmaBuilder()
                .setBoard(new PlugBoard())
                .setReflector(Rotor.Mapping.ReflectorB)
                .setLeft(Rotor.Mapping.I, 'a', 'a')
                .setMiddle(Rotor.Mapping.II, 'a', 'a')
                .setRight(Rotor.Mapping.III, 'a', 'a')
                .build();

        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to Enigma! Type help for a list of commands.");
        System.out.println("Enigma config: "+enigma.getConfiguration());

        while(true) {
            String cmd = scanner.nextLine().trim();

            try {
                if("help".equals(cmd)) {
                    String help = "set-rotors [reflector] [left] [middle] [right]\n" +
                            "\tDescription: sets the rotor settings for the Enigma\n" +
                            "\tExample: set-rotors B I IV III\n" +
                            "\tArgs:\n" +
                            "\t\t[reflector] is one of 'B', 'C'\n" +
                            "\t\t[left] is one of 'I', 'II', 'III', 'IV', 'V'\n" +
                            "\t\t[middle] is one of 'I', 'II', 'III', 'IV', 'V'\n" +
                            "\t\t[right] is one of 'I', 'II', 'III', 'IV', 'V'\n" +
                            "set-steckers [steckers]\n" +
                            "\tDescription: sets the steckered letters for the Enigma\n" +
                            "\tExample: set-steckers AB CE FG HL PQ RT\n" +
                            "encode [msg]\n" +
                            "\tDescription: encodes the message using current Enigma settings\n" +
                            "\tExample: encode the message to encrypt\n" +
                            "set-rings [left] [middle] [right]\n" +
                            "\tDescription: sets the ring positions for the Enigma\n" +
                            "\tExample: set-rings a r z\n" +
                            "\tArgs:\n" +
                            "\t\t[left] is a letter a-z\n" +
                            "\t\t[middle] is a letter a-z\n" +
                            "\t\t[right] is a letter a-z\n" +
                            "set-rotations [left] [middle] [right]\n" +
                            "\tDescription: sets the rotor rotations for the Enigma\n" +
                            "\tExample: set-rotations a l z\n" +
                            "\tArgs:\n" +
                            "\t\t[left] is a letter a-z\n" +
                            "\t\t[middle] is a letter a-z\n" +
                            "\t\t[right] is a letter a-z\n" +
                            "crack [cipher text] [crib]\n" +
                            "\tDescription: cracks the message using the current Enigma settings\n" +
                            "\tNote: cipher text and crib length must match\n" +
                            "\tExample: crack XJQWE HELLO\n" +
                            "farm-crack [cipher text] [crib]\n" +
                            "\tDescription: cracks the message using all possible rotor orderings\n" +
                            "\tNote: cipher text and crib length must match\n" +
                            "\tExample: crack XJQWE HELLO\n" +
                            "enigma\n" +
                            "\tDescription: Outputs current Enigma settings\n" +
                            "quit\n" +
                            "\tDescription: Quits the application";

                    System.out.println(help);
                } else if(cmd.startsWith("set-rotors")) {
                    String[] params = cmd.substring("set-rotors ".length()).split("\\s+");
                    final ArrayList<String> rotors = new ArrayList<>();
                    Collections.addAll(rotors, "I", "II", "III", "IV", "V");

                    Rotor reflector = "B".equals(params[0]) ? new Rotor(Rotor.Mapping.ReflectorB, 0, 0) : new Rotor(Rotor.Mapping.ReflectorC, 0, 0);
                    Rotor left = new Rotor(Rotor.Mapping.getMapping(rotors.indexOf(params[1])), 0, 0);
                    Rotor middle = new Rotor(Rotor.Mapping.getMapping(rotors.indexOf(params[2])), 0, 0);
                    Rotor right = new Rotor(Rotor.Mapping.getMapping(rotors.indexOf(params[3])), 0, 0);

                    enigma.setRotors(reflector, left, middle, right);
                    System.out.println("Enigma config: "+enigma.getConfiguration());
                } else if(cmd.startsWith("set-steckers")) {
                    enigma.setPlugBoard(cmd.substring("set-steckers ".length()));
                    System.out.println("Enigma config: "+enigma.getConfiguration());
                } else if(cmd.startsWith("set-rings")) {
                    String[] params = cmd.substring("set-rings ".length()).split("\\s+");

                    char l = Character.toLowerCase(params[0].charAt(0));
                    char m = Character.toLowerCase(params[1].charAt(0));
                    char r = Character.toLowerCase(params[2].charAt(0));

                    enigma.setRings(Utils.a2i(l), Utils.a2i(m), Utils.a2i(r));
                    System.out.println("Enigma config: "+enigma.getConfiguration());
                } else if(cmd.startsWith("set-rotations")) {
                    String[] params = cmd.substring("set-rotations ".length()).split("\\s+");

                    char l = Character.toLowerCase(params[0].charAt(0));
                    char m = Character.toLowerCase(params[1].charAt(0));
                    char r = Character.toLowerCase(params[2].charAt(0));

                    enigma.setRotations(Utils.a2i(l), Utils.a2i(m), Utils.a2i(r));
                    System.out.println("Enigma config: "+enigma.getConfiguration());
                } else if("enigma".equals(cmd)) {
                    System.out.println("Enigma config: "+enigma.getConfiguration());
                } else if("quit".equals(cmd)) {
                  break;
                } else if(cmd.startsWith("encode")) {
                    System.out.println(enigma.encode(cmd.substring("encode ".length())));
                } else if(cmd.startsWith("crack")) {
                    String[] params = cmd.substring("crack ".length()).split("\\s+");

                    long start = System.nanoTime();

                    Bombe bombe = new Bombe(enigma, params[0], params[1], true);
                    ArrayList<String[]> results = bombe.run();

                    long elapsed = (System.nanoTime() - start) / 1_000_000;

                    System.out.println("Cracked in "+elapsed+"ms");
                    System.out.println("Possible rotor rotations and plug board deductions:");
                    int index = 1;
                    for(String[] result : results) {
                        System.out.println(index+") "+result[0]+": "+result[1]);
                        index++;
                    }
                } else if(cmd.startsWith("farm-crack")) {
                    String[] params = cmd.substring("farm-crack ".length()).split("\\s+");

                    long start = System.nanoTime();

                    BombeFarm farm = new BombeFarm(params[0], params[1], true);
                    ArrayList<String[]> results = farm.run();

                    long elapsed = (System.nanoTime() - start) / 1_000_000;
                    System.out.println("Cracked in "+elapsed+"ms");

                    System.out.println("Possible rotor rotations, plug board deductions, and rotor orders:");
                    int index = 1;
                    for(String[] result : results) {
                        System.out.println(index+") "+result[0]+": "+result[1]+result[2]);
                        index++;
                    }
                } else {
                    System.out.println("Command not found");
                }
            } catch(Exception e) {
                e.printStackTrace();
                System.out.println("Incorrect syntax");
            }
        }
    }
}
