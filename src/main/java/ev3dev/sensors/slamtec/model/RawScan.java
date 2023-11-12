package ev3dev.sensors.slamtec.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RawScan {
    @Getter
    private final List<Byte> rawScan;
    public RawScan(final List<Byte> scans) {
        this.rawScan = Collections.synchronizedList(new ArrayList<>());
        this.rawScan.addAll(scans);
    }
}
