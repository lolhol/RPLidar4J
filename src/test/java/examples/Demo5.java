package examples;

import java.util.ArrayList;
import java.util.List;

public class Demo5 {
    public static void main(String[] args) {
        List<List<Integer>> fakeData = new ArrayList<>();

        for (int i = 0; i <= 100; i++) {
            List<Integer> tmp = new ArrayList<>();

            for (int j = 0; j <= 100; j++) {
                double random = Math.random();

                if (random < 0.3) {
                    tmp.add(-1);
                } else if (random > 0.7) {
                    tmp.add(1);
                } else {
                    tmp.add(2);
                }
            }

            fakeData.add(tmp);
        }

        //Board board = new Board(100, 100, fakeData);
    }
}
