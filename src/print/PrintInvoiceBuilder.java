package print;

import context.CompanyContext;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import model.CompanyProfile;
import model.Invoice;
import model.InvoiceCopyType;
import model.InvoiceItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import print.PrintFormat;

public class PrintInvoiceBuilder {

    /* ================= PAGE ================= */

    private static final Font FONT = Font.font(10);
    private static final DateTimeFormatter DF =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String CELL =
            "-fx-border-width: 0;";

    private static final String CELL_LAST =
            "-fx-border-width: 0;";


    private static final String HEADER_CELL =
            "-fx-border-color: black; -fx-border-width: 0 0 1 0; -fx-font-weight: bold;";

    private static final String HEADER_CELL_LAST =
            "-fx-border-color: black; -fx-border-width: 0 0 1 0; -fx-padding: 6; -fx-font-weight: bold;";
    private static final Insets LABEL_PAD = new Insets(2, 6, 2, 4);
    private static final Insets VALUE_PAD = new Insets(2, 4, 2, 8);



    /* ================= TABLE WIDTHS ================= */

    private static final double COL_SL   = 30;
    private static final double COL_DESC = 302;
    private static final double COL_HSN  = 80;
    private static final double COL_QTY  = 65;
    private static final double COL_UNIT = 45;
    private static final double COL_RATE = 80;
    private static final double COL_GST  = 60;
    private static final double COL_AMT  = 100;

    private static final double TABLE_WIDTH =
            COL_SL + COL_DESC + COL_HSN + COL_QTY +
                    COL_UNIT + COL_RATE + COL_GST + COL_AMT;

    private static final double CONTENT_WIDTH = TABLE_WIDTH;
    private static final double HALF = CONTENT_WIDTH / 2;
    private static final double LABEL_COL = HALF * 0.30;
    private static final double VALUE_COL = HALF * 0.70;
    private static final int TABLE_ROWS = 12;
    private static final double ROW_HEIGHT = 28;

    private SellerSnapshot seller;


    /* ================================================= */

    public VBox build(Invoice inv, List<InvoiceItem> items, InvoiceCopyType copy) {

        this.seller = resolveSeller(inv);
        VBox doc = new VBox();
        doc.setAlignment(Pos.CENTER);

        VBox page = new VBox();
        page.setPrefSize(PrintFormat.A4_WIDTH, PrintFormat.A4_HEIGHT);
        page.setStyle("-fx-background-color:white;");

        // Remove fixed width from pageBorder to let it wrap the content
        VBox pageBorder = new VBox();
        pageBorder.setStyle("-fx-border-color: black; -fx-border-width: 1;");

        // Ensure the border box is exactly the width of our table + internal padding
        pageBorder.setMaxWidth(TABLE_WIDTH + 32);
        pageBorder.setMinWidth(TABLE_WIDTH + 32);

        VBox pageContent = new VBox(8);
        pageContent.setPadding(new Insets(12, 16, 12, 16));
        pageContent.setAlignment(Pos.TOP_CENTER);

        pageContent.getChildren().add(buildHeader(inv, copy));
        pageContent.getChildren().add(buildMainBox(inv, items));
        pageContent.getChildren().add(buildPageNo(1, 1));

        pageBorder.getChildren().add(pageContent);


        // Center the border box within the A4 page
        page.getChildren().add(pageBorder);
        page.getChildren().add(compterLabel());
        page.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(pageBorder, new Insets(20, 0, 0, 0));

        doc.getChildren().add(page);
        return doc;
    }

    private static final class SellerSnapshot {
        String name;
        String description;
        String address;
        String gst;
        String phone;
        String email;
        String bank;
        String account;
        String ifsc;
    }

    private SellerSnapshot resolveSeller(Invoice inv) {

        SellerSnapshot s = new SellerSnapshot();

        // ✅ NEW invoices (snapshot stored in invoice)
        if (inv.getSellerName() != null && !inv.getSellerName().isBlank()) {

            s.name        = inv.getSellerName();
            s.description = inv.getSellerDescription();
            s.address     = inv.getSellerAddress();
            s.gst         = inv.getSellerGst();
            s.phone       = inv.getSellerPhone();
            s.email       = inv.getSellerEmail();
            s.bank        = inv.getSellerBankName();
            s.account     = inv.getSellerAccountNo();
            s.ifsc        = inv.getSellerIfsc();

            return s;
        }

        // ⚠ LEGACY invoices → fallback to context
        CompanyProfile c = CompanyContext.get();

        s.name        = c.getLegalName();
        s.description = c.getDescription();
        s.address     = c.getAddress();
        s.gst         = c.getGstin();
        s.phone       = c.getPhoneNo();
        s.email       = c.getEmail();
        s.bank        = c.getBankName();
        s.account     = c.getAccountNo();
        s.ifsc        = c.getIfsc();

        return s;
    }



