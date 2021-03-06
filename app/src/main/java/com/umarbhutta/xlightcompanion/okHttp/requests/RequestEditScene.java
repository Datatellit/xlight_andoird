package com.umarbhutta.xlightcompanion.okHttp.requests;

import android.content.Context;

import com.google.gson.Gson;
import com.umarbhutta.xlightcompanion.Tools.UserUtils;
import com.umarbhutta.xlightcompanion.okHttp.HttpUtils;
import com.umarbhutta.xlightcompanion.okHttp.NetConfig;
import com.umarbhutta.xlightcompanion.okHttp.model.EditSceneParams;
import com.umarbhutta.xlightcompanion.okHttp.requests.imp.CommentRequstCallback;

/**
 * Created by guangbinw on 2017/3/14.
 * 编辑场景
 */
public class RequestEditScene implements HttpUtils.OnHttpRequestCallBack {

    private Context context;
    private CommentRequstCallback mCommentRequstCallback;

    public static RequestEditScene getInstance() {
        return new RequestEditScene();
    }

    /**
     * 编辑场景
     *
     * @param context
     * @param params
     * @param mCommentRequstCallback
     */
    public void editScene(Context context, int id, EditSceneParams params, CommentRequstCallback mCommentRequstCallback) {
        this.context = context;
        this.mCommentRequstCallback = mCommentRequstCallback;
        if (UserUtils.isLogin(context)) {

            Gson gson = new Gson();
            String paramStr = gson.toJson(params);

            HttpUtils.getInstance().putRequestInfo(NetConfig.URL_EDIT_SCENE + id + "?access_token=" + UserUtils.getAccessToken(context),
                    paramStr, null, this);
        }
    }

    @Override
    public void onHttpRequestSuccess(Object result) {
        if (null != mCommentRequstCallback) {
            mCommentRequstCallback.onCommentRequstCallbackSuccess();
        }
    }

    @Override
    public void onHttpRequestFail(int code, String errMsg) {
        if (null != mCommentRequstCallback) {
            mCommentRequstCallback.onCommentRequstCallbackFail(code, errMsg);
        }
    }


}
