import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: leeping
 * @Date: 2019/4/16 16:17
 */
public class test {
    public static void main(String[] args) throws Exception {

        Long time = System.currentTimeMillis() + 30*1000*60;
        System.out.println("获取当前系统时间为："+new SimpleDateFormat("yyyy年-MM月dd日-HH时mm分ss秒").format(time));//转换成标准年月日的形式
        Date date = new Date(time);
        time += 30*1000*60;//在当前系统时间的基础上往后加30分钟
        System.out.println("三十分钟后的时间为："+new SimpleDateFormat("yyyy年-MM月dd日-HH时mm分ss秒").format(time));



    }
}
