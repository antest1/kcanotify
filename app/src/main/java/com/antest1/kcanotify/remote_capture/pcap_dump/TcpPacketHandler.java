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
            IPPacket ipPacket = tcpPacket.getParentPacket();

            int sport = tcpPacket.getSourcePort();
            int dport = tcpPacket.getDestinationPort();

            int pkt_type = KcaVpnData.NONE;
            if (dport == 80 || dport == 443) pkt_type = KcaVpnData.REQUEST;
            if (sport == 80 || sport == 443) pkt_type = KcaVpnData.RESPONSE;

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

        return true;
    }
}