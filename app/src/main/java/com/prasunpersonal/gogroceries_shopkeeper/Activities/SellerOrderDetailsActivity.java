package com.prasunpersonal.gogroceries_shopkeeper.Activities;

import static com.prasunpersonal.gogroceries_shopkeeper.App.MY_SHOP;
import static com.prasunpersonal.gogroceries_shopkeeper.Models.ModelOrder.ORDER_PACKED;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.prasunpersonal.gogroceries_shopkeeper.Adaptrers.AdapterOrderDetails;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelCartItem;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelCustomer;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelDeliveryGuy;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelOrder;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelProduct;
import com.prasunpersonal.gogroceries_shopkeeper.R;
import com.prasunpersonal.gogroceries_shopkeeper.databinding.ActivitySellerOrderDetailsBinding;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class SellerOrderDetailsActivity extends AppCompatActivity {
    ActivitySellerOrderDetailsBinding binding;
    private ModelOrder order;
    private ModelDeliveryGuy deliveryGuy;
    private ModelCustomer customer;
    private String orderID;
    double subTotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.orderDetailsToolbar);

        orderID = getIntent().getStringExtra("ORDER_ID");

        binding.orderDetailsToolbar.setNavigationOnClickListener(v -> finish());

        binding.assignDeliveryGuy.setOnClickListener(v -> {
            binding.assignDeliveryGuy.setEnabled(false);
            FirebaseFirestore.getInstance().collection("DeliveryGuys").whereEqualTo("available", true).orderBy("lastOrderAssigned").limit(1).get().addOnCompleteListener(task2 -> {
                if (task2.isSuccessful()) {
                    if (task2.getResult().isEmpty()) {
                        Toast.makeText(this, "No delivery guy is available now!", Toast.LENGTH_SHORT).show();
                    } else {
                        ModelDeliveryGuy tmpDeliveryGuy = task2.getResult().getDocuments().get(0).toObject(ModelDeliveryGuy.class);
                        assert tmpDeliveryGuy != null;

                        FirebaseFirestore.getInstance().collection("Orders").document(orderID).update("orderStatus", ORDER_PACKED);
                        FirebaseFirestore.getInstance().collection("Orders").document(orderID).update("deliveryGuyId", tmpDeliveryGuy.getUid());
                        FirebaseFirestore.getInstance().collection("DeliveryGuys").document(tmpDeliveryGuy.getUid()).update("lastOrderAssigned", System.currentTimeMillis());
                    }
                } else {
                    Toast.makeText(this, Objects.requireNonNull(task2.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
                binding.assignDeliveryGuy.setEnabled(true);
            });
        });

        binding.generateInvoice.setOnClickListener(v -> {
            if (order != null && customer != null) {
                binding.generateInvoice.setEnabled(false);
                try {
                    File pdf = generateInvoice(order, customer);
                    StorageReference reference = FirebaseStorage.getInstance().getReference(getString(R.string.app_name)).child(MY_SHOP.getUid()).child(order.getOrderId());
                    reference.putFile(Uri.fromFile(pdf)).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            reference.getDownloadUrl().addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    FirebaseFirestore.getInstance().collection("Orders").document(order.getOrderId()).update("invoice", task.getResult().toString()).addOnCompleteListener(task2 -> {
                                        if (task2.isSuccessful()) {
                                            Toast.makeText(this, "Invoice uploaded successfully.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(this, Objects.requireNonNull(task2.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        binding.generateInvoice.setEnabled(true);
                                    });
                                } else {
                                    Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                    binding.generateInvoice.setEnabled(true);
                                }
                            });
                        } else {
                            Toast.makeText(this, Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            binding.generateInvoice.setEnabled(true);
                        }
                    });
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    binding.generateInvoice.setEnabled(true);
                }
            } else {
                Toast.makeText(this, "Something went wrong! Please try again", Toast.LENGTH_SHORT).show();
            }
        });

        binding.callDeliveryGuy.setOnClickListener(v -> dialPhone());

        FirebaseFirestore.getInstance().collection("Orders").document(orderID).addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null && value.exists()) {
                order = value.toObject(ModelOrder.class);
                assert order != null;

                FirebaseFirestore.getInstance().collection("Customers").document(order.getCustomerId()).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() &&  task.getResult().exists()) {
                        customer = task.getResult().toObject(ModelCustomer.class);
                        binding.orderTitle.setText(String.format(Locale.getDefault(), "%d product(s) ordered by %s", order.getCartItems().size(), customer.getName()));
                    } else {
                        Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                if (order.getDeliveryGuyId() == null) {
                    binding.deliveryGuyArea.setVisibility(View.GONE);
                } else {
                    binding.deliveryGuyArea.setVisibility(View.VISIBLE);
                    FirebaseFirestore.getInstance().collection("DeliveryGuys").document(order.getDeliveryGuyId()).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() &&  task.getResult().exists()) {
                            deliveryGuy = task.getResult().toObject(ModelDeliveryGuy.class);
                            assert deliveryGuy != null;
                            Glide.with(this).load(deliveryGuy.getDp()).placeholder(R.drawable.ic_person).into(binding.deliveryGuyDp);
                            binding.deliveryGuyName.setText(deliveryGuy.getName());
                            binding.deliveryGuyPhone.setText(deliveryGuy.getPhone());
                        } else {
                            Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                if (order.getInvoice() == null) {
                    binding.generateInvoice.setVisibility(View.VISIBLE);
                } else {
                    binding.generateInvoice.setVisibility(View.GONE);
                    if (order.getOrderStatus() < ORDER_PACKED) {
                        binding.assignDeliveryGuy.setVisibility(View.VISIBLE);
                    } else {
                        binding.assignDeliveryGuy.setVisibility(View.GONE);
                    }
                }

                MaterialCardView[] points = {binding.orderPlacedPoint, binding.orderPackedPoint, binding.outForDeliveryPoint, binding.moneyReceivedPoint, binding.deliveredPoint};
                View[] lines = {binding.connectionLine0, binding.connectionLine1, binding.connectionLine2, binding.connectionLine3};
                for (int i=0; i < points.length; i++){
                    if (i < order.getOrderStatus()){
                        points[i].setCardBackgroundColor(Color.GREEN);
                        if (i < lines.length) lines[i].setBackgroundColor(Color.GREEN);
                    } else if (i == order.getOrderStatus()) {
                        points[i].setCardBackgroundColor(Color.GREEN);
                        if (i < lines.length) lines[i].setBackgroundColor(Color.LTGRAY);
                    } else {
                        points[i].setCardBackgroundColor(Color.LTGRAY);
                        if (i < lines.length) lines[i].setBackgroundColor(Color.LTGRAY);
                    }
                }

                binding.orderTime.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(order.getOrderTime())));
                binding.orderID.setText(order.getOrderId());
                binding.orderedItems.setLayoutManager(new LinearLayoutManager(this));
                binding.orderedItems.setAdapter(new AdapterOrderDetails(this, order.getCartItems()));
                for (ModelCartItem item : order.getCartItems()) {
                    subTotal += (item.getProduct().getOriginalPrice() - (item.getProduct().getOriginalPrice() * item.getProduct().getDiscountPercentage() / 100)) * item.getQuantity() * item.getProduct().getUnitMap().get(item.getUnit());
                }
                binding.orderDetailsSubTotal.setText(String.format(Locale.getDefault(), "%.02f", subTotal));
                binding.orderDetailsDeliveryCharge.setText(String.format(Locale.getDefault(), "%.02f", order.getDeliveryCharge()));
                binding.orderDetailsGrandTotal.setText(String.format(Locale.getDefault(), "%.02f", subTotal + order.getDeliveryCharge()));

                invalidateOptionsMenu();
            } else {
                Toast.makeText(this, "Sorry, the order may be canceled or not found!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_invoice, menu);
        menu.findItem(R.id.invoice).setVisible(order != null && order.getInvoice() != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.invoice) {
            startActivity(new Intent(this, PDFViewActivity.class).putExtra("PDF_URL", order.getInvoice()).putExtra("PDF_NAME", String.format(Locale.getDefault(), "%d.pdf", order.getOrderTime())));
        }
        return super.onOptionsItemSelected(item);
    }

    private void dialPhone() {
        if (!binding.deliveryGuyPhone.getText().toString().trim().isEmpty()) {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(binding.deliveryGuyPhone.getText().toString().trim()))));
        }
    }

    private File generateInvoice(@NonNull ModelOrder o, @NonNull ModelCustomer c) throws FileNotFoundException {
        int i=1;
        double total = 0;

        File file = new File(getDataDir(), "temp_invoice.pdf");
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);
        pdfDocument.setDefaultPageSize(PageSize.A4);
        document.setMargins(50,50,50,50);

        Point point = new Point();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getSize(point);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        QRGEncoder encoder = new QRGEncoder(o.getOrderId(), null, QRGContents.Type.TEXT, Math.min(point.x, point.y) * 3/4);
        encoder.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream);
        Image qrCodeImg = new Image(ImageDataFactory.create(stream.toByteArray())).setHeight(100f).setWidth(100f);

        // Adding Business Details & QR Code
        float[] headerTableSize = {385f, 120f};
        Table headerTable = new Table(headerTableSize);
        headerTable.addCell(new Cell().add(new Paragraph(MY_SHOP.getShopName()).setFontSize(20).setBold()).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell(4, 1).add(qrCodeImg.setAutoScale(true)).setVerticalAlignment(VerticalAlignment.MIDDLE).setHorizontalAlignment(HorizontalAlignment.CENTER).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(MY_SHOP.getOwnerName()).setFontSize(14)).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(MY_SHOP.getAddress()).setFontSize(12)).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "Contact: %s | %s", MY_SHOP.getOwnerPhone(), MY_SHOP.getOwnerEmail())).setFontSize(12)).setBorder(Border.NO_BORDER));


        // Adding Top Bar
        float[] topBarSize = {247.5f, 247.5f};
        Table topBar = new Table(topBarSize);
        topBar.setBorderTop(new SolidBorder(1));
        topBar.setBorderBottom(new SolidBorder(1));
        topBar.addCell(new Cell().add(new Paragraph("Invoice No.: " + String.format(Locale.getDefault(), "%d", System.currentTimeMillis())).setFontSize(12).setTextAlignment(TextAlignment.LEFT).setPaddingLeft(10)).setBorder(Border.NO_BORDER));
        topBar.addCell(new Cell().add(new Paragraph("Date: " + new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date())).setFontSize(12).setTextAlignment(TextAlignment.RIGHT).setPaddingRight(10)).setBorder(Border.NO_BORDER));


        // Adding Customer Details
        float[] detailsChartSize = {70f, 425f};
        Table detailsChart = new Table(detailsChartSize);
        detailsChart.addCell(new Cell().add(new Paragraph("Name:").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
        detailsChart.addCell(new Cell().add(new Paragraph(c.getName()).setFontSize(8)).setBorder(Border.NO_BORDER));
        detailsChart.addCell(new Cell().add(new Paragraph("Phone No.:").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
        detailsChart.addCell(new Cell().add(new Paragraph(c.getPhone()).setFontSize(8)).setBorder(Border.NO_BORDER));
        detailsChart.addCell(new Cell().add(new Paragraph("Email:").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
        detailsChart.addCell(new Cell().add(new Paragraph(c.getEmail()).setFontSize(8)).setBorder(Border.NO_BORDER));
        detailsChart.addCell(new Cell().add(new Paragraph("Full Address:").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
        detailsChart.addCell(new Cell().add(new Paragraph(o.getDeliveryAddress()).setFontSize(8)).setBorder(Border.NO_BORDER));

        // Adding Price Chart
        float[] priceChartSize = {30f, 125f, 75f, 50f, 75f, 75f, 65f};
        Table priceChart = new Table(priceChartSize);
        priceChart.setHorizontalAlignment(HorizontalAlignment.CENTER);
        priceChart.addCell(new Cell().add(new Paragraph("No.").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph("Name").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph("M.R.P. (Rs.)").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph("Discount").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph("Price (Rs.)").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph("Quantity").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph("Total (Rs.)").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        for (ModelCartItem item : o.getCartItems()){
            double tmp = (item.getProduct().getOriginalPrice() - (item.getProduct().getOriginalPrice() * item.getProduct().getDiscountPercentage() / 100)) * item.getQuantity() * item.getProduct().getUnitMap().get(item.getUnit());
            priceChart.addCell(new Cell().add(new Paragraph(String.valueOf(i)).setFontSize(8).setTextAlignment(TextAlignment.CENTER)).setVerticalAlignment(VerticalAlignment.MIDDLE));
            priceChart.addCell(new Cell().add(new Paragraph(item.getProduct().getProductName()).setFontSize(8)).setTextAlignment(TextAlignment.LEFT).setVerticalAlignment(VerticalAlignment.MIDDLE));
            priceChart.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.02f/%s", item.getProduct().getOriginalPrice(), new ArrayList<>(item.getProduct().getUnitMap().keySet()).get(0)))).setFontSize(8).setVerticalAlignment(VerticalAlignment.MIDDLE));
            priceChart.addCell(new Cell().add(new Paragraph((item.getProduct().getDiscountPercentage() == 0) ? "NA" : String.format(Locale.getDefault(), "%s%%", item.getProduct().getDiscountPercentage()))).setFontSize(8).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            priceChart.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.02f/%s", (item.getProduct().getOriginalPrice() - (item.getProduct().getOriginalPrice() * item.getProduct().getDiscountPercentage() / 100)), new ArrayList<>(item.getProduct().getUnitMap().keySet()).get(0))))).setFontSize(8).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            priceChart.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%s %s",(item.getProduct().getProductType() == ModelProduct.SINGLE_QUANTITY_PRODUCT) ? (int) item.getQuantity() : item.getQuantity(), item.getUnit()))).setFontSize(8).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            priceChart.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.02f", tmp)).setFontSize(8).setTextAlignment(TextAlignment.CENTER)).setVerticalAlignment(VerticalAlignment.MIDDLE));
            i++;
            total += tmp;
        }
        priceChart.addCell(new Cell(1,6).add(new Paragraph("Sub Total").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.02f", total)).setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell(1,6).add(new Paragraph("Delivery Charge").setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.02f", o.getDeliveryCharge())).setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell(1,6).add(new Paragraph("Grand Total").setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
        priceChart.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.02f", total+o.getDeliveryCharge())).setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER)));


        document.add(headerTable);
        document.add(topBar.setMarginBottom(15f));
        document.add(new Paragraph("Customer Details").setBold().setUnderline().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
        document.add(detailsChart.setMarginBottom(15f));
        document.add(new Paragraph("Price Chart").setBold().setUnderline().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
        document.add(priceChart);
        document.close();
        Toast.makeText(this, "Uploading Invoice.", Toast.LENGTH_SHORT).show();

        return file;
    }
}