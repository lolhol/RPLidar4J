package examples.autofind;

import brigero.board.GridBoardRenderer;
import examples.util.GetPathFinderPos;
import star.finder.main.AStar;
import star.finder.util.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AutoFindTo {
    private int[] posEnd, posStart;
    private boolean running;
    private final AStar finder = new AStar();
    private final HashSet<int[]> remove = new HashSet<>();
    int c = 0;

    public AutoFindTo(int[] positionEnd, int[] posStart) {
        this.posEnd = new int[] {positionEnd[0], positionEnd[1]};
        this.posStart = posStart;
    }

    public void tick(int[] curPos, GridBoardRenderer r) {
        if (running) return;

        if (Math.abs(curPos[0]) - Math.abs(posEnd[0]) > 3 || Math.abs(curPos[1]) - Math.abs(posEnd[1]) > 3) {
            this.posEnd = GetPathFinderPos.getNextPos(convertToBooleanList(r.data), curPos, remove);
        }

        if (Math.abs(curPos[0]) - Math.abs(posStart[0]) > 10 || Math.abs(curPos[1]) - Math.abs(posStart[1]) > 10) {
            this.posStart = new int[]{curPos[0] / r.getWH()[0], curPos[1] / r.getWH()[1]};
            running = true;
            runFinder(r);
        }

        if (c >= 5) {
            remove.clear();
        }
    }

    public void runFinder(GridBoardRenderer r) {
        new Thread(() -> {
            List<Node> result = finder.run(this.posStart, new int[]{posEnd[0], posEnd[1]}, r.data, true);

            r.extraData.clear();
            r.extraData.add(posEnd);

            if (result.isEmpty()) {
                remove.add(posEnd);
            }

            for (Node node : result) {
                r.extraData.add(new int[]{node.x, node.y});
            }
            r.reDraw();

            this.running = false;
        }).start();
    }

    List<List<Boolean>> convertToBooleanList(List<List<Integer>> init) {
        List<List<Boolean>> retList = new ArrayList<>();
        for (int x = 0; x < init.size(); ++x) {
            List<Boolean> tmp = new ArrayList<>();

            for (int y = 0; y < init.get(x).size(); ++y) {
                int cur = init.get(x).get(y);
                tmp.add(cur > 180);
            }

            retList.add(tmp);
        }

        return retList;
    }
}
