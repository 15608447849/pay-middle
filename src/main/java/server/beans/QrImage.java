package server.beans;

import com.egzosn.pay.common.util.str.StringUtils;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/18 10:49
 * PC 支付二维码
 */
@PropertiesFilePath("/application.properties")
public class QrImage {

    @PropertiesName("qrimage.exist.time")
    public static int qrExistTime;

    private static List<QrImage> list = new ArrayList<>();

    private static Thread delete_thread = new Thread(() -> {
        while (true){

            try{
              Thread.sleep(qrExistTime * 1000L);
              List<QrImage> cpList = new ArrayList<>(list);
                for(QrImage image : cpList){

                    try{
                        if (System.currentTimeMillis() - image.time > qrExistTime * 1000 ){
                            if (image.qrImage.delete())  {
                                list.remove(image);
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    });

    static {
        ApplicationPropertiesBase.initStaticFields(QrImage.class);
        delete_thread.setDaemon(true);
        delete_thread.setName("clear_qr_image_thread"+delete_thread.getId());
        delete_thread.start();
    }




    public String link;
    public File qrImage;
    private final long time;

    public QrImage(String rootDir,String parentDir,String  fileName) {

        if (StringUtils.isNotEmpty(rootDir)){
            if (rootDir.endsWith("/")) rootDir += rootDir.substring(0,rootDir.length()-1);
        }

        if (StringUtils.isNotEmpty(parentDir)){
            if (!parentDir.startsWith("/")) parentDir = "/"+parentDir;
            if (!parentDir.endsWith("/")) parentDir += "/";
        }

        if (StringUtils.isNotEmpty(fileName)){
            if (fileName.startsWith("/")) fileName = fileName.substring(1);
        }

        fileName+=".png";
        link = parentDir + fileName;

        qrImage = new File(rootDir + parentDir);
        if (!qrImage.exists()) qrImage.mkdirs();
        qrImage = new File(qrImage,fileName);
        time = System.currentTimeMillis();
        list.add(this);
    }
}
