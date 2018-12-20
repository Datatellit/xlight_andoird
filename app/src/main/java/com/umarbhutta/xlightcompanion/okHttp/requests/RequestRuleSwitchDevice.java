package com.umarbhutta.xlightcompanion.okHttp.requests;

import android.content.Context;

import com.umarbhutta.xlightcompanion.Tools.Logger;
import com.umarbhutta.xlightcompanion.Tools.UserUtils;
import com.umarbhutta.xlightcompanion.okHttp.HttpUtils;
import com.umarbhutta.xlightcompanion.okHttp.NetConfig;
import com.umarbhutta.xlightcompanion.okHttp.requests.imp.CommentRequestCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by guangbinw on 2017/3/14.
 * 启用、禁用规则
 */
public class RequestRuleSwitchDevice implements HttpUtils.OnHttpRequestCallBack {

    private Context context;
    private CommentRequestCallback mCommentRequestCallback;

    public static RequestRuleSwitchDevice getInstance() {
        return new RequestRuleSwitchDevice();
    }

    /**
     * 启用、禁用规则
     *
     * @param context
     * @param deviceId
     * @param status                 1代表启用，0代表禁用
     * @param mCommentRequestCallback
     */
    public void switchRule(Context context, int deviceId, int status, CommentRequestCallback mCommentRequestCallback) {
        this.context = context;
        this.mCommentRequestCallback = mCommentRequestCallback;

        try {
            JSONObject object = new JSONObject();
            object.put("status", status);
            String params = object.toString();
            if (UserUtils.isLogin(context))
                HttpUtils.getInstance().putRequestInfo(NetConfig.URL_RULE_SWITCH + deviceId + "/changestatus?access_token=" + UserUtils.getAccessToken(context),
                        params, null, this);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onHttpRequestSuccess(Object result) {
        Logger.i("result 开关 = 成功");
        if (null != mCommentRequestCallback) {
            mCommentRequestCallback.onCommentRequestCallbackSuccess();
        }
    }

    @Override
    public void onHttpRequestFail(int code, String errMsg) {
        if (null != mCommentRequestCallback) {
            mCommentRequestCallback.onCommentRequestCallbackFail(code, errMsg);
        }
    }


}
