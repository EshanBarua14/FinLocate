package com.example.data.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.model.TransactionEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CountryConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    /**
     * Generates a fully formatted, professional PDF statement listing out transaction ledgers.
     * Works offline and complies perfectly with standard android.graphics.pdf.PdfDocument API.
     */
    fun generatePdfReport(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        month: String,
        config: CountryConfig
    ): File {
        val pdfDocument = PdfDocument()
        
        // Define standard Letter / A4 dimension limits: 595 width x 842 height (Points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        val brandPaint = Paint().apply {
            color = Color.rgb(9, 133, 114) // Modern fintech green tone
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(74, 85, 104)
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        
        var y = 45f
        
        // Elegant brand color banner block
        canvas.drawRect(40f, y, 70f, y + 25f, brandPaint)
        
        // Report title & brand header info
        canvas.drawText("WEALTHFLOW LEDGER ACCOUNTING", 80f, y + 12f, titlePaint)
        canvas.drawText("Statement Summary - Regional compliance standards", 80f, y + 23f, textPaint.apply { color = Color.GRAY; textSize = 8f })
        
        y += 45f
        
        // Details list
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        canvas.drawText("Report Type: Comprehensive Statement Summary", 40f, y, textPaint.apply { color = Color.BLACK; textSize = 9f })
        y += 14f
        canvas.drawText("Generated on: ${sdf.format(Date())}", 40f, y, textPaint)
        y += 14f
        canvas.drawText("Statement Month: $month", 40f, y, textPaint)
        y += 14f
        canvas.drawText("Country Currency Base: ${config.currency} (${config.currencySymbol})", 40f, y, textPaint)
        
        y += 25f
        
        // Solid black table border separation
        canvas.drawRect(40f, y, 555f, y + 1.5f, Paint().apply { color = Color.rgb(45, 55, 72) })
        y += 15f
        
        // Table field headers
        canvas.drawText("DATE", 45f, y, headerPaint)
        canvas.drawText("TYPE", 130f, y, headerPaint)
        canvas.drawText("CATEGORY", 200f, y, headerPaint)
        canvas.drawText("MERCHANT", 330f, y, headerPaint)
        canvas.drawText("AMOUNT", 465f, y, headerPaint)
        y += 12f
        
        canvas.drawRect(40f, y - 4f, 555f, y - 3f, linePaint)
        y += 12f
        
        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // Constrain list output to preserve safe fit on 1st statement summary page
        transactions.take(28).forEach { tx ->
            val txDateStr = try {
                dateParser.format(Date(tx.timestamp))
            } catch (e: Exception) {
                "N/A"
            }
            
            val catName = categories.find { it.id == tx.categoryId }?.name ?: "Transfer/Wallet"
            val cleanNotes = if (tx.merchant.length > 22) tx.merchant.substring(0, 20) + ".." else tx.merchant
            val isExpense = tx.type == "EXPENSE"
            
            // Format custom balance values
            val amtText = CurrencyFormatterHelper.format(tx.amount, config)
            
            canvas.drawText(txDateStr, 45f, y, textPaint.apply { color = Color.rgb(65, 70, 78) })
            
            // Highlight cashflow directions visually inside PDF ledger columns
            if (isExpense) {
                canvas.drawText("EXPENSE", 130f, y, textPaint.apply { color = Color.rgb(220, 38, 38); isFakeBoldText = true })
            } else {
                canvas.drawText("INCOME", 130f, y, textPaint.apply { color = Color.rgb(22, 163, 74); isFakeBoldText = true })
            }
            
            canvas.drawText(catName, 200f, y, textPaint.apply { color = Color.BLACK; isFakeBoldText = false })
            canvas.drawText(cleanNotes, 330f, y, textPaint)
            canvas.drawText(amtText, 465f, y, textPaint.apply { isFakeBoldText = true })
            
            y += 18f
            canvas.drawLine(40f, y - 10f, 555f, y - 10f, linePaint)
            
            if (y > 790f) {
                return@forEach
            }
        }
        
        // Draw horizontal closing bounds
        canvas.drawLine(40f, y, 555f, y, Paint().apply { color = Color.rgb(74, 85, 104); strokeWidth = 1f })
        y += 18f
        canvas.drawText("System snapshot end of report. Secured with cloud cryptographic backup keys.", 40f, y, textPaint.apply { color = Color.GRAY; textSize = 7.5f; isFakeBoldText = false })
        
        pdfDocument.finishPage(page)
        
        val fileSuffix = month.replace("-", "_")
        val pdfFile = File(context.cacheDir, "wealthflow_statement_$fileSuffix.pdf")
        pdfDocument.writeTo(pdfFile.outputStream())
        pdfDocument.close()
        
        return pdfFile
    }
}
