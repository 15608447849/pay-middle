package server.beans;

import com.egzosn.pay.common.util.str.StringUtils;

import java.io.File;

/**
 * @Author: leeping
 * @Date: 2019/4/18 10:49
 */
public class QrImage {
    public String rootDir;
    public String parentDir;
    public String fileName;
    public String link;
    public File qrImage;
    public long time;

    public QrImage(String rootDir,String parentDir,String  fileName)
    {

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
    }
}
