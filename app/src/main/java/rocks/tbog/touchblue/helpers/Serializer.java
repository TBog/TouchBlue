package rocks.tbog.touchblue.helpers;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class Serializer {
    private static final String TAG = Serializer.class.getSimpleName();

    /**
     * Read the object from Base64 string.
     */
    public static Object fromString(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64.decode(s, Base64.DEFAULT);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    public static <T> T fromStringOrNull(String s, Class<T> clazz) {
        Object o;
        try {
            o = fromString(s);
        } catch (IOException | ClassNotFoundException e) {
            Log.w(TAG, "string " + s + " failed to unserialize", e);
            return null;
        }
        try {
            return clazz.cast(o);
        } catch (ClassCastException e) {
            Log.w(TAG, "unserialized object " + o + " is not " + clazz, e);
            return null;
        }
    }

    /**
     * Write the object to a Base64 string.
     */
    public static String toString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        var encodedArray = Base64.encode(baos.toByteArray(), Base64.NO_WRAP);
        return new String(encodedArray);
    }

    @Nullable
    public static String toStringOrNull(Serializable object) {
        try {
            return toString(object);
        } catch (IOException e) {
            Log.w(TAG, "object " + object + " failed to serialize", e);
        }
        return null;
    }

    public static String toBase64(String text) {
        var encodedArray = Base64.encode(text.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        return new String(encodedArray);
    }

    public static String fromBase64(String base64) {
        var decodedArray = Base64.decode(base64, Base64.DEFAULT);
        return new String(decodedArray);
    }
}
