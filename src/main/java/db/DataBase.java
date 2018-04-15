package db;

import com.google.common.collect.Maps;
import model.User;

import java.util.Collection;
import java.util.Map;

/**
 * Created by wyparks2@gmail.com on 2018. 4. 4.
 * Blog : http://WonYoungPark.github.io
 * Github : http://github.com/WonYoungPark
 */
public class DataBase {
    private static Map<String, User> users = Maps.newHashMap();

    public static void addUser(User user) {
        users.put(user.getUserId(), user);
    }

    public static User findUserById(String userId) {
        return users.get(userId);
    }

    public static Collection<User> findAll() {
        return users.values();
    }
}
