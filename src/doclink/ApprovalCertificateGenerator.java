package doclink;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import doclink.models.Plan;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.itextpdf.text.DocumentException; // Explicitly import DocumentException
import java.io.File;

public class ApprovalCertificateGenerator {

    public static String generateCertificate(Plan plan, String filePath) {
        Document document = new Document();
        try {
            File dir = new File(System.getProperty("user.home") + "/DocLink_Documents/");
            if (!dir.exists()) {
                dir.mkdirs(); // Create the directory if it doesn't exist
            }
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Building Plan Approval Certificate", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            // Department Info
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.BLUE);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK);

            document.add(new Paragraph("Department of Lands, Housing & Physical Planning", headerFont));
            document.add(new Paragraph("DocLink - Building Plan Approval Workflow System", normalFont));
            document.add(new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), normalFont));
            document.add(Chunk.NEWLINE);

            // Plan Details
            document.add(new Paragraph("Plan Details:", headerFont));
            document.add(new Paragraph("Reference No: " + plan.getReferenceNo(), normalFont));
            document.add(new Paragraph("Applicant Name: " + plan.getApplicantName(), normalFont));
            document.add(new Paragraph("Plot No: " + plan.getPlotNo(), normalFont));
            document.add(new Paragraph("Location: " + plan.getLocation(), normalFont));
            document.add(new Paragraph("Date Submitted: " + plan.getDateSubmitted().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), normalFont));
            document.add(Chunk.NEWLINE);

            // Approval Status
            document.add(new Paragraph("Approval Status:", headerFont));
            document.add(new Paragraph("Status: " + plan.getStatus(), normalFont));
            document.add(new Paragraph("Remarks: " + (plan.getRemarks() != null ? plan.getRemarks() : "N/A"), normalFont));
            document.add(Chunk.NEWLINE);

            // Signature
            document.add(new Paragraph("_________________________", normalFont));
            document.add(new Paragraph("Director's Signature", normalFont));
            document.add(new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), normalFont));

            document.close();
            System.out.println("Approval Certificate generated successfully at: " + filePath);
            return filePath;

        } catch (DocumentException | IOException e) {
            System.err.println("Error generating PDF certificate: " + e.getMessage());
            return null;
        }
    }
}