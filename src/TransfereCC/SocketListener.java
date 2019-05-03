package TransfereCC;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.AbstractMap.SimpleEntry;

import static TransfereCC.ConnectionControl.*;
import static TransfereCC.MySegment.*;

public class SocketListener implements Runnable  {
    DatagramSocket socket;
    AgenteUDP connectionManager;

    public SocketListener(AgenteUDP connectionManager){
        this.connectionManager = connectionManager;
        this.socket = connectionManager.serverSocket;
    }

    public void run(){
        System.out.println("Listening socket");

        while(true){
            MySegment to_process;
            byte[] receiveData = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try{
                socket.receive(receivePacket);
                to_process = fromByteArray(receivePacket.getData());

                InetAddress ip = receivePacket.getAddress();
                int port  = receivePacket.getPort();

                setPacketAtConnection(new SimpleEntry<InetAddress,Integer>(ip,port), to_process);
            }
            catch(IOException e){
                System.out.println("Error ocurred during receive method");
            }
            catch(ClassNotFoundException e){
                System.out.println("Can't convert UDP content to MySegment");
            }
        }
    }

    private void setPacketAtConnection(SimpleEntry key, MySegment to_process) {
        ConnectionHandler connection = connectionManager.connections.get(key);
        if(connection != null){
            connection.addSegmentToProcess(to_process);
            wakeUpConnectionHandler(connection);
        }
        else //if no connection matches key, new request was made
            if(isFileRequest(to_process))
                connectionManager.addSenderRoleConnection((InetAddress)key.getKey(), (Integer)key.getValue(),to_process);

    }

    private void wakeUpConnectionHandler(ConnectionHandler connection){
        connection.l.lock();
        connection.waitPacketCondition.signal();
        connection.l.unlock();
    }



}
