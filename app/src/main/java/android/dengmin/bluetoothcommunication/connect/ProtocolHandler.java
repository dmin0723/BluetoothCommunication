package android.dengmin.bluetoothcommunication.connect;

/**
 * Created by dmin on 2016/6/10.
 */
/**
    * 处理网络协议，对数据进行封包或解包
    *
    */
public interface ProtocolHandler<T> {

    public byte[] encodePackage(T data);

    public T decodePackage(byte[] netData);
}