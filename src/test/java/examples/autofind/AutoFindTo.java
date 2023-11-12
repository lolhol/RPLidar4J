package examples.autofind;

import brigero.board.GridBoardRenderer;
import star.finder.main.AStar;
import star.finder.util.Node;

import java.util.List;

public class AutoFindTo {
    private int[] posEnd, posStart;
    private boolean running;
    private final AStar finder = new AStar();
    public AutoFindTo(int[] positionEnd, int[] posStart) {
        this.posEnd = new int[] {positionEnd[0], positionEnd[1]};
        this.posStart = posStart;
    }

    public void tick(int[] curPos, GridBoardRenderer r) {
        if ((Math.abs(curPos[0]) - Math.abs(posStart[0]) > 10 || Math.abs(curPos[1]) - Math.abs(posStart[1]) > 10) && !running) {
            this.posStart = new int[]{curPos[0] / r.getWH()[0], curPos[1] / r.getWH()[1]};
            running = true;
            runFinder(r);
        }
    }

    public void runFinder(GridBoardRenderer r) {
        new Thread(() -> {
            List<Node> result = finder.run(this.posStart, new int[]{posEnd[0], posEnd[1]}, r.data, true);
            r.extraData.clear();
            r.extraData.add(posEnd);

            for (Node node : result) {
                r.extraData.add(new int[]{node.x, node.y});
            }
            r.reDraw();

            this.running = false;
        }).start();
    }
}
