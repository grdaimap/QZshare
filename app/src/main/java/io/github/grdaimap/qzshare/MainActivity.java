package io.github.grdaimap.qzshare;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private TextView title, url, pics;
    private String[][] dic =
            {
                    {".*bilibili.com.*", "meta[content$=.jpg]", "content", null, null}, {".*mp.weixin.qq.com.*", "meta[property=\"og:image\"]", "content", "meta[property=\"og:title\"]", "content"},
            };
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        //当有消息发送出来的时候就执行Handler的这个方法
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    String[] re = (String[]) msg.obj;
                    title.setText(re[0]);
                    url.setText(re[1]);
                    pics.setText(re[2]);
                    String s = "https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?url=" +
                            re[1] + "&title=" + re[0] + "&pics=" + re[2];
                    Log.d("qq", s);
                    Intent it = new Intent();
                    it.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(s);
                    it.setData(content_url);
                    startActivity(it);
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(), "网络错误", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        title = findViewById(R.id.title);
        url = findViewById(R.id.url);
        pics = findViewById(R.id.pics);
        Button button = findViewById(R.id.button);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qzshare();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        qzshare();
    }

    private void qzshare() {
        ClipboardManager myClipboard;
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData abc = myClipboard.getPrimaryClip();
        if (abc == null) {
            Toast.makeText(getApplicationContext(), "剪切板无内容", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData.Item item = abc.getItemAt(0);
        final String text = item.getText().toString();
        Log.d("cut", text);
        boolean finder = Pattern.matches("^http.*", text);
        if (finder) {
            Log.d("cut", "开始联网");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Document doc;
                    try {
                        String t, u, p;
                        u = text;
                        p = "";
                        doc = Jsoup.connect(u).userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64)").get();// Jsoup是我用的一个牛叉的jar包
                        t = doc.title();
                        Log.d("cut", "开始获取图片");
                        for (String[] strings : dic) {
                            if (Pattern.matches(strings[0], u)) {
                                if (strings[1] != null) {
                                    p = doc.select(strings[1]).attr(strings[2]);
                                }
                                if (strings[3] != null) {
                                    t = doc.select(strings[3]).attr(strings[4]);
                                }
                            } else {
                                p = doc.select("img[src~=.*jpg.*]").attr("abs:src");
                            }
                        }
                        Message msg = new Message();
                        msg.what = 0;
                        msg.obj = new String[]{t, u, p};
                        handler.sendMessage(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        handler.sendEmptyMessage(1);
                    }
                }
            }).start();
        } else {
            Toast.makeText(getApplicationContext(), "未获取网址", Toast.LENGTH_SHORT).show();
        }
    }
}