    /* ================= HEADER ================= */

    private Node buildHeader(Invoice inv, InvoiceCopyType copy) {

        GridPane g = new GridPane();
        g.setMinWidth(TABLE_WIDTH);
        g.setMaxWidth(TABLE_WIDTH);
        g.setHgap(10);

        ColumnConstraints l = new ColumnConstraints();
        ColumnConstraints r = new ColumnConstraints();
        l.setPercentWidth(50);
        r.setPercentWidth(50);
        g.getColumnConstraints().addAll(l, r);


        VBox left = new VBox(2,
                bold(seller.name, 28),
                headerText(seller.description),
                headerText(seller.address),
                headerText("GST No: " + seller.gst),
                headerText("Phone: " + seller.phone + "   Email: " + seller.email)
        );


        Label copyLabel = (copy != null)
                ? bold(copy.getLabel(), 12)
                : new Label("");

        VBox right = new VBox(2,
                bold("TAX INVOICE", 24),
                copyLabel,
                headerText("Invoice No: " + inv.getInvoiceNo()),
                headerText("Invoice Date: " + df(inv.getInvoiceDate())),
                headerText("Terms of Payment: " + safe(inv.getTermsOfPayment()))
//                headerText()
        );
        right.setAlignment(Pos.TOP_RIGHT);
        right.setMaxWidth(Double.MAX_VALUE);

        g.add(left, 0, 0);
        g.add(right, 1, 0);

        return g;
    }

    /* ================= MAIN BOX ================= */

    private Node buildMainBox(Invoice inv, List<InvoiceItem> items) {
        VBox box = new VBox(0);
        box.setStyle("-fx-border-color:black; -fx-border-width:1;");
        box.setMinWidth(CONTENT_WIDTH);
        box.setMaxWidth(CONTENT_WIDTH);


        box.getChildren().add(buildMetaAndParty(inv));
        box.getChildren().add(buildTable(items));
        box.getChildren().add(buildTotals(inv));
        box.getChildren().add(buildFooter());

        return box;
    }

    /* ================= META + PARTY ================= */

    private Node buildMetaAndParty(Invoice inv) {

        GridPane g = new GridPane();
        g.setStyle(PrintStyle.BORDER_BOTTOM);
        g.setMinWidth(CONTENT_WIDTH);
        g.setMaxWidth(CONTENT_WIDTH);


        g.getColumnConstraints().addAll(
                fixed(HALF),
                fixed(1),
                fixed(HALF)
        );

        Region divider = vLine();
        GridPane.setRowSpan(divider, 2);
        g.add(divider, 1, 0);

        g.add(buildMeta(inv), 0, 0, 3, 1);
        g.add(buildBuyer(inv), 0, 1);
        g.add(buildConsignee(inv), 2, 1);

        return g;
    }

    private Node buildMeta(Invoice inv) {

        BorderPane root = new BorderPane();
        root.setMinWidth(CONTENT_WIDTH);
        root.setMaxWidth(CONTENT_WIDTH);
        root.setStyle(PrintStyle.BORDER_BOTTOM);


// content wrapper with padding
        VBox content = new VBox();

        BorderPane.setAlignment(content, Pos.CENTER);
        content.setMaxWidth(Double.MAX_VALUE);
        root.setCenter(content);



        GridPane g = new GridPane();
        g.setVgap(6);

        g.getColumnConstraints().addAll(
                fixed(LABEL_COL),
                fixed(1),
                fixed(VALUE_COL),
                fixed(LABEL_COL),
                fixed(1),
                fixed(VALUE_COL)
        );

        // vertical dividers (FULL HEIGHT)
        Region v1 = vLine();
        Region v2 = vLine();
        GridPane.setRowSpan(v1, GridPane.REMAINING);
        GridPane.setRowSpan(v2, GridPane.REMAINING);
        g.add(v1, 1, 0);
        g.add(v2, 4, 0);

        int r = 0;

        addMetaRow(g, r++, "P.O. No", inv.getPoNo(), "P.O. Date", df(inv.getPoDate()));
        addMetaRow(g, r++, "D.C. No", inv.getDcNo(), "D.C. Date", df(inv.getDcDate()));
        addMetaRow(g, r++, "Dispatch", inv.getDispatchThrough(), "E-Way Bill", inv.getEwayBillNo());

        content.getChildren().add(g);
        return root;
    }


