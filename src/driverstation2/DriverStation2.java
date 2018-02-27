package driverstation2;

import java.util.ArrayList;
import java.util.Collection;
import javax.swing.*;
import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import com.RobotMsgv2p0.*;

/**
 *
 * @author owner
 * 
 * Link for reference
 * https://wpilib.screenstepslive.com/s/4485/m/24192/l/144976-frc-driver-station-powered-by-ni-labview
 */
public class DriverStation2 {

     /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
       DriverStationBase dsb = new DriverStationBase();
        
    }
    
}

/** Base Driver Station to start GUI, controller and runtime
 * 
 */

class DriverStationBase extends JPanel
   implements Runnable
{
    DriverStationGUI dsGUI;
    DriverStationNetwork dsNet;
    DriverStationVisual dsVisual;
    private ArrayList<Controller> foundControllers;
    Controller activeController;
    boolean controllerStatus;
    enum ControllerType {UNDEFINED, Xbox};
    ControllerType activeControllerType;
    boolean prevNetStatus;
    boolean prevRobotStatus;
    boolean distanceScanStatus;
    boolean firstControlFlag;
    int firstControlValue;
    
    // Messages to display on GUI
    private static ArrayList<String> messageList;
    
    int loopDelay = 0;  // used in dsbProcessLowPeriodic
    
    public DriverStationBase() {
        String ip;
    
        // create and setup the GUI
        dsGUI = new DriverStationGUI();
        dsVisual = new DriverStationVisual();
        dsVisual.start();
        ip = dsGUI.getSelectedIPAddr();
        dsNet = new DriverStationNetwork(ip);
        
        activeControllerType = ControllerType.UNDEFINED;
        activeControllerType = ControllerType.Xbox;
        
        dsGUI.writeToRobotInfo("\nDriver Station Initialized");
        
        // take care of initializing controllers
        foundControllers = new ArrayList<>();
        controllerStatus = false;
        prevNetStatus = false;
        prevRobotStatus = false;
        distanceScanStatus = false;
        firstControlFlag = false;
        firstControlValue = 0;
        dsGUI.showControllerDisconnected();
        searchForControllers();
        
        messageList = new ArrayList<>();

        
//        dsGUI.setRobotStatusComm(false);
//        dsGUI.setRobotStatusJoyStick(false);
        dsGUI.setRobotStatusRobot(false);
        
        // start thread
        Thread t = new Thread(this);
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
        
    }
    
    /**
     * Search (and save) for controllers of type Controller.Type.STICK,
     * Controller.Type.GAMEPAD, Controller.Type.WHEEL and Controller.Type.FINGERSTICK.
     * 
     * 
     */
    private void searchForControllers() {
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        System.out.println("Controller searching");
        dsGUI.writeToRobotInfo("\nController searching");
        
        for(int i = 0; i < controllers.length; i++){
            Controller controller = controllers[i];
            
            if (
                    controller.getType() == Controller.Type.STICK || 
                    controller.getType() == Controller.Type.GAMEPAD || 
                    controller.getType() == Controller.Type.WHEEL ||
                    controller.getType() == Controller.Type.FINGERSTICK
               )
            {
                // Add new controller to the list of all controllers.
                foundControllers.add(controller);
                dsGUI.addController(controller.getName());
                activeControllerType = ControllerType.Xbox;  // hard code for now
                controllerStatus = true;
                dsGUI.setRobotStatusJoyStick(true);
                System.out.println("Found Controller");
                dsGUI.writeToRobotInfo("\nFound Controller");
          }
        }
        
        if (controllerStatus == false)
           dsGUI.writeToRobotInfo("\nNo Controller Found");
            
    }
    
    /**
     * method : dsbProcessXboxController
     * 
     * This method reads the xbox controller data 
     * components for "Controller (Xbox One For Windows)"
     * See RobotControlMsgXbox for description
     * 
     * All analog values are from -1 to +1, are converted to percentage
     *   of 0 to 100.
     */
    
    private void dsbProcessXboxController(RobotControlMsgXbox msg) {
        float val;
        int   val2;
        
        
        // Set Basic Operational Mode
        switch (dsGUI.getRobotStatusMode()) {
            case 3:
                msg.operationalMode = RobotControlMsg.OperationalMode.TESTMODE;
                break;
            case 1:
                msg.operationalMode = RobotControlMsg.OperationalMode.TELEOP;
                break;
            case 2:
                msg.operationalMode = RobotControlMsg.OperationalMode.AUTONOMOUS;
                break;
            default:
                msg.operationalMode = RobotControlMsg.OperationalMode.UNDEFINED;
                break;
        }
        
        // Set Operational State
        if (dsGUI.getSelectedRobotStatus())
            msg.operationalState = RobotControlMsg.OperationalState.ENABLE;
        else 
            msg.operationalState = RobotControlMsg.OperationalState.DISABLE;
        
        // Set Misc parameters
        msg.distScanStatus = distanceScanStatus;
        if (dsGUI.getTest1Status() == true)
            msg.testNumber = 1;
        else if (dsGUI.getTest2Status() == true)
            msg.testNumber = 2;
        else if (dsGUI.getTest3Status() == true)
            msg.testNumber = 3;
        else if (dsGUI.getTest4Status() == true)
            msg.testNumber = 4;
        else
            msg.testNumber = 0;

        msg.ZRot = 0;
        msg.XRot = 51;
        msg.Zaxis = 0;
        
        // check for valid controller
        if (!controllerStatus)
            return;
        
        if (!activeController.poll()) {
            dsGUI.showControllerDisconnected();
            controllerStatus = false;
            dsGUI.setRobotStatusJoyStick(false);
            return;
        }
        
        
        
        // controller still active
        Component[] components = activeController.getComponents();
        
        for (int i=0; i < components.length; i++)
        {
            Component component = components[i];
            Identifier componentIdentifier = component.getIdentifier();
            
            val = component.getPollData();
            
            if (componentIdentifier.getName().matches("4"))
                msg.button4 = (int) val;
            
            if (componentIdentifier.getName().matches("5"))
                msg.button5 = (int) val;
            
            if (componentIdentifier.getName().matches("x"))
                msg.Xaxis = (int)(((2 - (1 - val)) * 100) / 2);
            
            if (componentIdentifier.getName().matches("rx"))
                msg.XRot = (int) (((2 - (1 - val)) *100) / 2);
                                    
            if (componentIdentifier.getName().matches("z")) {
                msg.Zaxis = (int)(((2 - (1 - val)) * 100) / 2);
                val2 = (int) (((2 - (1 - val)) * 100) / 2);
                dsGUI.setRightDrive(val2);                    
            }
            
            if (componentIdentifier.getName().matches("rz")) {
                msg.ZRot = (int)(((2 - (1 - val)) * 100) / 2);
                val2 = (int) (((2 - (1 - val)) * 100) / 2);
                dsGUI.setLeftDrive(val2);               
            }
                
                
        }  // for each component
        
        // For some reason, the controller starts up in a middle state.  This
        // results in setting the robot in motion before even touching the
        // controls.  This first check, keeps the controller at zero until 
        // a changed value
        if ((firstControlFlag == false) && (msg.ZRot == 0))
            firstControlFlag = true;
        
        if ((firstControlFlag == false) && (msg.ZRot != 0))
            msg.ZRot = 0;

            
        
        // Set controller Type
        if (activeControllerType == ControllerType.Xbox)
            msg.controllerType = RobotControlMsg.ControllerType.Xbox;
        else
            msg.controllerType = RobotControlMsg.ControllerType.UNDEFINED;
        
        
    }
    
    
    /**
     * dsbProcessDataFromRobot() 
     */
    private void dsbProcessDataFromRobot() {
        
        if (!dsNet.dsnGetMsgAvailable())
            return;
        
        RobotStatusMsg msg = dsNet.dsnGetMsg();
        
        if (msg.robotState == RobotControlMsg.RobotState.ACTVE) {
            dsGUI.setRobotStatusRobot(true);
        }
        
        if (msg.robotState == RobotControlMsg.RobotState.READY) {
            dsGUI.setRobotStatusRobot(false);
        }
        
    
        dsGUI.setBatteryBar(msg.batteryLevel);
        dsGUI.setDistanceBar(msg.distanceLevel);
        dsVisual.setDistanceSensor(msg.distanceLevel);
        dsVisual.setDistanceSensorAngle(msg.distRotAngle);
        dsVisual.setSteeringAngle(msg.steeringAngle);
        
    }
    
    
    /**
     * method dsbProcessLowPeriodic
     * 
     * This method should run at a 500mSec interval.
     * This method is responsible for checking GUI, controller and 
     *   communication status.
     */
    private void dsbProcessLowPeriodic() {
        String ip;

        // check to see if we should rescan the controllers
        if (dsGUI.getRescanStatus())
            searchForControllers();
    
        
        // check connection status and if failed, try again with 
        // currently selected IP address
        if (!dsNet.dsnGetNetworkState()) {
            ip = dsGUI.getSelectedIPAddr();
            dsNet.DriverStationNetworkConnectionRequest(ip);
        }
 
        
        // Figure out how to set Network status
        if (prevNetStatus == false && dsNet.dsnGetNetworkState())
        {
            dsGUI.setRobotStatusComm(true);
            dsGUI.writeToRobotInfo("\nRobot Connection Establised");
            prevNetStatus = true;
        }
        if (prevNetStatus == true && !dsNet.dsnGetNetworkState())
        {
            dsGUI.setRobotStatusComm(false);
            dsGUI.writeToRobotInfo("\nRobot Connection Lost");
            prevNetStatus = false;
        }
        
        // check for change in robot status
        if (prevRobotStatus == false && dsNet.getRobotStateEnabled())
        {
            dsGUI.setRobotStatusRobot(true);
            dsGUI.writeToRobotInfo("\nRobot Enabled");
            prevRobotStatus = true;
        }
        if (prevRobotStatus == true && !dsNet.getRobotStateEnabled())
        {
            dsGUI.setRobotStatusRobot(false);
            dsGUI.writeToRobotInfo("\nRobot Disabled");
            prevRobotStatus = false;
        }
        
        // check for distance scan request
        distanceScanStatus = dsGUI.getDistScanStatus();
        if (distanceScanStatus == true)
        {
            dsGUI.setDistScanStatus(false);  // one shot event
            dsGUI.writeToRobotInfo("\nRunning Distance Scan");
        }
        
        // Check for any messages
        driverStationPrintEvent();
        
    }
    
    /**
     * method : driverStationLogEvent
     * This is a static method to allow other objects to be able to call 
     * This is a utility function that takes a String and stores it into
     * a list. The paired function driverStationPrintEvent sends the messages
     * to the GUI object
     * @param s 
     */
    public static void driverStationLogEvent(String s) {
        messageList.add(s);
    }
    
    /**
     * This utility function takes all messages stored in the list and displays
     * them on the GUI output panel.
     */
    private void driverStationPrintEvent() {
        for (int i=0; i<messageList.size(); i++) {
            String s = messageList.remove(i);
            dsGUI.writeToRobotInfo(s);
            
       }
    }
    
    /**
     * Run method - loop forever
     * This method will perform the following activities
     * check for an controller inputs
     * send controller data to robot
     * check for any GUI updates
     * 
     * Note: The time base to robot is 100mSec.  This method is responsible for
     * sending a message to the robot every 100mSec.
     * 
     */
    
    public void run() {
     
        int loopCnt;
        loopCnt = 0;
        RobotControlMsgXbox robotSendMsg;
        
        // get selected controller
        if (controllerStatus) {
           int selControlIndex = dsGUI.getSelectedControllerIndex();    

           activeController = foundControllers.get(selControlIndex);
        }
        

        
        // Setup Network
        dsNet.DriverStationNetworkStart();
        String ip = dsGUI.getSelectedIPAddr();
        dsNet.DriverStationNetworkConnectionRequest(ip); 
        
        
        // Main processing loop for all Driver Station Activities
        try {
            while (true) {
                 
                // 1/2 sec for GUI and overhead activities
                if ((loopCnt % 5) == 0)
                   dsbProcessLowPeriodic();
  
                // Get controller info and send to robot           
                if (activeControllerType == ControllerType.Xbox)
                {
                   
                    // Get controller Data
                    robotSendMsg = new RobotControlMsgXbox();
                    dsbProcessXboxController(robotSendMsg);
                
                            
                    // Send controller Data
                    dsNet.dsnSendMsg(robotSendMsg);
                                    
                    // Process Data from Robot
                    dsbProcessDataFromRobot();
                }
                                       
                
                // take care of loop control
                loopCnt++; 
                Thread.sleep(100);
            } 
            } catch (InterruptedException ie){}
        }
    
} // end DriverStationBase

    
