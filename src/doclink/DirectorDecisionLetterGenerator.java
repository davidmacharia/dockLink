package doclink;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import doclink.models.Plan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DirectorDecisionLetterGenerator {

    public static String generateLetter(Plan plan, String decisionType, String remarks) {
        Document document = new Document();
        String documentsDir = System.getProperty("user.home") + "/DocLink_Documents/";
        File dir = new File(documentsDir);
        if (!dir.exists()) {
            dir.mkdirs(); // Create the directory if it doesn't exist
        }
        String fileName = plan.getReferenceNo() != null ? plan.getReferenceNo() : "Plan_" + plan.getId();
        String filePath = documentsDir + fileName + "_Director_" + decisionType.replace(" ", "") + "_Letter.pdf";

        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Director's Decision Letter", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(25);
            document.add(title);

            // Department Info
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLUE);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK);

            document.add(new Paragraph("Department of Lands, Housing & Physical Planning", headerFont));
            document.add(new Paragraph("DocLink - Building Plan Approval Workflow System", normalFont));
            document.add(new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), normalFont));
            document.add(Chunk.NEWLINE);

            // Plan Details
            document.add(new Paragraph("Regarding Plan Application:", headerFont));
            document.add(new Paragraph("Reference No: " + (plan.getReferenceNo() != null ? plan.getReferenceNo() : "N/A"), normalFont));
            document.add(new Paragraph("Applicant Name: " + plan.getApplicantName(), normalFont));
            document.add(new Paragraph("Plot No: " + plan.getPlotNo(), normalFont));
            document.add(new Paragraph("Location: " + plan.getLocation(), normalFont));
            document.add(new Paragraph("Date Submitted: " + plan.getDateSubmitted().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), normalFont));
            document.add(Chunk.NEWLINE);

            // Decision
            document.add(new Paragraph("Decision:", headerFont));
            document.add(new Paragraph("The Director's decision for this plan is: " + decisionType, normalFont));
            document.add(new Paragraph("Current Status: " + plan.getStatus(), normalFont));
            document.add(new Paragraph("Remarks: " + (remarks != null && !remarks.isEmpty() ? remarks : "No specific remarks provided."), normalFont));
            document.add(Chunk.NEWLINE);

            // Closing
            document.add(new Paragraph("This decision is made in accordance with the relevant building regulations and departmental policies.", normalFont));
            document.add(Chunk.NEWLINE);

            // Signature
            document.add(new Paragraph("_________________________", normalFont));
            document.add(new Paragraph("Director's Signature", normalFont));
            document.add(new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), normalFont));

            document.close();
            System.out.println("Director's Decision Letter generated successfully at: " + filePath);
            return filePath;

        } catch (DocumentException | IOException e) {
            System.err.println("Error generating PDF Director's Decision Letter: " + e.getMessage());
            return null;
        }
    }
}