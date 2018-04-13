package org.dora;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LocationIndex {
    private Integer                    sizeOfLocationInBytes = 0;

    private final Map<String, Integer> locationMap           = new HashMap<>();
    private final List<String>         locations             = new ArrayList<>();
    private final int                  startOffset;

    public LocationIndex(int startOffset) {
        this.startOffset = startOffset;
    }

    public Tuple<Integer, Integer> add(String location) {
        int locationSize = location.getBytes().length;
        Integer locationOffset = locationMap.get(location);
        if (locationOffset == null) {
            locationOffset = sizeOfLocationInBytes + startOffset;
            locationMap.put(location, locationOffset);
            locations.add(location);
            sizeOfLocationInBytes += locationSize;
        }
        return new Tuple(locationSize, locationOffset);
    }

    public byte[] persist() throws UnsupportedEncodingException {
        byte[] result = new byte[sizeOfLocationInBytes];
        ByteBuffer buffer = ByteBuffer.wrap(result);
        for (int i = 0; i < locations.size(); i++) {
            String location = locations.get(i);
            buffer.put(location.getBytes("UTF-8"));
        }
        return result;
    }
}
