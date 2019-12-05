package server.threads;

import server.beans.IceTrade;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/18 10:40
 */
public class IceNotifyThread extends Thread{
    private static List<IceTrade> list = new ArrayList<>();
    private volatile boolean flag = true;

    private IceNotifyThread(){
        setDaemon(true);
        setName("notify_ice_thread_"+getId());
        start();
    }

    public static void addTrade(IceTrade trade){
         list.add(trade);
    }

    @Override
    public void run() {
        while (flag){
            try{
                List<IceTrade> cpList;
                synchronized(this)
                {
                    try
                    {
                        this.wait(30 * 1000);
                    }
                    catch(InterruptedException ignored)
                    {

                    }

                    if(!flag)
                    {
                        break;
                    }

                    cpList = new ArrayList<>(list);
                }
                if(!cpList.isEmpty()){

                    for(IceTrade trade : cpList){
                        try {
                            //通知后台
                            if (trade.notifyIceServer()){
                                synchronized (this){
                                    list.remove(trade);
                                }
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
