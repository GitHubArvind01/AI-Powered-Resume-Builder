package com.resumeai.aiservice.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.resumeai.aiservice.service.ResumeTextExtractionService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResumeTextExtractionServiceImpl implements ResumeTextExtractionService {

	private static final List<String> SUPPORTED_EXTENSIONS = List.of(".pdf", ".doc", ".docx");
	private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

	@Override
	public String extractText(MultipartFile file) {
		validateFile(file);

		String extension = detectExtension(file.getOriginalFilename());

		try {
			return switch (extension) {
				case ".pdf" -> extractPdfText(file);
				case ".docx" -> extractDocxText(file);
				case ".doc" -> extractDocText(file);
				default -> throw new IllegalArgumentException("Unsupported resume format. Please upload PDF, DOC, or DOCX.");
			};
		} catch (IOException exception) {
			log.error("Failed to extract resume text from {}", file.getOriginalFilename(), exception);
			throw new IllegalArgumentException("We couldn't extract readable text from this file. Please try another file.");
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Please choose a resume file before starting ATS analysis.");
		}

		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new IllegalArgumentException("Resume files must be 10 MB or smaller.");
		}

		String extension = detectExtension(file.getOriginalFilename());
		if (!SUPPORTED_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException("Unsupported resume format. Please upload PDF, DOC, or DOCX.");
		}
	}

	private String extractPdfText(MultipartFile file) throws IOException {
		try (PDDocument document = Loader.loadPDF(file.getBytes())) {
			String extracted = normalize(new PDFTextStripper().getText(document));
			if (!extracted.isBlank()) {
				return extracted;
			}

			log.info("No embedded PDF text found for {}. Attempting OCR fallback.", file.getOriginalFilename());
			String ocrText = normalize(runPdfOcr(document));
			if (!ocrText.isBlank()) {
				return ocrText;
			}
		}

		throw new IllegalArgumentException("We couldn't extract readable text from this PDF. If it is a scanned file, please upload a text-based PDF/DOCX or install Tesseract OCR on the server.");
	}

	private String extractDocxText(MultipartFile file) throws IOException {
		try (InputStream inputStream = file.getInputStream();
				XWPFDocument document = new XWPFDocument(inputStream);
				XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
			String extracted = normalize(extractor.getText());
			if (!extracted.isBlank()) {
				return extracted;
			}
		}

		throw new IllegalArgumentException("We couldn't extract readable text from this DOCX file.");
	}

	private String extractDocText(MultipartFile file) throws IOException {
		try (InputStream inputStream = file.getInputStream();
				HWPFDocument document = new HWPFDocument(inputStream);
				WordExtractor extractor = new WordExtractor(document)) {
			String extracted = normalize(extractor.getText());
			if (!extracted.isBlank()) {
				return extracted;
			}
		}

		throw new IllegalArgumentException("We couldn't extract readable text from this DOC file.");
	}

	private String runPdfOcr(PDDocument document) throws IOException {
		if (!isTesseractInstalled()) {
			log.warn("Tesseract OCR is not installed or not available on PATH.");
			return "";
		}

		PDFRenderer renderer = new PDFRenderer(document);
		StringBuilder combinedText = new StringBuilder();

		for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
			BufferedImage image = renderer.renderImageWithDPI(pageIndex, 200, ImageType.RGB);
			String pageText = runTesseractOnImage(image);
			if (!pageText.isBlank()) {
				combinedText.append(pageText).append(System.lineSeparator());
			}
		}

		return combinedText.toString();
	}

	private String runTesseractOnImage(BufferedImage image) throws IOException {
		Path tempImage = Files.createTempFile("resume-ocr-", ".png");
		Path tempOutputBase = Files.createTempFile("resume-ocr-output-", "");
		try {
			ImageIO.write(image, "png", tempImage.toFile());

			ProcessBuilder processBuilder = new ProcessBuilder(
					"tesseract",
					tempImage.toString(),
					tempOutputBase.toString(),
					"-l",
					"eng",
					"--psm",
					"6");
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			int exitCode = waitFor(process);

			if (exitCode != 0) {
				String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				log.warn("Tesseract OCR failed with code {}: {}", exitCode, output);
				return "";
			}

			Path outputFile = Path.of(tempOutputBase + ".txt");
			if (!Files.exists(outputFile)) {
				return "";
			}

			return Files.readString(outputFile, StandardCharsets.UTF_8);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return "";
		} finally {
			Files.deleteIfExists(tempImage);
			Files.deleteIfExists(Path.of(tempOutputBase + ".txt"));
			Files.deleteIfExists(tempOutputBase);
		}
	}

	private boolean isTesseractInstalled() {
		try {
			Process process = new ProcessBuilder("tesseract", "--version").redirectErrorStream(true).start();
			return waitFor(process) == 0;
		} catch (Exception exception) {
			return false;
		}
	}

	private int waitFor(Process process) throws InterruptedException {
		return process.waitFor();
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}

		return value.replace('\u0000', ' ')
				.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
				.replaceAll("\\n{3,}", "\n\n")
				.trim();
	}

	private String detectExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}

		return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
	}
}
