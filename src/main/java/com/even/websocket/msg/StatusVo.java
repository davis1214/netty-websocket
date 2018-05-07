package com.even.websocket.msg;

/**
 * @param
 * @Author: DaviHe
 * @Description:
 * @Date: Created in 2018/4/27
 */
public class StatusVo {

    private int status;
    private String desc;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "StatusVo{" +
                "status=" + status +
                ", desc='" + desc + '\'' +
                '}';
    }
}
