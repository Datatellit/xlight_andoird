package com.umarbhutta.xlightcompanion.okHttp.requests;

import android.content.Context;

import com.umarbhutta.xlightcompanion.Tools.UserUtils;
import com.umarbhutta.xlightcompanion.okHttp.HttpUtils;
import com.umarbhutta.xlightcompanion.okHttp.NetConfig;
import com.umarbhutta.xlightcompanion.okHttp.requests.imp.CommentRequestCallback;

/**
 * Created by guangbinw on 2017/3/14.
 * 删除规则
 */
public class RequestDeleteRule implements HttpUtils.OnHttpRequestCallBack {

    private Context context;
    private CommentRequestCallback mCommentRequestCallback;

    public static RequestDeleteRule getInstance() {
        return new RequestDeleteRule();
    }

    /**
     * 删除规则
     */
    public void deleteRule(Context context, int ruleId, CommentRequestCallback mCommentRequestCallback) {
        this.context = context;
        this.mCommentRequestCallback = mCommentRequestCallback;
        if (UserUtils.isLogin(context))
            HttpUtils.getInstance().deleteRequestInfo(NetConfig.URL_DELETE_RULE + ruleId + "?access_token=" + UserUtils.getAccessToken(context),
                    null, null, this);
    }


    @Override
    public void onHttpRequestSuccess(Object result) {
        if (null != mCommentRequestCallback) {
            mCommentRequestCallback.onCommentRequestCallbackSuccess();
        }
    }

    @Override
    public void onHttpRequestFail(int code, String errMsg) {
        if (null != mCommentRequestCallback) {
            if (code == 0) {
                mCommentRequestCallback.onCommentRequestCallbackSuccess();
            }
            mCommentRequestCallback.onCommentRequestCallbackFail(code, errMsg);
        }
    }


}