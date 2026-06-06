package com.example.myapplication;

import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class EbookReaderActivity extends AppCompatActivity {

    private TextView tvReaderBack, tvReaderHeaderTitle;
    private TextView btnTextDecrease, btnTextIncrease;
    private TextView tvContentTitle, tvContentAuthor, tvBookBody;

    private float currentTextSizeSp = 16f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ebook_reader);

        // Bind Views
        tvReaderBack = findViewById(R.id.tvReaderBack);
        tvReaderHeaderTitle = findViewById(R.id.tvReaderHeaderTitle);
        btnTextDecrease = findViewById(R.id.btnTextDecrease);
        btnTextIncrease = findViewById(R.id.btnTextIncrease);
        tvContentTitle = findViewById(R.id.tvContentTitle);
        tvContentAuthor = findViewById(R.id.tvContentAuthor);
        tvBookBody = findViewById(R.id.tvBookBody);

        // Get Intent Data
        String title = getIntent().getStringExtra("title");
        String author = getIntent().getStringExtra("author");
        String description = getIntent().getStringExtra("description");

        if (title == null) title = "Tên Sách";
        if (author == null) author = "Tác giả";

        // Set Toolbar & Initial Details
        tvReaderHeaderTitle.setText(title);
        tvContentTitle.setText(title);
        tvContentAuthor.setText(author);

        // Setup Content text
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
                + "<p>Hãy dành thời gian chiêm nghiệm và áp dụng các bài học vào công việc lẫn cuộc sống thường nhật để cảm nhận những thay đổi tích cực rõ rệt nhất.</p>";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvBookBody.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvBookBody.setText(Html.fromHtml(htmlText));
        }

        // Click listeners
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
    }
}
