package com.gomax.led.data;

/**
 * UDP scan device
 */
public class DeviceObject {

    private String strDevice_mac, strDevice_ip;


    public DeviceObject(String strDevice_mac, String strDevice_ip) {
        this.strDevice_mac = strDevice_mac;
        this.strDevice_ip = strDevice_ip;
    }

    /**
     * set device mac
     * @param strDevice_mac : device mac
     */
    public void setDevice_mac(String strDevice_mac) {
        this.strDevice_mac = strDevice_mac;
    }

    /**
     * get device ip
     * @param strDevice_ip : device ip
     */
    public void setDevice_ip(String strDevice_ip) {
        this.strDevice_ip = strDevice_ip;
    }

    /**
     * get device mac
     * @return : device mac
     */
    public String getDevice_mac() {
        return this.strDevice_mac;
    }

    /**
     * get device ip
     * @return : device ip
     */
    public String getDevice_ip() {
        return this.strDevice_ip;
    }
}
