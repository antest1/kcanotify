package com.antest1.kcanotify.remote_capture.pcap_dump;

import static com.antest1.kcanotify.KcaVpnData.containsKcaServer;
import static com.antest1.kcanotify.KcaVpnData.getDataFromNative;

import com.antest1.kcanotify.KcaVpnData;

import io.pkts.PacketHandler;
import io.pkts.buffer.Buffer;
import io.pkts.packet.IPPacket;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.protocol.Protocol;

import java.io.IOException;
import java.util.Arrays;

public class TcpPacketHandler implements PacketHandler {
    public static final String TAG = "TcpPacketHandler";
    private static final int HEADER_INSPECT_SIZE = 256;

    @Override
    public boolean nextPacket(Packet packet) throws IOException {
        if (packet.hasProtocol(Protocol.TCP)) {
            TCPPacket tcpPacket = (TCPPacket) packet.getPacket(Protocol.TCP);
            int sport = tcpPacket.getSourcePort();
            int dport = tcpPacket.getDestinationPort();

            // check HTTP traffic only
            if (dport == 80 || sport == 80) {
                int pkt_type = (dport == 80) ? KcaVpnData.REQUEST : KcaVpnData.RESPONSE;
                IPPacket ipPacket = tcpPacket.getParentPacket();

                byte[] source = ipPacket.getSourceIP().getBytes();
                byte[] destination = ipPacket.getDestinationIP().getBytes();

                Buffer buffer = tcpPacket.getPayload();
                if (buffer != null) {
                    byte[] arr = buffer.getArray();
                    byte[] head = null;

                    if (pkt_type == 1 && arr.length > HEADER_INSPECT_SIZE) {
                        head = Arrays.copyOfRange(arr, 0, HEADER_INSPECT_SIZE);
                    }

                    if (containsKcaServer(pkt_type, source, destination, head) == 1) {
                        getDataFromNative(arr, arr.length, pkt_type, source, destination, sport, dport);
                    }
                }
            }
        }

        return true;
    }
}