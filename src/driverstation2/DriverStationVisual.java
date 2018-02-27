/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package driverstation2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 *
 * @author owner
 */
public class DriverStationVisual extends JComponent
     implements Runnable {
    
    BufferedImage mainImage = null;
    
    // info for distance display
    int sensorDistance;
    int sensorDistanceAngle;
    int sensorSteeringAngle;
    
    
    public DriverStationVisual () {
    
       mainImage = loadImage();
       
       // init variables
       sensorDistance = 0;
       sensorDistanceAngle = 90;
       sensorSteeringAngle = 0;
       
    }
    
    public void start () {
        
    JFrame frame = new JFrame("Robot Visuals");
       frame.getContentPane().add( this);
       frame.setSize(960, 300);
       frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
       frame.setVisible(true);
    
        Thread t = new Thread(this);
        t.start();
    }
    
    public void paint (Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        String s = new String();
        
        int cx = getSize().width;
        int cy = getSize().height;
        int bx1 = (cx - 40)/3 + 10;
        int bx2 = (cx - 40)/3 + 10 + bx1;
        int by1 = cy - 1;
        int by2 = cy - 1;
        int ix1 = 0;
        int ix2 = bx1 + 10;
        
        
        g2.setPaint(Color.BLUE);
        g2.fillRect(bx1, 0, 10, by1);
        g2.fillRect(bx2, 0, 10, by1);
        
        if (mainImage != null) {
           g.drawImage(mainImage,ix1,0,this);
           g.drawImage(mainImage,ix2,0, this);
        }
        
        
        // Update Sensor display
        g2.setFont(new Font("Times New Roman",Font.PLAIN, 20));
        g2.setPaint(Color.MAGENTA);
        s = "d(cm):  " + String.valueOf(sensorDistance);
        g2.drawString(s, bx2+20, 20);
        s = "da: " + String.valueOf(sensorDistanceAngle);
        g2.drawString(s, bx2+120, 20);
        s = "sa: " + String.valueOf(sensorSteeringAngle);
        g2.drawString(s, bx2+200,20);
        
    }
    

    
    private BufferedImage loadImage() {
        
//        String imgFileName = "images/weather-rain.png";
        String imgFileName = "images/orion-nebula.jpg";
        BufferedImage img = null;
        try {
            img =  ImageIO.read(new File(imgFileName));
        } catch (Exception e) {
            System.out.println("error here!" + imgFileName);
        }
        return img;
    }
    
    /**
     * Method : setSteeringAngle
     * 
     * This method sets the steeringAngle variable to be displayed
     * 
     * @param val 
     */
    public void setSteeringAngle(int val) {
        sensorSteeringAngle = val;
    }
    
    /**
     * Method : setDistanceSensor
     * 
     * This method sets the sensorDistance variable to be displayed
     * 
     * @param val 
     */
    public void setDistanceSensor (int val) {
        sensorDistance = val;
    }
    
    /**
     * Method : setDistanceSensorAngle
     * 
     * This method sets the distance Sensor Angle variable to be displayed
     * 
     * @param val 
     */
    public void setDistanceSensorAngle(int val) {
        sensorDistanceAngle = val;
    }
    

    
    public void run() {
        try {
            while (true) {
                repaint();
                Thread.sleep(1000);
            }
        }
        catch (InterruptedException ie) {}
    }
    
    
}
