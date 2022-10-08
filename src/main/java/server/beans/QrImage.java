package server.beans;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import com.egzosn.pay.common.util.str.StringUtils;



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
                        if (System.currentTimeMillis() - image.time > qrExistTime * 1000L ){
                            if (image.file.delete())  {
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
        delete_thread.setName("clear-qrimage-t-"+delete_thread.getId());
        delete_thread.start();
    }

    public String link;
    public File file;
    private final long time;

    public QrImage(String domain,String rootDir,String parentDir,String  fileName) {

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

        file = new File(rootDir + parentDir);
        if (!file.exists()) file.mkdirs();
        file = new File(file,fileName);

        link =  domain + parentDir + fileName;

        time = System.currentTimeMillis();

        list.add(this);
    }
}
