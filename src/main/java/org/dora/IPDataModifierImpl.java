package org.dora;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.*;


class IPDataModifierImpl implements IPDataModifier {
    private static final String[] EMPTY_LOCATION = "保留地址,保留地址,,,,,,,,,,*,*".split(",");

    private final Map<Integer, Set<IPNode>> indexNodes = new HashMap<>();

    public IPDataModifierImpl() {
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                indexNodes.put(i * 256 + j, new TreeSet<>(Comparator.comparingInt(IPNode::getIp)));
            }
        }
    }

    public String[] findFromIndex(String ip) {
        int intIp = IPUtils.ipToInt(ip);
        int index = (intIp >> 24 & 0xFF) << 8 | intIp >> 16 & 0xFF;
        Set<IPNode> ipNodes = indexNodes.get(index);
        for (IPNode ipNode : ipNodes) {
            if (ipNode.getIp() >= intIp) {
                return ipNode.getLocation();
            }
        }
        return new String[]{""};
    }

    public void load(String file) {
        final ByteBuffer dataBuffer = getDataBuffer(file);
        load(dataBuffer);
    }

    public void loadFromResource(String name) {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(name)) {
            load(ByteBuffer.wrap(StreamUtils.toBytes(inputStream)));
        } catch (IOException ex) {
            throw new RuntimeException("cannot load resource " + name);
        }

    }

    private void load(ByteBuffer dataBuffer) {
        final int length = dataBuffer.getInt();
        final int indexEnd = (int) Math.pow(2, 16) * 4;
        final int ipIndexSize = length - indexEnd;
        final int locationDataOffset = ipIndexSize;

        // load index buffer
        final byte[] bytes = new byte[length];
        dataBuffer.get(bytes, 0, length - 4);
        final ByteBuffer indexBuffer = ByteBuffer.wrap(bytes);
        indexBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // load ip index buffer
        final byte[] ipIndexBytes = new byte[ipIndexSize];
        indexBuffer.position(indexEnd);
        indexBuffer.get(ipIndexBytes, 0, ipIndexSize);
        final ByteBuffer ipIndexBuffer = ByteBuffer.wrap(ipIndexBytes).order(ByteOrder.BIG_ENDIAN);

        indexBuffer.position(0);

        // load all ip and locations
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                int index = i * 256 + j;
                Set<IPNode> ipNodes = indexNodes.get(index);

                int ipIndexPosition = indexBuffer.getInt();
                int intIP = ipIndexBuffer.getInt(ipIndexPosition * 9);

                while (indexForIP(intIP) == index) {
                    Tuple<Integer, Integer> sizeAndOffset = readSizeAndOffset(ipIndexBuffer, ipIndexPosition * 9);
                    ipNodes.add(new IPNode(intIP, getLocation(dataBuffer, locationDataOffset, sizeAndOffset)));
                    ipIndexPosition++;
                    intIP = ipIndexBuffer.getInt(ipIndexPosition * 9);
                }
            }
        }
    }

    private ByteBuffer getDataBuffer(String file) {
        File dataFile = new File(file);
        if (!dataFile.isFile() || !dataFile.exists()) {
            throw new RuntimeException(file + " does not exists.");
        }
        return ByteBuffer.wrap(FileOperate.getBytesByFile(dataFile));
    }

    private int indexForIP(int intIP) {
        return intIP >>> 16;
    }

    private String[] getLocation(ByteBuffer dataBuffer, int dataOffset, Tuple<Integer, Integer> sizeAndOffset) {
        String[] location = EMPTY_LOCATION;
        if (sizeAndOffset.getLeft() > 0 && sizeAndOffset.getRight() > 0) {
            byte[] locationBytes = new byte[sizeAndOffset.getLeft()];
            dataBuffer.position((int) sizeAndOffset.getRight() + dataOffset);
            dataBuffer.get(locationBytes, 0, sizeAndOffset.getLeft());
            location = new String(locationBytes, Charset.forName("UTF-8")).split("\t");
        }
        return location;
    }

    private Tuple<Integer, Integer> readSizeAndOffset(ByteBuffer ipIndexBuffer, int position) {
        int locationOffset = (ipIndexBuffer.get(position + 6) & 0xFF) << 16 | (ipIndexBuffer.get(position + 5) & 0xFF) << 8 | (ipIndexBuffer.get(position + 4)) & 0xFF;

        int index_length = ((0xFF & ipIndexBuffer.get(position + 7)) << 8) + (0xFF & ipIndexBuffer.get(position + 8));
        return new Tuple(index_length, locationOffset);
    }

    @Override
    public boolean add(String ip, String[] location) {
        int intIP = IPUtils.ipToInt(ip);
        int index = intIP >>> 16;
        Set<IPNode> ipNodes = indexNodes.get(index);
        IPNode node = null;
        for (IPNode ipNode : ipNodes) {
            if (ipNode.getIp() == intIP) {
                node = ipNode;
                break;
            }
        }
        if (node == null) {
            node = new IPNode(intIP, location);
            ipNodes.add(node);
        } else {
            node.setLocation(location);
        }
        return true;
    }

    @Override
    public void persist(OutputStream outputStream) throws IOException {
        int indexSize = 4 * (int) Math.pow(2, 16);
        int ipIndexSize = getIPsCount() * 9;

        int indexBufferSize = 4 + indexSize + ipIndexSize;
        final ByteBuffer indexBuffer = ByteBuffer.allocate(indexBufferSize);
        final ByteBuffer ipIndexBuffer = ByteBuffer.wrap(indexBuffer.array(), 4 + indexSize, ipIndexSize).order(ByteOrder.BIG_ENDIAN);
        LocationIndex locationIndex = new LocationIndex(indexSize);

        indexBuffer.putInt(indexBufferSize);
        int ipIndex = 0;
        int i = 0;
        while (i < indexNodes.size()) {
            indexBuffer.putInt(ipIndex);
            indexBuffer.order(ByteOrder.LITTLE_ENDIAN);
            Set<IPNode> ipNodes = indexNodes.get(i);
            i++;
            for (Iterator<IPNode> iterator = ipNodes.iterator(); iterator.hasNext(); ) {
                IPNode node = iterator.next();
                writeIP(ipIndexBuffer, node);
                String location = String.join("\t", node.getLocation());
                Tuple<Integer, Integer> sizeAndOffset = locationIndex.add(location);
                writeLocation(ipIndexBuffer, sizeAndOffset);
            }
            ipIndex += ipNodes.size();
        }

        outputStream.write(indexBuffer.array());
        outputStream.write(locationIndex.persist());
    }

    private Integer getIPsCount() {
        return indexNodes.values().stream().reduce(0, (s, list) -> s + list.size(), (x, y) -> x + y);
    }

    private void writeLocation(ByteBuffer ipIndexBuffer, Tuple<Integer, Integer> sizeAndOffset) {
        ipIndexBuffer.put((byte) (sizeAndOffset.getRight() & 0xFF));
        ipIndexBuffer.put((byte) ((sizeAndOffset.getRight() >> 8) & 0xFF));
        ipIndexBuffer.put((byte) ((sizeAndOffset.getRight() >> 16) & 0xFF));

        ipIndexBuffer.put((byte) ((sizeAndOffset.getLeft() >> 8) & 0xFF));
        ipIndexBuffer.put((byte) (sizeAndOffset.getLeft() & 0xFF));
    }

    private void writeIP(ByteBuffer ipIndexBuffer, IPNode node) {
        ipIndexBuffer.putInt(node.getIp());
    }
}
