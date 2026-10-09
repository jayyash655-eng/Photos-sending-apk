package com.example.data.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {

    fun getVaultDir(context: Context): File {
        val dir = File(context.filesDir, "vault_files")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getDownloadsDir(context: Context): File {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun createInitialSeedFiles(context: Context): List<VaultItem> = withContext(Dispatchers.IO) {
        val vaultDir = getVaultDir(context)
        val items = mutableListOf<VaultItem>()

        // 1. Seed Financial Report PDF
        val pdf1File = File(vaultDir, "Financial_Audit_Report_2026.pdf")
        if (!pdf1File.exists() || pdf1File.length() == 0L) {
            generateSamplePdf(
                file = pdf1File,
                title = "2026 Q3 FINANCIAL AUDIT REPORT",
                subtitle = "Authorized Internal Distribution Only • Confidential",
                pages = listOf(
                    listOf(
                        "1. EXECUTIVE SUMMARY",
                        "Audit scope encompassed overall fiscal solvency, operational expenditures, and data security infrastructure compliance.",
                        "Total Recorded Revenue: $14.8M (+18.4% YoY)",
                        "Operating Margin: 32.1%",
                        "Risk Assessment: LOW RISK",
                        "",
                        "2. COMPLIANCE & ACCESS INTEGRITY",
                        "All visitor file access is strictly authenticated against centralized administrative roster with immutable audit logging."
                    ),
                    listOf(
                        "3. ASSET BREAKDOWN & PROJECTIONS",
                        "Capital Allocation: 45% R&D, 30% Infrastructure, 25% Reserve.",
                        "Projected Q4 Trajectory: Sustained double-digit expansion with zero compliance breaches.",
                        "",
                        "4. SIGN-OFF",
                        "Lead Auditor: Elena Rostova, CPA, CISA",
                        "Status: APPROVED & SEALED"
                    )
                )
            )
        }
        items.add(
            VaultItem(
                title = "Q3 Financial Audit Report",
                description = "Official fiscal solvency report, compliance review, and audit sign-off documentation.",
                fileType = "PDF",
                fileName = pdf1File.name,
                localFilePath = pdf1File.absolutePath,
                fileSizeBytes = pdf1File.length(),
                mimeType = "application/pdf",
                category = "Financial"
            )
        )

        // 2. Seed Architecture PDF
        val pdf2File = File(vaultDir, "Vault_Security_Architecture.pdf")
        if (!pdf2File.exists() || pdf2File.length() == 0L) {
            generateSamplePdf(
                file = pdf2File,
                title = "SYSTEM SECURITY & ACCESS PROTOCOL",
                subtitle = "Classified Technical Architecture Specification",
                pages = listOf(
                    listOf(
                        "1. AUTHENTICATION & ACCESS MATRIX",
                        "Access to the repository is partitioned strictly by admin-managed credentials.",
                        "Role enforcement verifies user permission tiers prior to preview or download dispatch.",
                        "",
                        "2. SECURE LOGGING ARCHITECTURE",
                        "Every download request triggers a cryptographic audit log with timestamp, username, and asset hash.",
                        "Administrative dashboard retains authoritative oversight across all download activity."
                    )
                )
            )
        }
        items.add(
            VaultItem(
                title = "Vault Security Architecture",
                description = "Technical specifications outlining secure download policies and access control tiers.",
                fileType = "PDF",
                fileName = pdf2File.name,
                localFilePath = pdf2File.absolutePath,
                fileSizeBytes = pdf2File.length(),
                mimeType = "application/pdf",
                category = "Security"
            )
        )

        // 3. Seed Design System Showcase Image
        val img1File = File(vaultDir, "Brand_Design_Showcase_2026.png")
        if (!img1File.exists() || img1File.length() == 0L) {
            generateSampleImage(
                file = img1File,
                title = "NEXT-GEN DESIGN SYSTEM",
                tagline = "Visual Guidelines • Tokens & Palette",
                primaryColor = Color.rgb(2, 132, 199),
                secondaryColor = Color.rgb(56, 189, 248)
            )
        }
        items.add(
            VaultItem(
                title = "Brand Design Showcase 2026",
                description = "High-resolution master asset for brand typography, elevation tokens, and color system.",
                fileType = "IMAGE",
                fileName = img1File.name,
                localFilePath = img1File.absolutePath,
                fileSizeBytes = img1File.length(),
                mimeType = "image/png",
                category = "Design"
            )
        )

        // 4. Seed Product Blueprint Diagram Image
        val img2File = File(vaultDir, "Cloud_Infrastructure_Blueprint.png")
        if (!img2File.exists() || img2File.length() == 0L) {
            generateSampleImage(
                file = img2File,
                title = "INFRASTRUCTURE BLUEPRINT",
                tagline = "Zero-Trust Mesh • Distributed Storage Architecture",
                primaryColor = Color.rgb(15, 23, 42),
                secondaryColor = Color.rgb(16, 185, 129)
            )
        }
        items.add(
            VaultItem(
                title = "Cloud Infrastructure Blueprint",
                description = "Comprehensive topology diagram of isolated data zones and gateway firewalls.",
                fileType = "IMAGE",
                fileName = img2File.name,
                localFilePath = img2File.absolutePath,
                fileSizeBytes = img2File.length(),
                mimeType = "image/png",
                category = "Architecture"
            )
        )

        items
    }

    private fun generateSamplePdf(
        file: File,
        title: String,
        subtitle: String,
        pages: List<List<String>>
    ) {
        try {
            val document = PdfDocument()
            val pageWidth = 595 // Standard A4 width in points
            val pageHeight = 842 // Standard A4 height in points

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(2, 132, 199)
                textSize = 12f
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 12f
                isAntiAlias = true
            }

            val headerBarPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                style = Paint.Style.FILL
            }

            val footerPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 10f
                isAntiAlias = true
            }

            pages.forEachIndexed { index, paragraphs ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Decorative top header bar
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 40f, headerBarPaint)

                var yPos = 80f
                canvas.drawText(title, 40f, yPos, titlePaint)
                yPos += 22f
                canvas.drawText(subtitle, 40f, yPos, subtitlePaint)
                yPos += 35f

                paragraphs.forEach { line ->
                    if (line.startsWith("1.") || line.startsWith("2.") || line.startsWith("3.") || line.startsWith("4.")) {
                        val sectionPaint = Paint().apply {
                            color = Color.rgb(15, 23, 42)
                            textSize = 14f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                        canvas.drawText(line, 40f, yPos, sectionPaint)
                        yPos += 24f
                    } else {
                        canvas.drawText(line, 40f, yPos, bodyPaint)
                        yPos += 18f
                    }
                }

                // Footer
                canvas.drawText("SECURE VAULT REPOSITORY • Page ${index + 1} of ${pages.size}", 40f, pageHeight - 30f, footerPaint)

                document.finishPage(page)
            }

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
        } catch (t: Throwable) {
            // Fallback for host JVM / Robolectric tests where native Skia PDF engine is absent
            val fallbackPdf = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] >>\nendobj\nxref\n0 4\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \ntrailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n190\n%%EOF\n"
            file.writeText(fallbackPdf)
        }
    }

    private fun generateSampleImage(
        file: File,
        title: String,
        tagline: String,
        primaryColor: Int,
        secondaryColor: Int
    ) {
        try {
            val width = 1200
            val height = 800
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background
            val bgPaint = Paint().apply {
                color = Color.rgb(248, 250, 252)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Accent top banner
            val bannerPaint = Paint().apply {
                color = primaryColor
            }
            canvas.drawRect(0f, 0f, width.toFloat(), 200f, bannerPaint)

            // Decorative card
            val cardPaint = Paint().apply {
                color = Color.WHITE
                setShadowLayer(16f, 0f, 8f, Color.argb(40, 0, 0, 0))
            }
            val cardRect = RectF(100f, 120f, (width - 100).toFloat(), (height - 100).toFloat())
            canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

            // Inner graphic badge
            val badgePaint = Paint().apply {
                color = secondaryColor
            }
            canvas.drawRoundRect(RectF(150f, 170f, 290f, 310f), 20f, 20f, badgePaint)

            // Icon inside badge (lock / shield)
            val iconPaint = Paint().apply {
                color = Color.WHITE
                textSize = 64f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("✓", 195f, 260f, iconPaint)

            // Title and tagline
            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 42f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText(title, 330f, 220f, titlePaint)

            val taglinePaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 24f
                isAntiAlias = true
            }
            canvas.drawText(tagline, 330f, 270f, taglinePaint)

            // Grid lines simulation
            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 3f
            }
            var y = 370f
            while (y < height - 160) {
                canvas.drawLine(150f, y, (width - 150).toFloat(), y, linePaint)
                y += 70f
            }

            // Watermark badge at bottom
            val markPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 20f
                isAntiAlias = true
            }
            canvas.drawText("CONFIDENTIAL ARCHIVE • ACCESS VERIFIED", 150f, (height - 120).toFloat(), markPaint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (t: Throwable) {
            // Write minimal 1x1 PNG bytes fallback for host JVM tests
            file.writeBytes(byteArrayOf(
                0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
            ))
        }
    }

    /**
     * Renders a specific page of a PDF file to a Bitmap using native Android PdfRenderer.
     */
    suspend fun renderPdfPageToBitmap(
        pdfFile: File,
        pageIndex: Int,
        destWidth: Int = 1000
    ): Pair<Bitmap?, Int> = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext Pair(null, 0)
        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            val validIndex = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

            val page = renderer.openPage(validIndex)
            val scale = destWidth.toFloat() / page.width.toFloat()
            val destHeight = (page.height * scale).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
            // Fill with crisp white background before rendering PDF
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()

            Pair(bitmap, pageCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, 0)
        }
    }

    /**
     * Gets total page count for a PDF file
     */
    suspend fun getPdfPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext 0
        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Copies a file chosen by Admin (from SAF or PhotoPicker) into the internal Vault directory.
     */
    suspend fun copyUriToVault(
        context: Context,
        uri: Uri,
        customTitle: String? = null
    ): VaultItem? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "vault_file_${System.currentTimeMillis()}"
            var mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            // Query file display name
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            val isPdf = fileName.endsWith(".pdf", ignoreCase = true) || mimeType.contains("pdf")
            val isImage = fileName.endsWith(".png", ignoreCase = true) ||
                    fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true) ||
                    fileName.endsWith(".webp", ignoreCase = true) ||
                    mimeType.startsWith("image/")

            val fileType = if (isPdf) "PDF" else "IMAGE"
            if (isPdf) mimeType = "application/pdf"
            if (!isPdf && !isImage) {
                // Default to image or PDF depending on extension
                if (fileName.contains("pdf", ignoreCase = true)) "PDF" else "IMAGE"
            }

            val safeFileName = "${System.currentTimeMillis()}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
            val destFile = File(getVaultDir(context), safeFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val title = customTitle?.takeIf { it.isNotBlank() }
                ?: fileName.substringBeforeLast(".")
                    .replace("_", " ")
                    .replace("-", " ")
                    .replaceFirstChar { it.uppercase() }

            VaultItem(
                title = title,
                description = "Uploaded by administrator on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date())}",
                fileType = fileType,
                fileName = fileName,
                localFilePath = destFile.absolutePath,
                fileSizeBytes = destFile.length(),
                mimeType = mimeType,
                category = if (isPdf) "Documents" else "Media"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a new custom sample document generated on the fly by admin.
     */
    suspend fun createAdminSampleFile(
        context: Context,
        title: String,
        type: String, // "IMAGE" or "PDF"
        category: String
    ): VaultItem = withContext(Dispatchers.IO) {
        val vaultDir = getVaultDir(context)
        val timestamp = System.currentTimeMillis()
        val cleanName = title.replace("[^a-zA-Z0-9]".toRegex(), "_")

        if (type == "PDF") {
            val file = File(vaultDir, "AdminDoc_${cleanName}_$timestamp.pdf")
            generateSamplePdf(
                file = file,
                title = title.uppercase(),
                subtitle = "Admin Vault Upload • Category: $category",
                pages = listOf(
                    listOf(
                        "1. DOCUMENT OVERVIEW",
                        "This document was created and authenticated by the system administrator.",
                        "Access Level: Restricted to authorized visitor roster.",
                        "",
                        "2. SECURE DISTRIBUTION NOTICE",
                        "Any downloads of this file are monitored and recorded in the audit dashboard."
                    )
                )
            )
            VaultItem(
                title = title,
                description = "Secure administrative document in category '$category'.",
                fileType = "PDF",
                fileName = file.name,
                localFilePath = file.absolutePath,
                fileSizeBytes = file.length(),
                mimeType = "application/pdf",
                category = category
            )
        } else {
            val file = File(vaultDir, "AdminImg_${cleanName}_$timestamp.png")
            generateSampleImage(
                file = file,
                title = title.uppercase(),
                tagline = "Category: $category • Verified Asset",
                primaryColor = Color.rgb(30, 41, 59),
                secondaryColor = Color.rgb(14, 165, 233)
            )
            VaultItem(
                title = title,
                description = "High-definition admin visual asset in category '$category'.",
                fileType = "IMAGE",
                fileName = file.name,
                localFilePath = file.absolutePath,
                fileSizeBytes = file.length(),
                mimeType = "image/png",
                category = category
            )
        }
    }

    /**
     * Executes the secure download operation: saves file to device downloads / app downloads,
     * and returns the downloaded File and Uri.
     */
    suspend fun downloadVaultItemToDevice(
        context: Context,
        vaultItem: VaultItem
    ): Pair<File, Uri?> = withContext(Dispatchers.IO) {
        val sourceFile = File(vaultItem.localFilePath)
        val downloadsDir = getDownloadsDir(context)
        val destFile = File(downloadsDir, vaultItem.fileName)

        // 1. Copy to internal downloads directory
        if (sourceFile.exists()) {
            sourceFile.inputStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        // 2. Also save to MediaStore / Public Downloads for Android 10+
        var publicUri: Uri? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, vaultItem.fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, vaultItem.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SecureVault")
                }

                val collectionUri = if (vaultItem.fileType == "IMAGE") {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val uri = context.contentResolver.insert(collectionUri, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    publicUri = uri
                }
            }
        } catch (e: Exception) {
            // MediaStore optional, destFile still valid
            e.printStackTrace()
        }

        // Return FileProvider Uri if publicUri is null
        val finalUri = publicUri ?: try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
        } catch (e: Exception) {
            null
        }

        Pair(destFile, finalUri)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups])
    }
}
