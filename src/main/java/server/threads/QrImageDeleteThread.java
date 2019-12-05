package server.threads;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import server.beans.QrImage;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/18 10:40
 * 删除失效二维码
 */
@PropertiesFilePath("/application.properties")
public class QrImageDeleteThread extends Thread{


    @PropertiesName("qrimage.exist.time")
    public static int qrExistTime;

    static {
        ApplicationPropertiesBase.initStaticFields(QrImageDeleteThread.class);
    }

    private static List<QrImage> list = new ArrayList<>();

    private static QrImageDeleteThread thread = new QrImageDeleteThread();

    private volatile boolean flag = true;

    private QrImageDeleteThread(){
        setDaemon(true);
        setName("delete_qrimage_thread");
        start();
    }

    public static void addQrImage(QrImage image){
         list.add(image);
    }

    @Override
    public void run() {
        while (flag){
            try{
                List<QrImage> cpList;
                synchronized(this)
                {
                    try
                    {
                        this.wait(qrExistTime * 1000);
                    }
                    catch(java.lang.InterruptedException ignored)
                    {

                    }
                    if(!flag)
                    {
                        break;
                    }

                    cpList = new ArrayList<>(list);
                }
                if(!cpList.isEmpty()){

                    for(QrImage image : cpList){
                        try
                        {
                            if (System.currentTimeMillis() - image.time > qrExistTime*1000 ){
                                    if (image.qrImage.delete())  {
                                        synchronized (this){
                                            list.remove(image);
                                        }
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
