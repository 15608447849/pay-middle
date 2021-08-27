package server.beans;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import server.Launch;


/**
 * @Author: leeping
 * @Date: 2019/4/18 15:20
 */
public class IceResult {

    private static Gson gson = new GsonBuilder().setLongSerializationPolicy(LongSerializationPolicy.STRING).create();

    public int code = 0;
    public Object data;
    public String message;


    public IceResult set(int code, Object data){
        this.code = code;
        this.data = data;
        return this;
    }
    public IceResult set(int code, String message, Object data){
        this.code = code;
        this.data = data;
        this.message = message;
        return this;
    }

    public String toJson(){
        return gson.toJson(this);
    }

}
