package com.umarbhutta.xlightcompanion.userManager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gyf.barlibrary.ImmersionBar;
import com.umarbhutta.xlightcompanion.R;
import com.umarbhutta.xlightcompanion.Tools.AndroidBug54971Workaround;
import com.umarbhutta.xlightcompanion.Tools.StringUtil;
import com.umarbhutta.xlightcompanion.Tools.ToastUtil;
import com.umarbhutta.xlightcompanion.okHttp.requests.RequestSendVerifyCode;
import com.umarbhutta.xlightcompanion.okHttp.requests.imp.CommentRequestCallback;
import com.umarbhutta.xlightcompanion.settings.BaseActivity;
import com.umarbhutta.xlightcompanion.settings.ResetPasswordActivity;

/**
 * Created by Administrator on 2017/3/4.
 */

public class FindPasswordActivity extends BaseActivity implements View.OnClickListener {

    private LinearLayout llBack;
    private TextView tvTitle;
    private EditText et_user_account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_findpassword);
        AndroidBug54971Workaround.assistActivity(findViewById(android.R.id.content));
        initViews();
        ImmersionBar.with(this).titleBar(R.id.ll_top_edit).statusBarDarkFont(true).init();
    }

    private void initViews() {
        Button btn_finash_registered = (Button) findViewById(R.id.btn_finash_registered);
        btn_finash_registered.setOnClickListener(this);
        btn_finash_registered.setText(R.string.next);
        findViewById(R.id.tv_protocol).setOnClickListener(this);
        llBack = (LinearLayout) findViewById(R.id.ll_back);
        llBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.tvEditSure).setVisibility(View.INVISIBLE);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvTitle.setText(R.string.find_pwd);
        et_user_account = (EditText) findViewById(R.id.et_user_account);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImmersionBar.with(this).destroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_finash_registered:
                findPwd();
                break;
            case R.id.tv_protocol:
                //TODO
                onFabPressed();
                break;
        }
    }

    private void onFabPressed() {
        Intent intent = new Intent(FindPasswordActivity.this, UserResProtocalActivity.class);
        startActivityForResult(intent, 1);
    }

    private void findPwd() {

        final String email = et_user_account.getText().toString();

        if (TextUtils.isEmpty(email)) {
            ToastUtil.showToast(this, getString(R.string.input_email));
            return;
        }

        if (!StringUtil.isEmail(email)) {
            ToastUtil.showToast(this, R.string.email_error);
            return;
        }
        showProgressDialog(getString(R.string.loading));
        RequestSendVerifyCode.getInstance().sendCode(this, email, new CommentRequestCallback() {
            @Override
            public void onCommentRequestCallbackSuccess() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelProgressDialog();
                        Intent intent = new Intent(FindPasswordActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        FindPasswordActivity.this.finish();
                    }
                });


            }

            @Override
            public void onCommentRequestCallbackFail(int code, final String errMsg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelProgressDialog();
                        ToastUtil.showToast(FindPasswordActivity.this, "" + errMsg);
                    }
                });
            }
        });
    }

}
