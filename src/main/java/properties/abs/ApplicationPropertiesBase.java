package properties.abs;

import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import properties.infs.FieldConvert;
import properties.infs.baseImp.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Properties;

/**
 * 自动读取属性文件
 * 子类继承,属性使用注解 PropertiesName
 * 子类不标识,则默认属性文件名application.properties
 *
 */
@PropertiesFilePath("/application.properties")
public abstract class ApplicationPropertiesBase {

    private static final HashMap<String, FieldConvert> baseType = new HashMap<>();

    static {
        baseType.put("class java.lang.String",new StringConvertImp());
        baseType.put("boolean",new BooleanConvertImp());
        baseType.put("int",new IntConvertImp());
        baseType.put("float",new FloatConvertImp());
        baseType.put("double",new DoubleConvertImp());
        baseType.put("long",new LongConvertImp());
    }



    private static final Properties properties = new Properties();

    public ApplicationPropertiesBase() {
        try {
            String filePath = getPropertiesFilePath(this.getClass());
            InputStream in = readPathProperties(this.getClass(),filePath);
            if (in==null) throw new RuntimeException("配置文件获取失败: "+ filePath);
            properties.clear();
            properties.load(in);
            in.close();
            autoReadPropertiesMapToField();
            initialization();
        } catch (Exception e) {
           e.printStackTrace();
        }
    }
    //读取配置文件
    public static InputStream readPathProperties(Class clazz,String filePath) throws FileNotFoundException {
        //优先从外部配置文件获取
        String dirPath = new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        File file = new File(dirPath+"/resources"+filePath);
        if (file.exists()){
            return new FileInputStream(file);
        }
        return clazz.getResourceAsStream( filePath );
    }

    private static String getPropertiesFilePath(Class clazz) {
        PropertiesFilePath annotation = (PropertiesFilePath) clazz.getAnnotation(PropertiesFilePath.class);
        String fileName = annotation.value();
        if (!fileName.startsWith("/")) fileName = "/"+fileName;
        return fileName;
    }

    protected void autoReadPropertiesMapToField(){
        Class clazz = this.getClass();
        Field[] fields = clazz.getDeclaredFields();//某个类的所有声明的字段，即包括public、private和protected ,但是不包括父类的申明字段

        for (Field field : fields){
            PropertiesName name = field.getAnnotation(PropertiesName.class);
            if(null != name){
                boolean canAccess = field.isAccessible();//如果成员为私有，暂时让私有成员运行被访问和修改
                if(!canAccess){
                    field.setAccessible(true);
                }
                String key = name.value();
                String value = properties.getProperty(key);
                if (value==null || value.length()==0) {
                    continue;
                }
                //获取属性类型
                String type = field.getGenericType().toString();
                if(baseType.containsKey(type)){
                    try {
                        baseType.get(type).setValue(this,field,value);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                field.setAccessible(canAccess);//改回原来的访问方式
            }
        }
    }

    public static void initStaticFields(Class clazz) {
        try {
            String filePath = getPropertiesFilePath(clazz);
            InputStream in = readPathProperties(clazz,filePath);
            if (in==null) throw new RuntimeException("配置文件获取失败: "+ filePath);
            properties.clear();
            properties.load(in);
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                PropertiesName name = field.getAnnotation(PropertiesName.class);
                if (name==null) continue;
                field.setAccessible(true);
                String key = name.value();
                String value = properties.getProperty(key);
                if (value==null || value.length()==0) continue;
                //获取属性类型
                String type = field.getGenericType().toString();
                if(baseType.containsKey(type)){
                    try {
                        baseType.get(type).setValue(clazz,field,value);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void initialization(){
        Field[] fields = getClass().getDeclaredFields();
        for(Field field:fields){
            field.setAccessible(true); // 设置些属性是可以访问的
            PropertiesName name = field.getAnnotation(PropertiesName.class);
            if (name == null) continue;
            try {
                Object k = name.value();
                Object v = field.get(this);
                System.out.println( "\t" + k + " = "  + v);
            } catch (IllegalAccessException ignored) {
            }
        }

    }
}