    private void addMetaRow(GridPane g, int r,
                            String l1, String v1,
                            String l2, String v2) {

        Label ll1 = bold(l1, 10);
        ll1.setPadding(LABEL_PAD);

        Label vv1 = text(v1);
        vv1.setPadding(VALUE_PAD);

        Label ll2 = bold(l2, 10);
        ll2.setPadding(LABEL_PAD);

        Label vv2 = text(v2);
        vv2.setPadding(VALUE_PAD);

        g.add(ll1, 0, r);
        g.add(vv1, 2, r);
        g.add(ll2, 3, r);
        g.add(vv2, 5, r);
    }


    private Node buildBuyer(Invoice inv) {
        return partyBlock("DETAIL OF BUYER / BILL TO",
                inv.getBuyerName(),
                inv.getBuyerAddress(),
                inv.getBuyerGst(),
                inv.getBuyerState(),
                inv.getBuyerStateCode());
    }

    private Node buildConsignee(Invoice inv) {
        return partyBlock("DETAIL OF CONSIGNEE / SHIP TO",
                inv.getConsigneeName(),
                inv.getConsigneeAddress(),
                inv.getConsigneeGst(),
                inv.getConsigneeState(),
                inv.getConsigneeStateCode());
    }

    private Node partyBlock(String title,
                            String name,
                            String address,
                            String gst,
                            String state,
                            String code) {

        BorderPane root = new BorderPane();
        root.setPrefWidth(HALF);
        root.setMaxWidth(HALF);


//        root.setStyle(PrintStyle.BORDER_BOTTOM);

        // ===== TITLE =====
        VBox titleBox = new VBox();
        titleBox.setMaxWidth(Double.MAX_VALUE);
        titleBox.setFillWidth(true);

        Label t = bold(title, 11);
        t.setPadding(new Insets(2, 6, 2, 4));
        Region h = hLineHalf();
        h.setMaxWidth(CONTENT_WIDTH);

        titleBox.getChildren().addAll(t, h);

        titleBox.setPadding(new Insets(0));
        root.setTop(titleBox);

        // ===== CONTENT =====
        GridPane g = new GridPane();
        g.setPadding(new Insets(0));
        g.setVgap(6);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        valueCol.setMaxWidth(Double.MAX_VALUE);


        g.getColumnConstraints().addAll(
                fixed(HALF * 0.30),  // label
                fixed(1),            // vertical divider
                valueCol   // value
        );

        // SINGLE full-height vertical divider
        Region v = vLine();
        GridPane.setRowSpan(v, GridPane.REMAINING);
        g.add(v, 1, 0);

        int r = 0;

        addPartyRow(g, r++, "Name", name);
        addPartyRow(g, r++, "Address", address, true);
        addPartyRow(g, r++, "GST No", gst);
        addPartyRow(g, r++, "State", state);
        addPartyRow(g, r++, "Code", code);

        root.setCenter(g);
        return root;
    }


    private void addPartyRow(GridPane g, int r, String label, String value, boolean wrap) {

        Label l = bold(label, 10);
        l.setPadding(new Insets(2, 6, 2, 4));
        l.setAlignment(Pos.TOP_LEFT);   // 🔑 TOP align label

        Label v = text(value);
        v.setPadding(new Insets(2, 4, 2, 8));
        v.setAlignment(Pos.TOP_LEFT);   // 🔑 TOP align value

        if (wrap) {
            v.setWrapText(true);
            v.setTextOverrun(OverrunStyle.CLIP);
            v.setMaxWidth(Double.MAX_VALUE);
            v.setMinHeight(Region.USE_PREF_SIZE);   // 🔥 THIS IS THE KEY LINE
        }

        g.add(l, 0, r);
        g.add(v, 2, r);
    }


    private void addPartyRow(GridPane g, int r, String label, String value) {
        addPartyRow(g, r, label, value, false);
    }


    /* ================= TABLE ================= */

