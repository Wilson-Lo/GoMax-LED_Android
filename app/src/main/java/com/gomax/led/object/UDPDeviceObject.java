package com.gomax.led.object;

/**
 * UDP Scan result, device info object
 */
public class UDPDeviceObject {

    private String strDeviceName;
    private String strMac;
    private String strIP;
    private String strGateWay;
    private String strMask;

    public UDPDeviceObject(String strDeviceName, String strMac, String strIP, String strGateWay, String strMask){
        this.strDeviceName = strDeviceName;
        this.strMac = strMac;
        this.strIP = strIP;
        this.strGateWay = strGateWay;
        this.strMask = strMask;
    }

    /**
     * Get Device Name
     * @return : Device name
     */
    public String getStrDeviceName() {
        return strDeviceName;
    }

    /**
     * Get Device MAC
     * @return : Device MAC
     */
    public String getStrMac(){
        return strMac;
    }

    /**
     * Get Device Gateway
     * @return : Device Gateway
     */
    public String getStrGateWay() {
        return strGateWay;
    }

    /**
     * Get Device IP
     * @return : Device IP
     */
    public String getIP(){
        return strIP;
    }

    /**
     * Get Device Mask
     * @return Device Mask
     */
    public String getStrMask() {
        return strMask;
    }
}
