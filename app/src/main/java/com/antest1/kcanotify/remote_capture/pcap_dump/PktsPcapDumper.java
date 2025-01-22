package com.antest1.kcanotify.remote_capture.pcap_dump;

import com.antest1.kcanotify.remote_capture.CaptureService;
import com.antest1.kcanotify.remote_capture.interfaces.PcapDumper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.pkts.Pcap;


public class PktsPcapDumper implements PcapDumper {
    public static final String TAG = "PktsPcapDumper";
    private static byte[] pcapHeader;

    public PktsPcapDumper() {}

    @Override
    public void startDumper() throws IOException {
    }

    @Override
    public void stopDumper() throws IOException {
    }

    @Override
    public String getBpf() {
        return "";
    }

    @Override
    public void dumpData(byte[] data) throws IOException {
        if (pcapHeader == null) pcapHeader = CaptureService.getPcapHeader();

        ByteBuffer buffer = ByteBuffer.allocate(pcapHeader.length + data.length);
        buffer.order(ByteOrder.nativeOrder());
        buffer.put(pcapHeader);
        buffer.put(data);

        InputStream is = new ByteArrayInputStream(buffer.array());

        Pcap pcap = Pcap.openStream(is);
        pcap.loop(new TcpPacketHandler());
        pcap.close();
    }
}