    private Node buildTable(List<InvoiceItem> items) {

        // ---- GRID (only real rows) ----
        GridPane grid = new GridPane();
        grid.setMinWidth(CONTENT_WIDTH);
        grid.setMaxWidth(CONTENT_WIDTH);
        grid.getColumnConstraints().addAll(cols());

        // FIX ROW HEIGHTS (HEADER + BODY)
        for (int i = 0; i <= items.size(); i++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(ROW_HEIGHT);
            rc.setPrefHeight(ROW_HEIGHT);
            rc.setMaxHeight(ROW_HEIGHT);
            grid.getRowConstraints().add(rc);
        }


        // ---------- HEADER ----------
        addHeaderCell(grid, "SL", 0, false);
        addHeaderCell(grid, "DESCRIPTION", 1, false);
        addHeaderCell(grid, "HSN", 2, false);
        addHeaderCell(grid, "QTY", 3, false);
        addHeaderCell(grid, "UNIT", 4, false);
        addHeaderCell(grid, "RATE", 5, false);
        addHeaderCell(grid, "GST %", 6, false);
        addHeaderCell(grid, "AMOUNT", 7, true);

        int row = 1;
        int sl = 1;

        // ---------- ACTUAL ITEM ROWS ONLY ----------
        for (InvoiceItem it : items) {
            addBodyCell(grid, String.valueOf(sl++), row, 0, Pos.CENTER_LEFT, false);
            addBodyCell(grid, it.getItemName(), row, 1, Pos.CENTER_LEFT, false);
            addBodyCell(grid, it.getHsn(), row, 2, Pos.CENTER_LEFT, false);
            addBodyCell(grid, fmt(it.getQty()), row, 3, Pos.CENTER_LEFT, false);
            addBodyCell(grid, it.getUnit(), row, 4, Pos.CENTER_LEFT, false);
            addBodyCell(grid, fmt(it.getRate()), row, 5, Pos.CENTER_LEFT, false);
            addBodyCell(grid, fmt(it.getGstPercent()), row, 6, Pos.CENTER_LEFT, false);
            addBodyCell(grid, fmt(it.getAmount()), row, 7, Pos.CENTER_LEFT, true);
            row++;
        }

        // ---- FIXED TABLE HEIGHT (VISUAL) ----
        double fixedTableHeight = (1 + TABLE_ROWS) * ROW_HEIGHT;

// Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

// Bottom border
        Region bottomBorder = new Region();
        bottomBorder.setPrefHeight(1);
        bottomBorder.setMinHeight(1);
        bottomBorder.setMaxHeight(1);
        bottomBorder.setPrefWidth(CONTENT_WIDTH);
        bottomBorder.setStyle("-fx-background-color:black;");

// Vertical lines overlay
        Pane columnLines = buildVerticalColumnLines(fixedTableHeight);

        StackPane tableStack = new StackPane();
        tableStack.setMinHeight(fixedTableHeight);
        tableStack.setPrefHeight(fixedTableHeight);
        tableStack.setMaxHeight(fixedTableHeight);

        VBox content = new VBox(grid);
        content.setMinHeight(fixedTableHeight);
        content.setPrefHeight(fixedTableHeight);
        content.setMaxHeight(fixedTableHeight);


        tableStack.getChildren().addAll(
                content,      // rows + empty space
                columnLines   // full-height verticals
        );

        VBox wrapper = new VBox(tableStack, bottomBorder);
        return wrapper;

    }



    private void addHeaderCell(GridPane g, String text, int col, boolean last) {
        Label l = new Label(text);
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(4, 8, 4, 8));

        l.setMinHeight(ROW_HEIGHT);
        l.setPrefHeight(ROW_HEIGHT);
        l.setMaxHeight(ROW_HEIGHT);

