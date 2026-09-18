package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.data.repository.EmployeeRepository
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Extension function to safely extract an Activity Context from any Context.
 */
fun Context.findActivity(): Activity? {
    var currentContext: Context = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

object PdfPrintHelper {

    private const val PAGE_WIDTH = 595 // A4 standard width (points)
    private const val PAGE_HEIGHT = 842 // A4 standard height (points)
    private const val MARGIN = 30f

    /**
     * Helper PrintDocumentAdapter that generates and writes a PDF file to the Android Print Spooler.
     */
    private class PdfFilePrintAdapter(
        private val context: Context,
        private val documentTitle: String,
        private val generator: (File) -> Unit
    ) : PrintDocumentAdapter() {

        private var pdfFile: File? = null

        private fun getOrCreatePdf(): File {
            var file = pdfFile
            if (file == null || !file.exists()) {
                val f = File(context.cacheDir, "temp_print_${System.currentTimeMillis() % 100000}.pdf")
                generator(f)
                pdfFile = f
            }
            return pdfFile!!
        }

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }

            try {
                val file = getOrCreatePdf()
                val pageCount = getPdfPageCount(file)
                val info = PrintDocumentInfo.Builder("$documentTitle.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pageCount)
                    .build()
                callback.onLayoutFinished(info, newAttributes != oldAttributes)
            } catch (e: Exception) {
                callback.onLayoutFailed("خطأ أثناء تجهيز المستند للطباعة: ${e.localizedMessage}")
            }
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }

            var input: FileInputStream? = null
            var output: FileOutputStream? = null
            try {
                val file = getOrCreatePdf()
                input = FileInputStream(file)
                output = FileOutputStream(destination.fileDescriptor)

                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } >= 0) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed("فشل في كتابة مستند الطباعة: ${e.localizedMessage}")
            } finally {
                try { input?.close() } catch (_: Exception) {}
                try { output?.close() } catch (_: Exception) {}
            }
        }

        override fun onFinish() {
            super.onFinish()
            try {
                pdfFile?.delete()
                pdfFile = null
            } catch (_: Exception) {}
        }

        private fun getPdfPageCount(file: File): Int {
            return try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                renderer.close()
                pfd.close()
                if (count > 0) count else PrintDocumentInfo.PAGE_COUNT_UNKNOWN
            } catch (_: Exception) {
                PrintDocumentInfo.PAGE_COUNT_UNKNOWN
            }
        }
    }

    /**
     * Prints or exports an Employee Statement to PDF via Native Android PrintManager.
     * Returns true if print job was successfully handed off to the system.
     */
    fun printEmployeeStatement(
        context: Context,
        employee: Employee,
        departmentName: String,
        stats: EmployeeFinanceStats,
        rows: List<StatementRow>,
        settings: AppSettings
    ): Boolean {
        val activity = context.findActivity() ?: run {
            Toast.makeText(context, "تعذر تحديد واجهة التشغيل (Activity) لتشغيل الطباعة", Toast.LENGTH_LONG).show()
            return false
        }

        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(activity, "خدمة الطباعة غير متوفرة على هذا الجهاز. جاري فتح خيارات المشاركة والحفظ كملف بديل...", Toast.LENGTH_SHORT).show()
            shareEmployeeStatement(activity, employee, departmentName, stats, rows, settings)
            return false
        }

        val sanitizedEmpName = employee.name.trim().replace("\\s+".toRegex(), "_")
        val jobName = "كشف_حساب_${sanitizedEmpName}_${employee.no}"

        return try {
            val adapter = PdfFilePrintAdapter(activity, jobName) { targetFile ->
                generateStatementPdf(activity, employee, departmentName, stats, rows, settings, targetFile)
            }

            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("res_a4", "A4 Resolution", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            val printJob = printManager.print(jobName, adapter, printAttributes)
            if (printJob == null) {
                Toast.makeText(activity, "تعذر إطلاق خدمة الطباعة من النظام", Toast.LENGTH_SHORT).show()
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "حدث خطأ أثناء تشغيل خدمة الطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Exports and shares the Employee Statement as a physical PDF file.
     */
    fun shareEmployeeStatement(
        context: Context,
        employee: Employee,
        departmentName: String,
        stats: EmployeeFinanceStats,
        rows: List<StatementRow>,
        settings: AppSettings
    ) {
        try {
            val fileName = "كشف_حساب_${employee.no}_${System.currentTimeMillis() % 1000}.pdf"
            val file = File(context.cacheDir, fileName)
            generateStatementPdf(context, employee, departmentName, stats, rows, settings, file)
            sharePdfFile(context, file, "كشف حساب - ${employee.name}")
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء تصدير ملف PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Generates the multi-page Employee Statement PDF document.
     */
    fun generateStatementPdf(
        context: Context,
        employee: Employee,
        departmentName: String,
        stats: EmployeeFinanceStats,
        rows: List<StatementRow>,
        settings: AppSettings,
        targetFile: File
    ) {
        val document = PdfDocument()
        try {
            val firstPageRowsCount = 18
            val subsequentPageRowsCount = 26
            val totalRows = rows.size
            val totalPages = if (totalRows <= firstPageRowsCount) 1 else {
                1 + Math.ceil((totalRows - firstPageRowsCount).toDouble() / subsequentPageRowsCount).toInt()
            }

            var currentRowIdx = 0
            for (pageIndex in 0 until totalPages) {
                val pageNumber = pageIndex + 1
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                val isFirstPage = (pageIndex == 0)
                val isLastPage = (pageIndex == totalPages - 1)

                val rowsForThisPage = if (totalRows == 0) {
                    emptyList()
                } else if (isFirstPage) {
                    val end = minOf(totalRows, firstPageRowsCount)
                    val slice = rows.subList(0, end)
                    currentRowIdx = end
                    slice
                } else {
                    val end = minOf(totalRows, currentRowIdx + subsequentPageRowsCount)
                    val slice = rows.subList(currentRowIdx, end)
                    currentRowIdx = end
                    slice
                }

                drawStatementSinglePage(
                    canvas = canvas,
                    employee = employee,
                    departmentName = departmentName,
                    stats = stats,
                    pageRows = rowsForThisPage,
                    startRowNumber = if (isFirstPage) 1 else (currentRowIdx - rowsForThisPage.size + 1),
                    settings = settings,
                    pageNumber = pageNumber,
                    totalPages = totalPages,
                    isFirstPage = isFirstPage,
                    isLastPage = isLastPage
                )

                document.finishPage(page)
            }

            FileOutputStream(targetFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
        }
    }

    /**
     * Prints or exports a Comprehensive Financial Report to PDF
     */
    fun printFinancialReport(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        employees: List<Employee>,
        reportTitle: String = "التقرير المالي الشامل للعمليات"
    ): Boolean {
        val activity = context.findActivity() ?: run {
            Toast.makeText(context, "تعذر تحديد واجهة التشغيل (Activity) للطباعة", Toast.LENGTH_LONG).show()
            return false
        }

        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            shareFinancialReport(activity, settings, transactions, employees, reportTitle)
            return false
        }

        val jobName = "تقرير_مالي_${System.currentTimeMillis() % 1000}"

        return try {
            val adapter = PdfFilePrintAdapter(activity, jobName) { targetFile ->
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                val page = document.startPage(pageInfo)
                drawFinancialReportPage(page.canvas, settings, transactions, employees, reportTitle)
                document.finishPage(page)
                FileOutputStream(targetFile).use { out -> document.writeTo(out) }
                document.close()
            }

            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("res_a4", "A4 Resolution", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, adapter, printAttributes) != null
        } catch (e: Exception) {
            Toast.makeText(activity, "فشل تشغيل خدمة الطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun shareFinancialReport(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        employees: List<Employee>,
        reportTitle: String = "التقرير المالي الشامل للعمليات"
    ) {
        try {
            val fileName = "تقرير_مالي_${System.currentTimeMillis() % 1000}.pdf"
            val file = File(context.cacheDir, fileName)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            drawFinancialReportPage(page.canvas, settings, transactions, employees, reportTitle)
            document.finishPage(page)

            FileOutputStream(file).use { out -> document.writeTo(out) }
            document.close()

            sharePdfFile(context, file, reportTitle)
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء تصدير التقرير: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Prints or exports a comprehensive Employee Profile & History Report
     */
    fun printEmployeeProfile(
        context: Context,
        employee: Employee,
        departmentName: String,
        stats: EmployeeFinanceStats,
        advances: List<AdvanceRequest>,
        penalties: List<Penalty>,
        settings: AppSettings
    ): Boolean {
        val activity = context.findActivity() ?: run {
            Toast.makeText(context, "تعذر تحديد واجهة التشغيل (Activity) للطباعة", Toast.LENGTH_LONG).show()
            return false
        }

        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            shareEmployeeProfile(activity, employee, departmentName, stats, advances, penalties, settings)
            return false
        }

        val sanitizedEmpName = employee.name.trim().replace("\\s+".toRegex(), "_")
        val jobName = "ملف_الموظف_${sanitizedEmpName}_${employee.no}"

        return try {
            val adapter = PdfFilePrintAdapter(activity, jobName) { targetFile ->
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                val page = document.startPage(pageInfo)
                drawEmployeeProfilePage(page.canvas, employee, departmentName, stats, advances, penalties, settings)
                document.finishPage(page)
                FileOutputStream(targetFile).use { out -> document.writeTo(out) }
                document.close()
            }

            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("res_a4", "A4 Resolution", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, adapter, printAttributes) != null
        } catch (e: Exception) {
            Toast.makeText(activity, "فشل تشغيل خدمة الطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun shareEmployeeProfile(
        context: Context,
        employee: Employee,
        departmentName: String,
        stats: EmployeeFinanceStats,
        advances: List<AdvanceRequest>,
        penalties: List<Penalty>,
        settings: AppSettings
    ) {
        try {
            val fileName = "ملف_الموظف_${employee.no}_${System.currentTimeMillis() % 1000}.pdf"
            val file = File(context.cacheDir, fileName)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            drawEmployeeProfilePage(page.canvas, employee, departmentName, stats, advances, penalties, settings)
            document.finishPage(page)

            FileOutputStream(file).use { out -> document.writeTo(out) }
            document.close()

            sharePdfFile(context, file, "الملف المالي والإداري - ${employee.name}")
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء تصدير ملف PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdfFile(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title - تم التصدير عبر تطبيق إدارة الموظفين والسلف")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "حفظ أو طباعة أو مشاركة مستند PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Toast.makeText(context, "تم تجهيز ملف PDF بنجاح!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح نافذة المشاركة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawStatementSinglePage(
        canvas: Canvas,
        employee: Employee,
        departmentName: String,
        stats: EmployeeFinanceStats,
        pageRows: List<StatementRow>,
        startRowNumber: Int,
        settings: AppSettings,
        pageNumber: Int,
        totalPages: Int,
        isFirstPage: Boolean,
        isLastPage: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        var currentY: Float

        if (isFirstPage) {
            // Draw Main Header Banner
            paint.color = Color.rgb(15, 43, 72) // #0F2B48
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 62f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 15f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(settings.companyName, PAGE_WIDTH - MARGIN, 26f, textPaint)

            textPaint.textSize = 10.5f
            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = Color.rgb(180, 220, 255)
            canvas.drawText("كشف حساب مالي تفصيلي (سجل السلف والذمم)", PAGE_WIDTH - MARGIN, 46f, textPaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.WHITE
            textPaint.textSize = 9f
            canvas.drawText("التاريخ: ${EmployeeRepository.getCurrentDateTimeString()}", MARGIN, 32f, textPaint)
            if (totalPages > 1) {
                textPaint.color = Color.rgb(180, 220, 255)
                canvas.drawText("صفحة $pageNumber من $totalPages", MARGIN, 48f, textPaint)
            }

            // Employee Info Card
            currentY = 74f
            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRoundRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 68f, 6f, 6f, paint)
            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 68f, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = Color.rgb(30, 41, 59)
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            // Right side of info card
            canvas.drawText("اسم الموظف: ${employee.name}", PAGE_WIDTH - MARGIN - 12f, currentY + 20f, textPaint)
            canvas.drawText("الرقم الوظيفي: ${employee.no}", PAGE_WIDTH - MARGIN - 12f, currentY + 38f, textPaint)
            canvas.drawText("القسم / الإدارة: $departmentName", PAGE_WIDTH - MARGIN - 12f, currentY + 56f, textPaint)

            // Left side of info card
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText("المسمى الوظيفي: ${employee.job}", (PAGE_WIDTH / 2f) + 40f, currentY + 20f, textPaint)
            canvas.drawText("رقم الهاتف: ${employee.mobile}", (PAGE_WIDTH / 2f) + 40f, currentY + 38f, textPaint)
            canvas.drawText("تاريخ التعيين: ${employee.hireDate}", (PAGE_WIDTH / 2f) + 40f, currentY + 56f, textPaint)

            // Stats Summary Boxes (4 cards)
            currentY = 150f
            val boxWidth = (PAGE_WIDTH - (MARGIN * 2) - 18f) / 4f
            val boxHeight = 44f
            val curr = settings.currency

            drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 3, currentY, boxWidth, boxHeight, "إجمالي السلف", "${EmployeeRepository.formatAmount(stats.totalAdvances)} $curr", Color.rgb(240, 253, 244), Color.rgb(5, 122, 85))
            drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 2, currentY, boxWidth, boxHeight, "إجمالي المسدد", "${EmployeeRepository.formatAmount(stats.totalRepaid)} $curr", Color.rgb(239, 246, 255), Color.rgb(28, 100, 242))
            drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 1, currentY, boxWidth, boxHeight, "صافي الرصيد المتبقي", "${EmployeeRepository.formatAmount(stats.remainingBalance)} $curr", Color.rgb(255, 251, 235), Color.rgb(180, 83, 9))
            drawStatBox(canvas, MARGIN, currentY, boxWidth, boxHeight, "إجمالي الخصومات", "${EmployeeRepository.formatAmount(stats.totalDeductions)} $curr", Color.rgb(254, 242, 242), Color.rgb(224, 36, 36))

            currentY = 204f
        } else {
            // Continuation Page Header
            paint.color = Color.rgb(15, 43, 72)
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 44f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${settings.companyName} • تابع كشف حساب: ${employee.name} (${employee.no})", PAGE_WIDTH - MARGIN, 26f, textPaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 9f
            textPaint.color = Color.rgb(180, 220, 255)
            canvas.drawText("صفحة $pageNumber من $totalPages", MARGIN, 26f, textPaint)

            currentY = 56f
        }

        // Table Header
        paint.color = Color.rgb(15, 43, 72)
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("#", PAGE_WIDTH - MARGIN - 14f, currentY + 14f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("التاريخ", PAGE_WIDTH - MARGIN - 36f, currentY + 14f, textPaint)
        canvas.drawText("رقم السند", PAGE_WIDTH - MARGIN - 96f, currentY + 14f, textPaint)
        canvas.drawText("النوع", PAGE_WIDTH - MARGIN - 152f, currentY + 14f, textPaint)
        canvas.drawText("البيان والملاحظات", PAGE_WIDTH - MARGIN - 200f, currentY + 14f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("المبلغ (${settings.currency})", MARGIN + 85f, currentY + 14f, textPaint)
        canvas.drawText("الرصيد (${settings.currency})", MARGIN + 10f, currentY + 14f, textPaint)

        currentY += 22f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8.5f

        if (pageRows.isEmpty() && isFirstPage) {
            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 40f, paint)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText("لا توجد حركات مالية مقيدة في كشف الحساب حتى تاريخه.", PAGE_WIDTH / 2f, currentY + 24f, textPaint)
            currentY += 40f
        } else {
            pageRows.forEachIndexed { index, row ->
                val tx = row.transaction
                val rowHeight = 20f

                if (index % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)
                }

                paint.color = Color.rgb(226, 232, 240)
                paint.strokeWidth = 0.5f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(MARGIN, currentY + rowHeight, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)
                paint.style = Paint.Style.FILL

                val actualRowIndex = startRowNumber + index
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = Color.rgb(100, 116, 139)
                canvas.drawText("$actualRowIndex", PAGE_WIDTH - MARGIN - 14f, currentY + 14f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.color = Color.rgb(51, 65, 85)
                canvas.drawText(tx.date, PAGE_WIDTH - MARGIN - 36f, currentY + 14f, textPaint)
                canvas.drawText(tx.no, PAGE_WIDTH - MARGIN - 96f, currentY + 14f, textPaint)

                val typeName = when (tx.type) {
                    "advance" -> "سلفة"
                    "repayment" -> "سداد"
                    else -> "خصم / جزاء"
                }
                val typeColor = when (tx.type) {
                    "advance" -> Color.rgb(5, 122, 85)
                    "repayment" -> Color.rgb(28, 100, 242)
                    else -> Color.rgb(224, 36, 36)
                }
                textPaint.color = typeColor
                canvas.drawText(typeName, PAGE_WIDTH - MARGIN - 152f, currentY + 14f, textPaint)

                textPaint.color = Color.rgb(51, 65, 85)
                val notePart = if (tx.note.isNotBlank() && tx.note != tx.desc) " • ${tx.note}" else ""
                val fullDesc = tx.desc + notePart
                val desc = if (fullDesc.length > 34) fullDesc.take(32) + ".." else fullDesc
                canvas.drawText(desc, PAGE_WIDTH - MARGIN - 200f, currentY + 14f, textPaint)

                textPaint.textAlign = Paint.Align.LEFT
                val sign = if (tx.type == "advance") "+" else "-"
                textPaint.color = typeColor
                canvas.drawText("$sign ${EmployeeRepository.formatAmount(tx.amount)}", MARGIN + 85f, currentY + 14f, textPaint)

                textPaint.color = Color.rgb(15, 23, 42)
                canvas.drawText(EmployeeRepository.formatAmount(row.runningBalance), MARGIN + 10f, currentY + 14f, textPaint)

                currentY += rowHeight
            }
        }

        // Draw Signatures on the last page
        if (isLastPage) {
            drawSignatures(canvas, PAGE_HEIGHT - 90f)
        }

        // Footer Note
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 8f
        val pageText = if (totalPages > 1) " • صفحة $pageNumber من $totalPages" else ""
        canvas.drawText("تم استخراج هذه الوثيقة آلياً من نظام إدارة شؤون الموظفين والسلف • تاريخ الطباعة: ${EmployeeRepository.getCurrentDateTimeString()}$pageText", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, textPaint)
    }

    private fun drawFinancialReportPage(
        canvas: Canvas,
        settings: AppSettings,
        transactions: List<Transaction>,
        employees: List<Employee>,
        reportTitle: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        // Header Banner
        paint.color = Color.rgb(15, 43, 72)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 60f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(settings.companyName, PAGE_WIDTH - MARGIN, 28f, textPaint)

        textPaint.textSize = 11f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.rgb(180, 220, 255)
        canvas.drawText(reportTitle, PAGE_WIDTH - MARGIN, 48f, textPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        textPaint.textSize = 9f
        canvas.drawText("تاريخ التقرير: ${EmployeeRepository.getCurrentDateString()}", MARGIN, 36f, textPaint)

        // Summary Cards
        var currentY = 75f
        val totalAdvances = transactions.filter { it.type == "advance" }.sumOf { it.amount }
        val totalRepaid = transactions.filter { it.type == "repayment" }.sumOf { it.amount }
        val totalDeductions = transactions.filter { it.type == "deduction" || it.type == "penalty" }.sumOf { it.amount }
        val netRemaining = totalAdvances - totalRepaid
        val curr = settings.currency

        val boxWidth = (PAGE_WIDTH - (MARGIN * 2) - 18f) / 4f
        val boxHeight = 44f

        drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 3, currentY, boxWidth, boxHeight, "إجمالي السلف", "${EmployeeRepository.formatAmount(totalAdvances)} $curr", Color.rgb(240, 253, 244), Color.rgb(5, 122, 85))
        drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 2, currentY, boxWidth, boxHeight, "إجمالي المسترد", "${EmployeeRepository.formatAmount(totalRepaid)} $curr", Color.rgb(239, 246, 255), Color.rgb(28, 100, 242))
        drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 1, currentY, boxWidth, boxHeight, "صافي الذمم القائمة", "${EmployeeRepository.formatAmount(netRemaining)} $curr", Color.rgb(255, 251, 235), Color.rgb(180, 83, 9))
        drawStatBox(canvas, MARGIN, currentY, boxWidth, boxHeight, "إجمالي الخصومات", "${EmployeeRepository.formatAmount(totalDeductions)} $curr", Color.rgb(254, 242, 242), Color.rgb(224, 36, 36))

        // Table Header
        currentY = 130f
        paint.color = Color.rgb(15, 43, 72)
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("#", PAGE_WIDTH - MARGIN - 15f, currentY + 15f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("التاريخ", PAGE_WIDTH - MARGIN - 40f, currentY + 15f, textPaint)
        canvas.drawText("رقم السند", PAGE_WIDTH - MARGIN - 100f, currentY + 15f, textPaint)
        canvas.drawText("الموظف", PAGE_WIDTH - MARGIN - 155f, currentY + 15f, textPaint)
        canvas.drawText("النوع", PAGE_WIDTH - MARGIN - 260f, currentY + 15f, textPaint)
        canvas.drawText("البيان", PAGE_WIDTH - MARGIN - 300f, currentY + 15f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("المبلغ ($curr)", MARGIN + 10f, currentY + 15f, textPaint)

        // Rows
        currentY += 22f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8.5f

        val displayRows = transactions.take(26)
        displayRows.forEachIndexed { index, tx ->
            val emp = employees.find { it.id == tx.empId }
            val rowHeight = 20f

            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)
            }

            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 0.5f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(MARGIN, currentY + rowHeight, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)
            paint.style = Paint.Style.FILL

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText("${index + 1}", PAGE_WIDTH - MARGIN - 15f, currentY + 14f, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = Color.rgb(51, 65, 85)
            canvas.drawText(tx.date, PAGE_WIDTH - MARGIN - 40f, currentY + 14f, textPaint)
            canvas.drawText(tx.no, PAGE_WIDTH - MARGIN - 100f, currentY + 14f, textPaint)

            val empName = (emp?.name ?: "موظف").let { if (it.length > 18) it.take(16) + ".." else it }
            canvas.drawText(empName, PAGE_WIDTH - MARGIN - 155f, currentY + 14f, textPaint)

            val typeName = when (tx.type) {
                "advance" -> "سلفة"
                "repayment" -> "سداد"
                else -> "خصم"
            }
            val typeColor = when (tx.type) {
                "advance" -> Color.rgb(5, 122, 85)
                "repayment" -> Color.rgb(28, 100, 242)
                else -> Color.rgb(224, 36, 36)
            }
            textPaint.color = typeColor
            canvas.drawText(typeName, PAGE_WIDTH - MARGIN - 260f, currentY + 14f, textPaint)

            textPaint.color = Color.rgb(51, 65, 85)
            val desc = if (tx.desc.length > 22) tx.desc.take(20) + ".." else tx.desc
            canvas.drawText(desc, PAGE_WIDTH - MARGIN - 300f, currentY + 14f, textPaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = typeColor
            canvas.drawText(EmployeeRepository.formatAmount(tx.amount), MARGIN + 10f, currentY + 14f, textPaint)

            currentY += rowHeight
        }

        drawSignatures(canvas, PAGE_HEIGHT - 90f)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 8f
        canvas.drawText("التقرير المالي المعتمد • تم التصدير بتاريخ: ${EmployeeRepository.getCurrentDateTimeString()}", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, textPaint)
    }

    private fun drawEmployeeProfilePage(
        canvas: Canvas,
        employee: Employee,
        departmentName: String,
        stats: EmployeeFinanceStats,
        advances: List<AdvanceRequest>,
        penalties: List<Penalty>,
        settings: AppSettings
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        // Header Banner
        paint.color = Color.rgb(15, 43, 72)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 60f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(settings.companyName, PAGE_WIDTH - MARGIN, 28f, textPaint)

        textPaint.textSize = 11f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color.rgb(180, 220, 255)
        canvas.drawText("الملف المالي والإداري الشامل للموظف", PAGE_WIDTH - MARGIN, 48f, textPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        textPaint.textSize = 9f
        canvas.drawText("التاريخ: ${EmployeeRepository.getCurrentDateString()}", MARGIN, 36f, textPaint)

        // Employee Info Card
        var currentY = 75f
        paint.color = Color.rgb(245, 247, 250)
        canvas.drawRoundRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 68f, 6f, 6f, paint)
        paint.color = Color.rgb(220, 225, 230)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 68f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.rgb(30, 41, 59)
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("اسم الموظف: ${employee.name}", PAGE_WIDTH - MARGIN - 12f, currentY + 20f, textPaint)
        canvas.drawText("الرقم الوظيفي: ${employee.no}", PAGE_WIDTH - MARGIN - 12f, currentY + 38f, textPaint)
        canvas.drawText("القسم / الإدارة: $departmentName", PAGE_WIDTH - MARGIN - 12f, currentY + 54f, textPaint)

        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("المسمى الوظيفي: ${employee.job}", (PAGE_WIDTH / 2f) + 40f, currentY + 20f, textPaint)
        canvas.drawText("رقم الهاتف: ${employee.mobile}", (PAGE_WIDTH / 2f) + 40f, currentY + 38f, textPaint)
        canvas.drawText("تاريخ التعيين: ${employee.hireDate}", (PAGE_WIDTH / 2f) + 40f, currentY + 54f, textPaint)

        // Stats Summary
        currentY = 153f
        val boxWidth = (PAGE_WIDTH - (MARGIN * 2) - 18f) / 4f
        val boxHeight = 44f
        val curr = settings.currency

        drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 3, currentY, boxWidth, boxHeight, "إجمالي السلف", "${EmployeeRepository.formatAmount(stats.totalAdvances)} $curr", Color.rgb(240, 253, 244), Color.rgb(5, 122, 85))
        drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 2, currentY, boxWidth, boxHeight, "إجمالي المسدد", "${EmployeeRepository.formatAmount(stats.totalRepaid)} $curr", Color.rgb(239, 246, 255), Color.rgb(28, 100, 242))
        drawStatBox(canvas, MARGIN + (boxWidth + 6f) * 1, currentY, boxWidth, boxHeight, "الرصيد المتبقي", "${EmployeeRepository.formatAmount(stats.remainingBalance)} $curr", Color.rgb(255, 251, 235), Color.rgb(180, 83, 9))
        drawStatBox(canvas, MARGIN, currentY, boxWidth, boxHeight, "الخصومات", "${EmployeeRepository.formatAmount(stats.totalDeductions)} $curr", Color.rgb(254, 242, 242), Color.rgb(224, 36, 36))

        // Advances Section
        currentY = 210f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 10.5f
        textPaint.color = Color.rgb(15, 43, 72)
        canvas.drawText("سجل طلبات السلف المالية", PAGE_WIDTH - MARGIN, currentY, textPaint)

        currentY += 8f
        paint.color = Color.rgb(15, 43, 72)
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 20f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 9f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("#", PAGE_WIDTH - MARGIN - 15f, currentY + 14f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("التاريخ", PAGE_WIDTH - MARGIN - 40f, currentY + 14f, textPaint)
        canvas.drawText("رقم الطلب", PAGE_WIDTH - MARGIN - 100f, currentY + 14f, textPaint)
        canvas.drawText("السبب والبيان", PAGE_WIDTH - MARGIN - 170f, currentY + 14f, textPaint)
        canvas.drawText("الحالة", PAGE_WIDTH - MARGIN - 360f, currentY + 14f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("المبلغ ($curr)", MARGIN + 10f, currentY + 14f, textPaint)

        currentY += 20f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8.5f

        advances.take(8).forEachIndexed { idx, adv ->
            val rowHeight = 18f
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)
            }
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText("${idx + 1}", PAGE_WIDTH - MARGIN - 15f, currentY + 13f, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = Color.rgb(51, 65, 85)
            canvas.drawText(adv.date, PAGE_WIDTH - MARGIN - 40f, currentY + 13f, textPaint)
            canvas.drawText(adv.reqNo, PAGE_WIDTH - MARGIN - 100f, currentY + 13f, textPaint)
            canvas.drawText(if (adv.reason.length > 25) adv.reason.take(23) + ".." else adv.reason, PAGE_WIDTH - MARGIN - 170f, currentY + 13f, textPaint)

            val statusText = if (adv.status == "approved") "معتمد" else if (adv.status == "pending") "قيد المراجعة" else "مرفوض"
            val statusColor = if (adv.status == "approved") Color.rgb(5, 122, 85) else if (adv.status == "pending") Color.rgb(28, 100, 242) else Color.rgb(224, 36, 36)
            textPaint.color = statusColor
            canvas.drawText(statusText, PAGE_WIDTH - MARGIN - 360f, currentY + 13f, textPaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText(EmployeeRepository.formatAmount(adv.amount), MARGIN + 10f, currentY + 13f, textPaint)

            currentY += rowHeight
        }

        // Penalties Section
        currentY += 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 10.5f
        textPaint.color = Color.rgb(15, 43, 72)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("سجل الخصومات والجزاءات", PAGE_WIDTH - MARGIN, currentY, textPaint)

        currentY += 8f
        paint.color = Color.rgb(15, 43, 72)
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 20f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 9f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("#", PAGE_WIDTH - MARGIN - 15f, currentY + 14f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("التاريخ", PAGE_WIDTH - MARGIN - 40f, currentY + 14f, textPaint)
        canvas.drawText("رقم الإشعار", PAGE_WIDTH - MARGIN - 100f, currentY + 14f, textPaint)
        canvas.drawText("السبب والمبرر", PAGE_WIDTH - MARGIN - 170f, currentY + 14f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("المبلغ ($curr)", MARGIN + 10f, currentY + 14f, textPaint)

        currentY += 20f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 8.5f

        penalties.take(6).forEachIndexed { idx, pen ->
            val rowHeight = 18f
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)
            }
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText("${idx + 1}", PAGE_WIDTH - MARGIN - 15f, currentY + 13f, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = Color.rgb(51, 65, 85)
            canvas.drawText(pen.date, PAGE_WIDTH - MARGIN - 40f, currentY + 13f, textPaint)
            canvas.drawText(pen.reqNo, PAGE_WIDTH - MARGIN - 100f, currentY + 13f, textPaint)
            canvas.drawText(if (pen.reason.length > 30) pen.reason.take(28) + ".." else pen.reason, PAGE_WIDTH - MARGIN - 170f, currentY + 13f, textPaint)

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.rgb(224, 36, 36)
            canvas.drawText(EmployeeRepository.formatAmount(pen.amount), MARGIN + 10f, currentY + 13f, textPaint)

            currentY += rowHeight
        }

        drawSignatures(canvas, PAGE_HEIGHT - 90f)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 8f
        canvas.drawText("ملف الموظف الرسمي • تم الاستخراج بتاريخ: ${EmployeeRepository.getCurrentDateTimeString()}", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, textPaint)
    }

    private fun drawStatBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        bgColor: Int,
        valueColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        paint.color = bgColor
        canvas.drawRoundRect(x, y, x + width, y + height, 6f, 6f, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(x, y, x + width, y + height, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 8.5f
        textPaint.color = Color.rgb(100, 116, 139)
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(title, x + (width / 2f), y + 16f, textPaint)

        textPaint.textSize = 10.5f
        textPaint.color = valueColor
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + (width / 2f), y + 33f, textPaint)
    }

    private fun drawSignatures(canvas: Canvas, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 1f
        paint.pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        textPaint.textSize = 9.5f
        textPaint.color = Color.rgb(71, 85, 105)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER

        val colWidth = (PAGE_WIDTH - (MARGIN * 2)) / 3f

        // Column 1: Accountant
        canvas.drawText("إعداد المحاسب", PAGE_WIDTH - MARGIN - (colWidth / 2f), y + 18f, textPaint)
        paint.pathEffect = null
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawLine(PAGE_WIDTH - MARGIN - colWidth + 20f, y + 42f, PAGE_WIDTH - MARGIN - 20f, y + 42f, paint)

        // Column 2: Employee
        canvas.drawText("توقيع الموظف", PAGE_WIDTH - MARGIN - (colWidth * 1.5f), y + 18f, textPaint)
        canvas.drawLine(PAGE_WIDTH - MARGIN - (colWidth * 2) + 20f, y + 42f, PAGE_WIDTH - MARGIN - colWidth - 20f, y + 42f, paint)

        // Column 3: Management
        canvas.drawText("اعتماد الإدارة العامة", MARGIN + (colWidth / 2f), y + 18f, textPaint)
        canvas.drawLine(MARGIN + 20f, y + 42f, MARGIN + colWidth - 20f, y + 42f, paint)
    }
}
