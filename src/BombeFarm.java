import java.util.ArrayList;
import java.util.concurrent.*;

public class BombeFarm {

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(60, 60, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final CompletionService<ArrayList<String[]>> completionService = new ExecutorCompletionService<>(executor);

    private final String cipherText, crib;
    private final boolean check;

    public BombeFarm(String cipherText, String crib, boolean check) {
        this.cipherText = cipherText.toLowerCase();
        this.crib = crib.toLowerCase();
        this.check = check;
    }

    public ArrayList<String[]> run() {
        for(int reflector = 0; reflector < 1; reflector++) {
            for(int i = 0; i < 5; i++) {
                for(int j = 0; j < 5; j++) {
                    if(i == j) {
                        continue;
                    }

                    for(int k = 0; k < 5; k++) {
                        if(k == i || k == j) {
                            continue;
                        }

                        final Enigma enigma = new Enigma(
                                new PlugBoard(),
                                new Rotor(Rotor.Mapping.getMapping(5 + reflector), 0, 0),
                                new Rotor(Rotor.Mapping.getMapping(i), 0, 0),
                                new Rotor(Rotor.Mapping.getMapping(j), 0, 0),
                                new Rotor(Rotor.Mapping.getMapping(k), 0, 0)
                        );

                        Callable<ArrayList<String[]>> task = () -> {
                            Bombe bombe = new Bombe(enigma, cipherText, crib, check);
                            return bombe.run();
                        };

                        completionService.submit(task);
                    }
                }
            }
        }

        ArrayList<String[]> results = new ArrayList<>();

        int received = 0;
        while(received < 60) {
            try {
                Future<ArrayList<String[]>> result = completionService.take();

                results.addAll(result.get());

                received++;
            } catch(InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        return results;
    }
}
