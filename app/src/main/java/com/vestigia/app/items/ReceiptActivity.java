package com.vestigia.app.items;

import static com.vestigia.app.items.DetailUi.str;

import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

// Opened with a "claim_id" extra
public class ReceiptActivity extends BaseProtectedActivity {

    private WebView webView;
    private Button btnPrint;
    private ProgressBar progressBar;
    private TextView tvError;
    private String receiptNo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        ((TextView) findViewById(R.id.tvTopTitle)).setText("Handover Receipt");
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        webView = findViewById(R.id.webView);
        btnPrint = findViewById(R.id.btnPrint);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);

        // Printing only works once the page has finished loading
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                btnPrint.setEnabled(true);
            }
        });

        btnPrint.setOnClickListener(v -> printReceipt());

        int claimId = getIntent().getIntExtra("claim_id", -1);
        if (claimId == -1) {
            showError("Receipt not found.");
            return;
        }
        load(claimId);
    }

    private void load(int claimId) {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.RECEIPT_GET + "?claim_id=" + claimId, null, sessionManager.getToken(),
                response -> {
                    try {
                        JSONObject r = response.getJSONObject("receipt");
                        receiptNo = str(r, "receipt_no");
                        webView.loadDataWithBaseURL(null, buildHtml(r), "text/html", "UTF-8", null);
                    } catch (Exception e) {
                        progressBar.setVisibility(View.GONE);
                        showError("Could not read the receipt.");
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    String message = "Could not load the receipt.";
                    try {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            message = new JSONObject(new String(error.networkResponse.data)).optString("message", message);
                        }
                    } catch (Exception ignored) { }
                    showError(message);
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void printReceipt() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        String jobName = "Vestigia receipt " + receiptNo;
        PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);
        printManager.print(jobName, adapter,
                new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    // ---------------- Receipt page ----------------

    private String esc(String s) {
        return TextUtils.htmlEncode(s == null ? "" : s).replace("\n", "<br>");
    }

    private String row(String label, String value) {
        if (value == null || value.trim().isEmpty()) return "";
        return "<tr><td class='l'>" + esc(label) + "</td><td>" + esc(value) + "</td></tr>";
    }

    private String buildHtml(JSONObject r) {
        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>")
                .append("body{font-family:Arial,Helvetica,sans-serif;color:#0F172A;margin:0;padding:22px;}")
                .append(".brand{color:#0F766E;font-size:26px;font-weight:bold;letter-spacing:3px;margin:0;}")
                .append(".tag{color:#64748B;font-size:12px;margin:2px 0 16px;}")
                .append("h2{font-size:19px;margin:0;}")
                .append(".no{background:#E6F7F4;color:#0F766E;font-size:22px;font-weight:bold;padding:12px;")
                .append("border-radius:10px;text-align:center;margin:14px 0 6px;letter-spacing:1px;}")
                .append(".sub{color:#64748B;font-size:12px;text-align:center;margin-bottom:14px;}")
                .append("h3{font-size:12px;color:#0F766E;letter-spacing:2px;margin:20px 0 4px;}")
                .append("table{width:100%;border-collapse:collapse;}")
                .append("td{padding:8px 4px;border-bottom:1px solid #E2E8F0;vertical-align:top;font-size:14px;}")
                .append("td.l{color:#64748B;width:38%;}")
                .append(".sig{margin-top:46px;display:table;width:100%;}")
                .append(".sig div{display:table-cell;width:50%;text-align:center;font-size:12px;color:#64748B;padding:0 14px;}")
                .append(".sig span{display:block;border-top:1px solid #0F172A;padding-top:6px;}")
                .append(".foot{margin-top:30px;font-size:11px;color:#94A3B8;text-align:center;}")
                .append("</style></head><body>");

        h.append("<p class='brand'>VESTIGIA</p><p class='tag'>Tracing what's missing</p>")
                .append("<h2>Handover Receipt</h2>")
                .append("<div class='no'>").append(esc(receiptNo)).append("</div>")
                .append("<div class='sub'>Issued ").append(esc(DetailUi.friendlyDateTime(str(r, "pickup_datetime")))).append("</div>");

        h.append("<h3>ITEM</h3><table>")
                .append(row("Item", str(r, "item_name")))
                .append(row("Category", str(r, "category")))
                .append(row("Found at", str(r, "location")))
                .append(row("Date found", DetailUi.friendlyDate(str(r, "date_found"))))
                .append("</table>");

        h.append("<h3>RELEASED TO</h3><table>")
                .append(row("Name", str(r, "claimant_name")))
                .append(row("Student ID", str(r, "claimant_student_id")))
                .append(row("Claim filed", DetailUi.friendlyDateTime(str(r, "claimed_at"))))
                .append("</table>");

        h.append("<h3>VERIFICATION</h3><table>")
                .append(row("Claim approved by", str(r, "approved_by")))
                .append(row("Approved on", DetailUi.friendlyDateTime(str(r, "decision_date"))))
                .append(row("Handed over by", str(r, "handed_by")))
                .append(row("Picked up", DetailUi.friendlyDateTime(str(r, "pickup_datetime"))))
                .append(row("Notes", str(r, "notes")))
                .append("</table>");

        h.append("<div class='sig'><div><span>Claimant's signature</span></div>")
                .append("<div><span>Released by</span></div></div>")
                .append("<div class='foot'>Keep this receipt as proof that the item was returned to its owner.</div>")
                .append("</body></html>");
        return h.toString();
    }
}