        l.setStyle(last ? HEADER_CELL_LAST : HEADER_CELL);
        g.add(l, col, 0);
    }


    private void addBodyCell(GridPane g, String text, int row, int col, Pos align, boolean last) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", 12));
        l.setAlignment(align);
        l.setPadding(new Insets(4, 8, 4, 8));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle(last ? CELL_LAST : CELL);
        g.add(l, col, row);
    }




    /* ================= TOTALS ================= */

    private Node buildTotals(Invoice inv) {

        GridPane root = new GridPane();
        root.setMinWidth(CONTENT_WIDTH);
        root.setMaxWidth(CONTENT_WIDTH);

        // Top border only (table → totals separation)
//        root.setStyle("-fx-border-color:black; -fx-border-width:1 0 0 0;");
        root.setPadding(new Insets(8, 0, 0, 0)); // top spacing only

        root.getColumnConstraints().addAll(
                fixed(CONTENT_WIDTH * 0.55),
                fixed(CONTENT_WIDTH * 0.45)
        );

        /* ================= LEFT SIDE ================= */

        // ----- Amount in words -----
        Label words = bold(
                "Amount in Words: " + AmountInWords.rupees(inv.getGrandTotal()),
                11
        );
        words.setWrapText(true);

        VBox amountBox = new VBox(words);
        amountBox.setPadding(new Insets(6, 8, 6, 8));

// ----- Horizontal divider -----
        Region divider = hLineLeftTotals();

// ----- BANK DETAILS -----
        Node bankDetails = buildBankDetails();

// ----- FLEX SPACER (THIS IS THE KEY) -----
//        Region spacer = new Region();
//        VBox.setVgrow(spacer, Priority.ALWAYS);

// ----- LEFT COLUMN -----
        BorderPane left = new BorderPane();

        left.setTop(amountBox);

        VBox bottom = new VBox(divider, bankDetails);
        left.setBottom(bottom);

//        left.setPadding(new Insets(0, 6, 0, 6));


        /* ================= RIGHT SIDE ================= */

        VBox totalsBox = new VBox(buildTotalsRight(inv));
        totalsBox.setStyle("-fx-border-color:black; -fx-border-width:1;");
        totalsBox.setPadding(new Insets(8, 10, 8, 12));

        VBox rightWrapper = new VBox(totalsBox);
        rightWrapper.setPadding(new Insets(0, 9, 6, 0));

        /* ================= ASSEMBLE ================= */

        root.add(left, 0, 0);
        root.add(rightWrapper, 1, 0);

        return root;
    }


    private Node buildBankDetails() {

        // ---- LABEL COLUMN ----
        VBox labelBox = new VBox(6,
                bold("Bank", 11),
                bold("A/C No", 11),
                bold("IFSC", 11)
        );
        labelBox.setPadding(new Insets(6, 10, 6, 8));


        // ---- VALUE COLUMN ----
        VBox valueBox = new VBox(6,
                headerText(seller.bank),
                headerText(seller.account),
                headerText(seller.ifsc)
        );

        valueBox.setPadding(new Insets(6, 8, 6, 10));

        // ---- VERTICAL DIVIDER ----
        Region vLine = vLine();

        // ---- ASSEMBLE ----
        HBox bankBox = new HBox(
                labelBox,
                vLine,
                valueBox
        );
        bankBox.setAlignment(Pos.TOP_LEFT);

        return bankBox;
    }





    private Node buildTotalsRight(Invoice inv) {

        GridPane g = new GridPane();
        g.setHgap(100);
        g.setVgap(4);

        addTotal(g, 0, "Taxable Subtotal", inv.getTaxableAmount(), true);
        addTotal(g, 1, "CGST", inv.getCgstTotal(), true);
        addTotal(g, 2, "SGST", inv.getSgstTotal(), true);
        addTotal(g, 3, "IGST", inv.getIgstTotal(), true);
        addTotal(g, 4, "Round Off", inv.getRoundOff(), true);
        addTotal(g, 5, "Grand Total", inv.getGrandTotal(), true);

        return g;
    }

    /* ================= FOOTER ================= */

    private Node buildFooter() {

        GridPane g = new GridPane();
        g.setMinWidth(CONTENT_WIDTH);
        g.setMaxWidth(CONTENT_WIDTH);
        g.setStyle("-fx-border-color:black; -fx-border-width:1 0 0 0;");


        g.getColumnConstraints().addAll(
                fixed(CONTENT_WIDTH * 0.5),
                fixed(1),
                fixed(CONTENT_WIDTH * 0.5)
        );

        Region div = vLine();
        g.add(div, 1, 0);

        Label signature = bold("Receiver's Signature", 11);
        signature.setMaxWidth(Double.MAX_VALUE);
        signature.setAlignment(Pos.CENTER_RIGHT);

        VBox left = new VBox(6,
                text("Certified that the particulars given above are true and correct."),
                text("Goods once supplied will not be taken back."),
                text("Subject to Visakhapatnam jurisdiction."),
                new Region(),
                signature
        );
        left.setPadding(new Insets(10));

        VBox right = new VBox(53,
                bold("For " + seller.name.toUpperCase(), 15),
                bold("Authorised Signatory", 11)
        );
        right.setAlignment(Pos.TOP_RIGHT);
        right.setPadding(new Insets(10));

        g.add(left, 0, 0);
        g.add(right, 2, 0);

        return g;
    }

    /* ================= HELPERS ================= */

    private ColumnConstraints fixed(double w) {
        ColumnConstraints c = new ColumnConstraints();
        c.setMinWidth(w);
        c.setMaxWidth(w);
        return c;
    }

    private List<ColumnConstraints> cols() {
        return List.of(
                fixed(COL_SL),
                fixed(COL_DESC),
                fixed(COL_HSN),
                fixed(COL_QTY),
                fixed(COL_UNIT),
                fixed(COL_RATE),
                fixed(COL_GST),
                fixed(COL_AMT)
        );
    }

    private void addTotal(GridPane g, int r, String l, double v) {
        addTotal(g, r, l, v, false);
    }

    private void addTotal(GridPane g, int r, String l, double v, boolean bold) {
        Label L = new Label(l + " :");
        Label V = new Label(fmt(v));
        if (bold) {
            L.setStyle("-fx-font-weight:bold;");
            V.setStyle("-fx-font-weight:bold;");
        }
        g.add(L, 0, r);
        g.add(V, 1, r);
    }

    private Node buildPageNo(int p, int t) {
        Label l = new Label("Page " + p + " of " + t);
        l.setFont(Font.font(9));
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Node compterLabel() {
        Label l = new Label("This is a Computer Generated Invoice" );
        l.setFont(Font.font(11));
        l.setPadding(new Insets(8));
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Region vLine() {
        Region r = new Region();
        r.setPrefWidth(1);
        r.setMinWidth(1);
        r.setMaxWidth(1);
        r.setStyle("-fx-background-color:black;");
        return r;
    }


    // Divider for HALF-width blocks (Buyer / Consignee titles)
    private Region hLineHalf() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMinHeight(1);
        r.setMaxHeight(1);

        r.setPrefWidth(HALF);
        r.setMinWidth(HALF);
        r.setMaxWidth(HALF);

        r.setStyle("-fx-background-color:black;");
        return r;
    }

    // Divider for LEFT totals column ONLY
    private Region hLineLeftTotals() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMinHeight(1);
        r.setMaxHeight(1);

        double leftWidth = CONTENT_WIDTH * 0.55;

        r.setPrefWidth(leftWidth);
        r.setMinWidth(leftWidth);
        r.setMaxWidth(leftWidth);

        r.setStyle("-fx-background-color:black;");
        return r;
    }

    private Pane buildVerticalColumnLines(double height) {

        Pane overlay = new Pane();
        overlay.setMinWidth(CONTENT_WIDTH);
        overlay.setPrefWidth(CONTENT_WIDTH);
        overlay.setMaxWidth(CONTENT_WIDTH);

        overlay.setMinHeight(height);
        overlay.setPrefHeight(height);
        overlay.setMaxHeight(height);

        double x = 0;
        double[] cols = {
                COL_SL,
                COL_DESC,
                COL_HSN,
                COL_QTY,
                COL_UNIT,
                COL_RATE,
                COL_GST
        };

        for (double w : cols) {
            x += w;
            Region line = new Region();
            line.setLayoutX(x);
            line.setLayoutY(0);
            line.setPrefWidth(1);
            line.setMinWidth(1);
            line.setMaxWidth(1);
            line.setPrefHeight(height);
            line.setStyle("-fx-background-color:black;");
            overlay.getChildren().add(line);
        }

        return overlay;
    }





    private Label bold(String t, int s) {
        Label l = new Label(t);
        l.setFont(Font.font(s));
        l.setStyle("-fx-font-weight:bold;");
        return l;
    }

    private Label headerText(String t) {
        Label l = new Label(safe(t));
        l.setFont(Font.font("Arial", 12));
        return l;
    }

    private Label text(String t) {
        Label l = new Label(safe(t));
        l.setFont(FONT);
        return l;
    }

    private String df(LocalDate d) {
        return d == null ? "" : d.format(DF);
    }

    private String fmt(double d) {
        return String.format("%.2f", d);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
