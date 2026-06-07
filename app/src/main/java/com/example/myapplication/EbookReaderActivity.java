package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

public class EbookReaderActivity extends AppCompatActivity {

    private TextView tvReaderBack, tvReaderHeaderTitle;
    private TextView btnTextDecrease, btnTextIncrease;
    private TextView tvContentTitle, tvContentAuthor, tvBookBody;
    private NestedScrollView readerScrollView;

    private float currentTextSizeSp = 16f;
    private boolean hasNotifiedChapterFinished = false;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 200;
    private static final String ACTION_CHAPTER_FINISHED =
            "com.example.myapplication.ACTION_CHAPTER_FINISHED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ebook_reader);

        NotificationHelper.createNotificationChannel(this);
        requestNotificationPermission();

        tvReaderBack = findViewById(R.id.tvReaderBack);
        tvReaderHeaderTitle = findViewById(R.id.tvReaderHeaderTitle);
        btnTextDecrease = findViewById(R.id.btnTextDecrease);
        btnTextIncrease = findViewById(R.id.btnTextIncrease);
        tvContentTitle = findViewById(R.id.tvContentTitle);
        tvContentAuthor = findViewById(R.id.tvContentAuthor);
        tvBookBody = findViewById(R.id.tvBookBody);
        readerScrollView = findViewById(R.id.readerScrollView);

        String title = getIntent().getStringExtra("title");
        String author = getIntent().getStringExtra("author");
        String description = getIntent().getStringExtra("description");

        if (title == null) title = "Tên Sách";
        if (author == null) author = "Tác giả";

        tvReaderHeaderTitle.setText(title);
        tvContentTitle.setText(title);
        tvContentAuthor.setText(author);

        String defaultDesc = "Cuốn sách này là một tác phẩm xuất sắc chia sẻ về kiến thức và kinh nghiệm thực tiễn nhằm mang lại các giá trị sâu sắc cho người đọc.";
        String bookDescription = (description != null && !description.isEmpty()) ? description : defaultDesc;

        String htmlText = "<h3>Mở đầu</h3>"
                + "<p>" + bookDescription + "</p>"
                + "<br><h3>Chương 1: Khái niệm cốt lõi</h3>"
                + "<p>Trong chương này, tác giả <b>" + author + "</b> sẽ đưa chúng ta đi qua những khái niệm nền tảng. Việc thấu hiểu các nguyên lý cơ bản không chỉ giúp định hình tư duy mà còn đặt nền móng vững chắc cho các bước thực hành tiếp theo.</p>"
                + "<p><i>\"Hành trình vạn dặm bắt đầu từ một bước chân.\"</i> Điểm cốt lõi là sự kiên trì và ứng dụng mỗi ngày.</p>"
                + "<br><h3>Chương 2: Phương pháp thực hành</h3>"
                + "<p>Cuốn sách <i>" + title + "</i> gợi ý các phương pháp cụ thể:</p>"
                + "<ul>"
                + "  <li>Lập kế hoạch hành động chi tiết.</li>"
                + "  <li>Đo lường và đánh giá hiệu quả định kỳ.</li>"
                + "  <li>Không ngừng cải tiến và tối ưu hóa quy trình.</li>"
                + "</ul>"
                + "<p>Hãy dành thời gian chiêm nghiệm và áp dụng các bài học vào công việc lẫn cuộc sống thường nhật để cảm nhận những thay đổi tích cực rõ rệt nhất.</p>"
                + "<br><br><br><p><b>Kết thúc chương.</b></p>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvBookBody.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvBookBody.setText(Html.fromHtml(htmlText));
        }

        tvReaderBack.setOnClickListener(v -> finish());

        btnTextIncrease.setOnClickListener(v -> {
            if (currentTextSizeSp < 32f) {
                currentTextSizeSp += 2f;
                tvBookBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSizeSp);
            }
        });

        btnTextDecrease.setOnClickListener(v -> {
            if (currentTextSizeSp > 12f) {
                currentTextSizeSp -= 2f;
                tvBookBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSizeSp);
            }
        });

        setupChapterFinishedTracking(title);
    }

    private void setupChapterFinishedTracking(String bookTitle) {
        readerScrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (readerScrollView.getChildCount() == 0) {
                        return;
                    }

                    int childHeight = readerScrollView.getChildAt(0).getMeasuredHeight();
                    int scrollViewHeight = readerScrollView.getMeasuredHeight();

                    boolean isAtBottom = scrollY + scrollViewHeight >= childHeight - 30;

                    if (isAtBottom && !hasNotifiedChapterFinished) {
                        hasNotifiedChapterFinished = true;
                        sendChapterFinishedBroadcast(bookTitle);
                    }
                }
        );
    }

    private void sendChapterFinishedBroadcast(String bookTitle) {
        Intent intent = new Intent(this, ChapterFinishedReceiver.class);
        intent.setAction(ACTION_CHAPTER_FINISHED);
        intent.putExtra("chapter_name", "chương trong sách \"" + bookTitle + "\"");
        sendBroadcast(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }
}