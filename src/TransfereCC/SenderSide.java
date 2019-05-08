package TransfereCC;

import Common.Pair;

import java.io.File;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.*;




import static TransfereCC.AgenteUDP.*;
import static TransfereCC.ConnectionControl.*;
import static TransfereCC.Crypto.*;
import static TransfereCC.ErrorControl.*;


public class SenderSide extends ConnectionHandler implements Runnable {

    SenderSide(InetAddress ip, int port_number, String filename, int isn, AgenteUDP msgSender){
        super();
        this.segmentsToProcess = new TreeSet<>((s1,s2) -> Integer.compare(s1.ack_number, s2.ack_number ));

        this.st = new StateTable();

        this.st.setDestination(ip,port_number);
        this.st.setFilename(filename);
        this.st.last_ack_value = isn;

        this.msg_sender = msgSender;
    }

    public void run() {
        System.out.println("Playing sender role");
            try {
                // INICIO DE CONEXAO - Verifica se existe o ficheiro pedido
                boolean connection_accepted = establishConnection(this.st.file);

                if (connection_accepted) {
                    transferFile();

                    //TERMINO DE CONEXAO
                    endConnection();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        System.out.println("Numero de pacotes por confirmar = " + this.st.unAckedSegments.size());
    }



    private boolean establishConnection(String wanted_file) {
        if(!(new File(wanted_file).isFile())){
            System.out.println("Não existe o ficheiro pedido");

            msg_sender.sendMissingFileFYN(this.st);
            return false;
        }

        Pair<byte[], byte[]> assinatura = generateSignature(wanted_file, this.msg_sender.keys);
        this.st.setCrypto(assinatura.first, assinatura.second);
        msg_sender.sendSYNACK(st);

        waitSegment();
        MySegment to_process = getNextSegment();

        if(isACK(to_process)) {
            processReceivedAck(to_process,this.st);
            System.out.println("Recebi um ACK ao SYNACK - "+ LocalTime.now());
            return true;
        }
        else return false;
    }

    private void transferFile() throws Exception {
        MySegment received;

        //TRANSFERENCIA DE FICHEIRO
        List<byte[]> data_packets = dividePacket(this.st.file, 1024);

        int i=0;
        while(i < data_packets.size()){
            while(i < data_packets.size() && st.unAckedSegments.size() < st.windowSize)
                msg_sender.sendDataSegment(this.st, data_packets.get(i++));

            waitSegment();
            received = getNextSegment();
            if(isACK(received)) {
                int re_send = processReceivedAck(received,this.st);
                if(re_send == -2) System.out.printf("Recebi um ACK (ACK : %d ) - " + LocalTime.now() + "\n", received.ack_number);
                if(re_send == -1) System.out.printf("Recebi primeiro ack repetido (ACK : %d ) - " + LocalTime.now() + "\n", received.ack_number);
                if(re_send > 0) {System.out.printf("Recebi segundo ack repetido (ACK : %d ) - " + LocalTime.now() + "\n", re_send); reSend(re_send);}
            }
        }

        System.out.println("####### Supostamente todos os pacotes foram enviados. Pacotes:" + data_packets.size()+", enviados:" +i );
        while(st.unAckedSegments.size()>0) {
            waitSegment();
            received = getNextSegment();
            if (isACK(received)) {
                int re_send = processReceivedAck(received, this.st);
                if(re_send == -2) System.out.printf("Recebi um ACK (ACK : %d ) - " + LocalTime.now() + "\n", received.ack_number);
                if(re_send == -1) System.out.printf("Recebi primeiro ack repetido (ACK : %d ) - " + LocalTime.now() + "\n", received.ack_number);
                if(re_send > 0) {System.out.printf("Recebi segundo ack repetido (ACK : %d ) - " + LocalTime.now() + "\n", re_send); reSend(re_send);}
            }
        }

    }

    private void reSend(int re_send) {
        MySegment to_send = this.st.unAckedSegments.first();
        System.out.printf("A reenviar (SEQ : %d ) - " + LocalTime.now() + "\n", to_send.seq_number);
        this.msg_sender.directSend(to_send, this.st);
    }


    private void endConnection() throws InterruptedException {
        this.msg_sender.sendFYN(this.st);

        waitSegment();
        MySegment received = getNextSegment();

        if(isACK(received)) {
            processReceivedAck(received,this.st);
            System.out.println("A terminar - " + LocalTime.now());
        }
    }
}

