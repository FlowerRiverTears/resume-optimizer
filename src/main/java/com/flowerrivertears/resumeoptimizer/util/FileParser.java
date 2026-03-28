package com.flowerrivertears.resumeoptimizer.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class FileParser {

    private static final Logger log = LoggerFactory.getLogger(FileParser.class);

    public String parsePdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            StringBuilder text = new StringBuilder();
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                text.append(stripper.getText(document)).append("\n");
            }

            return text.toString();
        }
    }

    public String parseDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    public String parseTxt(MultipartFile file) throws IOException {
        return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    public String parseFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IOException("文件名不能为空");
        }

        log.info("========== 收到文件 ==========");
        log.info("文件名: {}", filename);
        log.info("文件大小: {} bytes", file.getSize());
        log.info("============================");

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        String content = switch (extension) {
            case "pdf" -> parsePdf(file);
            case "docx", "doc" -> parseDocx(file);
            case "txt" -> parseTxt(file);
            default -> throw new IOException("不支持的文件格式: " + extension);
        };

        log.info("解析完成，字符数: {}", content.length());
        return content;
    }
}
