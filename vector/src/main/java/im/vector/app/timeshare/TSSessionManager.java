/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.timeshare;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class TSSessionManager {
    SharedPreferences pref_login;
    SharedPreferences pref_emailverify;
    SharedPreferences.Editor editor_login;
    SharedPreferences.Editor editor_emailverify;
    Context mContext;
    int PRIVATE_MODE = 0;
    private static final String PREF_LOGIN = "login";
    private static final String PREF_EmailVerify = "fcm";

    public static String KEY_user_uuid = "userid";
    public static String KEY_first_name = "first_name";
    public static String KEY_last_name = "last_name";
    public static String KEY_email_id = "email_id";
    public static String KEY_profile_name = "profile_name";
    public static String KEY_mobile_number = "mobile_number";
    public static String KEY_chat_id = "chat_id";
    public static String KEY_chat_password = "chat_password";

    public static String KEY_isCategory = "is_category";
    public static String KEY_isSubCategory = "is_sub_category";

    private static final String IS_LOGIN = "isLogedIn";
    private static final String IS_EmailVerified = "isEmailVerified";


    public TSSessionManager(Context context) {
        this.mContext = context;
        pref_login = mContext.getSharedPreferences(PREF_LOGIN, PRIVATE_MODE);
        editor_login = pref_login.edit();
        pref_emailverify = mContext.getSharedPreferences(PREF_EmailVerify, PRIVATE_MODE);
        editor_emailverify = pref_emailverify.edit();
    }

    public void createLoginSession(
            boolean isLogin, String user_uuid, String first_name, String last_name,String email_id,String profile_name,String mobile_number,String chat_id,String chat_password,boolean is_category,boolean is_sub_category) {
        editor_login.putBoolean(IS_LOGIN, isLogin);
        editor_login.putString(KEY_user_uuid, user_uuid);
        editor_login.putString(KEY_first_name, first_name);
        editor_login.putString(KEY_last_name, last_name);
        editor_login.putString(KEY_email_id, email_id);
        editor_login.putString(KEY_profile_name, profile_name);
        editor_login.putString(KEY_mobile_number, mobile_number);
        editor_login.putString(KEY_chat_id, chat_id);
        editor_login.putString(KEY_chat_password, chat_password);
        editor_login.putBoolean(KEY_isCategory,is_category);
        editor_login.putBoolean(KEY_isSubCategory,is_sub_category);
        editor_login.apply();
    }

    public void createEmailVerify(boolean isEmailverify,String email){
        editor_emailverify.putBoolean(IS_EmailVerified,isEmailverify);
        editor_emailverify.putString(KEY_email_id, email);
        editor_emailverify.apply();
    }

    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();
        user.put(KEY_user_uuid, pref_login.getString(KEY_user_uuid, null));
        user.put(KEY_first_name, pref_login.getString(KEY_first_name, null));
        user.put(KEY_last_name, pref_login.getString(KEY_last_name, null));
        user.put(KEY_email_id, pref_login.getString(KEY_email_id, null));
        user.put(KEY_profile_name, pref_login.getString(KEY_profile_name, null));
        user.put(KEY_mobile_number, pref_login.getString(KEY_mobile_number, null));
        user.put(KEY_chat_id, pref_login.getString(KEY_chat_id, null));
        user.put(KEY_chat_password, pref_login.getString(KEY_chat_password, null));

        return user;
    }

    public HashMap<String, String> getUserEmail() {
        HashMap<String, String> email = new HashMap<String, String>();
        email.put(KEY_email_id, pref_emailverify.getString(KEY_email_id, null));

        return email;
    }

    public void logoutUser() {
        // Clearing all data from Shared Preferences
        editor_login.clear();
        editor_login.commit();
        // Toast.makeText(mContext, "Logout Successfully", Toast.LENGTH_SHORT).show();
    }

    public boolean isLoggedIn() {
        return pref_login.getBoolean(IS_LOGIN, false);
    }

    public boolean isEmailVerified() {
        return pref_emailverify.getBoolean(IS_EmailVerified, false);
    }
    public boolean isCategory(){
        return pref_login.getBoolean(KEY_isCategory,false);
    }
    public boolean isSubCategory(){
        return pref_login.getBoolean(KEY_isSubCategory,false);
    }
}
