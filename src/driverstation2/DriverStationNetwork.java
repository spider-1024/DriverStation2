package driverstation2;

import java.io.IOException;
import java.net.*;
import java.io.*;
import com.RobotMsgv2p0.*;
import java.util.ArrayList;

/**
 *
 * @author owner
 */
public class DriverStationNetwork 
   implements Runnable
{
    // Network variables
    private Socket robotSocket;
    private boolean connectionState;
    private boolean connectionRequestState;
    ObjectInputStream input;
    ObjectOutputStream output;
    
    // Data control
    private String inString;
    private boolean robotStateEnabled;
    private String ipAddress;
    
    
    // message handling
    RobotStatusMsg robotStatus;
    private static ArrayList<RobotStatusMsg> inRobotMessageList;

    /**
     * Constructor for network object
     */

    DriverStationNetwork(String ipAddr) {
        connectionState = false;
        connectionRequestState = false;
        robotStateEnabled = false;
        ipAddress = ipAddr;
        
        inRobotMessageList = new ArrayList();
    }
    
    
    /** 
     * This method starts the network task
     */
    public void DriverStationNetworkStart() {
        
        // setup thread
        Thread t = new Thread(this);
        
        t.setPriority(Thread.NORM_PRIORITY + 4);
        t.start();
    }
    
    /** 
     * This method takes a request to setup a connection
     * 
     */
    public void DriverStationNetworkConnectionRequest(String ipAddr) {
        connectionRequestState = true;
        ipAddress = ipAddr;
    }
    
    /**
     * This method attempts to setup the socket connection
     * 
     */
    private boolean DriverStationNetworkConnect() {
        
        try {
            
            System.out.println("Socket attempt for ".concat(ipAddress));
            DriverStationBase.driverStationLogEvent("\nRobot Connection Attempt for : ".concat(ipAddress));
            
            robotSocket = new Socket(ipAddress, 1034);
            
            input = new ObjectInputStream( robotSocket.getInputStream());
            output = new ObjectOutputStream( robotSocket.getOutputStream());
            
        } catch(IOException e) {
            System.out.println("Robot Connection Failed " + e);
            DriverStationBase.driverStationLogEvent("\nRobot Connection Failed for : ".concat(ipAddress));
            return false;
        }
        
        System.out.println("Robot Connected for : ".concat(ipAddress));
        DriverStationBase.driverStationLogEvent("\nRobot Connected for : ".concat(ipAddress));
        connectionState = true;
        connectionRequestState = false;
        return true;
    }
    
    /**
     * This method will take the message, format it and send it to the robot
     */
    public void dsnSendMsg(RobotControlMsgXbox msg) {
        
        // dont even try to send a message if no connection
        if (!connectionState)
            return;
        
        
        try {
 
        // send the message to the robot
        if (robotSocket.isConnected()) {
            
//            System.out.println("sending value " + msg.Xaxis);
           output.writeObject( msg);
           output.reset();
        }
        else {
            connectionState = false;
        }

            
       } catch (IOException e) {
            System.out.println("I/O error" + e);
       }       
       
 
    } // end dsnSendMsg
    
    /**
     * This method will return the actual active/disabled state
     * from the robot.  The value in the returned message from the robot 
     * will be set here for other to read
     */
    public boolean getRobotStateEnabled() {
        return robotStateEnabled;
    }
    
    
    public boolean dsnGetNetworkState() {
        return connectionState;
    }
    
    public boolean dsnGetMsgAvailable() {
        if (inRobotMessageList.size() != 0)
            return true;
        else
            return false;
    }
    
    public RobotStatusMsg dsnGetMsg() {
        return inRobotMessageList.remove(0);
    }
       
    
    /**
     * method run
     * Main processing loop to send/rcv robot messages
     */
    public void run() {
        
        System.out.println("Network Task Started");
        // go into forever loop
        try {
           while (true){
            
            
            // we will move the connection activity into this thread.  The 
            // reason is that it blocks until a connection is made or timeout
            // and that can hold up the driver station
            if (connectionState == false && connectionRequestState == true) {
                DriverStationNetworkConnect();
                Thread.sleep(1000); 
            }
            
            // Check to see if we no longer have a socket
            if (connectionState == true) {
               if (!robotSocket.isConnected()) {
                   connectionState = false;
                   connectionRequestState = true;
               }
            }
            
            if (connectionState == true) {
               // process data
               try {                 
                   
      
                    // Get message back
                    robotStatus = new RobotStatusMsg();
                    robotStatus = (RobotStatusMsg) input.readObject();                  
                    inRobotMessageList.add(robotStatus);
                                    
            
                   } 
                   catch (IOException e){
                     System.out.println("Error Reading from Robot " + e);
                     
                      try {
                         robotSocket.close();
                         connectionState = false;
                      } catch (IOException e3) {}
                     
                   } catch (ClassNotFoundException e2) {
                     System.out.println(e2);
                   }       
            }  // if connection state true
            
           Thread.sleep(50); 
        }  // while loop
      } catch (InterruptedException ie){}
        
    } // run method
    
}
